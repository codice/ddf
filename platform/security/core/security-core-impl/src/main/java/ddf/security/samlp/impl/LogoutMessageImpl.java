/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.security.samlp.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.UriBuilder;
import javax.xml.stream.XMLStreamException;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.rs.security.saml.sso.SSOConstants;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ddf.security.samlp.LogoutMessage;
import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;

public class LogoutMessageImpl implements LogoutMessage {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutMessageImpl.class);

    public static final String SOAP_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:SOAP";

    public static final String SAML_SOAP_ACTION = "http://www.oasis-open.org/committees/security";

    private SystemCrypto systemCrypto;

    static {
        OpenSAMLUtil.initSamlEngine();
    }

    public String getIdpSingleLogoutLocation(@NotNull IDPSSODescriptor descriptor) {
        return descriptor.getSingleLogoutServices()
                .stream()
                .filter(service -> SOAP_BINDING.equals(service.getBinding()))
                .map(SingleLogoutService::getLocation)
                .findFirst()
                .orElse("");
    }

    public SignableSAMLObject extractXmlObject(@NotNull String samlLogoutResponse)
            throws WSSecurityException, XMLStreamException {
        Document responseDoc =
                StaxUtils.read(new ByteArrayInputStream(samlLogoutResponse.getBytes(StandardCharsets.UTF_8)));
        XMLObject xmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
        if (xmlObject instanceof SignableSAMLObject) {
            return (SignableSAMLObject) xmlObject;
        }
        return null;
    }

    private <T extends SAMLObject> T extract(@NotNull String samlObject, @NotNull Class<T> clazz)
            throws WSSecurityException, XMLStreamException {
        Document responseDoc = StaxUtils.read(new ByteArrayInputStream(samlObject.getBytes(
                StandardCharsets.UTF_8)));
        XMLObject responseXmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
        if (clazz.isAssignableFrom(responseXmlObject.getClass())) {
            return clazz.cast(responseXmlObject);
        }
        return null;
    }

    @Override
    public LogoutResponse extractSamlLogoutResponse(@NotNull String samlLogoutResponse)
            throws XMLStreamException, WSSecurityException {
        return extract(samlLogoutResponse, LogoutResponse.class);
    }

    @Override
    public LogoutRequest extractSamlLogoutRequest(@NotNull String samlLogoutResponse)
            throws XMLStreamException, WSSecurityException {
        return extract(samlLogoutResponse, LogoutRequest.class);
    }

    /**
     * Returns a new <code>LogoutRequest</code> with a randomly generated ID, the current time
     * for the <code>IssueInstant</code>, and <code>Version</code> set to <code>"2.0"</code>
     *
     * @param nameIdString     NameId of user to log out
     * @param issuerOrEntityId The Issuer of the LogoutRequest
     * @return the built <code>LogoutRequest</code>
     */
    @Override
    public LogoutRequest buildLogoutRequest(@NotNull String nameIdString,
            @NotNull String issuerOrEntityId) {
        return buildLogoutRequest(nameIdString,
                issuerOrEntityId,
                UUID.randomUUID()
                        .toString());
    }

    public LogoutRequest buildLogoutRequest(@NotNull String nameIdString,
            @NotNull String issuerOrEntityId, @NotNull String id) {
        if (nameIdString == null) {
            throw new IllegalArgumentException("Name ID cannot be null");
        }
        if (issuerOrEntityId == null) {
            throw new IllegalArgumentException("Issuer cannot be null");
        }
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }

        return SamlProtocol.createLogoutRequest(SamlProtocol.createIssuer(issuerOrEntityId),
                SamlProtocol.createNameID(nameIdString),
                id);
    }

    /**
     *  @apiNote ONLY use this method in error cases when creating a valid response pass the inResponseTo id
     * @param issuerOrEntityId the issuer or entity id to use when building the response
     * @param statusCodeValue the success, failure or partial logout status code
     * @return LogoutResponse
     */
    @Override
    public LogoutResponse buildLogoutResponse(@NotNull String issuerOrEntityId,
            @NotNull String statusCodeValue) {
        return buildLogoutResponse(issuerOrEntityId, statusCodeValue, null);
    }

    @Override
    public LogoutResponse buildLogoutResponse(String issuerOrEntityId, String statusCodeValue,
            String inResponseTo) {
        return buildLogoutResponse(issuerOrEntityId,
                statusCodeValue,
                inResponseTo,
                UUID.randomUUID()
                        .toString());
    }

    public LogoutResponse buildLogoutResponse(@NotNull String issuerOrEntityId,
            @NotNull String statusCodeValue, String inResponseTo, @NotNull String id) {
        if (issuerOrEntityId == null) {
            throw new IllegalArgumentException("Issuer cannot be null");
        }
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        if (statusCodeValue == null) {
            throw new IllegalArgumentException("Status Code cannot be null");
        }

        return SamlProtocol.createLogoutResponse(SamlProtocol.createIssuer(issuerOrEntityId),
                SamlProtocol.createStatus(statusCodeValue),
                inResponseTo,
                id);
    }

    @Override
    public Element getElementFromSaml(@NotNull XMLObject xmlObject) throws WSSecurityException {
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        return OpenSAMLUtil.toDom(xmlObject, doc);
    }

    @Override
    public String sendSamlLogoutRequest(@NotNull LogoutRequest request, @NotNull String targetUri)
            throws IOException, WSSecurityException {
        Element requestElement = getElementFromSaml(request);
        String requestMessage = DOM2Writer.nodeToString(requestElement);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(targetUri);
            post.addHeader("Cache-Control", "no-cache, no-store");
            post.addHeader("Pragma", "no-cache");
            post.addHeader("SOAPAction", SAML_SOAP_ACTION);
            post.setEntity(new StringEntity(requestMessage, "utf-8"));
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            return httpClient.execute(post, responseHandler);
        }
    }

    @Override
    public URI signSamlGetResponse(@NotNull SAMLObject samlObject, @NotNull URI target,
            String relayState)
            throws WSSecurityException, SimpleSign.SignatureException, IOException {

        return signSamlGet(samlObject, target, relayState, SSOConstants.SAML_RESPONSE);
    }

    @Override
    public URI signSamlGetRequest(@NotNull SAMLObject samlObject, @NotNull URI target,
            String relayState)
            throws WSSecurityException, SimpleSign.SignatureException, IOException {

        return signSamlGet(samlObject, target, relayState, SSOConstants.SAML_REQUEST);
    }

    private URI signSamlGet(@NotNull SAMLObject samlObject, @NotNull URI target, String relayState,
            @NotNull String requestType)
            throws WSSecurityException, SimpleSign.SignatureException, IOException {
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        String encodedResponse =
                URLEncoder.encode(RestSecurity.deflateAndBase64Encode(DOM2Writer.nodeToString(
                        OpenSAMLUtil.toDom(samlObject, doc, false))), "UTF-8");
        String requestToSign = String.format("%s=%s&%s=%s",
                requestType,
                encodedResponse,
                SSOConstants.RELAY_STATE,
                relayState);
        UriBuilder uriBuilder = UriBuilder.fromUri(target);
        uriBuilder.queryParam(requestType, encodedResponse);
        uriBuilder.queryParam(SSOConstants.RELAY_STATE, relayState);
        new SimpleSign(systemCrypto).signUriString(requestToSign, uriBuilder);
        return uriBuilder.build();
    }

    public void setSystemCrypto(SystemCrypto systemCrypto) {
        this.systemCrypto = systemCrypto;
    }
}
