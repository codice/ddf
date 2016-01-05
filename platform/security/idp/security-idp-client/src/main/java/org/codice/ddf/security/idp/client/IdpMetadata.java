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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.opensaml.saml.common.SAMLException;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.security.credential.UsageType;

import ddf.security.samlp.MetadataConfigurationParser;

public class IdpMetadata {

    private static final String SAML_2_0_PROTOCOL = "urn:oasis:names:tc:SAML:2.0:protocol";

    private static final String BINDINGS_HTTP_POST =
            "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";

    private static final String BINDINGS_HTTP_REDIRECT =
            "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect";

    private String singleSignOnLocation;

    private String singleSignOnBinding;

    private String signingCertificate;

    private String encryptionCertificate;

    private Map<String, EntityDescriptor> entryDescriptions;

    private String singleLogoutBinding;

    private String singleLogoutLocation;

    public void setMetadata(String metadata)
            throws WSSecurityException, XMLStreamException, SAMLException, IOException {
        MetadataConfigurationParser metadataConfigurationParser = new MetadataConfigurationParser(
                Collections.singletonList(metadata));
        entryDescriptions = metadataConfigurationParser.getEntryDescriptions();
    }

    private void initSingleSignOn() {
        IDPSSODescriptor descriptor = getDescriptor();
        if (descriptor != null) {
            // Prefer HTTP-Redirect over HTTP-POST if both are present
            Optional<? extends Endpoint> service =
                    initSingleSomething(descriptor.getSingleSignOnServices());

            if (service.isPresent()) {
                singleSignOnBinding = service.get()
                        .getBinding();
                singleSignOnLocation = service.get()
                        .getLocation();
            }
        }
    }

    private void initSingleSignOut() {
        IDPSSODescriptor descriptor = getDescriptor();
        if (descriptor != null) {

            // Prefer HTTP-Redirect over HTTP-POST if both are present
            Optional<? extends Endpoint> service =
                    initSingleSomething(descriptor.getSingleLogoutServices());

            if (service.isPresent()) {
                singleLogoutBinding = service.get()
                        .getBinding();
                singleLogoutLocation = service.get()
                        .getLocation();
            }
        }
    }

    private Optional<? extends Endpoint> initSingleSomething(List<? extends Endpoint> endpoints) {
        IDPSSODescriptor descriptor = getDescriptor();
        if (descriptor == null) {
            return Optional.empty();
        }

        // Prefer HTTP-Redirect over HTTP-POST if both are present
        return endpoints.stream()
                .filter(Objects::nonNull)
                .filter(s -> Objects.nonNull(s.getBinding()))
                .filter(s -> s.getBinding()
                        .equals(BINDINGS_HTTP_POST) || s.getBinding()
                        .equals(BINDINGS_HTTP_REDIRECT))
                .reduce((acc, val) -> {
                    if (acc == null || !BINDINGS_HTTP_REDIRECT.equals(acc.getBinding())) {
                        return val;
                    }
                    return acc;
                });
    }

    private void initCertificates() {
        IDPSSODescriptor descriptor = getDescriptor();
        if (descriptor != null) {
            for (KeyDescriptor key : descriptor.getKeyDescriptors()) {
                String certificate = null;
                if (key.getKeyInfo()
                        .getX509Datas()
                        .size() > 0 && key.getKeyInfo()
                        .getX509Datas()
                        .get(0)
                        .getX509Certificates()
                        .size() > 0) {
                    certificate = key.getKeyInfo()
                            .getX509Datas()
                            .get(0)
                            .getX509Certificates()
                            .get(0)
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
    }

    private EntityDescriptor getEntityDescriptor() {
        Set<Map.Entry<String, EntityDescriptor>> entries = entryDescriptions.entrySet();
        if (!entries.isEmpty()) {
            return entries.iterator()
                    .next()
                    .getValue();
        }
        return null;
    }

    public IDPSSODescriptor getDescriptor() {
        EntityDescriptor entityDescriptor = getEntityDescriptor();
        if (entityDescriptor != null) {
            return entityDescriptor.getIDPSSODescriptor(SAML_2_0_PROTOCOL);
        }
        return null;
    }

    public String getSingleSignOnLocation() {
        initSingleSignOn();
        return singleSignOnLocation;
    }

    public String getSingleSignOnBinding() {
        initSingleSignOn();
        return singleSignOnBinding;
    }

    public String getSingleLogoutBinding() {
        initSingleSignOut();
        return singleLogoutBinding;
    }

    public String getSingleLogoutLocation() {
        initSingleSignOut();
        return singleLogoutLocation;
    }

    public String getEntityId() {
        EntityDescriptor entityDescriptor = getEntityDescriptor();
        if (entityDescriptor != null) {
            return entityDescriptor.getEntityID();
        }
        return null;
    }

    public String getSigningCertificate() {
        initCertificates();
        return signingCertificate;
    }

    public String getEncryptionCertificate() {
        initCertificates();
        return encryptionCertificate;
    }
}
