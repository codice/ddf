package ddf.security.samlp.impl;

import com.google.common.collect.ImmutableSet;
import ddf.security.samlp.SamlProtocol;
import org.apache.commons.lang.StringUtils;
import org.opensaml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.opensaml.xml.schema.XSBase64Binary;
import org.opensaml.xml.security.credential.UsageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.function.Predicate;

import static java.util.Objects.nonNull;

@Immutable
public class EntityInformation {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityInformation.class);

    private final String signingCertificate;

    private final String encryptionCertificate;

    private final AssertionInfo assertionInfo;

    private final String assertionConsumerServiceBinding;

    private EntityInformation(Builder builder) {
        signingCertificate = builder.signingCertificate;
        encryptionCertificate = builder.encryptionCertificate;
        assertionInfo = builder.assertionConsumerServiceURL;
        assertionConsumerServiceBinding = builder.assertionConsumerServiceBinding;
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

    public String getAssertConsumerServiceBinding() {
        return assertionInfo.binding;
    }

    public String getAssertionConsumerServiceBinding() {
        return assertionConsumerServiceBinding;
    }

    public static class Builder {
        private static final ImmutableSet<UsageType> SIGNING_TYPES = ImmutableSet.of(UsageType.UNSPECIFIED,
                UsageType.SIGNING);

        private static final ImmutableSet<String> IDP_BINDING_TYPES = ImmutableSet.of(SamlProtocol.POST_BINDING,
                SamlProtocol.REDIRECT_BINDING);

        private final SPSSODescriptor spssoDescriptor;

        private String signingCertificate;

        private String encryptionCertificate;

        private AssertionInfo assertionConsumerServiceURL;

        private String assertionConsumerServiceBinding;

        public Builder(EntityDescriptor ed) {
            spssoDescriptor = getSpssoDescriptor(ed);
        }

        public EntityInformation build() {
            return new EntityInformation(parseSigningCertificate().parseEncryptionCertificate().parseAssertionConsumerServiceURL());
        }

        Builder parseSigningCertificate() {
            signingCertificate = extractCertificate(spssoDescriptor, kd -> SIGNING_TYPES.contains(kd.getUse()));
            return this;
        }

        Builder parseEncryptionCertificate() {
            encryptionCertificate = extractCertificate(spssoDescriptor, kd -> UsageType.ENCRYPTION.equals(kd.getUse()));
            return this;
        }

        Builder parseAssertionConsumerServiceURL() {
            AssertionConsumerService defaultAssertionConsumerService = spssoDescriptor
                    .getDefaultAssertionConsumerService();
            //see if the default service uses our supported bindings, and then use that
            //as we add more bindings, we'll need to update this
            if (IDP_BINDING_TYPES.contains(defaultAssertionConsumerService.getBinding())) {
                LOGGER.debug(
                        "Using AssertionConsumerServiceURL from default assertion consumer service: {}",
                        defaultAssertionConsumerService.getLocation());
                assertionConsumerServiceURL = new AssertionInfo(defaultAssertionConsumerService);
                return this;
            }

            assertionConsumerServiceURL = spssoDescriptor.getAssertionConsumerServices().stream()
                    .filter(acs -> IDP_BINDING_TYPES.contains(acs.getBinding()))
                    .findFirst()
                    .map(AssertionInfo::new)
                    .orElse(null);

            return this;
        }

        SPSSODescriptor getSpssoDescriptor(EntityDescriptor ed) {
            SPSSODescriptor spssoDescriptor =
                    ed.getSPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL);
            if (spssoDescriptor == null) {
                LOGGER.warn("Unable to find supported protocol in EntityDescriptor {}", ed.getEntityID());
            }
            return spssoDescriptor;
        }

        String extractCertificate(SPSSODescriptor spssoDescriptor, Predicate<KeyDescriptor> usageTypePredicate) {
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

    private static class AssertionInfo {
        private final String url;
        private final String binding;

        public AssertionInfo(AssertionConsumerService acs) {
            this.url = acs.getLocation();
            this.binding = acs.getBinding();
        }
    }
}