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

import com.github.drapostolos.typeparser.TypeParser;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.codice.ddf.admin.core.api.ConfigurationAdmin;
import org.codice.ddf.admin.core.api.ConfigurationStatus;
import org.codice.ddf.admin.core.api.Metatype;
import org.codice.ddf.admin.core.api.MetatypeAttribute;
import org.codice.ddf.admin.core.api.Service;
import org.codice.ddf.admin.core.api.jmx.AdminConsoleServiceMBean;
import org.codice.ddf.admin.core.impl.module.ValidationDecorator;
import org.codice.ddf.ui.admin.api.module.AdminModule;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.metatype.AttributeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides convenience methods for interacting with OSGi/Felix ConfigurationAdmin
 * services.
 */
public class AdminConsoleService extends StandardMBean implements AdminConsoleServiceMBean {

  private static final String GUEST_CLAIMS_CONFIG_PID = "ddf.security.guest.realm";

  private static final String IDP_CLIENT_CONFIG_PID =
      "(service.pid=org.codice.ddf.security.idp.client.IdpMetadata)";

  private static final String IDP_SERVER_CONFIG_PID =
      "(service.pid=org.codice.ddf.security.idp.server.IdpEndpoint)";

  private static final String OIDC_HANDLER_CONFIG_PID =
      "(service.pid=org.codice.ddf.security.handler.api.OidcHandlerConfiguration)";

  private static final String UI_CONFIG_PID = "ddf.platform.ui.config";

  private static final String PROFILE_KEY = "profile";

  private static final Logger LOGGER = LoggerFactory.getLogger(AdminConsoleService.class);

  private static final Set<Character> ILLEGAL_CHARACTER_SET =
      new HashSet<>(Arrays.asList(';', '<', '>', '{', '}'));

  private final org.osgi.service.cm.ConfigurationAdmin configurationAdmin;

  private final ConfigurationAdmin configurationAdminImpl;

  private GuestClaimsHandlerExt guestClaimsHandlerExt;

  private ObjectName objectName;

  private MBeanServer mBeanServer;

  private List<AdminModule> moduleList;

  private static final String ILLEGAL_PID_MESSAGE = "Argument pid cannot be null or empty";

  private static final String ILLEGAL_TABLE_MESSAGE = "Argument configurationTable cannot be null";

  /**
   * Constructor for use in unit tests. Needed for testing listServices() and getService().
   *
   * @param configurationAdmin instance of org.osgi.service.cm.ConfigurationAdmin service
   * @param configurationAdminImpl mocked instance of ConfigurationAdminImpl
   */
  public AdminConsoleService(
      org.osgi.service.cm.ConfigurationAdmin configurationAdmin,
      ConfigurationAdmin configurationAdminImpl)
      throws NotCompliantMBeanException {
    super(AdminConsoleServiceMBean.class);
    this.configurationAdmin = configurationAdmin;
    this.configurationAdminImpl = configurationAdminImpl;
  }

  /** Initialize this MBean and register it with the MBean server */
  public void init() {
    try {
      if (objectName == null) {
        objectName = new ObjectName(AdminConsoleServiceMBean.OBJECT_NAME);
      }
      if (mBeanServer == null) {
        mBeanServer = ManagementFactory.getPlatformMBeanServer();
      }
      try {
        mBeanServer.registerMBean(this, objectName);
      } catch (InstanceAlreadyExistsException iaee) {
        // Try to remove and re-register
        LOGGER.debug("Re-registering SchemaLookup MBean");
        mBeanServer.unregisterMBean(objectName);
        mBeanServer.registerMBean(this, objectName);
      }
    } catch (Exception e) {
      LOGGER.info("Exception during initialization: ", e);
      throw new RuntimeException(e);
    }
  }

  /** Unregister this MBean with the MBean server */
  public void destroy() {
    try {
      if (objectName != null && mBeanServer != null) {
        mBeanServer.unregisterMBean(objectName);
      }
    } catch (Exception e) {
      LOGGER.debug("Exception unregistering mbean: ", e);
      throw new RuntimeException(e);
    }
  }

