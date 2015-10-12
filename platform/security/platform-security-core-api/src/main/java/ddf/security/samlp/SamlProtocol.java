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
package ddf.security.samlp;

import java.util.List;
import java.util.UUID;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml2.metadata.NameIDFormat;
import org.opensaml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml2.metadata.SingleSignOnService;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.X509Certificate;
import org.opensaml.xml.signature.X509Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class SamlProtocol {

    private static final Logger LOGGER = LoggerFactory.getLogger(SamlProtocol.class);

    public static final String SUPPORTED_PROTOCOL = "urn:oasis:names:tc:SAML:2.0:protocol";

    private static XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();

    private static SAMLObjectBuilder<Response> responseSAMLObjectBuilder = (SAMLObjectBuilder<org.opensaml.saml2.core.Response>) builderFactory
            .getBuilder(org.opensaml.saml2.core.Response.DEFAULT_ELEMENT_NAME);

    private static SAMLObjectBuilder<Issuer> issuerBuilder = (SAMLObjectBuilder<Issuer>) builderFactory
            .getBuilder(Issuer.DEFAULT_ELEMENT_NAME);

    private static SAMLObjectBuilder<Status> statusBuilder = (SAMLObjectBuilder<Status>) builderFactory
            .getBuilder(Status.DEFAULT_ELEMENT_NAME);

    private static SAMLObjectBuilder<StatusCode> statusCodeBuilder = (SAMLObjectBuilder<StatusCode>) builderFactory
            .getBuilder(StatusCode.DEFAULT_ELEMENT_NAME);

    private static SAMLObjectBuilder<EntityDescriptor> entityDescriptorBuilder = (SAMLObjectBuilder<EntityDescriptor>) builderFactory
            .getBuilder(EntityDescriptor.DEFAULT_ELEMENT_NAME);

    private static SAMLObjectBuilder<IDPSSODescriptor> idpssoDescriptorBuilder = (SAMLObjectBuilder<IDPSSODescriptor>) builderFactory
            .getBuilder(IDPSSODescriptor.DEFAULT_ELEMENT_NAME);

    private static SAMLObjectBuilder<KeyDescriptor> keyDescriptorBuilder = (SAMLObjectBuilder<KeyDescriptor>) builderFactory
            .getBuilder(KeyDescriptor.DEFAULT_ELEMENT_NAME);

    private static XMLObjectBuilder<KeyInfo> keyInfoBuilder = (XMLObjectBuilder<KeyInfo>) builderFactory
            .getBuilder(KeyInfo.DEFAULT_ELEMENT_NAME);

    private static XMLObjectBuilder<X509Data> x509DataBuilder = (XMLObjectBuilder<X509Data>) builderFactory
            .getBuilder(X509Data.DEFAULT_ELEMENT_NAME);

    private static XMLObjectBuilder<X509Certificate> x509CertificateBuilder = (XMLObjectBuilder<X509Certificate>) builderFactory
            .getBuilder(X509Certificate.DEFAULT_ELEMENT_NAME);

    private static SAMLObjectBuilder<NameIDFormat> nameIdFormatBuilder = (SAMLObjectBuilder<NameIDFormat>) builderFactory
            .getBuilder(NameIDFormat.DEFAULT_ELEMENT_NAME);

    private static SAMLObjectBuilder<SingleSignOnService> singleSignOnServiceBuilder = (SAMLObjectBuilder<SingleSignOnService>) builderFactory
            .getBuilder(SingleSignOnService.DEFAULT_ELEMENT_NAME);

    private static SAMLObjectBuilder<SingleLogoutService> singleLogOutServiceBuilder = (SAMLObjectBuilder<SingleLogoutService>) builderFactory
            .getBuilder(SingleLogoutService.DEFAULT_ELEMENT_NAME);

    private SamlProtocol() {
    }

    public static Response createResponse(Issuer issuer, Status status, String requestId,
            Element samlAssertion) throws WSSecurityException {
        Response response = responseSAMLObjectBuilder.buildObject();
        response.setIssuer(issuer);
        response.setStatus(status);
        response.setID("_" + UUID.randomUUID().toString());
        response.setIssueInstant(new DateTime());
        response.setInResponseTo(requestId);
        response.setVersion(SAMLVersion.VERSION_20);
        if (samlAssertion != null) {
            SamlAssertionWrapper samlAssertionWrapper = new SamlAssertionWrapper(samlAssertion);
            response.getAssertions().add(samlAssertionWrapper.getSaml2());
        }
        return response;
    }

    public static Issuer createIssuer(String issuerValue) {
        Issuer issuer = issuerBuilder.buildObject();
        issuer.setValue(issuerValue);

        return issuer;
    }

    public static Status createStatus(String statusValue) {
        Status status = statusBuilder.buildObject();
        StatusCode statusCode = statusCodeBuilder.buildObject();
        statusCode.setValue(statusValue);
        status.setStatusCode(statusCode);

        return status;
    }

    public static EntityDescriptor createMetadata(String entityId, String signingCert,
            String encryptionCert, List<String> nameIds, String singleSignOnLocationRedirect, String singleSignOnLocationPost,
            String singleLogOutLocation) {
        EntityDescriptor entityDescriptor = entityDescriptorBuilder.buildObject();
        entityDescriptor.setEntityID(entityId);
        IDPSSODescriptor idpssoDescriptor = idpssoDescriptorBuilder.buildObject();
        //signing
        KeyDescriptor signingKeyDescriptor = keyDescriptorBuilder.buildObject();
        signingKeyDescriptor.setUse(UsageType.SIGNING);
        KeyInfo signingKeyInfo = keyInfoBuilder.buildObject(KeyInfo.DEFAULT_ELEMENT_NAME);
        X509Data signingX509Data = x509DataBuilder.buildObject(X509Data.DEFAULT_ELEMENT_NAME);
        X509Certificate signingX509Certificate = x509CertificateBuilder
                .buildObject(X509Certificate.DEFAULT_ELEMENT_NAME);
        signingX509Certificate.setValue(signingCert);
        signingX509Data.getX509Certificates().add(signingX509Certificate);
        signingKeyInfo.getX509Datas().add(signingX509Data);
        signingKeyDescriptor.setKeyInfo(signingKeyInfo);
        idpssoDescriptor.getKeyDescriptors().add(signingKeyDescriptor);
        //encryption
        KeyDescriptor encKeyDescriptor = keyDescriptorBuilder.buildObject();
        encKeyDescriptor.setUse(UsageType.ENCRYPTION);
        KeyInfo encKeyInfo = keyInfoBuilder.buildObject(KeyInfo.DEFAULT_ELEMENT_NAME);
        X509Data encX509Data = x509DataBuilder.buildObject(X509Data.DEFAULT_ELEMENT_NAME);
        X509Certificate encX509Certificate = x509CertificateBuilder
                .buildObject(X509Certificate.DEFAULT_ELEMENT_NAME);
        encX509Certificate.setValue(encryptionCert);
        encX509Data.getX509Certificates().add(encX509Certificate);
        encKeyInfo.getX509Datas().add(encX509Data);
        encKeyDescriptor.setKeyInfo(encKeyInfo);
        idpssoDescriptor.getKeyDescriptors().add(encKeyDescriptor);

        for (String nameId : nameIds) {
            NameIDFormat nameIDFormat = nameIdFormatBuilder.buildObject();
            nameIDFormat.setFormat(nameId);
            idpssoDescriptor.getNameIDFormats().add(nameIDFormat);
        }

        SingleSignOnService singleSignOnServiceRedir = singleSignOnServiceBuilder.buildObject();
        singleSignOnServiceRedir.setBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect");
        singleSignOnServiceRedir.setLocation(singleSignOnLocationRedirect);
        idpssoDescriptor.getSingleSignOnServices().add(singleSignOnServiceRedir);

        SingleSignOnService singleSignOnServicePost = singleSignOnServiceBuilder.buildObject();
        singleSignOnServicePost.setBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
        singleSignOnServicePost.setLocation(singleSignOnLocationPost);
        idpssoDescriptor.getSingleSignOnServices().add(singleSignOnServicePost);

        SingleLogoutService singleLogoutServiceRedir = singleLogOutServiceBuilder.buildObject();
        singleLogoutServiceRedir.setBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect");
        singleLogoutServiceRedir.setLocation(singleLogOutLocation);
        idpssoDescriptor.getSingleLogoutServices().add(singleLogoutServiceRedir);

        SingleLogoutService singleLogoutServicePost = singleLogOutServiceBuilder.buildObject();
        singleLogoutServicePost.setBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
        singleLogoutServicePost.setLocation(singleLogOutLocation);
        idpssoDescriptor.getSingleLogoutServices().add(singleLogoutServicePost);

        idpssoDescriptor.setWantAuthnRequestsSigned(true);

        idpssoDescriptor.addSupportedProtocol(SUPPORTED_PROTOCOL);

        entityDescriptor.getRoleDescriptors().add(idpssoDescriptor);

        return entityDescriptor;
    }
}
