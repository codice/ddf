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
package org.codice.ddf.configuration;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The DDF Configuration Manager manages the DDF system settings. Some of these settings are
 * displayed in the Web Admin Console's Configuration tab under the DDF System Settings
 * configuration. Other settings are read-only, not displayed in the DDF System Settings
 * configuration (but appear in other OSGi bundle configurations such as CXF). These read-only
 * settings are included in the list of configuration settings pushed to registered listeners.
 *
 * <p>Registered listeners implement the ConfigurationWatcher interface and have these DDF
 * configuration settings pushed to them when they come online (aka bind) and when one or more of
 * the settings are changed in the Admin Console.
 */
public class ConfigurationManager {
  /** Service PID to use to look up System Settings Configuration. */
  public static final String PID = "ddf.platform.config";

  // Constants for the DDF system settings appearing in the Admin Console

  /** The directory where DDF is installed */
  public static final String HOME_DIR = "homeDir";

  /** The port number that CXF's underlying Jetty server is listening on, e.g., 8181 */
  public static final String HTTP_PORT = "httpPort";

  /**
   * The context root for all DDF services, e.g., the /services portion of the
   * http://hostname:8181/services URL
   */
  public static final String SERVICES_CONTEXT_ROOT = "servicesContextRoot";

  /** The hostname or IP address of the machine that DDF is running on */
  public static final String HOST = "host";

  /** The port number that DDF is listening on, e.g., 8181 */
  public static final String PORT = "port";

  /** The protocol that DDF is using http/https */
  public static final String PROTOCOL = "protocol";

  /** Trust store to use for outgoing DDF connections */
  public static final String TRUST_STORE = "trustStore";

  /** Password associated with the trust store */
  public static final String TRUST_STORE_PASSWORD = "trustStorePassword";

  /** Key store to use for outgoing DDF connections */
  public static final String KEY_STORE = "keyStore";

  /** Password associated with the key store */
  public static final String KEY_STORE_PASSWORD = "keyStorePassword";

  /** The site name for this DDF instance */
  public static final String SITE_NAME = "id";

  /** The version of DDF currently running */
  public static final String VERSION = "version";

  /** The organization that this instance of DDF is running for */
  public static final String ORGANIZATION = "organization";