  public List<AdminModule> getModuleList() {
    return moduleList;
  }

  public void setModuleList(List<AdminModule> moduleList) {
    this.moduleList = moduleList;
  }

  public List<Service> listServices() {
    return configurationAdminImpl.listServices(
        configurationAdminImpl.getDefaultFactoryLdapFilter(),
        configurationAdminImpl.getDefaultLdapFilter());
  }

  public Service getService(String filter) {
    List<Service> services = configurationAdminImpl.listServices(filter, filter);

    Service service = null;

    if (!services.isEmpty()) {
      // just grab the first one, they should have specified a filter that returned just a single
      // result
      // if not, that is not our problem
      service = services.get(0);
    }

    return service;
  }

  public List<Map<String, Object>> listModules() {
    List<ValidationDecorator> adminModules = ValidationDecorator.wrap(moduleList);
    Collections.sort(adminModules);
    List<Map<String, Object>> modules = new ArrayList<>();

    for (ValidationDecorator module : adminModules) {
      if (module.isValid()) {
        modules.add(module.toMap());
      } else {
        LOGGER.debug("Couldn't add invalid module, {}", module.getName());
      }
    }

    if (!modules.isEmpty()) {
      modules.get(0).put("active", true);
    }
    return modules;
  }

  /** @see AdminConsoleServiceMBean#createFactoryConfiguration(java.lang.String) */
  public String createFactoryConfiguration(String factoryPid) throws IOException {
    return createFactoryConfigurationForLocation(factoryPid, null);
  }

  /**
   * @see AdminConsoleServiceMBean#createFactoryConfigurationForLocation(java.lang.String,
   *     java.lang.String)
   */
  public String createFactoryConfigurationForLocation(String factoryPid, String location)
      throws IOException {
    if (StringUtils.isBlank(factoryPid)) {
      throw new IOException("Argument factoryPid cannot be null or empty");
    }

    if (isPermittedToViewService(factoryPid)) {
      Configuration config = configurationAdmin.createFactoryConfiguration(factoryPid);
      config.setBundleLocation(location);
      return config.getPid();
    }

    return null;
  }

  /** @see AdminConsoleServiceMBean#delete(java.lang.String) */
  public void delete(String pid) throws IOException {
    deleteForLocation(pid, null);
  }

  /** @see AdminConsoleServiceMBean#deleteForLocation(java.lang.String, java.lang.String) */
  public void deleteForLocation(String pid, String location) throws IOException {
    if (pid == null || pid.length() < 1) {
      throw new IOException(ILLEGAL_PID_MESSAGE);
    }

    if (isPermittedToViewService(pid)) {
      Configuration config = configurationAdmin.getConfiguration(pid, location);
      config.delete();
    }
  }

  /** @see AdminConsoleServiceMBean#deleteConfigurations(java.lang.String) */
  public void deleteConfigurations(String filter) throws IOException {
    if (filter == null || filter.length() < 1) {
      throw new IOException("Argument filter cannot be null or empty");
    }
    Configuration[] configuations;
    try {
      configuations = configurationAdmin.listConfigurations(filter);
    } catch (InvalidSyntaxException e) {
      throw new IOException("Invalid filter [" + filter + "] : " + e);
    }
    if (configuations != null) {
      for (Configuration config : configuations) {
        config.delete();
      }
    }
  }

  /** @see AdminConsoleServiceMBean#getBundleLocation(java.lang.String) */
  public String getBundleLocation(String pid) throws IOException {
    if (StringUtils.isBlank(pid)) {
      throw new IOException(ILLEGAL_PID_MESSAGE);
    }
    Configuration config = configurationAdmin.getConfiguration(pid, null);
    return (config.getBundleLocation() == null)
        ? "Configuration is not yet bound to a bundle location"
        : config.getBundleLocation();
  }

