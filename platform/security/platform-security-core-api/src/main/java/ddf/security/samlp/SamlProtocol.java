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
package ddf.security.samlp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.joda.time.DateTime;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLRuntimeException;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.NameIDFormat;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.security.credential.UsageType;
import org.opensaml.soap.soap11.Body;
import org.opensaml.soap.soap11.Envelope;
import org.opensaml.soap.soap11.Header;
import org.opensaml.soap.soap11.impl.BodyBuilder;
import org.opensaml.soap.soap11.impl.EnvelopeBuilder;
import org.opensaml.soap.soap11.impl.HeaderBuilder;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.X509Data;
import org.w3c.dom.Element;

public class SamlProtocol {

    public static final String SUPPORTED_PROTOCOL = "urn:oasis:names:tc:SAML:2.0:protocol";

    public static final String REDIRECT_BINDING =
            "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect";

    public static final String POST_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";

    /**
     * This static block must be before the builder factories to ensure the engine is initialized
     */
    static {
        OpenSAMLUtil.initSamlEngine();

        ClassLoader tccl = Thread.currentThread()
                .getContextClassLoader();
        Thread.currentThread()
                .setContextClassLoader(SamlProtocol.class.getClassLoader());
        try {
            InitializationService.initialize();
        } catch (InitializationException e) {
            throw new SAMLRuntimeException("Unable to Initialize SAML SOAP builders.");
        } finally {
            Thread.currentThread()
                    .setContextClassLoader(tccl);
        }
    }

    private static XMLObjectBuilderFactory builderFactory =
            XMLObjectProviderRegistrySupport.getBuilderFactory();

