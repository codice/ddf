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
package org.codice.ddf.admin.core.impl;

import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID;

import com.google.common.collect.Sets;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.admin.core.api.ConfigurationDetails;
import org.codice.ddf.admin.core.api.ConfigurationProperties;
import org.codice.ddf.admin.core.api.ConfigurationStatus;
import org.codice.ddf.admin.core.api.Metatype;
import org.codice.ddf.admin.core.api.MetatypeAttribute;
import org.codice.ddf.admin.core.api.Service;
import org.codice.ddf.ui.admin.api.plugin.ConfigurationAdminPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationAdminImpl implements org.codice.ddf.admin.core.api.ConfigurationAdmin {

  /**
   * The implementation of the {@link IdGetter} interface returning the PIDs listed in the meta type
   * information.
   *
   * @see #getPidObjectClasses()
   */
  private static final IdGetter PID_GETTER = MetaTypeInformation::getPids;

  /**
   * The implementation of the {@link IdGetter} interface returning the factory PIDs listed in the
   * meta type information.
   */
  private static final IdGetter FACTORY_PID_GETTER = MetaTypeInformation::getFactoryPids;

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationAdminImpl.class);

  private static final Map<Long, String> BUNDLE_LOCATIONS = new ConcurrentHashMap<>();

  private static final String CONFIGURATION_REGISTRY_ID_PROPERTY = "registry-id";

  private final ConfigurationAdmin configurationAdmin;

  private final Map<String, ServiceTracker> services = new HashMap<String, ServiceTracker>();

  private List<ConfigurationAdminPlugin> configurationAdminPluginList;

  /**
   * @param configurationAdmin
   * @throws ClassCastException if {@code service} is not a MetaTypeService instances
   */
  public ConfigurationAdminImpl(
      final Object configurationAdmin,
      List<ConfigurationAdminPlugin> configurationAdminPluginList) {
    this.configurationAdmin = (ConfigurationAdmin) configurationAdmin;
    this.configurationAdminPluginList = configurationAdminPluginList;
  }

  private static String getBundleLocation(Bundle bundle) {
    return BUNDLE_LOCATIONS.computeIfAbsent(bundle.getBundleId(), id -> bundle.getLocation());
  }

  static Bundle getBundle(final BundleContext bundleContext, final String bundleLocation) {
    if (bundleLocation == null) {
      return null;
    }

    Bundle[] bundles = bundleContext.getBundles();
    for (Bundle bundle : bundles) {
      if (bundleLocation.equals(getBundleLocation(bundle))) {
        return bundle;
      }
    }

    return null;
  }

  BundleContext getBundleContext() {
    Bundle cxfBundle = FrameworkUtil.getBundle(ConfigurationAdminImpl.class);
    if (cxfBundle != null) {
      return cxfBundle.getBundleContext();
    }
    return null;
  }

  public final Configuration getConfiguration(String pid) {
    if (pid != null) {
      try {
        // we use listConfigurations to not create configuration
        // objects persistently without the user providing actual
        // configuration
        String filter = '(' + SERVICE_PID + '=' + pid + ')';
        Configuration[] configs = this.configurationAdmin.listConfigurations(filter);
        if (configs != null && configs.length > 0 && isPermittedToViewService(pid)) {
          return configs[0];
        }
      } catch (InvalidSyntaxException ise) {
        LOGGER.info("Invalid LDAP filter", ise);
      } catch (IOException ioe) {
        LOGGER.info("Unable to retrieve list of configurations.", ioe);
      }
    }

    // fallback to no configuration at all
    return null;
  }

  private Bundle getBoundBundle(Configuration config) {

    if (null == config) {
      return null;
    }

    final String location = config.getBundleLocation();
    if (null == location) {
      return null;
    }

    final Bundle bundles[] = getBundleContext().getBundles();
    for (int i = 0; bundles != null && i < bundles.length; i++) {
      if (location.equals(getBundleLocation(bundles[i]))) {
        return bundles[i];
      }
    }
    return null;
  }

  public List<Service> listServices(String serviceFactoryFilter, String serviceFilter) {
    List<Service> serviceList = null;
    List<Service> serviceFactoryList = null;

    Map<Long, MetaTypeInformation> metaTypeInformationByBundle = new HashMap<>();

    try {
      // Get ManagedService instances
      serviceList = getServices(ManagedService.class.getName(), serviceFilter, true);

      Map<String, ObjectClassDefinition> configPidToOcdMap =
          getPidObjectClasses(metaTypeInformationByBundle);

      // Get ManagedService Metatypes
      List<Metatype> metatypeList =
          addMetaTypeNamesToMap(configPidToOcdMap, serviceFilter, SERVICE_PID);

      // Get ManagedServiceFactory instances
      serviceFactoryList =
          getServices(ManagedServiceFactory.class.getName(), serviceFactoryFilter, true);

      // Get ManagedServiceFactory Metatypes
      metatypeList.addAll(
          addMetaTypeNamesToMap(
              getFactoryPidObjectClasses(metaTypeInformationByBundle),
              serviceFactoryFilter,
              SERVICE_FACTORYPID));

      for (Service service : serviceFactoryList) {

        service.setFactory(true);

        for (Metatype metatype : metatypeList) {
          if (metatype.getId() != null && metatype.getId().equals(service.getId())) {
            service.putAll(metatype);
          }
        }

        Configuration[] configs =
            configurationAdmin.listConfigurations(
                "(|(service.factoryPid="
                    + service.getId()
                    + ")(service.factoryPid="
                    + service.getId()
                    + "_disabled))");
        if (configs != null) {
          addConfigurationData(service, configs, configPidToOcdMap);
        }
      }

      for (Service service : serviceList) {
        service.setFactory(false);

        for (Metatype metatype : metatypeList) {
          if (metatype.getId() != null && metatype.getId().equals(service.getId())) {
            service.putAll(metatype);
          }
        }

        Configuration[] configs =
            configurationAdmin.listConfigurations("(" + SERVICE_PID + "=" + service.getId() + ")");
        if (configs != null) {
          addConfigurationData(service, configs, configPidToOcdMap);
        }
      }

      serviceList.addAll(serviceFactoryList);
    } catch (IOException e) {
      LOGGER.warn("Unable to obtain list of Configuration objects from ConfigurationAdmin.", e);
    } catch (InvalidSyntaxException e) {
      LOGGER.info("Provided LDAP filter is incorrect: {}", serviceFilter, e);
    }

    if (serviceList != null) {
      return serviceList
          .stream()
          .filter(service -> isPermittedToViewService(service.getId()))
          .collect(Collectors.toList());
    } else {
      return new ArrayList<>();
    }
  }

  private void addConfigurationData(
      Service service,
      Configuration[] configs,
      Map<String, ObjectClassDefinition> objectClassDefinitions) {
    for (Configuration config : configs) {
      // ignore configuration object if it is invalid
      final String pid = config.getPid();
      if (!isAllowedPid(pid)) {
        continue;
      }

      ConfigurationDetails configData = new ConfigurationDetailsImpl();
      configData.setId(pid);
      String fpid = config.getFactoryPid();
      if (fpid != null) {
        configData.setFactoryPid(fpid);
      }
      // insert an entry for the PID
      try {
        ObjectClassDefinition ocd = objectClassDefinitions.get(config.getPid());
        if (ocd != null) {
          configData.setName(ocd.getName());
        } else {
          // no object class definition, use plain PID
          configData.setName(pid);
        }
      } catch (IllegalArgumentException t) {
        // Catch exception thrown by getObjectClassDefinition so other configurations
        // are displayed
        // no object class definition, use plain PID
        configData.setName(pid);
      }

      final Bundle bundle = getBoundBundle(config);
      if (null != bundle) {
        configData.setBundle(bundle.getBundleId());
        configData.setBundleName(getName(bundle));
        configData.setBundleLocation(bundle.getLocation());
      }

      ConfigurationProperties propertiesTable = new ConfigurationPropertiesImpl();
      Dictionary<String, Object> properties = config.getProperties();
      if (properties != null) {
        Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement();
          propertiesTable.put(key, properties.get(key));
        }
      }

      // If the configuration property is a password that has been set,
      // mask its value to "password" so that the real password value will be hidden.
      List<MetatypeAttribute> metatypeList = service.getAttributeDefinitions();
      metatypeList
          .stream()
          .filter(metatype -> AttributeDefinition.PASSWORD == metatype.getType())
          .forEach(
              metatype -> {
                setPasswordMask(metatype, propertiesTable);
              });

      configData.setConfigurationProperties(propertiesTable);

      Map<String, Object> pluginDataMap =
          getConfigurationPluginData(configData.getId(), Collections.unmodifiableMap(configData));
      if (pluginDataMap != null && !pluginDataMap.isEmpty()) {
        configData.putAll(pluginDataMap);
      }

      List<ConfigurationDetails> configurationDetails;
      if (service.containsKey(Service.CONFIGURATIONS)) {
        configurationDetails = service.getConfigurations();
      } else if (service.containsKey(Service.DISABLED_CONFIGURATIONS)) {
        configurationDetails = service.getDisabledConfigurations();
      } else {
        configurationDetails = new ArrayList<>();
      }

      configurationDetails.add(configData);
      if (configData.getId().contains(ConfigurationDetails.DISABLED_SERVICE_IDENTIFIER)) {
        configData.setEnabled(false);
      } else {
        configData.setEnabled(true);
      }
      service.setConfigurations(configurationDetails);
    }
  }

  private void setPasswordMask(
      MetatypeAttribute metatype, ConfigurationProperties propertiesTable) {
    String passwordProperty = metatype.getId();
    if (propertiesTable.get(passwordProperty) == null) {
      propertiesTable.put(passwordProperty, "");
    } else if (!(propertiesTable.get(passwordProperty).toString().equals(""))) {
      propertiesTable.put(passwordProperty, "password");
    }
  }

  private Map<String, Object> getConfigurationPluginData(
      String servicePid, Map<String, Object> dataMap) {
    Map<String, Object> allPluginMap = new HashMap<>();
    if (configurationAdminPluginList != null) {
      for (ConfigurationAdminPlugin plugin : configurationAdminPluginList) {
        Map<String, Object> pluginDataMap =
            plugin.getConfigurationData(servicePid, dataMap, getBundleContext());
        allPluginMap.putAll(pluginDataMap);
      }
    }
    return allPluginMap;
  }

  /**
   * Return a display name for the given <code>bundle</code>:
   *
   * <ol>
   *   <li>If the bundle has a non-empty <code>Bundle-Name</code> manifest header that value is
   *       returned.
   *   <li>Otherwise the symbolic name is returned if set
   *   <li>Otherwise the bundle's location is returned if defined
   *   <li>Finally, as a last resort, the bundles id is returned
   * </ol>
   *
   * @param bundle the bundle which name to retrieve
   * @return the bundle name - see the description of the method for more details.
   */
  public String getName(Bundle bundle) {
    Locale locale = Locale.getDefault();
    final String loc = locale == null ? null : locale.toString();
    String name = bundle.getHeaders(loc).get(Constants.BUNDLE_NAME);
    if (name == null || name.length() == 0) {
      name = bundle.getSymbolicName();
      if (name == null) {
        name = bundle.getLocation();
        if (name == null) {
          name = String.valueOf(bundle.getBundleId());
        }
      }
    }
    return name;
  }

  private boolean isAllowedPid(final String pid) {
    if (pid == null) {
      return false;
    } else {
      for (int i = 0; i < pid.length(); i++) {
        final char c = pid.charAt(i);
        if (c == '&' || c == '<' || c == '>' || c == '"' || c == '\'') {
          return false;
        }
      }
      return true;
    }
  }

  public String getDefaultFactoryLdapFilter() {
    return "(" + SERVICE_FACTORYPID + "=" + "*)";
  }

  public String getDefaultLdapFilter() {
    return "(" + SERVICE_PID + "=" + "*)";
  }

  void setConfigurationAdminPluginList(
      List<ConfigurationAdminPlugin> configurationAdminPluginList) {
    this.configurationAdminPluginList = configurationAdminPluginList;
  }

  public Metatype findMetatypeForConfig(Configuration config) {
    List<Service> services = listServices(getDefaultFactoryLdapFilter(), getDefaultLdapFilter());
    for (Service service : services) {
      String id = service.getId();
      if (id.equals(config.getPid())
          || ((id.equals(config.getFactoryPid())
                  || (id + "_disabled").equals(config.getFactoryPid()))
              && Boolean.valueOf(String.valueOf(service.isFactory())))) {
        return service;
      }
    }

    return null;
  }

  /**
   * Returns a map of PIDs and providing bundles of MetaType information. The map is indexed by PID
   * and the value of each entry is the bundle providing the MetaType information for that PID.
   *
   * @return see the method description
   */
  private Map<String, ObjectClassDefinition> getPidObjectClasses(
      Map<Long, MetaTypeInformation> metaTypeInformationByBundle) {
    return getObjectClassDefinitions(PID_GETTER, metaTypeInformationByBundle);
  }

  /**
   * Returns the <code>ObjectClassDefinition</code> objects for the IDs returned by the <code>
   * idGetter</code>. Depending on the <code>idGetter</code> implementation this will be for factory
   * PIDs or plain PIDs.
   *
   * @param idGetter The {@link IdGetter} used to get the list of factory PIDs or PIDs from <code>
   *     MetaTypeInformation</code> objects.
   * @return Map of <code>ObjectClassDefinition</code> objects indexed by the PID (or factory PID)
   *     to which they pertain
   */
  private Map<String, ObjectClassDefinition> getObjectClassDefinitions(
      final IdGetter idGetter, Map<Long, MetaTypeInformation> metaTypeInformationByBundle) {
    final Map<String, ObjectClassDefinition> objectClassesDefinitions = new HashMap<>();
    final MetaTypeService mts = this.getMetaTypeService();
    if (mts == null) {
      return objectClassesDefinitions;
    }
    final Bundle[] bundles = this.getBundleContext().getBundles();
    for (Bundle bundle : bundles) {
      final MetaTypeInformation mti =
          metaTypeInformationByBundle.computeIfAbsent(
              bundle.getBundleId(), id -> mts.getMetaTypeInformation(bundle));
      objectClassesDefinitions.putAll(findOcdById(idGetter, mti));
    }
    return objectClassesDefinitions;
  }

  private Map<String, ObjectClassDefinition> findOcdById(
      IdGetter idGetter, MetaTypeInformation mti) {
    if (mti == null) {
      return Collections.emptyMap();
    }
    Map<String, ObjectClassDefinition> objectClassesDefinitions = new HashMap<>();
    final String[] idList = idGetter.getIds(mti);
    for (int j = 0; idList != null && j < idList.length; j++) {
      // After getting the list of PIDs, a configuration might be
      // removed. So the getObjectClassDefinition will throw
      // an exception, and this will prevent ALL configuration from
      // being displayed. By catching it, the configurations will be
      // visible
      ObjectClassDefinition ocd = null;
      try {
        ocd = mti.getObjectClassDefinition(idList[j], Locale.getDefault().toString());
      } catch (IllegalArgumentException ignore) {
        // ignore - just don't show this configuration
      }
      if (ocd != null) {
        objectClassesDefinitions.put(idList[j], ocd);
      }
    }
    return objectClassesDefinitions;
  }

  public ObjectClassDefinition getObjectClassDefinition(Configuration config) {
    // if the configuration is bound, try to get the object class
    // definition from the bundle installed from the given location
    if (config.getBundleLocation() != null) {
      Bundle bundle = getBundle(this.getBundleContext(), config.getBundleLocation());
      if (bundle != null) {
        String id = config.getFactoryPid();
        if (null == id) {
          id = config.getPid();
        }
        return getObjectClassDefinition(bundle, id);
      }
    }

    // get here if the configuration is not bound or if no
    // bundle with the bound location is installed. We search
    // all bundles for a matching [factory] PID
    // if the configuration is a factory one, use the factory PID
    if (config.getFactoryPid() != null) {
      return getObjectClassDefinition(config.getFactoryPid());
    }

    // otherwise use the configuration PID
    return getObjectClassDefinition(config.getPid());
  }

  public ObjectClassDefinition getObjectClassDefinition(Bundle bundle, String pid) {
    Locale locale = Locale.getDefault();
    if (bundle != null) {
      MetaTypeService mts = this.getMetaTypeService();
      if (mts != null) {
        MetaTypeInformation mti = mts.getMetaTypeInformation(bundle);
        if (mti != null) {
          // see #getObjectClasses( final IdGetter idGetter, final String locale )
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

  MetaTypeService getMetaTypeService() {
    return (MetaTypeService) getService(MetaTypeService.class.getName());
  }

  /**
   * Gets the service with the specified class name. Will create a new {@link ServiceTracker} if the
   * service is not already retrieved.
   *
   * @param serviceName the service name to obtain
   * @return the service or <code>null</code> if missing.
   */
  private Object getService(String serviceName) {
    ServiceTracker serviceTracker = services.get(serviceName);
    if (serviceTracker == null) {
      serviceTracker = new ServiceTracker(getBundleContext(), serviceName, null);
      serviceTracker.open();

      services.put(serviceName, serviceTracker);
    }

    return serviceTracker.getService();
  }

  public ObjectClassDefinition getObjectClassDefinition(String pid) {
    Bundle[] bundles = this.getBundleContext().getBundles();
    for (Bundle bundle : bundles) {
      try {
        ObjectClassDefinition ocd = this.getObjectClassDefinition(bundle, pid);
        if (ocd != null) {
          return ocd;
        }
      } catch (IllegalArgumentException iae) {
        // don't care
      }
    }
    return null;
  }

  /**
   * Returns a map of factory PIDs and providing bundles of MetaType information. The map is indexed
   * by factory PID and the value of each entry is the bundle providing the MetaType information for
   * that factory PID.
   *
   * @return see the method description
   */
  private Map<String, ObjectClassDefinition> getFactoryPidObjectClasses(
      Map<Long, MetaTypeInformation> metaTypeInformationByBundle) {
    return getObjectClassDefinitions(FACTORY_PID_GETTER, metaTypeInformationByBundle);
  }

  private List<Service> getServices(String serviceClass, String serviceFilter, boolean ocdRequired)
      throws InvalidSyntaxException {
    List<Service> serviceList = new ArrayList<>();

    // service.factoryPid cannot be searched, but service.pid can be searched,
    // and can contain a factoryPid
    String newFilter = null;
    if (serviceFilter != null) {
      newFilter = serviceFilter.replace("service.factoryPid", "service.pid");
    }

    // find all ManagedServiceFactories to get the factoryPIDs
    ServiceReference[] refs =
        this.getBundleContext().getAllServiceReferences(serviceClass, newFilter);

    for (int i = 0; refs != null && i < refs.length; i++) {
      Object pidObject = refs[i].getProperty(SERVICE_PID);
      // only include valid PIDs
      if (pidObject instanceof String && isAllowedPid((String) pidObject)) {
        String pid = (String) pidObject;
        String name = pid;
        boolean haveOcd = !ocdRequired;
        final ObjectClassDefinition ocd = getObjectClassDefinition(refs[i].getBundle(), pid);
        if (ocd != null) {
          name = ocd.getName();
          haveOcd = true;
        }

        if (haveOcd && ocd != null) {
          Service service = new ServiceImpl();
          String description = ocd.getDescription();
          service.setId(pid);
          if (StringUtils.isNotEmpty(description)) {
            service.setDescription(description);
          }
          service.setName(name);
          serviceList.add(service);
        }
      }
    }

    return serviceList
        .stream()
        .filter(service -> isPermittedToViewService(service.getId()))
        .collect(Collectors.toList());
  }

  private List<Metatype> addMetaTypeNamesToMap(
      final Map<String, ObjectClassDefinition> objectClassDefinitions,
      final String filterSpec,
      final String type) {
    Filter filter = null;
    if (filterSpec != null) {
      try {
        filter = getBundleContext().createFilter(filterSpec);
      } catch (InvalidSyntaxException ignore) {
        // don't care
      }
    }

    List<Metatype> metatypeList = new ArrayList<>();
    for (Entry<String, ObjectClassDefinition> ociEntry : objectClassDefinitions.entrySet()) {
      final String pid = ociEntry.getKey();
      final ObjectClassDefinition ocd = ociEntry.getValue();
      if (filter == null) {
        AttributeDefinition[] defs = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
        metatypeList.add(new MetatypeImpl(pid, ocd.getName(), createMetatypeMap(defs)));
      } else {
        final Dictionary<String, String> props = new Hashtable<>();
        props.put(type, pid);
        if (filter.match(props)) {
          AttributeDefinition[] defs = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
          metatypeList.add(new MetatypeImpl(pid, ocd.getName(), createMetatypeMap(defs)));
        }
      }
    }
    return metatypeList;
  }

  private List<MetatypeAttribute> createMetatypeMap(AttributeDefinition[] definitions) {
    List<MetatypeAttribute> metatypeList;

    if (definitions != null) {
      metatypeList =
          Arrays.stream(definitions).map(MetatypeAttributeImpl::new).collect(Collectors.toList());
    } else {
      metatypeList = new ArrayList<>();
    }

    return metatypeList;
  }

  public boolean isPermittedToViewService(String servicePid, Subject subject) {
    KeyValueCollectionPermission serviceToCheck =
        new KeyValueCollectionPermission(
            "view-service.pid", new KeyValuePermission("service.pid", Sets.newHashSet(servicePid)));
    return subject.isPermitted(serviceToCheck);
  }

  public boolean isPermittedToViewService(String servicePid) {
    return isPermittedToViewService(servicePid, SecurityUtils.getSubject());
  }

  public ConfigurationStatus disableManagedServiceFactoryConfiguration(
      String servicePid, Configuration originalConfig) throws IOException {
    Dictionary<String, Object> properties = originalConfig.getProperties();
    String originalFactoryPid =
        (String) properties.get(org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID);
    if (originalFactoryPid == null) {
      throw new IOException("Configuration does not belong to a managed service factory.");
    }
    if (StringUtils.endsWith(originalFactoryPid, ConfigurationStatus.DISABLED_EXTENSION)) {
      throw new IOException("Configuration is already disabled.");
    }

    // Copy configuration from the original configuration and change its factory PID to end with
    // "disabled"
    Dictionary<String, Object> disabledProperties =
        copyConfigProperties(properties, originalFactoryPid);
    String disabledServiceFactoryPid = originalFactoryPid + ConfigurationStatus.DISABLED_EXTENSION;
    disabledProperties.put(
        org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID, disabledServiceFactoryPid);
    Configuration disabledConfig =
        configurationAdmin.createFactoryConfiguration(disabledServiceFactoryPid, null);
    disabledConfig.update(disabledProperties);

    // remove original configuration
    originalConfig.delete();
    return new ConfigurationStatusImpl(
        disabledServiceFactoryPid, disabledConfig.getPid(), originalFactoryPid, servicePid);
  }

  public ConfigurationStatus enableManagedServiceFactoryConfiguration(
      String servicePid, Configuration disabledConfig) throws IOException {
    Dictionary<String, Object> properties = disabledConfig.getProperties();
    String disabledFactoryPid =
        (String) properties.get(org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID);
    if (disabledFactoryPid == null) {
      throw new IOException("Configuration does not belong to a managed service factory.");
    }
    if (!StringUtils.endsWith(disabledFactoryPid, ConfigurationStatus.DISABLED_EXTENSION)) {
      throw new IOException("Configuration is already enabled.");
    }

    String enabledFactoryPid =
        StringUtils.removeEnd(disabledFactoryPid, ConfigurationStatus.DISABLED_EXTENSION);
    Dictionary<String, Object> enabledProperties =
        copyConfigProperties(properties, enabledFactoryPid);
    enabledProperties.put(
        org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID, enabledFactoryPid);
    Configuration enabledConfiguration =
        configurationAdmin.createFactoryConfiguration(enabledFactoryPid, null);
    enabledConfiguration.update(enabledProperties);

    disabledConfig.delete();

    return new ConfigurationStatusImpl(
        enabledFactoryPid, enabledConfiguration.getPid(), disabledFactoryPid, servicePid);
  }

  private Dictionary<String, Object> copyConfigProperties(
      Dictionary<String, Object> properties, String factoryPid) {
    final Dictionary<String, Object> copiedProperties = new Hashtable<>();
    ObjectClassDefinition objectClassDefinition = getObjectClassDefinition(factoryPid);
    if (objectClassDefinition == null) {
      LOGGER.debug(
          "ObjectClassDefinition not found for factoryPid: {}. Unable to copy properties.",
          factoryPid);
      throw new IllegalStateException(
          "ObjectClassDefinition not found for factoryPid: " + factoryPid);
    }
    Stream.of(objectClassDefinition)
        .map(ocd -> ocd.getAttributeDefinitions(ObjectClassDefinition.ALL))
        .flatMap(Arrays::stream)
        .map(AttributeDefinition::getID)
        .forEach(id -> copyIfDefined(id, properties, copiedProperties));
    copyIfDefined(CONFIGURATION_REGISTRY_ID_PROPERTY, properties, copiedProperties);
    return copiedProperties;
  }

  private void copyIfDefined(
      String id, Dictionary<String, Object> source, Dictionary<String, Object> destination) {
    final Object value = source.get(id);

    if (value != null) {
      destination.put(id, value);
    }
  }

  /**
   * The <code>IdGetter</code> interface is an internal helper to abstract retrieving object class
   * definitions from all bundles for either pids or factory pids.
   *
   * @see #PID_GETTER
   * @see #FACTORY_PID_GETTER
   */
  private interface IdGetter {
    String[] getIds(MetaTypeInformation metaTypeInformation);
  }
}
