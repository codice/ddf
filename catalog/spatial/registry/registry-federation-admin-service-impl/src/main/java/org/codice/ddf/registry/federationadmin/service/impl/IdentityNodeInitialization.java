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
package org.codice.ddf.registry.federationadmin.service.impl;

import static org.codice.ddf.registry.schemabindings.EbrimConstants.RIM_FACTORY;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import java.io.File;
import java.io.IOException;
import java.security.PrivilegedActionException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.VersionInfoType;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.utils.properties.Properties;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminService;
import org.codice.ddf.registry.schemabindings.helper.InternationalStringTypeHelper;
import org.codice.ddf.registry.schemabindings.helper.MetacardMarshaller;
import org.codice.ddf.registry.schemabindings.helper.RegistryPackageTypeHelper;
import org.codice.ddf.registry.schemabindings.helper.SlotTypeHelper;
import org.codice.ddf.security.common.Security;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a registry identity node when DDF Registry is first instantiated. If a previous registry
 * node is not already found, then a registry identity metacard and its attributes are created.
 * FederationAdminService adds the newly created identity metacard as a registry entry.
 */
public class IdentityNodeInitialization {

  private static final Logger LOGGER = LoggerFactory.getLogger(IdentityNodeInitialization.class);

  private static final String UNKNOWN_SITE_NAME = "Unknown Site Name";

  private static final String DATE_TIME =
      CswConstants.XML_SCHEMA_NAMESPACE_PREFIX.concat(":dateTime");

  private static final long DEFAULT_RETRY_INTERVAL_SECONDS = 30;

  private static final int SHUTDOWN_TIMEOUT_SECONDS = 60;

  private static final String KARAF_ETC = "karaf.etc";

  private static final String SYSTEM_PROPERTIES_FILE = "custom.system.properties";

  private FederationAdminService federationAdminService;

  private InputTransformer registryTransformer;

  private MetacardMarshaller metacardMarshaller;

  private SlotTypeHelper slotTypeHelper = new SlotTypeHelper();

  private InternationalStringTypeHelper internationalStringTypeHelper =
      new InternationalStringTypeHelper();

  private long retryInterval = DEFAULT_RETRY_INTERVAL_SECONDS;

  private final ScheduledExecutorService executor;

  public IdentityNodeInitialization(ScheduledExecutorService executor) {
    this(executor, DEFAULT_RETRY_INTERVAL_SECONDS);
  }

  public IdentityNodeInitialization(ScheduledExecutorService executor, long retryInterval) {
    this.retryInterval = retryInterval;
    this.executor = executor;
  }

  public void init() {
    // will keep checking for changes and shouldn't hold up anything
    executor.scheduleAtFixedRate(this::updateOrCreateIdentity, 1, retryInterval, TimeUnit.SECONDS);
  }

  public void destroy() {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        executor.shutdownNow();
        if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
          LOGGER.error(
              "Identity node initialization thread didn't terminate. This thread will continue until a system restart.");
        }
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  boolean updateOrCreateIdentity() {
    try {
      Security security = Security.getInstance();

      security.runAsAdminWithException(
          () -> {
            Optional<Metacard> optional = federationAdminService.getLocalRegistryIdentityMetacard();
            if (optional.isPresent()) {
              Metacard metacard = optional.get();
              if (!metacard.getTitle().equals(SystemInfo.getSiteName())) {
                updateIdentityNodeName(metacard);
              }
            }
            if (!optional.isPresent()) {
              createIdentityNode();
            }
            return null;
          });
    } catch (PrivilegedActionException e) {
      LOGGER.debug("Error checking for local registry identity node. Will try again later");
      return false;
    }
    return true;
  }

  private void updateIdentityNodeName(Metacard metacard)
      throws FederationAdminException, ParserException {
    RegistryPackageType registryPackage =
        metacardMarshaller.getRegistryPackageFromMetacard(metacard);

    String siteName = SystemInfo.getSiteName();
    RegistryPackageTypeHelper registryPackageTypeHelper = new RegistryPackageTypeHelper();

    registryPackageTypeHelper
        .getExtrinsicObjects(registryPackage.getRegistryObjectList())
        .stream()
        .filter(
            extrinsicObjectType ->
                extrinsicObjectType
                    .getObjectType()
                    .equals(RegistryConstants.REGISTRY_NODE_OBJECT_TYPE))
        .findFirst()
        .ifPresent(
            extrinsicObjectType ->
                extrinsicObjectType.setName(internationalStringTypeHelper.create(siteName)));
    Metacard identityMetacard = getRegistryMetacardFromRegistryPackage(registryPackage);
    if (identityMetacard != null) {
      copyTransientAttributes(identityMetacard, metacard);
      federationAdminService.updateRegistryEntry(identityMetacard);
    }
  }

  private void copyTransientAttributes(Metacard updateMetacard, Metacard existingMetacard) {
    RegistryObjectMetacardType.TRANSIENT_ATTRIBUTES
        .stream()
        .filter(
            key ->
                updateMetacard.getAttribute(key) == null
                    && existingMetacard.getAttribute(key) != null)
        .forEach(key -> updateMetacard.setAttribute(existingMetacard.getAttribute(key)));
    // Set the id to protect against a timing issue sometimes seen with framework plugins
    updateMetacard.setAttribute(existingMetacard.getAttribute(Core.ID));
  }

