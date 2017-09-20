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
package org.codice.ddf.registry.source.configuration;

import ddf.catalog.data.Metacard;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ServiceBindingType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryUtility;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminService;
import org.codice.ddf.registry.federationadmin.service.internal.RegistrySourceConfiguration;
import org.codice.ddf.registry.schemabindings.helper.MetacardMarshaller;
import org.codice.ddf.registry.schemabindings.helper.RegistryPackageTypeHelper;
import org.codice.ddf.registry.schemabindings.helper.SlotTypeHelper;
import org.codice.ddf.security.common.Security;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This handler class is responsible for the create/update/delete of source configurations that come
 * from registry nodes. It listens to the create/update/delete events to trigger its logic.
 */
@SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
public class SourceConfigurationHandler implements EventHandler, RegistrySourceConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(SourceConfigurationHandler.class);

  private static final String METACARD_PROPERTY = "ddf.catalog.event.metacard";

  private static final String BINDING_TYPE = "bindingType";

  private static final String DISABLED_CONFIGURATION_SUFFIX = "_disabled";

  private static final String ID = "id";

  private static final String SHORTNAME = "shortname";

  private static final String CONFIGURATION_FILTER =
      "(&(%s=%s)(|(service.factoryPid=*source*)(service.factoryPid=*Source*)(service.factoryPid=*service*)(service.factoryPid=*Service*)))";

  private static final String CREATED_TOPIC = "ddf/catalog/event/CREATED";

  private static final String UPDATED_TOPIC = "ddf/catalog/event/UPDATED";

  private static final String DELETED_TOPIC = "ddf/catalog/event/DELETED";

  private static final int SHUTDOWN_TIMEOUT_SECONDS = 60;

  private ConfigurationAdmin configurationAdmin;

  private FederationAdminService federationAdminService;

  private MetaTypeService metaTypeService;

  private MetacardMarshaller metacardMarshaller;

  private ExecutorService executor;

  private String urlBindingName;

  private Map<String, String> bindingTypeToFactoryPidMap = new ConcurrentHashMap<>();

  private List<String> sourceActivationPriorityOrder = new CopyOnWriteArrayList<>();

  private boolean activateConfigurations;

  private boolean preserveActiveConfigurations;

  private boolean cleanUpOnDelete;

  private SlotTypeHelper slotHelper;

  private RegistryPackageTypeHelper registryTypeHelper;

  public SourceConfigurationHandler(
      FederationAdminService federationAdminService, ExecutorService executor) {
    this.federationAdminService = federationAdminService;
    this.executor = executor;
  }

  public void destroy() {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        executor.shutdownNow();
        if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
          LOGGER.debug("Thread pool didn't terminate");
        }
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
    }
  }

  @Override
  public void handleEvent(Event event) {
    Metacard mcard = (Metacard) event.getProperty(METACARD_PROPERTY);
    if (mcard == null
        || !RegistryUtility.isRegistryMetacard(mcard)
        || RegistryUtility.isIdentityNode(mcard)) {
      return;
    }

    executor.execute(
        () -> {
          if (event.getTopic().equals(CREATED_TOPIC)) {
            processCreate(mcard);
          } else if (event.getTopic().equals(UPDATED_TOPIC)) {
            processUpdate(mcard);
          } else if (event.getTopic().equals(DELETED_TOPIC)) {
            processDelete(mcard);
          }
        });
  }

  private void processCreate(Metacard metacard) {
    try {
      updateRegistryConfigurations(metacard, true);
    } catch (IOException | InvalidSyntaxException | ParserException | RuntimeException e) {
      LOGGER.info(
          "Unable to update registry source configurations for {}. Sources maybe out of sync with registry entry",
          metacard.getTitle());
    }
  }

  private void processUpdate(Metacard metacard) {
    try {
      updateRegistryConfigurations(metacard, false);
    } catch (IOException | InvalidSyntaxException | ParserException | RuntimeException e) {
      LOGGER.info(
          "Unable to update registry source configurations for {}. Sources maybe out of sync with registry entry",
          metacard.getTitle());
    }
  }

  private void processDelete(Metacard metacard) {
    try {
      if (cleanUpOnDelete) {
        deleteRegistryConfigurations(metacard);
      }
    } catch (IOException | InvalidSyntaxException e) {
      LOGGER.info("Unable to delete registry source configurations for {}", metacard.getTitle());
    }
  }

  /**
   * Finds all source configurations associated with the registry metacard creates/updates them with
   * the information in the metacards service bindings. This method will enable/disable
   * configurations if the right conditions are met but will never delete a configuration other than
   * for switching a configuration from enabled to disabled or vice-versa
   *
   * @param metacard Registry metacard with new service binding info
   * @param activationHint Flag indicating if the created/updated configuration should be activated.
   *     Configuration activation is not determined solely on this field but in conjunction with
   *     {@see setActivateConfigurations} and {@see setPreserveActiveConfigurations}.
   * @throws IOException
   * @throws InvalidSyntaxException
   * @throws ParserException
   */
  private synchronized void updateRegistryConfigurations(Metacard metacard, boolean activationHint)
      throws IOException, InvalidSyntaxException, ParserException {
    if (RegistryUtility.isIdentityNode(metacard)) {
      return;
    }

    boolean autoActivateConfigurations =
        activateConfigurations && (activationHint || !preserveActiveConfigurations);

    List<ServiceBindingType> bindingTypes =
        registryTypeHelper.getBindingTypes(
            metacardMarshaller.getRegistryPackageFromMetacard(metacard));

    String registryId = RegistryUtility.getRegistryId(metacard);

    String configId = getDeconflictedConfigId(metacard.getTitle(), registryId);

    Map<String, Configuration> fpidToConfigurationMap = getCurrentConfigurations(registryId);

    String bindingTypeToActivate = "";

    if (autoActivateConfigurations) {
      bindingTypeToActivate = getBindingTypeToActivate(bindingTypes);
      if (StringUtils.isNotEmpty(bindingTypeToActivate)) {
        String fPidToActivate = bindingTypeToFactoryPidMap.get(bindingTypeToActivate);
        activateDeactivateExistingConfiguration(
            fPidToActivate, fpidToConfigurationMap, bindingTypeToActivate);
      }
    }

    for (ServiceBindingType bindingType : bindingTypes) {
      Map<String, Object> slotMap = this.getServiceBindingProperties(bindingType);

      String factoryPidMask = (String) slotMap.get(BINDING_TYPE);
      if (factoryPidMask == null) {
        continue;
      }
      String factoryPid = bindingTypeToFactoryPidMap.get(factoryPidMask);

      if (StringUtils.isBlank(factoryPid)) {
        continue;
      }
      Configuration curConfig =
          findOrCreateConfig(
              factoryPid,
              fpidToConfigurationMap,
              factoryPidMask,
              (autoActivateConfigurations && factoryPidMask.equals(bindingTypeToActivate)));

      Hashtable<String, Object> serviceConfigurationProperties = new Hashtable<>();
      if (fpidToConfigurationMap.containsKey(
          curConfig.getFactoryPid().concat(getConfigStringProperty(curConfig, BINDING_TYPE)))) {
        serviceConfigurationProperties.putAll(
            getConfigurationsFromDictionary(curConfig.getProperties()));
      } else {
        serviceConfigurationProperties.putAll(getMetatypeDefaults(factoryPid));
      }

      serviceConfigurationProperties.putAll(slotMap);
      serviceConfigurationProperties.put(ID, configId);
      serviceConfigurationProperties.put(SHORTNAME, configId);
      serviceConfigurationProperties.put(
          RegistryConstants.CONFIGURATION_REGISTRY_ID_PROPERTY, registryId);

      curConfig.update(serviceConfigurationProperties);
      LOGGER.debug("Updating source configuration {} with registry-id {}", configId, registryId);
      fpidToConfigurationMap.remove(curConfig.getFactoryPid().concat(factoryPidMask));
    }

    //if a binding was removed the configuration could still exist, If the registry
    //entry gets renamed the non-binding configs will not get renamed appropriately so we
    //go through them here and make sure the name is set correctly.
    fpidToConfigurationMap
        .values()
        .stream()
        .forEach(
            config -> {
              try {
                Dictionary<String, Object> properties = config.getProperties();
                properties.put(ID, configId);
                properties.put(SHORTNAME, configId);
                config.update(properties);
              } catch (IOException e) {
                LOGGER.debug(
                    "Could not remove configuration for {}:{}",
                    config.getProperties().get(ID),
                    config.getFactoryPid());
              }
            });
  }

  /**
   * Finds a configuration in the map of current configurations or creates a new one if one doesn't
   * exist yet.
   *
   * @param factoryPid The factory pid to lookup/create
   * @param fpidToConfigurationMap Map containing current configurations
   * @param createActiveConfig If a configuration needs to be created, indicates if it should be
   *     created as an active or disable configuration.
   * @return
   * @throws IOException
   */
  private Configuration findOrCreateConfig(
      String factoryPid,
      Map<String, Configuration> fpidToConfigurationMap,
      String bindingType,
      boolean createActiveConfig)
      throws IOException {
    String factoryPidDisabled = factoryPid.concat(DISABLED_CONFIGURATION_SUFFIX);
    String factoryPidDisabledWithBinding = factoryPidDisabled.concat(bindingType);
    String factoryPidWithBinding = factoryPid.concat(bindingType);
    Configuration curConfig = fpidToConfigurationMap.get(factoryPidWithBinding);
    if (curConfig == null) {
      curConfig = fpidToConfigurationMap.get(factoryPidDisabledWithBinding);
    }
    if (curConfig == null) {
      String pid = factoryPidDisabled;
      if (createActiveConfig) {
        pid = factoryPid;
      }
      curConfig = configurationAdmin.createFactoryConfiguration(pid, null);
    }
    return curConfig;
  }

  /**
   * Activates an existing configuration if it isn't active and has the fpid matching the
   * fPidToActivate. If there is an acticve configuration that isn't the fPidToActivate it will
   * deactivate it.
   *
   * @param fPidToActivate Factory PID that should be active
   * @param fpidToConfigurationMap Map of existing fpid -> configuration mappings
   * @throws IOException
   */
  private void activateDeactivateExistingConfiguration(
      String fPidToActivate, Map<String, Configuration> fpidToConfigurationMap, String bindingType)
      throws IOException {
    String fpid;
    String fpidToActivateWithBinding = fPidToActivate.concat(bindingType);
    String disabledFpidToActivateWithBinding =
        fPidToActivate.concat(DISABLED_CONFIGURATION_SUFFIX).concat(bindingType);
    Configuration configToEnable = null;
    Configuration configToDisable = null;
    for (Map.Entry<String, Configuration> entry : fpidToConfigurationMap.entrySet()) {
      fpid = entry.getKey();
      if (!fpid.contains(DISABLED_CONFIGURATION_SUFFIX)
          && !fpid.equals(fpidToActivateWithBinding)) {
        configToDisable = entry.getValue();
      } else if (fpid.equals(disabledFpidToActivateWithBinding)) {
        configToEnable = entry.getValue();
      }
      if (configToDisable != null && configToEnable != null) {
        break;
      }
    }
    //Order of disable/enable important. Can't have two enabled configurations with the same
    //id so if there is one to disable, disable it before enabling another one.
    if (configToDisable != null) {
      fpidToConfigurationMap.remove(
          configToDisable
              .getFactoryPid()
              .concat(getConfigStringProperty(configToDisable, BINDING_TYPE)));
      Configuration config = toggleConfiguration(configToDisable);
      fpidToConfigurationMap.put(
          config.getFactoryPid().concat(getConfigStringProperty(config, BINDING_TYPE)), config);
    }
    if (configToEnable != null) {
      fpidToConfigurationMap.remove(
          configToEnable
              .getFactoryPid()
              .concat(getConfigStringProperty(configToEnable, BINDING_TYPE)));
      Configuration config = toggleConfiguration(configToEnable);
      fpidToConfigurationMap.put(
          config.getFactoryPid().concat(getConfigStringProperty(config, BINDING_TYPE)), config);
    }
  }

  private void deleteRegistryConfigurations(Metacard metacard)
      throws IOException, InvalidSyntaxException {
    String registryId = RegistryUtility.getRegistryId(metacard);
    if (registryId == null) {
      return;
    }

    Configuration[] configurations =
        configurationAdmin.listConfigurations(
            String.format(
                CONFIGURATION_FILTER,
                RegistryConstants.CONFIGURATION_REGISTRY_ID_PROPERTY,
                registryId));
    if (configurations != null && configurations.length > 0) {

      LOGGER.debug(
          "Deleting {} source configurations for registry-id {}",
          configurations.length,
          registryId);

      String sourceName = (String) configurations[0].getProperties().get(ID);
      configurations =
          configurationAdmin.listConfigurations(
              String.format(CONFIGURATION_FILTER, ID, sourceName));
      for (Configuration configuration : configurations) {
        configuration.delete();
      }
    }
  }

  /**
   * Toggles a configuration between enabled and disabled.
   *
   * @param config The configuration to enable/disable
   * @return The new enabled/disabled configuration
   * @throws IOException
   */
  private Configuration toggleConfiguration(Configuration config) throws IOException {
    String newFpid;
    if (config.getFactoryPid().contains(DISABLED_CONFIGURATION_SUFFIX)) {
      newFpid = config.getFactoryPid().replace(DISABLED_CONFIGURATION_SUFFIX, "");

    } else {
      newFpid = config.getFactoryPid().concat(DISABLED_CONFIGURATION_SUFFIX);
    }
    Dictionary<String, Object> properties = config.getProperties();
    config.delete();
    Configuration newConfig = configurationAdmin.createFactoryConfiguration(newFpid, null);
    newConfig.update(properties);
    return newConfig;
  }

  /**
   * Returns the service binding slots as a map of string properties with the slot name as the key
   *
   * @param binding ServiceBindingType to generate the map from
   * @return A map of service binding slots
   */
  private Map<String, Object> getServiceBindingProperties(ServiceBindingType binding) {
    Map<String, Object> properties = new HashMap<>();
    for (SlotType1 slot : binding.getSlot()) {
      List<String> slotValues = slotHelper.getStringValues(slot);
      if (CollectionUtils.isEmpty(slotValues)) {
        continue;
      }
      properties.put(slot.getName(), slotValues.size() == 1 ? slotValues.get(0) : slotValues);
    }
    if (binding.isSetAccessURI() && properties.get(urlBindingName) != null) {
      properties.put(properties.get(urlBindingName).toString(), binding.getAccessURI());
    }
    return properties;
  }

  /**
   * Gets all the configurations that have a matching registry id
   *
   * @param registryId The registry id to match
   * @return A map of configurations with factory pids as keys
   * @throws IOException
   * @throws InvalidSyntaxException
   */
  private Map<String, Configuration> getCurrentConfigurations(String registryId)
      throws IOException, InvalidSyntaxException {
    Map<String, Configuration> configurationMap = new HashMap<>();
    Configuration[] configurations =
        configurationAdmin.listConfigurations(
            String.format(
                CONFIGURATION_FILTER,
                RegistryConstants.CONFIGURATION_REGISTRY_ID_PROPERTY,
                registryId));

    if (configurations == null) {
      LOGGER.debug("No sources found with registry-id {}", registryId);
      return configurationMap;
    }

    //we have found a set of source configurations with a matching registry-id
    //now we need to search again with the id of one of the matching configs because
    //all source configurations for an id might not all have a registry id if they
    //were manually added but we want to make sure we include them here.
    configurations =
        configurationAdmin.listConfigurations(
            String.format(CONFIGURATION_FILTER, "id", configurations[0].getProperties().get("id")));

    for (Configuration config : configurations) {
      configurationMap.put(
          config.getFactoryPid().concat(getConfigStringProperty(config, BINDING_TYPE)), config);
    }

    if (LOGGER.isDebugEnabled()) {

      StringBuilder buffer = new StringBuilder();
      buffer.append(System.lineSeparator());
      for (String fpid : configurationMap.keySet()) {
        buffer.append(String.format("%s%s", fpid, System.lineSeparator()));
      }
      LOGGER.debug("Configurations found for {}: {}", registryId, buffer.toString());
    }

    return configurationMap;
  }

  /**
   * Gets the deconflicted configuration id. Checks for current configurations with the same ID and
   * if present makes sure they belong to this registry entry. If configuration not present or
   * present with the same registryId then the id passed in will be returned. If configuration is
   * present and does not have the same registry id then return a new id that is a combination of
   * the id and registry id which is guaranteed to be unique.
   *
   * @param id Initial configuration ID
   * @param registryId Associated registry ID
   * @return A unique valid configuration ID
   * @throws IOException
   * @throws InvalidSyntaxException
   */
  private String getDeconflictedConfigId(String id, String registryId)
      throws IOException, InvalidSyntaxException {
    String configId = id;
    if (StringUtils.isEmpty(configId)) {
      configId = registryId;
    }

    Configuration[] configurations =
        configurationAdmin.listConfigurations(String.format("(id=%s)", configId));

    if (configurations == null) {
      return configId;
    }

    String configRegistryId = null;
    for (Configuration config : configurations) {
      configRegistryId =
          (String) config.getProperties().get(RegistryConstants.CONFIGURATION_REGISTRY_ID_PROPERTY);
      if (configRegistryId != null) {
        break;
      }
    }

    if (configRegistryId == null || !configRegistryId.equals(registryId)) {
      configId = String.format("%s - %s", configId, registryId);
      LOGGER.debug(
          "Source with id {} and registry-id {} already exists. Switching to modified id {}",
          id,
          configRegistryId,
          configId);
    }
    return configId;
  }

  private String getBindingTypeToActivate(List<ServiceBindingType> bindingTypes) {

    String bindingTypeToActivate = null;
    String topPriority = sourceActivationPriorityOrder.get(0);
    List<String> bindingTypesNames = new ArrayList<>();

    for (ServiceBindingType bindingType : bindingTypes) {
      Map<String, Object> slotMap = this.getServiceBindingProperties(bindingType);

      if (slotMap.get(BINDING_TYPE) == null) {
        continue;
      }

      String factoryPidMask = slotMap.get(BINDING_TYPE).toString();

      if (StringUtils.isNotBlank(factoryPidMask)) {
        if (factoryPidMask.equals(topPriority)) {
          return factoryPidMask;
        }

        bindingTypesNames.add(factoryPidMask);
      }
    }

    for (String prioritySource :
        sourceActivationPriorityOrder.subList(1, sourceActivationPriorityOrder.size())) {
      if (bindingTypesNames.contains(prioritySource)) {
        return prioritySource;
      }
    }

    return bindingTypeToActivate;
  }

  private Hashtable<String, Object> getConfigurationsFromDictionary(
      Dictionary<String, Object> properties) {
    Hashtable<String, Object> configProperties = new Hashtable<>();

    Enumeration<String> enumeration = properties.keys();
    while (enumeration.hasMoreElements()) {
      String key = enumeration.nextElement();
      configProperties.put(key, properties.get(key));
    }

    return configProperties;
  }

  private Map<String, Object> getMetatypeDefaults(String factoryPid) {
    Map<String, Object> properties = new HashMap<>();
    ObjectClassDefinition bundleMetatype = getObjectClassDefinition(factoryPid);
    if (bundleMetatype != null) {
      for (AttributeDefinition attributeDef :
          bundleMetatype.getAttributeDefinitions(ObjectClassDefinition.ALL)) {
        if (attributeDef.getID() != null) {
          if (attributeDef.getDefaultValue() != null) {
            if (attributeDef.getCardinality() == 0) {
              properties.put(
                  attributeDef.getID(),
                  getAttributeValue(attributeDef.getDefaultValue()[0], attributeDef.getType()));
            } else {
              properties.put(attributeDef.getID(), attributeDef.getDefaultValue());
            }
          } else if (attributeDef.getCardinality() != 0) {
            properties.put(attributeDef.getID(), new String[0]);
          }
        }
      }
    }

    return properties;
  }

  private String getConfigStringProperty(Configuration config, String property) {
    if (config == null || config.getProperties() == null || StringUtils.isBlank(property)) {
      return "";
    }

    String propertyValue = null;
    if (config.getProperties().get(property) instanceof String) {
      propertyValue = (String) config.getProperties().get(property);
    }

    if (propertyValue == null) {
      return "";
    }

    return propertyValue;
  }

  private ObjectClassDefinition getObjectClassDefinition(String pid) {
    Bundle[] bundles = this.getBundleContext().getBundles();
    for (int i = 0; i < bundles.length; i++) {
      try {
        ObjectClassDefinition ocd = this.getObjectClassDefinition(bundles[i], pid);
        if (ocd != null) {
          return ocd;
        }
      } catch (IllegalArgumentException iae) {
        // don't care
      }
    }
    return null;
  }

  private ObjectClassDefinition getObjectClassDefinition(Bundle bundle, String pid) {
    Locale locale = Locale.getDefault();
    if (bundle != null) {
      if (metaTypeService != null) {
        MetaTypeInformation mti = metaTypeService.getMetaTypeInformation(bundle);
        if (mti != null) {
          try {
            return mti.getObjectClassDefinition(pid, locale.toString());
          } catch (IllegalArgumentException e) {
            // MetaTypeProvider.getObjectClassDefinition might throw illegal
            // argument exception. So we must catch it here, otherwise the
            // other configurations will not be shown
            // See https://issues.apache.org/jira/browse/FELIX-2390
            // https://issues.apache.org/jira/browse/FELIX-3694
          }
        }
      }
    }

    // fallback to nothing found
    return null;
  }

  private Object getAttributeValue(String value, int type) {
    switch (type) {
      case AttributeDefinition.BOOLEAN:
        return Boolean.valueOf(value);
      case AttributeDefinition.BYTE:
        return Byte.valueOf(value);
      case AttributeDefinition.DOUBLE:
        return Double.valueOf(value);
      case AttributeDefinition.CHARACTER:
        return value.toCharArray()[0];
      case AttributeDefinition.FLOAT:
        return Float.valueOf(value);
      case AttributeDefinition.INTEGER:
        return Integer.valueOf(value);
      case AttributeDefinition.LONG:
        return Long.valueOf(value);
      case AttributeDefinition.SHORT:
        return Short.valueOf(value);
      case AttributeDefinition.PASSWORD:
      case AttributeDefinition.STRING:
      default:
        return value;
    }
  }

  protected BundleContext getBundleContext() {
    return FrameworkUtil.getBundle(this.getClass()).getBundleContext();
  }

  private void updateRegistrySourceConfigurations() {
    this.updateRegistrySourceConfigurations(false);
  }

  private void updateRegistrySourceConfigurations(boolean deleteOldConfig) {
    try {
      List<Metacard> metacards =
          Security.getInstance()
              .runAsAdminWithException(() -> federationAdminService.getRegistryMetacards());

      for (Metacard metacard : metacards) {
        try {
          if (deleteOldConfig) {
            deleteRegistryConfigurations(metacard);
          }
          updateRegistryConfigurations(metacard, deleteOldConfig);
        } catch (InvalidSyntaxException | ParserException | IOException e) {
          LOGGER.debug(
              "Unable to update registry configurations. Registry source configurations won't be updated for metacard id: {}",
              metacard.getId());
        }
      }

    } catch (PrivilegedActionException e) {
      LOGGER.debug(
          "Error getting registry metacards. Registry source configurations won't be updated.");
    }
  }

  public void setActivateConfigurations(boolean activateConfigurations) {
    if (!(activateConfigurations == this.activateConfigurations)) {
      this.activateConfigurations = activateConfigurations;

      if (!activateConfigurations || preserveActiveConfigurations) {
        return;
      }

      updateRegistrySourceConfigurations();
    }
  }

  public void setPreserveActiveConfigurations(boolean preserveActiveConfigurations) {
    if (!(preserveActiveConfigurations == this.preserveActiveConfigurations)) {
      this.preserveActiveConfigurations = preserveActiveConfigurations;

      if (preserveActiveConfigurations || !activateConfigurations) {
        return;
      }

      updateRegistrySourceConfigurations();
    }
  }

  public void setCleanUpOnDelete(boolean cleanUpOnDelete) {
    this.cleanUpOnDelete = cleanUpOnDelete;
  }

  public synchronized void setBindingTypeFactoryPid(List<String> bindingTypeFactoryPid) {
    bindingTypeToFactoryPidMap.clear();

    for (String mapping : bindingTypeFactoryPid) {
      String bindingType = StringUtils.substringBefore(mapping, "=");
      String factoryPid = StringUtils.substringAfter(mapping, "=");

      if (StringUtils.isNotBlank(bindingType) && StringUtils.isNotBlank(factoryPid)) {
        bindingTypeToFactoryPidMap.put(bindingType, factoryPid);
      }
    }
  }

  public synchronized void setSourceActivationPriorityOrder(
      List<String> sourceActivationPriorityOrder) {
    if (!sourceActivationPriorityOrder.equals(this.sourceActivationPriorityOrder)) {
      this.sourceActivationPriorityOrder.clear();
      this.sourceActivationPriorityOrder.addAll(sourceActivationPriorityOrder);

      if (!activateConfigurations && preserveActiveConfigurations) {
        return;
      }

      updateRegistrySourceConfigurations();
    }
  }

  public void setUrlBindingName(String urlBindingName) {
    this.urlBindingName = urlBindingName;
  }

  public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
    this.configurationAdmin = configurationAdmin;
  }

  public void setMetaTypeService(MetaTypeService metaTypeService) {
    this.metaTypeService = metaTypeService;
  }

  public void setRegistryTypeHelper(RegistryPackageTypeHelper registryTypeHelper) {
    this.registryTypeHelper = registryTypeHelper;
  }

  public void setSlotHelper(SlotTypeHelper slotHelper) {
    this.slotHelper = slotHelper;
  }

  public void setMetacardMarshaller(MetacardMarshaller helper) {
    this.metacardMarshaller = helper;
  }

  @Override
  public void regenerateAllSources() throws FederationAdminException {
    updateRegistrySourceConfigurations(true);
  }

  @Override
  public void regenerateOneSource(String registryId) throws FederationAdminException {
    try {
      List<Metacard> metacards =
          federationAdminService.getRegistryMetacardsByRegistryIds(
              Collections.singletonList(registryId));
      if (metacards.size() != 1) {
        throw new FederationAdminException(
            "Error looking up metacard to regenerate sources. registry-id=" + registryId);
      }
      deleteRegistryConfigurations(metacards.get(0));
      updateRegistryConfigurations(metacards.get(0), true);
    } catch (IOException | ParserException | InvalidSyntaxException e) {
      throw new FederationAdminException(
          "Error regenerating sources for registry entry " + registryId, e);
    }
  }
}