  /** Site (email) contact */
  public static final String CONTACT = "contact";

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationManager.class);

  // Constants for the read-only DDF system settings
  private static final String DDF_HOME_ENVIRONMENT_VARIABLE = "DDF_HOME";

  private static final String SSL_KEYSTORE_JAVA_PROPERTY = "javax.net.ssl.keyStore";

  private static final String SSL_KEYSTORE_PASSWORD_JAVA_PROPERTY =
      "javax.net.ssl.keyStorePassword";

  private static final String SSL_TRUSTSTORE_JAVA_PROPERTY = "javax.net.ssl.trustStore";

  private static final String SSL_TRUSTSTORE_PASSWORD_JAVA_PROPERTY =
      "javax.net.ssl.trustStorePassword";

  /** List of DdfManagedServices to push the DDF system settings to. */
  protected List<ConfigurationWatcher> services;

  /** The map of DDF system settings, including the read-only settings. */
  protected Map<String, String> configuration;

  private static Map<String, String> propertyMapping = new HashMap<>();

  static {
    propertyMapping.put(PROTOCOL, SystemBaseUrl.PROTOCOL);
    propertyMapping.put(HOST, SystemBaseUrl.HOST);
    propertyMapping.put(PORT, SystemBaseUrl.PORT);
    propertyMapping.put(SITE_NAME, SystemInfo.SITE_NAME);
    propertyMapping.put(CONTACT, SystemInfo.SITE_CONTACT);
    propertyMapping.put(ORGANIZATION, SystemInfo.ORGANIZATION);
    propertyMapping.put(VERSION, SystemInfo.VERSION);
  }

  /**
   * The map of DDF system settings that are read-only, i.e., they are set in OSGi system bundles,
   * not displayed in Admin Console's DDF System Settings configuration, but are pushed out in the
   * configuration settings to ConfigurationWatchers.
   */
  protected Map<String, String> readOnlySettings;

  protected ConfigurationAdmin configurationAdmin;

  /** The initial configuration values from blueprint. */
  private Map<String, String> configurationProperties = new HashMap<>();

  /**
   * Constructs the list of DDF system Settings (read-only and configurable settings) to be pushed
   * to registered ConfigurationWatchers.
   *
   * @param services the list of watchers of changes to the DDF System Settings
   * @param configurationAdmin the OSGi Configuration Admin service handle
   */
  public ConfigurationManager(
      List<ConfigurationWatcher> services, ConfigurationAdmin configurationAdmin) {
    LOGGER.debug("ENTERING: ctor");
    this.services = services;
    this.configurationAdmin = configurationAdmin;

    this.readOnlySettings = new HashMap<>();
    if (System.getenv(DDF_HOME_ENVIRONMENT_VARIABLE) != null) {
      readOnlySettings.put(HOME_DIR, System.getenv(DDF_HOME_ENVIRONMENT_VARIABLE));
    } else {
      readOnlySettings.put(HOME_DIR, System.getProperty("user.dir"));
    }

    // Add the system properties
    configurationProperties.putAll(getSystemProperties());

    readOnlySettings.put(KEY_STORE, System.getProperty(SSL_KEYSTORE_JAVA_PROPERTY));
    readOnlySettings.put(
        KEY_STORE_PASSWORD, System.getProperty(SSL_KEYSTORE_PASSWORD_JAVA_PROPERTY));
    readOnlySettings.put(TRUST_STORE, System.getProperty(SSL_TRUSTSTORE_JAVA_PROPERTY));
    readOnlySettings.put(
        TRUST_STORE_PASSWORD, System.getProperty(SSL_TRUSTSTORE_PASSWORD_JAVA_PROPERTY));

    this.configuration = new HashMap<>();

    // Append the read-only settings to the DDF System Settings so that all
    // settings are pushed to registered listeners
    configuration.putAll(readOnlySettings);

    LOGGER.debug("EXITING: ctor");
  }

  public void setProtocol(String protocol) {
    configurationProperties.put(PROTOCOL, protocol);
    LOGGER.debug("protocol set to {}", protocol);
  }

  public void setHost(String host) {
    configurationProperties.put(HOST, host);
    LOGGER.debug("host set to {}", host);
  }

  public void setPort(String port) {
    configurationProperties.put(PORT, port);
    LOGGER.debug("port set to {}", port);
  }

  public void setId(String id) {
    configurationProperties.put(SITE_NAME, id);
    LOGGER.debug("site name set to {}", id);
  }

  public void setVersion(String version) {
    configurationProperties.put(VERSION, version);
    LOGGER.debug("version set to {}", version);
  }

  public void setOrganization(String organization) {
    configurationProperties.put(ORGANIZATION, organization);
    LOGGER.debug("organization set to {}", organization);
  }

  public void setContact(String contact) {
    configurationProperties.put(CONTACT, contact);
    LOGGER.debug("contact set to {}", contact);
  }

  /** Called once after all managed property setters have been called. */
  public void init() {
    updated(Collections.unmodifiableMap(configurationProperties));
  }

  /**
   * Invoked when the DDF system settings are changed in the Admin Console, this method then pushes
   * those DDF system settings to each of the registered ConfigurationWatchers.
   *
   * @param updatedConfig map of DDF system settings, not including the read-only settings. Can be
   *     null.
   */
  public void updated(Map<String, ?> updatedConfig) {
    String methodName = "updated";
    LOGGER.debug("ENTERING: {}", methodName);

    if (updatedConfig != null && !updatedConfig.isEmpty()) {
      configuration.clear();

      for (Map.Entry<String, ?> entry : updatedConfig.entrySet()) {
        if (entry.getValue() != null) {
          configuration.put(entry.getKey(), entry.getValue().toString());
        }
      }

      // Add the read-only settings to list to be pushed out to watchers
      configuration.putAll(readOnlySettings);
    }

    Map<String, String> readOnlyConfig = Collections.unmodifiableMap(this.configuration);
    for (ConfigurationWatcher service : services) {
      service.configurationUpdateCallback(readOnlyConfig);
    }

    LOGGER.debug("EXITING: {}", methodName);
  }

  /**
   * Invoked when a ConfigurationWatcher first comes online, e.g., when a federated source is
   * configured, this method pushes the DDF system settings to the newly registered (bound)
   * ConfigurationWatcher.
   *
   * @param service
   * @param properties does nothing
   */
  public void bind(ConfigurationWatcher service, @SuppressWarnings("rawtypes") Map properties) {
    String methodName = "bind";
    LOGGER.debug("ENTERING: {}", methodName);

    if (service != null) {
      service.configurationUpdateCallback(Collections.unmodifiableMap(this.configuration));
    }

    LOGGER.debug("EXITING: {}", methodName);
  }

  /** @return OSGi Configuratrion Admin service handle */
  public ConfigurationAdmin getConfigurationAdmin() {
    return configurationAdmin;
  }

  /** @param configurationAdmin */
  public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
    this.configurationAdmin = configurationAdmin;
  }

  /**
   * Retrieves the value of an OSGi bundle's configuration property
   *
   * @param servicePid PID for an OSGi bundle
   * @param propertyName name of the bundle's configuration property to get a value for
   * @return the value of the specified bundle's configuration property
   */
  public String getConfigurationValue(String servicePid, String propertyName) {
    String methodName = "getConfigurationValue";
    LOGGER.debug(
        "ENTERING: {},   servicePid = {},  propertyName = {}",
        methodName,
        servicePid,
        propertyName);

    String value = "";

    try {
      if (this.configurationAdmin != null) {
        Configuration currentConfiguration = this.configurationAdmin.getConfiguration(servicePid);

        if (currentConfiguration != null) {
          Dictionary<String, Object> properties = currentConfiguration.getProperties();

          if (properties != null && properties.get(propertyName) != null) {
            value = (String) properties.get(propertyName);
          } else {
            LOGGER.debug("properties for servicePid = {} were NULL or empty", servicePid);
          }
        } else {
          LOGGER.debug("configuration for servicePid = {} was NULL", servicePid);
        }
      } else {
        LOGGER.debug("configurationAdmin is NULL");
      }
    } catch (IOException e) {
      LOGGER.info("Exception while getting configuration value.", e);
    }

    LOGGER.debug("EXITING: {}    value = [{}]", methodName, value);

    return value;
  }

  private Map<String, String> getSystemProperties() {
    Map<String, String> map = new HashMap<>();
    map.put(HTTP_PORT, SystemBaseUrl.getHttpPort());
    map.put(HOST, SystemBaseUrl.getHost());
    map.put(PROTOCOL, SystemBaseUrl.getProtocol());
    map.put(PORT, SystemBaseUrl.getPort());
    map.put(SITE_NAME, SystemInfo.getSiteName());
    map.put(VERSION, SystemInfo.getVersion());
    map.put(ORGANIZATION, SystemInfo.getOrganization());
    map.put(SERVICES_CONTEXT_ROOT, SystemBaseUrl.getRootContext());
    return map;
  }
}
