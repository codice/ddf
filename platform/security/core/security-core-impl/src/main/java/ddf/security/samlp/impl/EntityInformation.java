package ddf.security.samlp.impl;

import static java.util.Objects.nonNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import javax.annotation.concurrent.Immutable;

import org.apache.commons.lang.StringUtils;
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

@Immutable
public class EntityInformation {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityInformation.class);

    private final String signingCertificate;

    private final String encryptionCertificate;

    private final ServiceInfo assertionInfo;

    private final ServiceInfo logoutInfo;

    private EntityInformation(Builder builder) {
        signingCertificate = builder.signingCertificate;
        encryptionCertificate = builder.encryptionCertificate;
        assertionInfo = builder.assertionConsumerServiceInfo;
        logoutInfo = builder.logoutServiceInfo;
    }

    public String getSigningCertificate() {
        return signingCertificate;
    }

    public String getEncryptionCertificate() {
        return encryptionCertificate;
    }

    public String getAssertionConsumerServiceURL() {
        return assertionInfo.url;
    }

    public String getAssertionConsumerServiceBinding() {
        return assertionInfo.binding;
    }

    public String getLogoutServiceUrl() {
        return logoutInfo.url;
    }

    public String getLogoutServiceBinding() {
        return logoutInfo.binding;
    }

    public static class Builder {
        private static final ImmutableSet<UsageType> SIGNING_TYPES =
                ImmutableSet.of(UsageType.UNSPECIFIED, UsageType.SIGNING);

        private static final ImmutableSet<String> IDP_BINDING_TYPES =
                ImmutableSet.of(SamlProtocol.POST_BINDING, SamlProtocol.REDIRECT_BINDING);

        private final SPSSODescriptor spssoDescriptor;

        private String signingCertificate;

        private String encryptionCertificate;

        private ServiceInfo assertionConsumerServiceInfo;

        private ServiceInfo logoutServiceInfo;

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
                    .parseLogoutServiceInfo());
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
            AssertionConsumerService defaultAssertionConsumerService =
                    spssoDescriptor.getDefaultAssertionConsumerService();
            //see if the default service uses our supported bindings, and then use that
            //as we add more bindings, we'll need to update this
            if (defaultAssertionConsumerService != null && IDP_BINDING_TYPES.contains(
                    defaultAssertionConsumerService.getBinding())) {
                LOGGER.debug(
                        "Using AssertionConsumerServiceURL from default assertion consumer service: {}",
                        defaultAssertionConsumerService.getLocation());
                assertionConsumerServiceInfo =
                        new ServiceInfo(defaultAssertionConsumerService.getLocation(),
                                defaultAssertionConsumerService.getBinding());
                return this;
            }

            assertionConsumerServiceInfo =
                    parseServiceInfo(spssoDescriptor.getAssertionConsumerServices());

            return this;
        }

        Builder parseLogoutServiceInfo() {
            logoutServiceInfo = parseServiceInfo(spssoDescriptor.getSingleLogoutServices());
            return this;
        }

        <T> ServiceInfo parseServiceInfo(List<? extends Endpoint> services) {
            return services.stream()
                    .filter(si -> IDP_BINDING_TYPES.contains(si.getBinding()))
                    .findFirst()
                    .map(si -> new ServiceInfo(si.getLocation(), si.getBinding()))
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

    private static class ServiceInfo {
        private final String url;

        private final String binding;

        ServiceInfo(String url, String binding) {
            this.url = url;
            this.binding = binding;
        }
    }
}