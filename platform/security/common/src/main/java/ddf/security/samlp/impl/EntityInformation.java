/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.security.samlp.impl;

import static java.util.Objects.nonNull;

import com.google.common.collect.ImmutableSet;
import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SamlProtocol.Binding;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.lang.StringUtils;
import org.opensaml.core.xml.schema.XSBase64Binary;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.opensaml.security.credential.UsageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
public class EntityInformation {
  private static final Logger LOGGER = LoggerFactory.getLogger(EntityInformation.class);

  private final String signingCertificate;

  private final String encryptionCertificate;

  private final ServiceInfo defaultAssertionConsumerService;

  private final Map<Binding, ServiceInfo> assertionConsumerServices;

  private final Map<Binding, ServiceInfo> logoutServices;

  private final Set<Binding> supportedBindings;

  protected static final Binding PREFERRED_BINDING = Binding.HTTP_POST;

  private static final ImmutableSet<Binding> IDP_SUPPORTED_BINDINGS =
      ImmutableSet.of(Binding.HTTP_POST, Binding.HTTP_REDIRECT, Binding.SOAP);

  private static final ImmutableSet<Binding> SSO_RESPONSE_BINDINGS =
      ImmutableSet.of(Binding.HTTP_POST, Binding.HTTP_ARTIFACT);

  private EntityInformation(Builder builder) {
    signingCertificate = builder.signingCertificate;
    encryptionCertificate = builder.encryptionCertificate;
    defaultAssertionConsumerService = builder.defaultAssertionConsumerService;
    assertionConsumerServices = builder.assertionConsumerServices;
    logoutServices = builder.logoutServices;
    supportedBindings = builder.supportedBindings;
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
      logoutServiceInfo = logoutServices.values().stream().findFirst().orElse(null);
    }