  /** @see AdminConsoleServiceMBean#getConfigurations(java.lang.String) */
  public String[][] getConfigurations(String filter) throws IOException {
    if (filter == null || filter.length() < 1) {
      throw new IOException("Argument filter cannot be null or empty");
    }
    List<String[]> result = new ArrayList<>();
    Configuration[] configurations;
    try {
      configurations = configurationAdmin.listConfigurations(filter);
    } catch (InvalidSyntaxException e) {
      throw new IOException("Invalid filter [" + filter + "] : " + e);
    }
    if (configurations != null) {
      for (Configuration config : configurations) {
        if (isPermittedToViewService(config.getPid())) {
          result.add(new String[] {config.getPid(), config.getBundleLocation()});
        }
      }
    }
    return result.toArray(new String[result.size()][]);
  }

  /** @see AdminConsoleServiceMBean#getFactoryPid(java.lang.String) */
  public String getFactoryPid(String pid) throws IOException {
    return getFactoryPidForLocation(pid, null);
  }

  /** @see AdminConsoleServiceMBean#getFactoryPidForLocation(java.lang.String, java.lang.String) */
  public String getFactoryPidForLocation(String pid, String location) throws IOException {
    if (pid == null || pid.length() < 1) {
      throw new IOException(ILLEGAL_PID_MESSAGE);
    }
    Configuration config = configurationAdmin.getConfiguration(pid, location);
    return config.getFactoryPid();
  }

  /** @see AdminConsoleServiceMBean#getProperties(java.lang.String) */
  public Map<String, Object> getProperties(String pid) throws IOException {
    return getPropertiesForLocation(pid, null);
  }

