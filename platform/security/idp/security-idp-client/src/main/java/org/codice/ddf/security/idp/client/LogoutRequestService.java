/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.idp.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.rs.security.saml.sso.SSOConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.opensaml.saml1.core.StatusCode;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.xml.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import ddf.security.http.SessionFactory;
import ddf.security.samlp.LogoutService;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;


//TODO rename this class since it will have to handle requests and responses
@Path("logout")
public class LogoutRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutRequestService.class);

    private static final String ACS_URL = "ACSURL";//TODO fix acronym

    private static final String SAML_Request = "SAMLRequest";

    private static final String RELAY_STATE = "RelayState";

    private static final String SIG_ALG = "SigAlg";

    private static final String SIGNATURE = "Signature";



    private SimpleSign simpleSign;

    private IdpMetadata idpMetadata;

    @Context
    private HttpServletRequest request;

    private SystemCrypto systemCrypto;

    private SessionFactory sessionFactory;

    //    private XMLObjectBuilderFactory builderFactory;

    //    private SAMLObjectBuilder<LogoutResponse> logoutResponseBuilder;

    private LogoutService logoutService;

    //    public void LogoutRequestService() {
    //        OpenSAMLUtil.initSamlEngine();
    //        builderFactory = Configuration.getBuilderFactory();
    //        logoutResponseBuilder = (SAMLObjectBuilder<LogoutResponse>) builderFactory
    //                .getBuilder(LogoutResponse.DEFAULT_ELEMENT_NAME);
    //    }


    private String submitForm;

    private String redirectPage;

    public LogoutRequestService(SimpleSign simpleSign, IdpMetadata idpMetadata, SystemCrypto systemCrypto) {
        this.simpleSign = simpleSign;
        this.idpMetadata = idpMetadata;
        this.systemCrypto = systemCrypto;

    }


    public void init() {
        try {
            submitForm = IOUtils.toString(
                    LogoutRequestService.class.getResourceAsStream("/templates/submitForm.handlebars"));
            redirectPage = IOUtils.toString(
                    LogoutRequestService.class.getResourceAsStream("/templates/redirect.handlebars"));
        } catch (Exception e) {
            LOGGER.error("Unable to load index page for IDP.", e);
        }

        OpenSAMLUtil.initSamlEngine();
    }

    @POST
    @Produces(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postLogoutRequest(@FormParam(SAML_Request) String encodedSamlResponse,
            @FormParam(RELAY_STATE) String relayState) throws IdpClientException {

        LogoutRequest logoutRequest =  processSamlLogoutRequest(decodeBase64(encodedSamlResponse));
        if(logoutRequest.getSignature()==null)
        {
            throw new IdpClientSignatureException("Could not validate signature. No signature was found.");
        }

        LogoutResponse logoutResponse = logoutService.buildLogoutResponse(logoutRequest.getIssuer()
                .getValue(), StatusCode.SUCCESS.toString());
        try {


            return getSamlpPostLogoutResponse(relayState,logoutRequest,logoutResponse,submitForm);
        } catch (SimpleSign.SignatureException|WSSecurityException e) {
            return Response.serverError()
                    .entity("Failed to sign logout response.")
                    .build();
        }
    }

    @GET
    public Response getLogoutRequest(@QueryParam(SAML_Request) String deflatedSamlRequest,
            @QueryParam(RELAY_STATE) String relayState,
            @QueryParam(SIG_ALG) String signatureAlgorithm,
            @QueryParam(SIGNATURE) String signature) throws IdpClientException {

        //validateSignature(deflatedSamlRequest, relayState, signatureAlgorithm, signature);//TODO figure out how to get simplesaml sp to sign requests
        try {
            LogoutRequest logoutRequest = processSamlLogoutRequest(RestSecurity.inflateBase64(deflatedSamlRequest));
            //TODO this seems inefficent since we have the issuer to pass in and the status is an enum
            LogoutResponse logoutResponse = logoutService.buildLogoutResponse(logoutRequest.getIssuer().getValue(), StatusCode.SUCCESS.toString());
            try {
                return getSamlpRedircetLogoutResponse(relayState, logoutRequest, logoutResponse,
                        redirectPage);
            } catch (SimpleSign.SignatureException|WSSecurityException|IOException e) {
                return Response.serverError()
                        .entity("Failed to sign logout response.")
                        .build();
            }

        } catch (IOException e) {
            String msg = "Unable to decode and inflate logout request.";
            LOGGER.warn(msg, e);
            return Response.serverError()
                    .entity(msg)
                    .build();
        }
//        } else {
//            return Response.serverError()
//                    .entity("Invalid logout request signature.")
//                    .build();
//        }

    }

    public LogoutRequest processSamlLogoutRequest(String logoutRequestStr)
            throws IdpClientException {
        LOGGER.trace(logoutRequestStr);

        LogoutRequest logoutRequest;
        try {
            logoutRequest = logoutService.extractSamlLogoutRequest(logoutRequestStr);
        } catch (XMLStreamException | WSSecurityException e) {
            throw new IdpClientParseException("Unable to parse logout request.", e);//TODO create an cxf exception mapper to handle all of these exceptions and create the appropriate response for the user.
            //            return Response.serverError()
            //                    .entity("Unable to parse logout request.")
            //                    .build();
        }
        if (logoutRequest == null) {
            throw new IdpClientParseException("Unable to parse logout request.");
            //            return Response.serverError()
            //                    .entity("Unable to parse logout request.")
            //                    .build();
        }

        validateRequest(logoutRequest);
        //            return Response.serverError()
        //                    .entity("logout request failed validation.")
        //                    .build();
        //        }


        //TODO don't do this do the securitytokenholder logic
        synchronized (sessionFactory) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            //          //TODO if they are logged in as multiple users to this sp should we just log them out as the one that they requested to logout or all of them.
            //            SecurityTokenHolder tokenHolder = ((SecurityTokenHolder) session
            //                    .getAttribute(SecurityConstants.SAML_ASSERTION));
            //
            //            SecurityToken token = tokenHolder.getSecurityToken(realm);
        }
        return logoutRequest;
    }



    //TODO move to a common location
    public Response getSamlpPostLogoutResponse(String relayState, LogoutRequest logoutRequest,
            org.opensaml.saml2.core.LogoutResponse samlResponse,
            String responseTemplate) throws WSSecurityException, SimpleSign.SignatureException {
        LOGGER.debug("Configuring SAML Response for POST.");
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        LOGGER.debug("Signing SAML POST Response.");
        new SimpleSign(systemCrypto).signSamlObject(samlResponse);
        LOGGER.debug("Converting SAML Response to DOM");
        String assertionResponse = DOM2Writer.nodeToString(OpenSAMLUtil.toDom(samlResponse, doc));
        String encodedSamlResponse = new String(Base64.encodeBase64(assertionResponse.getBytes()));
        //        String assertionConsumerServiceURL = getAssertionConsumerServiceURL(authnRequest);
        String singleLogoutLocation = idpMetadata.getPostSingleLogoutLocation();
        String submitFormUpdated = responseTemplate
                        .replace("{{" + ACS_URL + "}}", singleLogoutLocation);//TODO is this right?
                submitFormUpdated = submitFormUpdated
                        .replace("{{" + SAML_Request + "}}", encodedSamlResponse);
                submitFormUpdated = submitFormUpdated.replace("{{" +RELAY_STATE + "}}", relayState);
        Response.ResponseBuilder ok = Response.ok(submitFormUpdated);
        return ok.build();
    }

    public Response getSamlpRedircetLogoutResponse(String relayState, LogoutRequest logoutRequest,
            LogoutResponse samlResponse, String responseTemplate)
            throws IOException, SimpleSign.SignatureException, WSSecurityException {
        LOGGER.debug("Configuring SAML Response for Redirect.");
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        URI location = signSamlGetResponse(samlResponse, logoutRequest, relayState);
        String redirectUpdated = responseTemplate.replace("{{redirect}}", location.toString());
        Response.ResponseBuilder ok = Response.ok(redirectUpdated);
        return ok.build();
    }

    protected URI signSamlGetResponse(LogoutResponse samlResponse, LogoutRequest logoutRequest,
            String relayState)
            throws WSSecurityException, SimpleSign.SignatureException, IOException {
        LOGGER.debug("Signing SAML response for redirect.");
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        String encodedResponse = URLEncoder.encode(RestSecurity.deflateAndBase64Encode(
                DOM2Writer.nodeToString(OpenSAMLUtil.toDom(samlResponse, doc, false))), "UTF-8");
        String requestToSign = String.format("SAMLResponse=%s&RelayState=%s", encodedResponse,
                relayState);
        //        String assertionConsumerServiceURL = getAssertionConsumerServiceURL(logoutRequest);
        UriBuilder uriBuilder = UriBuilder.fromUri(idpMetadata.getRedirectSingleLogoutLocation());//TODO is this right?
        uriBuilder.queryParam(SSOConstants.SAML_RESPONSE, encodedResponse);
        uriBuilder.queryParam(SSOConstants.RELAY_STATE, relayState);
        new SimpleSign(systemCrypto).signUriString(requestToSign, uriBuilder);
        LOGGER.debug("Signing successful.");
        return uriBuilder.build();
    }

    //TODO refactor to common area
    private void validateSignature(String deflatedSamlResponse, String relayState,
            String signatureAlgorithm, String signature) throws IdpClientSignatureException {
        boolean signaturePasses;
        if (signature != null) {
            if (StringUtils.isNotBlank(deflatedSamlResponse) && StringUtils.isNotBlank(relayState)
                    && StringUtils.isNotBlank(signatureAlgorithm)) {
                try {
                    String signedMessage = String.format("%s=%s&%s=%s&%s=%s", SAML_Request,
                            URLEncoder.encode(deflatedSamlResponse, "UTF-8"), RELAY_STATE,
                            URLEncoder.encode(relayState, "UTF-8"), SIG_ALG,
                            URLEncoder.encode(signatureAlgorithm, "UTF-8"));
                    signaturePasses = simpleSign.validateSignature(signedMessage, signature,
                            idpMetadata.getSigningCertificate());
                    if(!signaturePasses)
                    {
                        throw new IdpClientSignatureException("Failed to validate logout request signature.");
                    }
                } catch (SimpleSign.SignatureException | UnsupportedEncodingException e) {
                    LOGGER.debug("Failed to validate logout request signature.", e);
                    throw new IdpClientSignatureException("Failed to validate logout request signature.",e);
                }
            }
        } else {
            throw new IdpClientSignatureException("Received unsigned logout request.  Could not verify identity or request integrity.");
        }
    }

    //TODO move to common area
    private void validateRequest(org.opensaml.saml2.core.LogoutRequest logoutRequest)
            throws IdpClientValidationException {
        try {
            logoutRequest.registerValidator(new LogoutRequestValidator(simpleSign));
            logoutRequest.validate(false);
        } catch (ValidationException e) {
            throw new IdpClientValidationException("Invalid Logout Request received from " + logoutRequest.getIssuer(),e);
        }

    }

    private String decodeBase64(String encoded) {
        return new String(Base64.decodeBase64(encoded.getBytes()));
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public void setSystemCrypto(SystemCrypto systemCrypto) {
        this.systemCrypto = systemCrypto;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    //    public void setBuilderFactory(XMLObjectBuilderFactory builderFactory) {
    //        this.builderFactory = builderFactory;
    //    }
    //
    //    public void setLogoutResponseBuilder(SAMLObjectBuilder<LogoutResponse> logoutResponseBuilder) {
    //        this.logoutResponseBuilder = logoutResponseBuilder;
    //    }

    public void setLogoutService(LogoutService logoutService) {
        this.logoutService = logoutService;
    }
}
