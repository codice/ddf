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
package org.codice.ddf.security.idp.client;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.opensaml.common.SAMLException;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml2.metadata.SingleSignOnService;
import org.opensaml.xml.security.credential.UsageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.samlp.MetadataConfigurationParser;

public class IdpMetadata {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdpMetadata.class);

    private IDPSSODescriptor descriptor;

    private String singleSignOnLocation;

    private String singleSignOnBinding;

    private String entityId;

    private String signingCertificate;

    private String encryptionCertificate;

    public void setMetadata(String metadata)
        throws WSSecurityException, XMLStreamException, SAMLException {
        descriptor = null;
        entityId = null;
        singleSignOnLocation = null;
        singleSignOnBinding = null;
        signingCertificate = null;
        encryptionCertificate = null;

        if (StringUtils.isBlank(metadata)) {
            return;
        }

        EntityDescriptor entity = null;
        try {
            entity = MetadataConfigurationParser.buildEntityDescriptor(metadata);
        } catch (IOException e) {
            LOGGER.error("Unable to parse IDP metadata.", e);
            return;
        }

        descriptor = entity
                .getIDPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol");

        if (descriptor == null) {
            throw new SAMLException("Unable to find SAML 2.0 IdP Entity Descriptor in metadata.");
        }

        entityId = entity.getEntityID();

        for (SingleSignOnService service : descriptor.getSingleSignOnServices()) {
            // Prefer HTTP-Redirect over HTTP-POST if both are present
            if (singleSignOnBinding == null || "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
                    .equals(singleSignOnBinding)) {
                singleSignOnBinding = service.getBinding();
                singleSignOnLocation = service.getLocation();
            }
        }

        for (KeyDescriptor key : descriptor.getKeyDescriptors()) {
            String certificate = null;
            if (key.getKeyInfo().getX509Datas().size() > 0
                    && key.getKeyInfo().getX509Datas().get(0).getX509Certificates().size() > 0) {
                certificate = key.getKeyInfo().getX509Datas().get(0).getX509Certificates().get(0)
                        .getValue();
            }

            if (StringUtils.isBlank(certificate)) {
                break;
            }

            if (UsageType.UNSPECIFIED.equals(key.getUse())) {
                encryptionCertificate = certificate;
                signingCertificate = certificate;
            }

            if (UsageType.ENCRYPTION.equals(key.getUse())) {
                encryptionCertificate = certificate;
            }

            if (UsageType.SIGNING.equals(key.getUse())) {
                signingCertificate = certificate;
            }
        }

    }

    public IDPSSODescriptor getDescriptor() {
        return descriptor;
    }

    public String getSingleSignOnLocation() {
        return singleSignOnLocation;
    }

    public String getSingleSignOnBinding() {
        return singleSignOnBinding;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getSigningCertificate() {
        return signingCertificate;
    }

    public String getEncryptionCertificate() {
        return encryptionCertificate;
    }
}
