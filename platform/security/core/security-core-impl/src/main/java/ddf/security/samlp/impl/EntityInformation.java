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

import static java.util.Objects.nonNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import javax.annotation.concurrent.Immutable;

import org.apache.commons.lang.StringUtils;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml2.metadata.Endpoint;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.opensaml.xml.schema.XSBase64Binary;
import org.opensaml.xml.security.credential.UsageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SamlProtocol.Binding;

@Immutable
public class EntityInformation {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityInformation.class);

    private static final ImmutableSet<Binding> SUPPORTED_BINDINGS =
            ImmutableSet.of(Binding.HTTP_POST, Binding.HTTP_REDIRECT);

    private static final Binding PREFERRED_BINDING = Binding.HTTP_REDIRECT;

    private final String signingCertificate;

    private final String encryptionCertificate;

    private final ServiceInfo defaultAssertionConsumerService;

    private final Map<Binding, ServiceInfo> assertionConsumerServices;

    private final Map<Binding, ServiceInfo> logoutServices;

    private EntityInformation(Builder builder) {
        signingCertificate = builder.signingCertificate;
        encryptionCertificate = builder.encryptionCertificate;
        defaultAssertionConsumerService = builder.defaultAssertionConsumerService;
        assertionConsumerServices = builder.assertionConsumerServices;
        logoutServices = builder.logoutServices;
    }

    public String getSigningCertificate() {
        return signingCertificate;
    }

    public String getEncryptionCertificate() {
        return encryptionCertificate;
    }

    public ServiceInfo getLogoutService() {
        return getLogoutService(null);
    }

    public ServiceInfo getLogoutService(Binding preferred) {
        Binding binding = getBinding(null, preferred);
        ServiceInfo logoutServiceInfo = logoutServices.get(binding);
        if (logoutServiceInfo == null) {
            logoutServiceInfo = logoutServices.values()
                    .stream()
                    .findFirst()
                    .orElse(null);
        }

        return logoutServiceInfo;
    }

    Binding getBinding(AuthnRequest request, Binding preferred) {
        if (request != null && request.getProtocolBinding() != null && SUPPORTED_BINDINGS.contains(
                Binding.from(request.getProtocolBinding()))) {
            return Binding.from(request.getProtocolBinding());
        }

        return preferred != null ? preferred : PREFERRED_BINDING;
    }

    public ServiceInfo getAssertionConsumerService(AuthnRequest request, Binding preferred) {
        if (defaultAssertionConsumerService != null) {
            return defaultAssertionConsumerService;
        }

        Binding binding = preferred != null ? preferred : PREFERRED_BINDING;
        if (request != null && request.getProtocolBinding() != null && SUPPORTED_BINDINGS.contains(
                Binding.from(request.getProtocolBinding()))) {
            binding = Binding.from(request.getProtocolBinding());
        }

        ServiceInfo si = assertionConsumerServices.get(binding);
        if (si != null) {
            return si;
        }
        return assertionConsumerServices.values()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public static class Builder {
        private static final ImmutableSet<UsageType> SIGNING_TYPES =
                ImmutableSet.of(UsageType.UNSPECIFIED, UsageType.SIGNING);

        private final SPSSODescriptor spssoDescriptor;

        private String signingCertificate;

        private String encryptionCertificate;

        private ServiceInfo defaultAssertionConsumerService;

        private Map<Binding, ServiceInfo> assertionConsumerServices;

        private Map<Binding, ServiceInfo> logoutServices;

        public Builder(EntityDescriptor ed) {
            spssoDescriptor = getSpssoDescriptor(ed);
        }

        public EntityInformation build() {
            if (spssoDescriptor == null) {
                LOGGER.warn("Unable to build EntityInformation without a descriptor");
                return null;
            }
            return new EntityInformation(this.parseSigningCertificate()
                    .parseEncryptionCertificate()
                    .parseAssertionConsumerServiceInfo()
                    .parseLogoutServices());
        }

        SPSSODescriptor getSpssoDescriptor(EntityDescriptor ed) {
            SPSSODescriptor spssoDescriptor =
                    ed.getSPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL);
            if (spssoDescriptor == null) {
                LOGGER.warn("Unable to find supported protocol in EntityDescriptor {}",
                        ed.getEntityID());
            }
            return spssoDescriptor;
        }

        Builder parseSigningCertificate() {
            signingCertificate = extractCertificate(spssoDescriptor,
                    kd -> SIGNING_TYPES.contains(kd.getUse()));
            return this;
        }

        Builder parseEncryptionCertificate() {
            encryptionCertificate = extractCertificate(spssoDescriptor,
                    kd -> UsageType.ENCRYPTION.equals(kd.getUse()));
            return this;
        }

        Builder parseAssertionConsumerServiceInfo() {
            AssertionConsumerService defaultACS =
                    spssoDescriptor.getDefaultAssertionConsumerService();
            //see if the default service uses our supported bindings, and then use that
            //as we add more bindings, we'll need to update this
            if (defaultACS != null
                    && SUPPORTED_BINDINGS.contains(Binding.from(defaultACS.getBinding()))) {
                LOGGER.debug(
                        "Using AssertionConsumerServiceURL from default assertion consumer service: {}",
                        defaultACS.getLocation());
                defaultAssertionConsumerService = new ServiceInfo(defaultACS.getLocation(),
                        Binding.from(defaultACS.getBinding()));
                return this;
            }

            putAllSupported(assertionConsumerServices,
                    spssoDescriptor.getAssertionConsumerServices());
            return this;
        }

        Builder parseLogoutServices() {
            putAllSupported(logoutServices, spssoDescriptor.getSingleLogoutServices());
            return this;
        }

        void putAllSupported(Map<Binding, ServiceInfo> target, List<? extends Endpoint> services) {
            for (Binding binding : SUPPORTED_BINDINGS) {
                ServiceInfo serviceInfo = parseServiceInfo(services,
                        e -> binding.isEqual(e.getBinding()));
                if (serviceInfo.url != null) {
                    target.put(binding, serviceInfo);
                }
            }
        }

        ServiceInfo parseServiceInfo(List<? extends Endpoint> services,
                Predicate<Endpoint> bindingFilter) {
            return services.stream()
                    .filter(bindingFilter)
                    .findFirst()
                    .map(si -> new ServiceInfo(si.getLocation(), Binding.from(si.getBinding())))
                    .orElse(new ServiceInfo(null, null));
        }

        String extractCertificate(SPSSODescriptor spssoDescriptor,
                Predicate<KeyDescriptor> usageTypePredicate) {
            return spssoDescriptor.getKeyDescriptors()
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(kd -> nonNull(kd.getUse()))
                    .filter(usageTypePredicate)
                    .filter(kd -> nonNull(extractCertificateFromKeyDescriptor(kd)))
                    .reduce((acc, val) -> val.getUse()
                            .equals(UsageType.SIGNING) || acc == null ? val : acc)
                    .map(this::extractCertificateFromKeyDescriptor)
                    .orElse(null);
        }

        String extractCertificateFromKeyDescriptor(KeyDescriptor kd) {
            return kd.getKeyInfo()
                    .getX509Datas()
                    .stream()
                    .flatMap(datas -> datas.getX509Certificates()
                            .stream())
                    .map(XSBase64Binary::getValue)
                    .filter(StringUtils::isNotBlank)
                    .findFirst()
                    .orElse(null);
        }
    }

    public static class ServiceInfo {
        private final String url;

        private final Binding binding;

        ServiceInfo(String url, Binding binding) {
            this.url = url;
            this.binding = binding;
        }

        public String getUrl() {
            return url;
        }

        public Binding getBinding() {
            return binding;
        }
    }
}