    @SuppressWarnings("unchecked")
    private static SAMLObjectBuilder<Response> responseSAMLObjectBuilder =
            (SAMLObjectBuilder<org.opensaml.saml.saml2.core.Response>) builderFactory.getBuilder(org.opensaml.saml.saml2.core.Response.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static SAMLObjectBuilder<Issuer> issuerBuilder =
            (SAMLObjectBuilder<Issuer>) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static SAMLObjectBuilder<Status> statusBuilder =
            (SAMLObjectBuilder<Status>) builderFactory.getBuilder(Status.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static SAMLObjectBuilder<StatusCode> statusCodeBuilder =
            (SAMLObjectBuilder<StatusCode>) builderFactory.getBuilder(StatusCode.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static SAMLObjectBuilder<Subject> subjectBuilder =
            (SAMLObjectBuilder<Subject>) builderFactory.getBuilder(Subject.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static SAMLObjectBuilder<EntityDescriptor> entityDescriptorBuilder =
            (SAMLObjectBuilder<EntityDescriptor>) builderFactory.getBuilder(EntityDescriptor.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static SAMLObjectBuilder<IDPSSODescriptor> idpssoDescriptorBuilder =
            (SAMLObjectBuilder<IDPSSODescriptor>) builderFactory.getBuilder(IDPSSODescriptor.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static SAMLObjectBuilder<SPSSODescriptor> spSsoDescriptorBuilder =
            (SAMLObjectBuilder<SPSSODescriptor>) builderFactory.getBuilder(SPSSODescriptor.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static SAMLObjectBuilder<KeyDescriptor> keyDescriptorBuilder =
            (SAMLObjectBuilder<KeyDescriptor>) builderFactory.getBuilder(KeyDescriptor.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static SAMLObjectBuilder<NameID> nameIdBuilder =
            (SAMLObjectBuilder<NameID>) builderFactory.getBuilder(NameID.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static SAMLObjectBuilder<NameIDFormat> nameIdFormatBuilder =
            (SAMLObjectBuilder<NameIDFormat>) builderFactory.getBuilder(NameIDFormat.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static SAMLObjectBuilder<SingleSignOnService> singleSignOnServiceBuilder =
            (SAMLObjectBuilder<SingleSignOnService>) builderFactory.getBuilder(SingleSignOnService.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static SAMLObjectBuilder<SingleLogoutService> singleLogOutServiceBuilder =
            (SAMLObjectBuilder<SingleLogoutService>) builderFactory.getBuilder(SingleLogoutService.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static SAMLObjectBuilder<AssertionConsumerService> assertionConsumerServiceBuilder =
            (SAMLObjectBuilder<AssertionConsumerService>) builderFactory.getBuilder(
                    AssertionConsumerService.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static XMLObjectBuilder<KeyInfo> keyInfoBuilder =
            (XMLObjectBuilder<KeyInfo>) builderFactory.getBuilder(KeyInfo.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static XMLObjectBuilder<X509Data> x509DataBuilder =
            (XMLObjectBuilder<X509Data>) builderFactory.getBuilder(X509Data.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static XMLObjectBuilder<X509Certificate> x509CertificateBuilder =
            (XMLObjectBuilder<X509Certificate>) builderFactory.getBuilder(X509Certificate.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static SAMLObjectBuilder<AttributeQuery> attributeQueryBuilder =
            (SAMLObjectBuilder<AttributeQuery>) builderFactory.getBuilder(AttributeQuery.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static SAMLObjectBuilder<LogoutRequest> logoutRequestBuilder =
            (SAMLObjectBuilder<LogoutRequest>) builderFactory.getBuilder(LogoutRequest.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static SAMLObjectBuilder<LogoutResponse> logoutResponseBuilder =
            (SAMLObjectBuilder<LogoutResponse>) builderFactory.getBuilder(LogoutResponse.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static BodyBuilder soapBodyBuilder =
            (BodyBuilder) builderFactory.getBuilder(Body.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static EnvelopeBuilder soapEnvelopeBuilder =
            (EnvelopeBuilder) builderFactory.getBuilder(Envelope.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static HeaderBuilder soapHeaderBuilder = (HeaderBuilder) builderFactory.getBuilder(
            Header.DEFAULT_ELEMENT_NAME);

    public enum Binding {
        HTTP_POST("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"),
        HTTP_REDIRECT("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect"),
        HTTP_ARTIFACT("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact"),
        SOAP("urn:oasis:names:tc:SAML:2.0:bindings:SOAP"),
        PAOS("urn:oasis:names:tc:SAML:2.0:bindings:PAOS");

        private final String uri;

        private static Map<String, Binding> stringToBinding = new HashMap<>();

        static {
            for (Binding binding : Binding.values()) {
                stringToBinding.put(binding.getUri(), binding);
            }
        }

        Binding(String uri) {
            this.uri = uri;
        }

        public String getUri() {
            return uri;
        }

        public static Binding from(String value) {
            return stringToBinding.get(value);
        }

        public boolean isEqual(String uri) {
            return this.uri.equals(uri);
        }
    }

    public enum Type {
        REQUEST("SAMLRequest"),
        RESPONSE("SAMLResponse"),
        NULL("");

        private final String key;

        Type(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    private SamlProtocol() {

    }

    public static Response createResponse(Issuer issuer, Status status, String requestId,
            Element samlAssertion) throws WSSecurityException {
        Response response = responseSAMLObjectBuilder.buildObject();
        response.setIssuer(issuer);
        response.setStatus(status);
        response.setID("_" + UUID.randomUUID()
                .toString());
        response.setIssueInstant(new DateTime());
        response.setInResponseTo(requestId);
        response.setVersion(SAMLVersion.VERSION_20);
        if (samlAssertion != null) {
            SamlAssertionWrapper samlAssertionWrapper = new SamlAssertionWrapper(samlAssertion);
            response.getAssertions()
                    .add(samlAssertionWrapper.getSaml2());
        }
        return response;
    }

    public static Issuer createIssuer(String issuerValue) {
        Issuer issuer = issuerBuilder.buildObject();
        issuer.setValue(issuerValue);

        return issuer;
    }

    public static NameID createNameID(String nameIdValue) {
        NameID nameId = nameIdBuilder.buildObject();
        nameId.setValue(nameIdValue);

        return nameId;
    }

    public static Subject createSubject(NameID nameId) {
        Subject subject = subjectBuilder.buildObject();
        subject.setNameID(nameId);

        return subject;
    }

    public static Status createStatus(String statusValue) {
        Status status = statusBuilder.buildObject();
        StatusCode statusCode = statusCodeBuilder.buildObject();
        statusCode.setValue(statusValue);
        status.setStatusCode(statusCode);

        return status;
    }

    public static EntityDescriptor createIdpMetadata(String entityId, String signingCert,
            String encryptionCert, List<String> nameIds, String singleSignOnLocationRedirect,
            String singleSignOnLocationPost, String singleLogOutLocation) {
        EntityDescriptor entityDescriptor = entityDescriptorBuilder.buildObject();
        entityDescriptor.setEntityID(entityId);
        IDPSSODescriptor idpssoDescriptor = idpssoDescriptorBuilder.buildObject();
        //signing
        KeyDescriptor signingKeyDescriptor = keyDescriptorBuilder.buildObject();
        signingKeyDescriptor.setUse(UsageType.SIGNING);
        KeyInfo signingKeyInfo = keyInfoBuilder.buildObject(KeyInfo.DEFAULT_ELEMENT_NAME);
        X509Data signingX509Data = x509DataBuilder.buildObject(X509Data.DEFAULT_ELEMENT_NAME);
        X509Certificate signingX509Certificate =
                x509CertificateBuilder.buildObject(X509Certificate.DEFAULT_ELEMENT_NAME);
        signingX509Certificate.setValue(signingCert);
        signingX509Data.getX509Certificates()
                .add(signingX509Certificate);
        signingKeyInfo.getX509Datas()
                .add(signingX509Data);
        signingKeyDescriptor.setKeyInfo(signingKeyInfo);
        idpssoDescriptor.getKeyDescriptors()
                .add(signingKeyDescriptor);
        //encryption
        KeyDescriptor encKeyDescriptor = keyDescriptorBuilder.buildObject();
        encKeyDescriptor.setUse(UsageType.ENCRYPTION);
        KeyInfo encKeyInfo = keyInfoBuilder.buildObject(KeyInfo.DEFAULT_ELEMENT_NAME);
        X509Data encX509Data = x509DataBuilder.buildObject(X509Data.DEFAULT_ELEMENT_NAME);
        X509Certificate encX509Certificate =
                x509CertificateBuilder.buildObject(X509Certificate.DEFAULT_ELEMENT_NAME);
        encX509Certificate.setValue(encryptionCert);
        encX509Data.getX509Certificates()
                .add(encX509Certificate);
        encKeyInfo.getX509Datas()
                .add(encX509Data);
        encKeyDescriptor.setKeyInfo(encKeyInfo);
        idpssoDescriptor.getKeyDescriptors()
                .add(encKeyDescriptor);

        for (String nameId : nameIds) {
            NameIDFormat nameIDFormat = nameIdFormatBuilder.buildObject();
            nameIDFormat.setFormat(nameId);
            idpssoDescriptor.getNameIDFormats()
                    .add(nameIDFormat);
        }

        if (StringUtils.isNotBlank(singleSignOnLocationRedirect)) {
            SingleSignOnService singleSignOnServiceRedirect =
                    singleSignOnServiceBuilder.buildObject();
            singleSignOnServiceRedirect.setBinding(REDIRECT_BINDING);
            singleSignOnServiceRedirect.setLocation(singleSignOnLocationRedirect);
            idpssoDescriptor.getSingleSignOnServices()
                    .add(singleSignOnServiceRedirect);
        }

        if (StringUtils.isNotBlank(singleSignOnLocationPost)) {
            SingleSignOnService singleSignOnServicePost = singleSignOnServiceBuilder.buildObject();
            singleSignOnServicePost.setBinding(POST_BINDING);
            singleSignOnServicePost.setLocation(singleSignOnLocationPost);
            idpssoDescriptor.getSingleSignOnServices()
                    .add(singleSignOnServicePost);
        }

        if (StringUtils.isNotBlank(singleLogOutLocation)) {
            SingleLogoutService singleLogoutServiceRedir = singleLogOutServiceBuilder.buildObject();
            singleLogoutServiceRedir.setBinding(REDIRECT_BINDING);
            singleLogoutServiceRedir.setLocation(singleLogOutLocation);
            idpssoDescriptor.getSingleLogoutServices()
                    .add(singleLogoutServiceRedir);

            SingleLogoutService singleLogoutServicePost = singleLogOutServiceBuilder.buildObject();
            singleLogoutServicePost.setBinding(POST_BINDING);
            singleLogoutServicePost.setLocation(singleLogOutLocation);
            idpssoDescriptor.getSingleLogoutServices()
                    .add(singleLogoutServicePost);
        }

        idpssoDescriptor.setWantAuthnRequestsSigned(true);

        idpssoDescriptor.addSupportedProtocol(SUPPORTED_PROTOCOL);

        entityDescriptor.getRoleDescriptors()
                .add(idpssoDescriptor);

        return entityDescriptor;
    }

    public static EntityDescriptor createSpMetadata(String entityId, String signingCert,
            String encryptionCert, String singleLogOutLocation,
            String assertionConsumerServiceLocationRedirect,
            String assertionConsumerServiceLocationPost) {
        EntityDescriptor entityDescriptor = entityDescriptorBuilder.buildObject();
        entityDescriptor.setEntityID(entityId);
        SPSSODescriptor spSsoDescriptor = spSsoDescriptorBuilder.buildObject();
        //signing
        KeyDescriptor signingKeyDescriptor = keyDescriptorBuilder.buildObject();
        signingKeyDescriptor.setUse(UsageType.SIGNING);
        KeyInfo signingKeyInfo = keyInfoBuilder.buildObject(KeyInfo.DEFAULT_ELEMENT_NAME);
        X509Data signingX509Data = x509DataBuilder.buildObject(X509Data.DEFAULT_ELEMENT_NAME);
        X509Certificate signingX509Certificate =
                x509CertificateBuilder.buildObject(X509Certificate.DEFAULT_ELEMENT_NAME);
        signingX509Certificate.setValue(signingCert);
        signingX509Data.getX509Certificates()
                .add(signingX509Certificate);
        signingKeyInfo.getX509Datas()
                .add(signingX509Data);
        signingKeyDescriptor.setKeyInfo(signingKeyInfo);
        spSsoDescriptor.getKeyDescriptors()
                .add(signingKeyDescriptor);
        //encryption
        KeyDescriptor encKeyDescriptor = keyDescriptorBuilder.buildObject();
        encKeyDescriptor.setUse(UsageType.ENCRYPTION);
        KeyInfo encKeyInfo = keyInfoBuilder.buildObject(KeyInfo.DEFAULT_ELEMENT_NAME);
        X509Data encX509Data = x509DataBuilder.buildObject(X509Data.DEFAULT_ELEMENT_NAME);
        X509Certificate encX509Certificate =
                x509CertificateBuilder.buildObject(X509Certificate.DEFAULT_ELEMENT_NAME);
        encX509Certificate.setValue(encryptionCert);
        encX509Data.getX509Certificates()
                .add(encX509Certificate);
        encKeyInfo.getX509Datas()
                .add(encX509Data);
        encKeyDescriptor.setKeyInfo(encKeyInfo);
        spSsoDescriptor.getKeyDescriptors()
                .add(encKeyDescriptor);

        if (StringUtils.isNotBlank(singleLogOutLocation)) {
            SingleLogoutService singleLogoutServiceRedirect =
                    singleLogOutServiceBuilder.buildObject();
            singleLogoutServiceRedirect.setBinding(REDIRECT_BINDING);
            singleLogoutServiceRedirect.setLocation(singleLogOutLocation);
            spSsoDescriptor.getSingleLogoutServices()
                    .add(singleLogoutServiceRedirect);

            SingleLogoutService singleLogoutServicePost = singleLogOutServiceBuilder.buildObject();
            singleLogoutServicePost.setBinding(POST_BINDING);
            singleLogoutServicePost.setLocation(singleLogOutLocation);
            spSsoDescriptor.getSingleLogoutServices()
                    .add(singleLogoutServicePost);
        }

        int acsIndex = 0;

        if (StringUtils.isNotBlank(assertionConsumerServiceLocationRedirect)) {
            AssertionConsumerService assertionConsumerService =
                    assertionConsumerServiceBuilder.buildObject();
            assertionConsumerService.setBinding(REDIRECT_BINDING);
            assertionConsumerService.setIndex(acsIndex++);
            assertionConsumerService.setLocation(assertionConsumerServiceLocationRedirect);
            spSsoDescriptor.getAssertionConsumerServices()
                    .add(assertionConsumerService);
        }

        if (StringUtils.isNotBlank(assertionConsumerServiceLocationPost)) {
            AssertionConsumerService assertionConsumerService =
                    assertionConsumerServiceBuilder.buildObject();
            assertionConsumerService.setBinding(POST_BINDING);
            assertionConsumerService.setIndex(acsIndex++);
            assertionConsumerService.setLocation(assertionConsumerServiceLocationPost);
            spSsoDescriptor.getAssertionConsumerServices()
                    .add(assertionConsumerService);
        }

        spSsoDescriptor.addSupportedProtocol(SUPPORTED_PROTOCOL);

        entityDescriptor.getRoleDescriptors()
                .add(spSsoDescriptor);

        return entityDescriptor;
    }

    public static AttributeQuery createAttributeQuery(Issuer issuer, Subject subject,
            String destination) {
        AttributeQuery attributeQuery = attributeQueryBuilder.buildObject();
        attributeQuery.setID(UUID.randomUUID()
                .toString());
        attributeQuery.setIssueInstant(new DateTime());
        attributeQuery.setIssuer(issuer);
        attributeQuery.setSubject(subject);
        attributeQuery.setVersion(SAMLVersion.VERSION_20);
        if (StringUtils.isNotBlank(destination)) {
            attributeQuery.setDestination(destination);
        }
        return attributeQuery;
    }

    public static AttributeQuery createAttributeQuery(Issuer issuer, Subject subject) {
        return createAttributeQuery(issuer, subject, null);
    }

    public static LogoutRequest createLogoutRequest(Issuer issuer, NameID nameId, String id) {
        LogoutRequest logoutRequest = logoutRequestBuilder.buildObject();
        logoutRequest.setID(id);
        logoutRequest.setIssuer(issuer);
        logoutRequest.setNameID(nameId);
        logoutRequest.setIssueInstant(DateTime.now());
        logoutRequest.setVersion(SAMLVersion.VERSION_20);
        return logoutRequest;
    }

    public static LogoutResponse createLogoutResponse(Issuer issuer, Status status,
            String inResponseTo, String id) {
        LogoutResponse logoutResponse = logoutResponseBuilder.buildObject();
        logoutResponse.setID(id);
        logoutResponse.setIssuer(issuer);
        logoutResponse.setStatus(status);
        if (StringUtils.isNotBlank(inResponseTo)) {
            logoutResponse.setInResponseTo(inResponseTo);
        }
        logoutResponse.setIssueInstant(DateTime.now());
        logoutResponse.setVersion(SAMLVersion.VERSION_20);
        return logoutResponse;
    }

    public static LogoutResponse createLogoutResponse(Issuer issuer, Status status, String id) {
        return createLogoutResponse(issuer, status, null, id);
    }

    public static Envelope createSoapMessage(SignableSAMLObject signableSAMLObject) {
        Body body = soapBodyBuilder.buildObject();
        body.getUnknownXMLObjects()
                .add(signableSAMLObject);
        Envelope envelope = soapEnvelopeBuilder.buildObject();
        envelope.setBody(body);
        Header header = soapHeaderBuilder.buildObject();
        envelope.setHeader(header);

        return envelope;
    }
}
