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
import java.net.URISyntaxException;
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
import javax.xml.stream.XMLStreamException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.codice.ddf.security.session.RelayStates;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.xml.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import ddf.security.SecurityConstants;
import ddf.security.common.util.SecurityTokenHolder;
import ddf.security.encryption.EncryptionService;
import ddf.security.http.SessionFactory;
import ddf.security.samlp.LogoutService;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;

@Path("logout")
public class LogoutRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutRequestService.class);

    private static final String URL = "URL";

    private static final String SAML_REQUEST = "SAMLRequest";

    private static final String SAML_RESPONSE = "SAMLResponse";

    private static final String RELAY_STATE = "RelayState";

    private static final String SIG_ALG = "SigAlg";

    private static final String SIGNATURE = "Signature";

    private SimpleSign simpleSign;

    private IdpMetadata idpMetadata;

    @Context
    private HttpServletRequest request;

    private SystemCrypto systemCrypto;

    private LogoutService logoutService;

    private String submitForm;

    private String redirectPage;

    private SystemBaseUrl baseUrl;

    private final RelayStates<String> relayStates;

    private EncryptionService encryptionService;

    private SessionFactory sessionFactory;

    private long logOutPageTimeOut = 3600000;

    public LogoutRequestService(SimpleSign simpleSign, IdpMetadata idpMetadata,
            SystemCrypto systemCrypto, SystemBaseUrl systemBaseUrl,
            RelayStates<String> relayStates) {
        this.simpleSign = simpleSign;
        this.idpMetadata = idpMetadata;
        this.systemCrypto = systemCrypto;
        this.baseUrl = systemBaseUrl;
        this.relayStates = relayStates;

    }

    public void init() {
        try {
            submitForm = IOUtils.toString(LogoutRequestService.class.getResourceAsStream(
                    "/templates/submitForm.handlebars"));
            redirectPage = IOUtils.toString(LogoutRequestService.class.getResourceAsStream(
                    "/templates/redirect.handlebars"));
        } catch (Exception e) {
            LOGGER.error("Unable tpo load index page for SP.", e);
        }

        OpenSAMLUtil.initSamlEngine();
    }

    //TODO change it to a post since its not idempotent and we don't want prefetching.
    @GET
    @Path("/request")
    public Response sendLogoutRequest(@QueryParam("EncryptedNameIdTime") String encryptedNameIdTime) {
        String nameIdTime = encryptionService.decrypt(encryptedNameIdTime);
        String[] nameIdTimeArray = StringUtils.split(nameIdTime,"\n");
        if(nameIdTimeArray.length==2) {
            try {
                String name = nameIdTimeArray[0];
                Long time = Long.parseLong(nameIdTimeArray[1]);
                if(System.currentTimeMillis()-time>logOutPageTimeOut)//+skew
                {
                    String msg = String.format("Logout request was older than %sms old so it was rejected. Please refresh page and request again.",logOutPageTimeOut);
                    LOGGER.error(msg);
                    return Response.serverError()
                            .entity(msg)
                            .build();
                }
                logout();
                LogoutRequest logoutRequest = logoutService.buildLogoutRequest(name, getEntityId());

                String relayState = relayStates.encode(name);

                return getSamlpRedirectLogoutRequest(relayState, logoutRequest, redirectPage);
            }catch (Exception e)
            {
                LOGGER.error("Failed to create logout request.", e);
                return Response.serverError()
                        .entity("Failed to create logout request.")
                        .build();
            }

        }else
        {
            String msg = "Failed to decrypt logout request params. Invalid number of params.";
            LOGGER.error(msg);
            return Response.serverError()
                    .entity(msg)
                    .build();
        }


    }

    public Response getSamlpRedirectLogoutRequest(String relayState, LogoutRequest logoutRequest,
            String responseTemplate)
            throws IOException, SimpleSign.SignatureException, WSSecurityException,
            URISyntaxException {
        LOGGER.debug("Configuring SAML Response for Redirect.");
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        URI location = logoutService.signSamlGetRequest(logoutRequest,
                new URI(idpMetadata.getRedirectSingleLogoutLocation()),
                relayState);
        String redirectUpdated = responseTemplate.replace("{{redirect}}", location.toString());
        Response.ResponseBuilder ok =
                Response.ok(redirectUpdated);
        return ok.build();
    }

    @POST
    @Produces(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postLogoutRequest(@FormParam(SAML_REQUEST) String encodedSamlRequest,
            @FormParam(SAML_REQUEST) String encodedSamlResponse,
            @FormParam(RELAY_STATE) String relayState) throws IdpClientException {

        if (encodedSamlRequest != null) {
            LogoutRequest logoutRequest =
                    processSamlLogoutRequest(decodeBase64(encodedSamlRequest));
            if (logoutRequest.getSignature() == null) {
                throw new IdpClientException(
                        "Could not validate signature. No signature was found.");
            }

            LogoutResponse logoutResponse =
                    logoutService.buildLogoutResponse(logoutRequest.getIssuer()
                            .getValue(), StatusCode.SUCCESS_URI);
            try {

                return getSamlpPostLogoutResponse(relayState, logoutResponse, submitForm);
            } catch (SimpleSign.SignatureException | WSSecurityException e) {
                LOGGER.error("Failed to sign logout response.", e);
                return Response.serverError()
                        .entity("Failed to sign logout response.")
                        .build();
            }
        } else {
            String nameId = relayStates.decode(relayState);
            processSamlLogoutResponse(decodeBase64(encodedSamlResponse));
            return Response.ok(nameId + " logged out successfully.")
                    .build();
        }
    }

    @GET
    public Response getLogoutRequest(@QueryParam(SAML_REQUEST) String deflatedSamlRequest,
            @QueryParam(SAML_RESPONSE) String deflatedSamlResponse,
            @QueryParam(RELAY_STATE) String relayState,
            @QueryParam(SIG_ALG) String signatureAlgorithm, @QueryParam(SIGNATURE) String signature)
            throws IdpClientException {

        if (deflatedSamlRequest != null) {
            validateRequestSignature(deflatedSamlRequest,
                    relayState,
                    signatureAlgorithm,
                    signature);
            try {
                LogoutRequest logoutRequest = processSamlLogoutRequest(RestSecurity.inflateBase64(
                        deflatedSamlRequest));
                String entityId = getEntityId();
                LogoutResponse logoutResponse = logoutService.buildLogoutResponse(entityId,
                        StatusCode.SUCCESS_URI);
                try {
                    return getSamlpRedirectLogoutResponse(relayState, logoutResponse, redirectPage);
                } catch (SimpleSign.SignatureException | WSSecurityException | IOException | URISyntaxException e) {
                    LOGGER.error("Failed to sign logout response.", e);
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
        } else {
            try {
                validateResponseSignature(deflatedSamlResponse,
                        relayState,
                        signatureAlgorithm,
                        signature);
                processSamlLogoutResponse(RestSecurity.inflateBase64(deflatedSamlResponse));

                String nameId = relayStates.decode(relayState);
                return Response.ok(nameId + " logged out successfully.")
                        .build();
            } catch (IOException e) {
                String msg = "Unable to decode and inflate logout response.";
                LOGGER.warn(msg, e);
                return Response.serverError()
                        .entity(msg)
                        .build();
            }
        }
    }

    private void processSamlLogoutResponse(String logoutResponseStr) throws IdpClientException {
        LOGGER.trace(logoutResponseStr);

        LogoutResponse logoutResponse;
        try {
            logoutResponse = logoutService.extractSamlLogoutResponse(logoutResponseStr);
        } catch (XMLStreamException | WSSecurityException e) {
            throw new IdpClientException("Unable to parse logout response.", e);
        }
        if (logoutResponse == null) {
            throw new IdpClientException("Unable to parse logout response.");
        }

        validateResponse(logoutResponse);
    }

    private void validateResponse(LogoutResponse logoutResponse)
            throws IdpClientException {
        try {
            logoutResponse.registerValidator(new LogoutResponseValidator(simpleSign));
            logoutResponse.validate(false);
        } catch (ValidationException e) {
            throw new IdpClientException(
                    "Invalid Logout Request received from " + logoutResponse.getIssuer(), e);
        }

    }

    private String getEntityId() {
        String hostname = baseUrl.getHost();
        String port = baseUrl.getPort();
        String rootContext = baseUrl.getRootContext();

        return String.format("https://%s:%s%s/saml", hostname, port, rootContext);
    }

    public LogoutRequest processSamlLogoutRequest(String logoutRequestStr)
            throws IdpClientException {
        LOGGER.trace(logoutRequestStr);

        LogoutRequest logoutRequest;
        try {
            logoutRequest = logoutService.extractSamlLogoutRequest(logoutRequestStr);
        } catch (XMLStreamException | WSSecurityException e) {
            throw new IdpClientException("Unable to parse logout request.",
                    e);
        }
        if (logoutRequest == null) {
            throw new IdpClientException("Unable to parse logout request.");
        }

        validateRequest(logoutRequest);
        logout();

        return logoutRequest;
    }

    private void logout() {
        HttpSession session = sessionFactory.getOrCreateSession(request);

        SecurityTokenHolder tokenHolder = ((SecurityTokenHolder) session.getAttribute(
                SecurityConstants.SAML_ASSERTION));
        tokenHolder.remove("idp");
    }

    //TODO move to a common location
    public Response getSamlpPostLogoutResponse(String relayState, LogoutResponse samlResponse,
            String responseTemplate) throws WSSecurityException, SimpleSign.SignatureException {
        LOGGER.debug("Configuring SAML Response for POST.");
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        LOGGER.debug("Signing SAML POST Response.");
        new SimpleSign(systemCrypto).signSamlObject(samlResponse);
        LOGGER.debug("Converting SAML Response to DOM");
        String assertionResponse = DOM2Writer.nodeToString(OpenSAMLUtil.toDom(samlResponse, doc));
        String encodedSamlResponse = new String(Base64.encodeBase64(assertionResponse.getBytes()));
        String singleLogoutLocation = idpMetadata.getPostSingleLogoutLocation();
        String submitFormUpdated = responseTemplate.replace("{{" + URL + "}}",
                singleLogoutLocation);
        submitFormUpdated = submitFormUpdated.replace("{{" + SAML_REQUEST + "}}",
                encodedSamlResponse);
        submitFormUpdated = submitFormUpdated.replace("{{" + RELAY_STATE + "}}", relayState);
        Response.ResponseBuilder ok = Response.ok(submitFormUpdated);
        return ok.build();
    }

    public Response getSamlpRedirectLogoutResponse(String relayState, LogoutResponse samlResponse,
            String responseTemplate)
            throws IOException, SimpleSign.SignatureException, WSSecurityException,
            URISyntaxException {
        LOGGER.debug("Configuring SAML Response for Redirect.");
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        URI location = logoutService.signSamlGetResponse(samlResponse,
                new URI(idpMetadata.getRedirectSingleLogoutLocation()),
                relayState);
        String redirectUpdated = responseTemplate.replace("{{redirect}}", location.toString());
        Response.ResponseBuilder ok =
                Response.ok(redirectUpdated);
        return ok.build();
    }

    //TODO refactor to common area
    private void validateRequestSignature(String deflatedSamlrequest, String relayState,
            String signatureAlgorithm, String signature) throws IdpClientException {
        this.validateSignature(deflatedSamlrequest,
                relayState,
                signatureAlgorithm,
                signature,
                SAML_REQUEST);

    }

    private void validateResponseSignature(String deflatedSamlResponse, String relayState,
            String signatureAlgorithm, String signature) throws IdpClientException {
        this.validateSignature(deflatedSamlResponse,
                relayState,
                signatureAlgorithm,
                signature,
                SAML_RESPONSE);

    }

    private void validateSignature(String deflatedSamlResponse, String relayState,
            String signatureAlgorithm, String signature, String paramType)
            throws IdpClientException {
        boolean signaturePasses;
        if (signature != null) {
            if (StringUtils.isNotBlank(deflatedSamlResponse) && StringUtils.isNotBlank(relayState)
                    && StringUtils.isNotBlank(signatureAlgorithm)) {
                try {
                    String signedMessage = String.format("%s=%s&%s=%s&%s=%s",
                            paramType,
                            URLEncoder.encode(deflatedSamlResponse, "UTF-8"),
                            RELAY_STATE,
                            URLEncoder.encode(relayState, "UTF-8"),
                            SIG_ALG,
                            URLEncoder.encode(signatureAlgorithm, "UTF-8"));
                    signaturePasses = simpleSign.validateSignature(signedMessage,
                            signature,
                            idpMetadata.getSigningCertificate());
                    if (!signaturePasses) {
                        throw new IdpClientException(
                                "Failed to validate logout request signature.");
                    }
                } catch (SimpleSign.SignatureException | UnsupportedEncodingException e) {
                    LOGGER.debug("Failed to validate logout request signature.", e);
                    throw new IdpClientException(
                            "Failed to validate logout request signature.",
                            e);
                }
            }
        } else {
            throw new IdpClientException(
                    "Received unsigned logout request.  Could not verify identity or request integrity.");
        }
    }

    private void validateRequest(org.opensaml.saml2.core.LogoutRequest logoutRequest)
            throws IdpClientException {
        try {
            logoutRequest.registerValidator(new LogoutRequestValidator(simpleSign));
            logoutRequest.validate(false);
        } catch (ValidationException e) {
            throw new IdpClientException(
                    "Invalid Logout Request received from " + logoutRequest.getIssuer(), e);
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

    public void setLogoutService(LogoutService logoutService) {
        this.logoutService = logoutService;
    }

    public void setEncryptionService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void setLogOutPageTimeOut(long logOutPageTimeOut) {
        this.logOutPageTimeOut = logOutPageTimeOut;
    }


}
