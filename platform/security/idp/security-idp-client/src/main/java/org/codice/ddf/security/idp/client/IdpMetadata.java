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
package org.codice.ddf.security.idp.client;

import ddf.security.samlp.MetadataConfigurationParser;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang.StringUtils;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.security.credential.UsageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdpMetadata {
  private static final Logger LOGGER = LoggerFactory.getLogger(IdpMetadata.class);

  private static final String SAML_2_0_PROTOCOL = "urn:oasis:names:tc:SAML:2.0:protocol";

  private static final String BINDINGS_HTTP_POST = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";

  private static final String BINDINGS_HTTP_REDIRECT =
      "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect";

  private String singleSignOnLocation;

  private String singleSignOnBinding;

  private String signingCertificate;

  private String encryptionCertificate;

  private String metadata;

  private Instant created;

  private Instant validUntil;

  private Duration cacheDuration;

  private AtomicReference<Map<String, EntityDescriptor>> entryDescriptions =
      new AtomicReference<>();

  private String singleLogoutBinding;

  private String singleLogoutLocation;

  public void setMetadata(String metadata) {
    this.metadata = metadata;
    eraseEntityDescriptions();
  }

  private void eraseEntityDescriptions() {
    entryDescriptions.getAndSet(null);
    cacheDuration = null;
    validUntil = null;
  }

  private void initSingleSignOn() {
    IDPSSODescriptor descriptor = getDescriptor();
    if (descriptor != null) {
      // Prefer HTTP-Redirect over HTTP-POST if both are present
      Optional<? extends Endpoint> service =
          initSingleSomething(descriptor.getSingleSignOnServices());

      if (service.isPresent()) {
        singleSignOnBinding = service.get().getBinding();
        singleSignOnLocation = service.get().getLocation();
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
        singleLogoutBinding = service.get().getBinding();
        singleLogoutLocation = service.get().getLocation();
      }
    }
  }

  private Optional<? extends Endpoint> initSingleSomething(List<? extends Endpoint> endpoints) {
    IDPSSODescriptor descriptor = getDescriptor();
    if (descriptor == null) {
      return Optional.empty();
    }

    // Prefer HTTP-Redirect over HTTP-POST if both are present
    return endpoints
        .stream()
        .filter(Objects::nonNull)
        .filter(s -> Objects.nonNull(s.getBinding()))
        .filter(
            s ->
                s.getBinding().equals(BINDINGS_HTTP_POST)
                    || s.getBinding().equals(BINDINGS_HTTP_REDIRECT))
        .reduce(
            (acc, val) -> {
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
        if (key.getKeyInfo().getX509Datas().size() > 0
            && key.getKeyInfo().getX509Datas().get(0).getX509Certificates().size() > 0) {
          certificate =
              key.getKeyInfo().getX509Datas().get(0).getX509Certificates().get(0).getValue();
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

  /**
   * If the metadata is past its validity date, the SAML standard prohibits the metadata data from
   * being used. The SAML entity is cleared if the metadata is invalid. This forces the class to
   * attempt to retrieve the SAML entity from its source. If metadata is expired, it can continue to
   * be used, but class attempts to get a new copy of it from the source. If successful, it clears
   * the entity cache. If the entity cache is empty, the class attempts to retrieve it from the
   * source.
   *
   * @return The root SAML entity descriptor or null
   */
  EntityDescriptor getEntityDescriptor() {
    Map<String, EntityDescriptor> parsedMetadata = null;

    if (!isMetadataValid()) {
      LOGGER.debug("SSO metadata is invalid. Purging metadata cache.");
      eraseEntityDescriptions();
    }

    if (isMetadataExpired()) {
      LOGGER.debug("SSO metadata cache is expired. Attempting to retrieve metadata from source.");
      try {
        parsedMetadata = parseMetadata();
        eraseEntityDescriptions();
      } catch (IOException e) {
        LOGGER.debug("Error parsing SSO metadata", e);
      }
    }

    Map<String, EntityDescriptor> edMap = entryDescriptions.get();
    if (edMap == null) {
      try {
        // Use the metadata if it was collected earlier in the method.
        if (parsedMetadata == null) {
          parsedMetadata = parseMetadata();
        }
        edMap = parsedMetadata;
      } catch (IOException e) {
        LOGGER.debug("Error parsing SSO metadata", e);
        return null;
      }

      boolean updated = entryDescriptions.compareAndSet(null, edMap);
      if (!updated) {
        LOGGER.debug("Safe but concurrent update to serviceProviders map; using processed value");
      } else {
        setCacheInformation(extractRootEntityFromMap(edMap));
      }
    }

    return extractRootEntityFromMap(edMap);
  }

  private void setCacheInformation(EntityDescriptor entityDescriptor) {
    if (entityDescriptor != null) {
      created = Instant.now();
      if (entityDescriptor.getCacheDuration() != null) {
        cacheDuration = Duration.ofMillis(entityDescriptor.getCacheDuration());
      }
      if (entityDescriptor.getValidUntil() != null) {
        validUntil = entityDescriptor.getValidUntil().toDate().toInstant();
      }
    }
  }

  /**
   * Return true if this the cache metadata is expired and should be retrieved from the SAML entity
   * that provides the metadata. From the SAML standard: "Note that cache expiration does not imply
   * a lack of validity in the absence of a validUntil attribute or other information; failure to
   * update a cached instance (e.g., due to network failure) need not render metadata invalid..."
   * Because cacheDuration is optional (per the standard), the absence of a cache duration implies
   * indefinite expiration.
   *
   * @return true if metadata should be reacquired from the IDP based solely on cacheDuration
   */
  public boolean isMetadataExpired() {
    return !(cacheDuration == null || Instant.now().isAfter(created.plus(cacheDuration)));
  }

  /**
   * Return true if the metadata may still be used. Invalid data must NOT be used, per the SAML
   * standard. The SAML standard does not give explicit instruction on what to do if the validUntil
   * is not specified, but implies The attribute is optional. That implies that the absence of
   * valdiUntil means indefinite validity: "validUntil ...optional attribute indicates the
   * expiration time of the metadata contained in the element and any contained elements." However,
   * the standard also sates "When used as the root element of a metadata instance, this element
   * MUST contain either a validUntil or cacheDuration attribute."
   *
   * @return true if metadata may still be used
   */
  public boolean isMetadataValid() {
    return validUntil == null ? cacheDuration != null : Instant.now().isBefore(validUntil);
  }

  private EntityDescriptor extractRootEntityFromMap(Map<String, EntityDescriptor> edMap) {
    Set<Map.Entry<String, EntityDescriptor>> entries = edMap.entrySet();
    if (!entries.isEmpty()) {
      return entries.iterator().next().getValue();
    }
    return null;
  }

  private Map<String, EntityDescriptor> parseMetadata() throws IOException {
    final Map<String, EntityDescriptor> processMap = new ConcurrentHashMap<>();
    MetadataConfigurationParser metadataConfigurationParser = //
        new MetadataConfigurationParser(
            Collections.singletonList(metadata), ed -> processMap.put(ed.getEntityID(), ed));
    processMap.putAll(metadataConfigurationParser.getEntryDescriptions());
    return processMap;
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