  /** @see AdminConsoleServiceMBean#getPropertiesForLocation(java.lang.String, java.lang.String) */
  public Map<String, Object> getPropertiesForLocation(String pid, String location)
      throws IOException {
    if (pid == null || pid.length() < 1) {
      throw new IOException(ILLEGAL_PID_MESSAGE);
    }
    Map<String, Object> propertiesTable = new HashMap<>();
    Configuration config = configurationAdmin.getConfiguration(pid, location);

    if (isPermittedToViewService(config.getPid())) {
      Dictionary<String, Object> properties = config.getProperties();
      if (properties != null) {
        Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement();
          propertiesTable.put(key, properties.get(key));
        }
      }
    }
    return propertiesTable;
  }

  /** @see AdminConsoleServiceMBean#setBundleLocation(java.lang.String, java.lang.String) */
  public void setBundleLocation(String pid, String location) throws IOException {
    if (pid == null || pid.length() < 1) {
      throw new IOException("Argument factoryPid cannot be null or empty");
    }
    Configuration config = configurationAdmin.getConfiguration(pid, null);
    config.setBundleLocation(location);
  }

  public boolean updateGuestClaimsProfile(String pid, Map<String, Object> configurationTable)
      throws IOException {
    try {
      Object profileObj = configurationTable.get(PROFILE_KEY);
      if (profileObj == null) {
        return false;
      }

      if (!(profileObj instanceof String)) {
        LOGGER.debug("Selected guest claims profile was not a String");
        return false;
      }

      String profile = (String) profileObj;
      guestClaimsHandlerExt.setSelectedClaimsProfileName(profile);

      comprehensiveUpdate(GUEST_CLAIMS_CONFIG_PID, configurationTable);

      List<Map<String, Object>> configs = guestClaimsHandlerExt.getProfileConfigs();
      if (configs != null) {
        configs.forEach(
            config -> {
              String configPid = (String) config.get(GuestClaimsHandlerExt.PID_KEY);
              Map<String, Object> configProps =
                  (Map<String, Object>) config.get(GuestClaimsHandlerExt.PROPERTIES_KEY);
              comprehensiveUpdate(configPid, configProps);
            });
      }

      return true;

    } catch (RuntimeException e) {
      LOGGER.debug("An invalid guest claims profile was selected, caused by: {}", e);
      return false;
    }
  }

  /** @see AdminConsoleServiceMBean#update(java.lang.String, java.util.Map) */
  public boolean update(String pid, Map<String, Object> configurationTable) throws IOException {
    updateForLocation(pid, null, configurationTable);
    return true;
  }

  /**
   * @see AdminConsoleServiceMBean#updateForLocation(java.lang.String, java.lang.String,
   *     java.util.Map)
   */
  public void updateForLocation(
      final String pid, String location, Map<String, Object> configurationTable)
      throws IOException {
    if (pid == null || pid.length() < 1) {
      throw loggedException(ILLEGAL_PID_MESSAGE);
    }
    if (configurationTable == null) {
      throw loggedException(ILLEGAL_TABLE_MESSAGE);
    }

    final Configuration config = configurationAdmin.getConfiguration(pid, location);
    if (isPermittedToViewService(config.getPid())) {
      final Metatype metatype = configurationAdminImpl.findMetatypeForConfig(config);
      if (metatype == null) {
        throw loggedException("Could not find metatype for " + pid);
      }

      final List<Map.Entry<String, Object>> configEntries = new ArrayList<>();
      CollectionUtils.addAll(configEntries, configurationTable.entrySet().iterator());
      CollectionUtils.transform(
          configEntries, new CardinalityTransformer(metatype.getAttributeDefinitions(), pid));

      final Dictionary<String, Object> configProperties = config.getProperties();
      final Dictionary<String, Object> newConfigProperties =
          (configProperties != null) ? configProperties : new Hashtable<>();

      // If the configuration entry is a password, and its updated configuration value is
      // "password", do not update the password.
      for (Map.Entry<String, Object> configEntry : configEntries) {
        final String configEntryKey = configEntry.getKey();
        Object configEntryValue =
            sanitizeUIConfiguration(pid, configEntryKey, configEntry.getValue());
        if (configEntryValue.equals("password")) {
          for (Map<String, Object> metatypeProperties : metatype.getAttributeDefinitions()) {
            if (metatypeProperties.get("id").equals(configEntry.getKey())
                && AttributeDefinition.PASSWORD == (Integer) metatypeProperties.get("type")
                && configProperties != null) {
              configEntryValue = configProperties.get(configEntryKey);
              break;
            }
          }
        }
        newConfigProperties.put(configEntryKey, configEntryValue);
      }

      config.update(newConfigProperties);
    }
  }

  private Object sanitizeUIConfiguration(
      String pid, String configEntryKey, Object configEntryValue) {
    if (UI_CONFIG_PID.equals(pid)
        && ("color".equalsIgnoreCase(configEntryKey)
            || "background".equalsIgnoreCase(configEntryKey))
        && (Arrays.stream(ArrayUtils.toObject(String.valueOf(configEntryValue).toCharArray()))
            .parallel()
            .anyMatch(ILLEGAL_CHARACTER_SET::contains))) {
      throw loggedException(
          "Invalid UI Configuration: The color and background properties must only contain a color value");
    }
    return configEntryValue;
  }

  public ConfigurationStatus disableConfiguration(String servicePid) throws IOException {
    if (!isPermittedToViewService(servicePid)) {
      return null;
    }

    if (StringUtils.isEmpty(servicePid)) {
      throw new IOException(
          "Service PID of Source to be disabled must be specified.  Service PID provided: "
              + servicePid);
    }

    Configuration originalConfig = configurationAdminImpl.getConfiguration(servicePid);

    if (originalConfig == null) {
      throw new IOException("No Configuration exists with the service PID: " + servicePid);
    }

    return configurationAdminImpl.disableManagedServiceFactoryConfiguration(
        servicePid, originalConfig);
  }

  @Override
  public Map<String, Object> getClaimsConfiguration(String filter) {
    Map<String, Object> config = this.getService(filter);
    if (config != null && guestClaimsHandlerExt != null) {
      config.put("claims", guestClaimsHandlerExt.getClaims());
      config.put("profiles", guestClaimsHandlerExt.getClaimsProfiles());
    }
    return config;
  }

  @Override
  public Map<String, Object>[] getSsoConfigurations() {
    return new Map[] {
      this.getService(IDP_CLIENT_CONFIG_PID),
      this.getService(IDP_SERVER_CONFIG_PID),
      this.getService(OIDC_HANDLER_CONFIG_PID)
    };
  }

  @Override
  public void setSsoConfigurations(Map<String, Object>[] configs) throws IOException {
    for (Map config : configs) {
      String pid = (String) config.get("metatypeId");
      Map<String, Object> updateValues = getUpdateValues((JSONArray) config.get("metatypeEntries"));

      comprehensiveUpdate(pid, updateValues);
    }
  }

  public ConfigurationStatus enableConfiguration(String servicePid) throws IOException {
    if (StringUtils.isEmpty(servicePid)) {
      throw new IOException(
          "Service PID of Source to be disabled must be specified.  Service PID provided: "
              + servicePid);
    }

    if (!isPermittedToViewService(servicePid)) {
      return null;
    }

    Configuration disabledConfig = configurationAdminImpl.getConfiguration(servicePid);

    if (disabledConfig == null) {
      throw new IOException("No Configuration exists with the service PID: " + servicePid);
    }

    return configurationAdminImpl.enableManagedServiceFactoryConfiguration(
        servicePid, disabledConfig);
  }

  /** Setter method for mBeanServer. Needed for testing init() and destroy(). */
  void setMBeanServer(MBeanServer server) {
    mBeanServer = server;
  }

  private Map<String, Object> getUpdateValues(JSONArray metatypeEntries) {
    Map<String, Object> updateValues = new HashMap<>();

    for (Object entry : metatypeEntries) {
      Map<String, Object> configEntry = (Map<String, Object>) entry;
      updateValues.put((String) configEntry.get("id"), configEntry.get("value"));
    }

    return updateValues;
  }

  private static class CardinalityTransformer implements Transformer {
    private final List<MetatypeAttribute> metatype;

    private final String pid;

    public CardinalityTransformer(List<MetatypeAttribute> metatype, String pid) {
      this.metatype = metatype;
      this.pid = pid;
    }

    @Override
    // the method signature precludes a safer parameter type
    public Object transform(Object input) {
      if (!(input instanceof Map.Entry)) {
        throw loggedException("Cannot transform " + input);
      }
      @SuppressWarnings("unchecked")
      Map.Entry<String, Object> entry = (Map.Entry<String, Object>) input;
      String attrId = entry.getKey();
      if (attrId == null) {
        throw loggedException("Found null key for " + pid);
      }
      Integer cardinality = null;
      Integer type = null;
      for (MetatypeAttribute attribute : metatype) {
        if (attrId.equals(attribute.getId()) || attrId.equals(attribute.getName())) {
          cardinality = attribute.getCardinality();
          type = attribute.getType();
        }
      }
      if (cardinality == null) {
        LOGGER.debug("Could not find property {} in metatype for config {}", attrId, pid);
        cardinality = 0;
        type = TYPE.STRING.getType();
      }
      Object value = entry.getValue();

      // ensure we don't allow any empty values
      if (value == null || StringUtils.isEmpty(String.valueOf(value))) {
        value = "";
      }
      // negative cardinality means a vector, 0 is a string, and positive is an array
      CardinalityEnforcer cardinalityEnforcer = TYPE.forType(type).getCardinalityEnforcer();
      if (value instanceof String && cardinality != 0) {
        try {
          value = new JSONParser().parse(String.valueOf(value));
        } catch (ParseException e) {
          LOGGER.debug("{} is not a JSON array.", value, e);
        }
      }
      if (cardinality < 0) {
        value = cardinalityEnforcer.negativeCardinality(value);
      } else if (cardinality == 0) {
        value = cardinalityEnforcer.zerothCardinality(value);
      } else if (cardinality > 0) {
        value = cardinalityEnforcer.positiveCardinality(value);
      }

      entry.setValue(value);

      return entry;
    }
  }

  private static class CardinalityEnforcer<T> {
    private final Class<T> clazz;

    public CardinalityEnforcer(Class<T> clazz) {
      this.clazz = clazz;
    }

    @SuppressWarnings("unchecked")
    public T[] positiveCardinality(Object value) {
      List<T> list = negativeCardinality(value);
      return list.toArray((T[]) Array.newInstance(clazz, list.size()));
    }

    public List<T> negativeCardinality(Object value) {
      if (!(value.getClass().isArray() || value instanceof Collection)) {
        if (String.valueOf(value).isEmpty()) {
          value = new Object[] {};
        } else {
          value = new Object[] {value};
        }
      }
      List<T> ret = new ArrayList<>();
      for (int i = 0; i < CollectionUtils.size(value); i++) {
        Object currentValue = CollectionUtils.get(value, i);
        ret.add(zerothCardinality(currentValue));
      }
      return ret;
    }

    public T zerothCardinality(Object value) {
      if (value.getClass().isArray() || value instanceof Collection) {
        if (CollectionUtils.size(value) != 1) {
          throw loggedException(
              "Attempt on 0-cardinality property to set multiple values:" + value);
        }
        value = CollectionUtils.get(value, 0);
      }
      if (!clazz.isInstance(value)) {
        value = TypeParser.newBuilder().build().parse(String.valueOf(value), clazz);
      }
      if (clazz.isInstance(value)) {
        return clazz.cast(value);
      }
      throw loggedException("Failed to parse " + value + " as " + clazz);
    }
  }

  private static IllegalArgumentException loggedException(String message) {
    IllegalArgumentException exception = new IllegalArgumentException(message);
    LOGGER.info(message, exception);
    return exception;
  }

  // felix won't take Object[] or Vector<Object>, so here we
  // map all the osgi constants to strongly typed arrays/vectors
  enum TYPE {
    STRING(AttributeDefinition.STRING, String.class) {},
    LONG(AttributeDefinition.LONG, Long.class) {},
    INTEGER(AttributeDefinition.INTEGER, Integer.class) {},
    SHORT(AttributeDefinition.SHORT, Short.class) {},
    CHARACTER(AttributeDefinition.CHARACTER, Character.class) {},
    BYTE(AttributeDefinition.BYTE, Byte.class) {},
    DOUBLE(AttributeDefinition.DOUBLE, Double.class) {},
    FLOAT(AttributeDefinition.FLOAT, Float.class) {},
    BIGINTEGER(AttributeDefinition.BIGINTEGER, BigInteger.class) {},
    BIGDECIMAL(AttributeDefinition.BIGDECIMAL, BigDecimal.class) {},
    BOOLEAN(AttributeDefinition.BOOLEAN, Boolean.class) {},
    PASSWORD(AttributeDefinition.PASSWORD, String.class) {};

    private final int type;

    private final Class clazz;

    TYPE(int type, Class clazz) {
      this.type = type;
      this.clazz = clazz;
    }

    @SuppressWarnings("unchecked")
    public CardinalityEnforcer getCardinalityEnforcer() {
      return new CardinalityEnforcer(clazz);
    }

    public int getType() {
      return type;
    }

    public static TYPE forType(int type) {
      for (TYPE theType : TYPE.values()) {
        if (theType.getType() == type) {
          return theType;
        }
      }
      return STRING;
    }
  }

  public void setGuestClaimsHandlerExt(GuestClaimsHandlerExt guestClaimsHandlerExt) {
    this.guestClaimsHandlerExt = guestClaimsHandlerExt;
  }

  public boolean isPermittedToViewService(String servicePid) {
    return configurationAdminImpl.isPermittedToViewService(servicePid, SecurityUtils.getSubject());
  }

  private void comprehensiveUpdate(String pid, Map<String, Object> configurationTable) {
    try {
      Map<String, Object> existingConfig = this.getProperties(pid);
      if (configurationTable == null || configurationTable.isEmpty()) {
        return;
      }
      existingConfig.putAll(configurationTable);
      this.update(pid, existingConfig);
    } catch (IOException e) {
      LOGGER.debug("Exception while updating configs: ", e);
    }
  }
}