    return logoutServiceInfo;
  }

  Binding getBinding(AuthnRequest request, Binding preferred) {
    if (request != null
        && request.getProtocolBinding() != null
        && supportedBindings.contains(Binding.from(request.getProtocolBinding()))) {
      return Binding.from(request.getProtocolBinding());
    }

    return preferred != null ? preferred : PREFERRED_BINDING;
  }

  public ServiceInfo getAssertionConsumerService(
      AuthnRequest request, Binding preferred, Integer index) {
    ServiceInfo si = null;

    if (request != null && request.getAssertionConsumerServiceURL() != null) {
      si = getAssertionConsumerServiceInfoByUrl(request);

      if (si != null) {
        return si;
      }
    }

    if (index != null) {
      si = getAssertionConsumerServiceInfoByIndex(index);

      if (si != null) {
        return si;
      }
    }

    if (request != null && request.getProtocolBinding() != null) {
      si = getAssertionConsumerServiceInfoByProtocolBinding(request);

      if (si != null) {
        return si;
      }
    }

    Binding binding = preferred != null ? preferred : PREFERRED_BINDING;
    si = assertionConsumerServices.get(binding);
    if (si != null) {
      return si;
    }

    if (defaultAssertionConsumerService != null) {
      return defaultAssertionConsumerService;
    }

    return assertionConsumerServices.values().stream().findFirst().orElse(null);
  }

  private ServiceInfo getAssertionConsumerServiceInfoByUrl(AuthnRequest request) {
    return getACS(s -> request.getAssertionConsumerServiceURL().equals(s.getUrl()));
  }

  private ServiceInfo getAssertionConsumerServiceInfoByIndex(Integer index) {
    return getACS(s -> index.equals(s.getIndex()));
  }

  private ServiceInfo getAssertionConsumerServiceInfoByProtocolBinding(AuthnRequest request) {
    return getACS(s -> Binding.from(request.getProtocolBinding()).equals(s.getBinding()));
  }

  private ServiceInfo getACS(Predicate<ServiceInfo> predicate) {
    return assertionConsumerServices
        .values()
        .stream()
        .filter(predicate)
        .filter(serviceInfo -> supportedBindings.contains(serviceInfo.getBinding()))
        .filter(serviceInfo -> IDP_SUPPORTED_BINDINGS.contains(serviceInfo.getBinding()))
        .filter(serviceInfo -> SSO_RESPONSE_BINDINGS.contains(serviceInfo.getBinding()))
        .findFirst()
        .orElse(null);
  }

  public static class Builder {
    private static final ImmutableSet<UsageType> SIGNING_TYPES =
        ImmutableSet.of(UsageType.UNSPECIFIED, UsageType.SIGNING);

    private final SPSSODescriptor spssoDescriptor;

    private final Set<Binding> supportedBindings;

    private String signingCertificate;

    private String encryptionCertificate;

    private ServiceInfo defaultAssertionConsumerService;

    private final Map<Binding, ServiceInfo> assertionConsumerServices =
        new EnumMap<>(Binding.class);

    private final Map<Binding, ServiceInfo> logoutServices = new EnumMap<>(Binding.class);

    public Builder(EntityDescriptor ed, Set<Binding> supportedBindings) {
      spssoDescriptor = getSpssoDescriptor(ed);
      this.supportedBindings = supportedBindings;
    }

    public EntityInformation build() {
      if (spssoDescriptor == null) {
        LOGGER.debug("Unable to build EntityInformation without a descriptor");
        return null;
      }
      return new EntityInformation(
          parseSigningCertificate()
              .parseEncryptionCertificate()
              .parseAssertionConsumerServiceInfo()
              .parseLogoutServices());
    }

    SPSSODescriptor getSpssoDescriptor(EntityDescriptor ed) {
      SPSSODescriptor supportedSpssoDescriptor =
          ed.getSPSSODescriptor(SamlProtocol.SUPPORTED_PROTOCOL);
      if (supportedSpssoDescriptor == null) {
        LOGGER.debug("Unable to find supported protocol in EntityDescriptor {}", ed.getEntityID());
      }
      return supportedSpssoDescriptor;
    }

    Builder parseSigningCertificate() {
      signingCertificate =
          extractCertificate(spssoDescriptor, kd -> SIGNING_TYPES.contains(kd.getUse()));
      return this;
    }

    Builder parseEncryptionCertificate() {
      encryptionCertificate =
          extractCertificate(spssoDescriptor, kd -> UsageType.ENCRYPTION.equals(kd.getUse()));
      return this;
    }

    Builder parseAssertionConsumerServiceInfo() {
      AssertionConsumerService defaultACS = spssoDescriptor.getDefaultAssertionConsumerService();
      // see if the default service uses our supported bindings, and then use that
      // as we add more bindings, we'll need to update this
      if (defaultACS != null && supportedBindings.contains(Binding.from(defaultACS.getBinding()))) {
        LOGGER.debug(
            "Using AssertionConsumerServiceURL from default assertion consumer service: {}",
            defaultACS.getLocation());
        defaultAssertionConsumerService =
            new ServiceInfo(
                defaultACS.getLocation(),
                Binding.from(defaultACS.getBinding()),
                defaultACS.getIndex());
      }

      putAllSupported(
          assertionConsumerServices, spssoDescriptor.getAssertionConsumerServices(), null);
      return this;
    }

    Builder parseLogoutServices() {
      putAllSupported(logoutServices, null, spssoDescriptor.getSingleLogoutServices());
      return this;
    }

    void putAllSupported(
        Map<Binding, ServiceInfo> target,
        List<AssertionConsumerService> acServices,
        List<SingleLogoutService> slServices) {
      for (Binding binding : supportedBindings) {
        ServiceInfo serviceInfo = null;
        if (acServices != null) {
          serviceInfo =
              parseAssertionConsumerServiceInfo(acServices, e -> binding.isEqual(e.getBinding()));
        } else if (slServices != null) {
          serviceInfo =
              parseSingleLogoutServiceInfo(slServices, e -> binding.isEqual(e.getBinding()));
        }
        if (serviceInfo != null && serviceInfo.url != null) {
          target.put(binding, serviceInfo);
        }
      }
    }

    ServiceInfo parseAssertionConsumerServiceInfo(
        List<AssertionConsumerService> services, Predicate<Endpoint> bindingFilter) {
      return services
          .stream()
          .filter(bindingFilter)
          .findFirst()
          .map(
              si -> new ServiceInfo(si.getLocation(), Binding.from(si.getBinding()), si.getIndex()))
          .orElse(new ServiceInfo(null, null, null));
    }

    ServiceInfo parseSingleLogoutServiceInfo(
        List<SingleLogoutService> services, Predicate<Endpoint> bindingFilter) {
      return services
          .stream()
          .filter(bindingFilter)
          .findFirst()
          .map(si -> new ServiceInfo(si.getLocation(), Binding.from(si.getBinding()), null))
          .orElse(new ServiceInfo(null, null, null));
    }

    String extractCertificate(
        SPSSODescriptor spssoDescriptor, Predicate<KeyDescriptor> usageTypePredicate) {
      return spssoDescriptor
          .getKeyDescriptors()
          .stream()
          .filter(Objects::nonNull)
          .filter(kd -> nonNull(kd.getUse()))
          .filter(usageTypePredicate)
          .filter(kd -> nonNull(extractCertificateFromKeyDescriptor(kd)))
          .reduce((acc, val) -> val.getUse().equals(UsageType.SIGNING) || acc == null ? val : acc)
          .map(this::extractCertificateFromKeyDescriptor)
          .orElse(null);
    }

    String extractCertificateFromKeyDescriptor(KeyDescriptor kd) {
      return kd.getKeyInfo()
          .getX509Datas()
          .stream()
          .flatMap(datas -> datas.getX509Certificates().stream())
          .map(XSBase64Binary::getValue)
          .filter(StringUtils::isNotBlank)
          .findFirst()
          .orElse(null);
    }
  }

  public static class ServiceInfo {
    private final String url;

    private final Binding binding;

    private final Integer index;

    ServiceInfo(String url, Binding binding, Integer index) {
      this.url = url;
      this.binding = binding;
      this.index = index;
    }

    public Integer getIndex() {
      return index;
    }

    public String getUrl() {
      return url;
    }

    public Binding getBinding() {
      return binding;
    }
  }
}