  private void createIdentityNode() throws FederationAdminException {
    String registryPackageId = System.getProperty(RegistryConstants.REGISTRY_ID_PROPERTY);
    if (StringUtils.isEmpty(registryPackageId)) {
      registryPackageId =
          RegistryConstants.GUID_PREFIX + UUID.randomUUID().toString().replaceAll("-", "");
      setSystemRegistryId(registryPackageId);
    }
    LOGGER.info(
        "Creating registry identity node: {} {}", SystemInfo.getSiteName(), registryPackageId);
    RegistryPackageType registryPackage = RIM_FACTORY.createRegistryPackageType();
    registryPackage.setId(registryPackageId);
    registryPackage.setObjectType(RegistryConstants.REGISTRY_NODE_OBJECT_TYPE);

    ExtrinsicObjectType extrinsicObject = RIM_FACTORY.createExtrinsicObjectType();
    extrinsicObject.setObjectType(RegistryConstants.REGISTRY_NODE_OBJECT_TYPE);

    String extrinsicObjectId =
        RegistryConstants.GUID_PREFIX + UUID.randomUUID().toString().replaceAll("-", "");
    extrinsicObject.setId(extrinsicObjectId);

    String siteName = SystemInfo.getSiteName();
    if (StringUtils.isNotBlank(siteName)) {
      extrinsicObject.setName(internationalStringTypeHelper.create(siteName));
    } else {
      extrinsicObject.setName(internationalStringTypeHelper.create(UNKNOWN_SITE_NAME));
    }

    String home = SystemBaseUrl.EXTERNAL.getBaseUrl();
    extrinsicObject.setHome(home);

    String version = SystemInfo.getVersion();
    if (StringUtils.isNotBlank(version)) {
      VersionInfoType versionInfo = RIM_FACTORY.createVersionInfoType();
      versionInfo.setVersionName(version);

      extrinsicObject.setVersionInfo(versionInfo);
    }

    OffsetDateTime now = OffsetDateTime.now(ZoneId.of(ZoneOffset.UTC.toString()));
    String rightNow = now.toString();

    SlotType1 lastUpdated =
        slotTypeHelper.create(RegistryConstants.XML_LAST_UPDATED_NAME, rightNow, DATE_TIME);
    extrinsicObject.getSlot().add(lastUpdated);

    SlotType1 liveDate =
        slotTypeHelper.create(RegistryConstants.XML_LIVE_DATE_NAME, rightNow, DATE_TIME);
    extrinsicObject.getSlot().add(liveDate);

    if (registryPackage.getRegistryObjectList() == null) {
      registryPackage.setRegistryObjectList(RIM_FACTORY.createRegistryObjectListType());
    }

    registryPackage
        .getRegistryObjectList()
        .getIdentifiable()
        .add(RIM_FACTORY.createIdentifiable(extrinsicObject));

    Metacard identityMetacard = getRegistryMetacardFromRegistryPackage(registryPackage);
    if (identityMetacard != null) {
      identityMetacard.setAttribute(
          new AttributeImpl(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, true));
      identityMetacard.setAttribute(
          new AttributeImpl(RegistryObjectMetacardType.REGISTRY_LOCAL_NODE, true));
      federationAdminService.addRegistryEntry(identityMetacard);
    }
  }

  private void setSystemRegistryId(String regId) {
    String etcDir = System.getProperty(KARAF_ETC);
    String systemPropertyFilename = etcDir + File.separator + SYSTEM_PROPERTIES_FILE;

    File systemPropertiesFile = new File(systemPropertyFilename);
    try {
      Properties systemProperties = new Properties(systemPropertiesFile);
      systemProperties.put(RegistryConstants.REGISTRY_ID_PROPERTY, regId);
      System.setProperty(RegistryConstants.REGISTRY_ID_PROPERTY, regId);
      systemProperties.save();
    } catch (IOException e) {
      LOGGER.warn("Exception while writing registry-id to system.properties file.", e);
    }
  }

  private Metacard getRegistryMetacardFromRegistryPackage(RegistryPackageType registryPackage)
      throws FederationAdminException {
    Metacard metacard;
    try {
      metacard =
          registryTransformer.transform(
              metacardMarshaller.getRegistryPackageAsInputStream(registryPackage));

    } catch (IOException | CatalogTransformerException | ParserException e) {
      String message = "Error creating metacard from registry package.";
      LOGGER.debug("{} Registry id: {}", message, registryPackage.getId());
      throw new FederationAdminException(message, e);
    }

    return metacard;
  }

  public void setMetacardMarshaller(MetacardMarshaller helper) {
    this.metacardMarshaller = helper;
  }

  public void setRegistryTransformer(InputTransformer inputTransformer) {
    this.registryTransformer = inputTransformer;
  }

  public void setFederationAdminService(FederationAdminService federationAdminService) {
    this.federationAdminService = federationAdminService;
  }
}
