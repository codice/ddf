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
package ddf.security.samlp.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.UriBuilder;
import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

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
import org.joda.time.DateTime;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml2.core.impl.LogoutRequestBuilder;
import org.opensaml.saml2.core.impl.LogoutResponseBuilder;
import org.opensaml.saml2.core.impl.NameIDBuilder;
import org.opensaml.saml2.core.impl.StatusBuilder;
import org.opensaml.saml2.core.impl.StatusCodeBuilder;
import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml2.metadata.SingleLogoutService;
import org.opensaml.ws.soap.soap11.Body;
import org.opensaml.ws.soap.soap11.Envelope;
import org.opensaml.ws.soap.soap11.impl.BodyBuilder;
import org.opensaml.ws.soap.soap11.impl.EnvelopeBuilder;
import org.opensaml.xml.XMLObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ddf.security.samlp.LogoutService;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;

public class LogoutServiceImpl implements LogoutService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutServiceImpl.class);

    public static final String SOAP_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:SOAP";

    public static final String SAML_SOAP_ACTION = "http://www.oasis-open.org/committees/security";

    private LogoutRequestBuilder logoutRequestBuilder;

    private LogoutResponseBuilder logoutResponseBuilder;

    private NameIDBuilder nameIDBuilder;

    private IssuerBuilder issuerBuilder;

    private EnvelopeBuilder envelopeBuilder;

    private BodyBuilder bodyBuilder;

    private StatusBuilder statusBuilder;

    private StatusCodeBuilder statusCodeBuilder;

    private SystemCrypto systemCrypto;

    static {
        OpenSAMLUtil.initSamlEngine();
    }

    public String getIdpSingleLogoutLocation(IDPSSODescriptor descriptor) {
        return descriptor.getSingleLogoutServices()
                .stream()
                .filter(service -> SOAP_BINDING.equals(service.getBinding()))
                .map(SingleLogoutService::getLocation)
                .findFirst()
                .orElse("");
    }

    public String extractSoapMessageBody(String soapMessage)
            throws XMLStreamException, WSSecurityException, XPathExpressionException {
        Document responseDoc = StaxUtils.read(new ByteArrayInputStream(soapMessage.getBytes()));
        Node node = (Node) XPathFactory.newInstance()
                .newXPath()
                .compile("//*[local-name(.) = 'Envelope']/*[local-name(.) = 'Body']")
                .evaluate(responseDoc.getDocumentElement(), XPathConstants.NODE);
        return DOM2Writer.nodeToString(node);
    }

    public String insertMsgIntoSoap(Element element) {
        Envelope e = envelopeBuilder.buildObject();
        Body b = bodyBuilder.buildObject();
        b.setDOM(element);
        e.setBody(b);
        return DOM2Writer.nodeToString(e.getDOM());
    }

    public XMLObject extractXmlObject(String samlLogoutResponse)
            throws WSSecurityException, XMLStreamException {
        Document responseDoc = StaxUtils.read(
                new ByteArrayInputStream(samlLogoutResponse.getBytes()));
        return OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
    }

    private <T extends SAMLObject> T extract(String samlObject, Class<T> clazz)
            throws WSSecurityException, XMLStreamException {
        Document responseDoc = StaxUtils.read(new ByteArrayInputStream(samlObject.getBytes()));
        XMLObject responseXmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
        if (clazz.isAssignableFrom(responseXmlObject.getClass())) {
            return clazz.cast(responseXmlObject);
        }
        return null;
    }

    public LogoutResponse extractSamlLogoutResponse(String samlLogoutResponse)
            throws XMLStreamException, WSSecurityException {
        return extract(samlLogoutResponse, LogoutResponse.class);
    }

    public LogoutRequest extractSamlLogoutRequest(String samlLogoutResponse)
            throws XMLStreamException, WSSecurityException {
        return extract(samlLogoutResponse, LogoutRequest.class);
    }

    // TODO (RCZ) 11/11/15 - Hide instance of + dom efficiency in here somehow? DDF-1605

    /**
     * Returns a new <code>LogoutRequest</code> with a randomly generated ID, the current time
     * for the <code>IssueInstant</code>, and <code>Version</code> set to <code>"2.0"</code>
     *
     * @param nameIdString     NameId of user to log out
     * @param issuerOrEntityId The Issuer of the LogoutRequest
     * @return the built <code>LogoutRequest</code>
     */
    public LogoutRequest buildLogoutRequest(String nameIdString, String issuerOrEntityId) {
        return buildLogoutRequest(nameIdString, issuerOrEntityId, UUID.randomUUID()
                .toString());
    }

    public LogoutRequest buildLogoutRequest(String nameIdString, String issuerOrEntityId,
            String id) {
        LogoutRequest logoutRequest = logoutRequestBuilder.buildObject();

        NameID nameID = nameIDBuilder.buildObject();
        nameID.setValue(nameIdString);
        logoutRequest.setNameID(nameID);

        logoutRequest.setID(id);

        Issuer issuer = issuerBuilder.buildObject();
        issuer.setValue(issuerOrEntityId);
        logoutRequest.setIssuer(issuer);

        // TODO (RCZ) - What do about jodatime/java 8 java.time ?
        logoutRequest.setIssueInstant(DateTime.now());

        logoutRequest.setVersion(SAMLVersion.VERSION_20);
        return logoutRequest;
    }

    public LogoutResponse buildLogoutResponse(String issuerOrEntityId, String statusCodeValue) {
        return buildLogoutResponse(issuerOrEntityId, statusCodeValue, null);
    }

    public LogoutResponse buildLogoutResponse(String issuerOrEntityId, String statusCodeValue,
            String inResponseTo) {
        return buildLogoutResponse(issuerOrEntityId, statusCodeValue, inResponseTo,
                UUID.randomUUID().toString());
    }

    public LogoutResponse buildLogoutResponse(String issuerOrEntityId, String statusCodeValue,
            String inResponseTo, String id) {
        LogoutResponse logoutResponse = logoutResponseBuilder.buildObject();

        Issuer issuer = issuerBuilder.buildObject();
        issuer.setValue(issuerOrEntityId);
        logoutResponse.setIssuer(issuer);

        Status status = statusBuilder.buildObject();
        StatusCode statusCode = statusCodeBuilder.buildObject();
        statusCode.setValue(statusCodeValue);
        status.setStatusCode(statusCode);
        logoutResponse.setStatus(status);

        if (inResponseTo != null) {
            logoutResponse.setInResponseTo(inResponseTo);
        }

        logoutResponse.setIssueInstant(DateTime.now());

        logoutResponse.setVersion(SAMLVersion.VERSION_20);
        return logoutResponse;
    }

    public Element getElementFromSaml(XMLObject xmlObject) throws WSSecurityException {
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        Element requestElement = null;
        return OpenSAMLUtil.toDom(xmlObject, doc);
    }

    public String sendSamlLogoutRequest(@NotNull LogoutRequest request, String targetUri)
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

    public URI signSamlGetResponse(SAMLObject samlObject, URI uriNameMeLater, String relayState)
            throws WSSecurityException, SimpleSign.SignatureException, IOException {
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
        String encodedResponse = URLEncoder.encode(RestSecurity.deflateAndBase64Encode(
                DOM2Writer.nodeToString(OpenSAMLUtil.toDom(samlObject, doc, false))), "UTF-8");
        String requestToSign =
                SSOConstants.SAML_RESPONSE + "=" + encodedResponse + "&" + SSOConstants.RELAY_STATE
                        + "=" + relayState;
        UriBuilder uriBuilder = UriBuilder.fromUri(uriNameMeLater);
        uriBuilder.queryParam(SSOConstants.SAML_RESPONSE, encodedResponse);
        uriBuilder.queryParam(SSOConstants.RELAY_STATE, relayState);
        new SimpleSign(systemCrypto).signUriString(requestToSign, uriBuilder);
        return uriBuilder.build();
    }

    public void setLogoutRequestBuilder(LogoutRequestBuilder logoutRequestBuilder) {
        this.logoutRequestBuilder = logoutRequestBuilder;
    }

    public void setNameIDBuilder(NameIDBuilder nameIDBuilder) {
        this.nameIDBuilder = nameIDBuilder;
    }

    public void setIssuerBuilder(IssuerBuilder issuerBuilder) {
        this.issuerBuilder = issuerBuilder;
    }

    public void setSystemCrypto(SystemCrypto systemCrypto) {
        this.systemCrypto = systemCrypto;
    }

    public void setEnvelopeBuilder(EnvelopeBuilder envelopeBuilder) {
        this.envelopeBuilder = envelopeBuilder;
    }

    public void setBodyBuilder(BodyBuilder bodyBuilder) {
        this.bodyBuilder = bodyBuilder;
    }

    public void setLogoutResponseBuilder(LogoutResponseBuilder logoutResponseBuilder) {
        this.logoutResponseBuilder = logoutResponseBuilder;
    }

    public void setStatusBuilder(StatusBuilder statusBuilder) {
        this.statusBuilder = statusBuilder;
    }

    public void setStatusCodeBuilder(StatusCodeBuilder statusCodeBuilder) {
        this.statusCodeBuilder = statusCodeBuilder;
    }

}
