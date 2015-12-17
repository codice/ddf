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
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

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
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
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
import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;
import ddf.security.samlp.impl.RelayStates;
import ddf.security.samlp.impl.SamlValidator;

@Path("logout")
public class LogoutRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutRequestService.class);

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

    static {
        OpenSAMLUtil.initSamlEngine();
    }

    public void init() {
        try (InputStream submitStream = LogoutRequestService.class.getResourceAsStream(
                "/templates/submitForm.handlebars");
                InputStream redirectStream = LogoutRequestService.class.getResourceAsStream(
                        "/templates/redirect.handlebars")) {
            submitForm = IOUtils.toString(submitStream);
            redirectPage = IOUtils.toString(redirectStream);
        } catch (Exception e) {
            LOGGER.error("Unable to load index page for SP.", e);
        }
    }

    @GET
    @Path("/request")
    public Response sendLogoutRequest(
            @QueryParam("EncryptedNameIdTime") String encryptedNameIdTime) {
        String nameIdTime = encryptionService.decrypt(encryptedNameIdTime);
        String[] nameIdTimeArray = StringUtils.split(nameIdTime, "\n");
        if (nameIdTimeArray.length == 2) {
            try {
                String name = nameIdTimeArray[0];
                Long time = Long.parseLong(nameIdTimeArray[1]);
                if (System.currentTimeMillis() - time > logOutPageTimeOut) {
                    String msg = String.format(
                            "Logout request was older than %sms old so it was rejected. Please refresh page and request again.",
                            logOutPageTimeOut);
                    LOGGER.error(msg);
                    return buildLogoutResponse(msg);
                }
                logout();
                LogoutRequest logoutRequest = logoutService.buildLogoutRequest(name, getEntityId());

                String relayState = relayStates.encode(name);

                return getLogoutRequest(relayState, logoutRequest);
            } catch (Exception e) {
                String msg = "Failed to create logout request.";
                LOGGER.error(msg, e);
                return buildLogoutResponse(msg);
            }

        } else {
            String msg = "Failed to decrypt logout request params. Invalid number of params.";
            LOGGER.error(msg);
            return buildLogoutResponse(msg);
        }

    }

    private Response getLogoutRequest(String relayState, LogoutRequest logoutRequest)
            throws IdpClientException {
        try {

            String binding = idpMetadata.getSingleLogoutBinding();
            if (SamlProtocol.POST_BINDING.equals(binding)) {
                return getSamlpPostLogoutRequest(relayState, logoutRequest);
            } else if (SamlProtocol.REDIRECT_BINDING.equals(binding)) {
                return getSamlpRedirectLogoutRequest(relayState, logoutRequest);
            } else {
                return buildLogoutResponse(
                        "The IDP does not support either POST or Redirect bindings.");
            }
        } catch (Exception e) {
            String msg = "Failed to create logout request";
            LOGGER.error(msg, e);
            throw new IdpClientException(msg, e);
        }
    }

    private Response getSamlpPostLogoutRequest(String relayState, LogoutRequest logoutRequest)
            throws SimpleSign.SignatureException, WSSecurityException {
        LOGGER.debug("Configuring SAML LogoutRequest for POST.");
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        LOGGER.debug("Signing SAML POST LogoutRequest.");
        simpleSign.signSamlObject(logoutRequest);
        LOGGER.debug("Converting SAML Request to DOM");
        String assertionResponse = DOM2Writer.nodeToString(OpenSAMLUtil.toDom(logoutRequest, doc));
        String encodedSamlRequest = new String(Base64.encodeBase64(assertionResponse.getBytes()));
        String singleLogoutLocation = idpMetadata.getSingleLogoutLocation();
        String submitFormUpdated = String.format(submitForm,
                singleLogoutLocation,
                SAML_REQUEST,
                encodedSamlRequest,
                relayState);
        Response.ResponseBuilder ok = Response.ok(submitFormUpdated);
        return ok.build();
    }

    private Response getSamlpRedirectLogoutRequest(String relayState, LogoutRequest logoutRequest)
            throws IOException, SimpleSign.SignatureException, WSSecurityException,
            URISyntaxException {
        LOGGER.debug("Configuring SAML Response for Redirect.");
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        URI location = logoutService.signSamlGetRequest(logoutRequest,
                new URI(idpMetadata.getSingleLogoutLocation()),
                relayState);
        String redirectUpdated = String.format(redirectPage, location.toString());
        Response.ResponseBuilder ok = Response.ok(redirectUpdated);
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

            try {
                new SamlValidator.Builder(simpleSign).buildAndValidate(request.getRequestURL()
                        .toString(), SamlProtocol.Binding.HTTP_POST, logoutRequest);
                LogoutResponse logoutResponse =
                        logoutService.buildLogoutResponse(logoutRequest.getIssuer()
                                .getValue(), StatusCode.SUCCESS_URI);

                return getSamlpPostLogoutResponse(relayState, logoutResponse);
            } catch (SimpleSign.SignatureException | WSSecurityException e) {
                String msg = "Failed to sign logout response.";
                LOGGER.error(msg, e);
                return buildLogoutResponse(msg);
            } catch (ValidationException e) {
                throw new IdpClientException("Unable to validate", e);
            }
        } else {

            LogoutResponse logoutResponse =
                    processLogoutResponse(decodeBase64(encodedSamlResponse));
            try {
                new SamlValidator.Builder(simpleSign).buildAndValidate(request.getRequestURL()
                        .toString(), SamlProtocol.Binding.HTTP_POST, logoutResponse);
            } catch (ValidationException e) {
                throw new IdpClientException("Unable to validate", e);
            }
            String nameId = "You";
            if (relayState != null) {
                nameId = relayStates.decode(relayState);
            }
            return buildLogoutResponse(nameId + " logged out successfully.");

        }
    }

    @GET
    public Response getLogoutRequest(@QueryParam(SAML_REQUEST) String deflatedSamlRequest,
            @QueryParam(SAML_RESPONSE) String deflatedSamlResponse,
            @QueryParam(RELAY_STATE) String relayState,
            @QueryParam(SIG_ALG) String signatureAlgorithm, @QueryParam(SIGNATURE) String signature)
            throws IdpClientException {

        if (deflatedSamlRequest != null) {
            try {
                LogoutRequest logoutRequest = processSamlLogoutRequest(RestSecurity.inflateBase64(
                        deflatedSamlRequest));
                new SamlValidator.Builder(simpleSign).setRedirectParams(relayState,
                        signature,
                        signatureAlgorithm,
                        deflatedSamlRequest,
                        idpMetadata.getSigningCertificate())
                        .buildAndValidate(request.getRequestURL()
                                .toString(), SamlProtocol.Binding.HTTP_REDIRECT, logoutRequest);
                logout();
                String entityId = getEntityId();
                LogoutResponse logoutResponse = logoutService.buildLogoutResponse(entityId,
                        StatusCode.SUCCESS_URI);
                return getLogoutResponse(relayState, logoutResponse);
            } catch (IOException e) {
                String msg = "Unable to decode and inflate logout request.";
                LOGGER.warn(msg, e);
                return buildLogoutResponse(msg);
            } catch (ValidationException e) {
                String msg = "Unable to validate";
                LOGGER.warn(msg, e);
                throw new IdpClientException(msg, e);
            }
        } else {
            try {

                LogoutResponse logoutResponse = processLogoutResponse(RestSecurity.inflateBase64(
                        deflatedSamlResponse));
                new SamlValidator.Builder(simpleSign).setRedirectParams(relayState,
                        signature,
                        signatureAlgorithm,
                        deflatedSamlResponse,
                        idpMetadata.getSigningCertificate())
                        .buildAndValidate(request.getRequestURL()
                                .toString(), SamlProtocol.Binding.HTTP_REDIRECT, logoutResponse);
                String nameId = "You";
                if (relayState != null) {
                    nameId = relayStates.decode(relayState);
                }
                return buildLogoutResponse(nameId + " logged out successfully.");
            } catch (IOException e) {
                String msg = "Unable to decode and inflate logout response.";
                LOGGER.warn(msg, e);
                return buildLogoutResponse(msg);
            } catch (ValidationException e) {
                throw new IdpClientException("Unable to validate", e);
            }
        }
    }

    private LogoutResponse processLogoutResponse(String logoutResponseStr)
            throws IdpClientException {
        LogoutResponse logoutResponse;
        try {
            logoutResponse = logoutService.extractSamlLogoutResponse(logoutResponseStr);
        } catch (XMLStreamException | WSSecurityException e) {
            throw new IdpClientException("Unable to parse logout response.", e);
        }
        if (logoutResponse == null) {
            throw new IdpClientException("Unable to parse logout response.");
        }
        return logoutResponse;
    }

    private String getEntityId() {
        String hostname = baseUrl.getHost();
        String port = baseUrl.getPort();
        String rootContext = baseUrl.getRootContext();

        return String.format("https://%s:%s%s/saml", hostname, port, rootContext);
    }

    private LogoutRequest processSamlLogoutRequest(String logoutRequestStr)
            throws IdpClientException {
        LOGGER.trace(logoutRequestStr);

        LogoutRequest logoutRequest;
        try {
            logoutRequest = logoutService.extractSamlLogoutRequest(logoutRequestStr);
        } catch (XMLStreamException | WSSecurityException e) {
            throw new IdpClientException("Unable to parse logout request.", e);
        }
        if (logoutRequest == null) {
            throw new IdpClientException("Unable to parse logout request.");
        }

        return logoutRequest;
    }

    private void logout() {
        HttpSession session = sessionFactory.getOrCreateSession(request);

        SecurityTokenHolder tokenHolder = ((SecurityTokenHolder) session.getAttribute(
                SecurityConstants.SAML_ASSERTION));
        tokenHolder.remove("idp");
    }

    private Response getLogoutResponse(String relayState, LogoutResponse samlResponse)
            throws IdpClientException {
        try {

            String binding = idpMetadata.getSingleLogoutBinding();
            if (SamlProtocol.POST_BINDING.equals(binding)) {
                return getSamlpPostLogoutResponse(relayState, samlResponse);
            } else if (SamlProtocol.REDIRECT_BINDING.equals(binding)) {
                return getSamlpRedirectLogoutResponse(relayState, samlResponse);
            } else {
                return buildLogoutResponse(
                        "The IDP does not support either POST or Redirect bindings.");
            }
        } catch (Exception e) {
            String msg = "Failed to create logout response";
            LOGGER.error(msg, e);
            throw new IdpClientException(msg, e);
        }

    }

    private Response getSamlpPostLogoutResponse(String relayState, LogoutResponse samlResponse)
            throws WSSecurityException, SimpleSign.SignatureException {
        LOGGER.debug("Configuring SAML Response for POST.");
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        LOGGER.debug("Signing SAML POST Response.");
        simpleSign.signSamlObject(samlResponse);
        LOGGER.debug("Converting SAML Response to DOM");
        String assertionResponse = DOM2Writer.nodeToString(OpenSAMLUtil.toDom(samlResponse, doc));
        String encodedSamlResponse = new String(Base64.encodeBase64(assertionResponse.getBytes()));
        String singleLogoutLocation = idpMetadata.getSingleLogoutLocation();
        String submitFormUpdated = String.format(submitForm,
                singleLogoutLocation,
                SAML_RESPONSE,
                encodedSamlResponse,
                relayState);
        Response.ResponseBuilder ok = Response.ok(submitFormUpdated);
        return ok.build();
    }

    private Response getSamlpRedirectLogoutResponse(String relayState, LogoutResponse samlResponse)
            throws IOException, SimpleSign.SignatureException, WSSecurityException,
            URISyntaxException {
        LOGGER.debug("Configuring SAML Response for Redirect.");
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        URI location = logoutService.signSamlGetResponse(samlResponse,
                new URI(idpMetadata.getSingleLogoutLocation()),
                relayState);
        String redirectUpdated = String.format(redirectPage, location.toString());
        Response.ResponseBuilder ok = Response.ok(redirectUpdated);
        return ok.build();
    }

    private String decodeBase64(String encoded) {
        return new String(Base64.decodeBase64(encoded.getBytes()));
    }

    private Response buildLogoutResponse(String message) {
        UriBuilder uriBuilder = UriBuilder.fromUri(baseUrl.getBaseUrl());
        uriBuilder.path("logout/logout-response.html");
        uriBuilder.queryParam("msg", message);
        return Response.seeOther(uriBuilder.build())
                .build();
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
