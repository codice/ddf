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

import static org.codice.gsonsupport.GsonTypeAdapters.MAP_STRING_TO_OBJECT_TYPE;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import org.apache.commons.io.FileUtils;
import org.apache.felix.utils.properties.Properties;
import org.codice.ddf.admin.core.api.SystemPropertyDetails;
import org.codice.ddf.admin.core.api.jmx.SystemPropertiesAdminMBean;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemPropertiesAdmin extends StandardMBean implements SystemPropertiesAdminMBean {

  public static final String HTTP_PROTOCOL = "http://";
  public static final String HTTPS_PROTOCOL = "https://";
  private static final String DEFAULT_LOCALHOST_DN = "localhost.local";
  private static final String KARAF_ETC = "karaf.etc";
  private static final String LOCAL_HOST = "localhost";
  private static final String SYSTEM_PROPERTIES_FILE = "custom.system.properties";
  private static final String USERS_PROPERTIES_FILE = "users.properties";
  private static final String USERS_ATTRIBUTES_FILE = "users.attributes";
  private static final String EXTERNAL_HOST_TITLE = "External Host";
  private static final String INTERNAL_HOST_TITLE = "Internal Host";
  private static final String INTERNAL_HOST_DESCRIPTION =
      "The hostname or IP address this system runs on. NOTE: This setting will take effect after a system restart.";
  private static final String EXTERNAL_HOST_DESCRIPTION =
      "The host name or IP address used to advertise the system. Possibilities include the address of a single node of that of a load balancer in a multi-node deployment. NOTE: This setting will take effect after a system restart.";
  private static final ArrayList<String> PROTOCOL_OPTIONS = new ArrayList<>();
  private static final String EXTERNAL_HTTP_PORT_TITLE = "External HTTP Port";
  private static final String INTERNAL_HTTP_PORT_TITLE = "Internal HTTP Port";
  private static final String EXTERNAL_HTTP_PORT_DESCRIPTION =
      "The port used to advertise the system. Possibilities include the port of a single node of that of a load balancer in a multi-node deployment. NOTE: This setting will take effect after a system restart.";
  private static final String INTERNAL_HTTP_PORT_DESCRIPTION =
      "The http port that the system uses. NOTE: This *DOES* change the port the system runs on.";
  private static final String EXTERNAL_HTTPS_PORT_TITLE = "External HTTPS Port";
  private static final String INTERNAL_HTTPS_PORT_TITLE = "Internal HTTPS Port";
  private static final String EXTERNAL_HTTPS_PORT_DESCRIPTION =
      "The secure port used to advertise the system. Possibilities include the port of a single node of that of a load balancer in a multi-node deployment. NOTE: This setting will take effect after a system restart.";
  private static final String INTERNAL_HTTPS_PORT_DESCRIPTION =
      "The https port that the system uses. NOTE: This *DOES* change the port the system runs on.";
  private static final String ORGANIZATION_TITLE = "Organization";
  private static final String ORGANIZATION_DESCRIPTION =
      "The name of the organization that runs this instance.";
  private static final String SITE_CONTACT_TITLE = "Site Contact";
  private static final String SITE_CONTACT_DESCRIPTION = "The email address of the site contact.";
  private static final String SITE_NAME_TITLE = "Site Name";
  private static final String SITE_NAME_DESCRIPTION =
      "The unique name of this instance. This name will be provided via web services that ask for the name.";
  private static final String VERSION_TITLE = "Version";
  private static final String VERSION_DESCRIPTION = "The version of this instance.";
  private static final String LOCALHOST_DATA_MANAGER = "localhost-data-manager";
  private static final String DATA_MANAGER = "data-manager";

  private String etcDir = System.getProperty(KARAF_ETC);
  private String systemPropertyFilename = etcDir + File.separator + SYSTEM_PROPERTIES_FILE;
  private String userPropertiesFilename = etcDir + File.separator + USERS_PROPERTIES_FILE;
  private String userAttributesFilename = etcDir + File.separator + USERS_ATTRIBUTES_FILE;

  private File systemPropertiesFile = new File(systemPropertyFilename);
  private File userPropertiesFile = new File(userPropertiesFilename);
  private File userAttributesFile = new File(userAttributesFilename);

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .setPrettyPrinting()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .create();

  private static final Logger LOGGER = LoggerFactory.getLogger(SystemPropertiesAdmin.class);

  static {
    PROTOCOL_OPTIONS.add(HTTPS_PROTOCOL);
    PROTOCOL_OPTIONS.add(HTTP_PROTOCOL);
  }

  private MBeanServer mbeanServer;
  private ObjectName objectName;
  private String oldHostName = SystemBaseUrl.INTERNAL.getHost();
  private GuestClaimsHandlerExt guestClaimsHandlerExt;

  public SystemPropertiesAdmin(GuestClaimsHandlerExt guestClaimsHandlerExt)
      throws NotCompliantMBeanException {
    super(SystemPropertiesAdminMBean.class);
    this.guestClaimsHandlerExt = guestClaimsHandlerExt;
    configureMBean();
  }

  @Override
  public List<SystemPropertyDetails> readSystemProperties() {
    LOGGER.debug("get system properties");

    ArrayList<SystemPropertyDetails> properties = new ArrayList<>();

    Properties systemDotProperties = null;
    try {
      systemDotProperties = new Properties(systemPropertiesFile);
      properties.add(
          getSystemPropertyDetails(
              SystemBaseUrl.EXTERNAL_HOST,
              EXTERNAL_HOST_TITLE,
              EXTERNAL_HOST_DESCRIPTION,
              null,
              systemDotProperties));
      properties.add(
          getSystemPropertyDetails(
              SystemBaseUrl.EXTERNAL_HTTP_PORT,
              EXTERNAL_HTTP_PORT_TITLE,
              EXTERNAL_HTTP_PORT_DESCRIPTION,
              null,
              systemDotProperties));
      properties.add(
          getSystemPropertyDetails(
              SystemBaseUrl.EXTERNAL_HTTPS_PORT,
              EXTERNAL_HTTPS_PORT_TITLE,
              EXTERNAL_HTTPS_PORT_DESCRIPTION,
              null,
              systemDotProperties));
      properties.add(
          getSystemPropertyDetails(
              SystemBaseUrl.INTERNAL_HOST,
              INTERNAL_HOST_TITLE,
              INTERNAL_HOST_DESCRIPTION,
              null,
              systemDotProperties));
      properties.add(
          getSystemPropertyDetails(
              SystemBaseUrl.INTERNAL_HTTP_PORT,
              INTERNAL_HTTP_PORT_TITLE,
              INTERNAL_HTTP_PORT_DESCRIPTION,
              null,
              systemDotProperties));
      properties.add(
          getSystemPropertyDetails(
              SystemBaseUrl.INTERNAL_HTTPS_PORT,
              INTERNAL_HTTPS_PORT_TITLE,
              INTERNAL_HTTPS_PORT_DESCRIPTION,
              null,
              systemDotProperties));
      properties.add(
          getSystemPropertyDetails(
              SystemInfo.ORGANIZATION,
              ORGANIZATION_TITLE,
              ORGANIZATION_DESCRIPTION,
              null,
              systemDotProperties));
      properties.add(
          getSystemPropertyDetails(
              SystemInfo.SITE_CONTACT,
              SITE_CONTACT_TITLE,
              SITE_CONTACT_DESCRIPTION,
              null,
              systemDotProperties));
      properties.add(
          getSystemPropertyDetails(
              SystemInfo.SITE_NAME,
              SITE_NAME_TITLE,
              SITE_NAME_DESCRIPTION,
              null,
              systemDotProperties));
      properties.add(
          getSystemPropertyDetails(
              SystemInfo.VERSION, VERSION_TITLE, VERSION_DESCRIPTION, null, systemDotProperties));
    } catch (IOException e) {
      LOGGER.warn("Exception while reading the system.properties file.", e);
    }

    return properties;
  }

  @Override
  public void writeSystemProperties(Map<String, String> updatedSystemProperties) {
    if (updatedSystemProperties == null) {
      return;
    }

    Properties systemDotProperties;
    try {
      systemDotProperties = new Properties(systemPropertiesFile);

    } catch (IOException e) {
      LOGGER.warn("Exception reading system.properties file.", e);
      return;
    }

    // save off the current/old hostname before we make any changes
    oldHostName = systemDotProperties.getProperty(SystemBaseUrl.INTERNAL_HOST);
    updatedSystemProperties.forEach(
        (key, value) -> {
          // Clears out the property value before setting it
          //
          // We have to do this because when we read in the properties, the values are
          // expanded which can lead to a state where the value is erroneously not updated
          // after being checked.
          systemDotProperties.put(key, "");
          systemDotProperties.put(key, value);
        });

    try {
      systemDotProperties.save();
    } catch (IOException e) {
      LOGGER.warn("Exception writing to system.properties file.", e);
    }

    writeOutUsersDotPropertiesFile(userPropertiesFile);
    writeOutUsersDotAttributesFile(userAttributesFile);
  }

  /*
   * Writes user property data to the relevant file after replacing the default hostname where
   * necessary.
   */
  private void writeOutUsersDotPropertiesFile(File userPropertiesFile) {
    try {
      Properties usersDotProperties = new Properties(userPropertiesFile);

      if (!usersDotProperties.isEmpty()) {
        String oldHostValue = usersDotProperties.getProperty(oldHostName);

        if (oldHostValue != null) {
          Properties systemDotProperties = new Properties(systemPropertiesFile);
          String newInternalHost = systemDotProperties.getProperty(SystemBaseUrl.INTERNAL_HOST);
          String newHostValue =
              oldHostValue.replaceAll(
                  LOCALHOST_DATA_MANAGER, String.format("%s-%s", newInternalHost, DATA_MANAGER));
          usersDotProperties.remove(oldHostName);
          usersDotProperties.setProperty(newInternalHost, newHostValue);
          usersDotProperties.save();
        }
      }
    } catch (IOException e) {
      LOGGER.warn("Exception while writing to users.properties file.", e);
    }
  }

  /*
   * Writes security and claims data for the system-level user and default admin.
   */
  private void writeOutUsersDotAttributesFile(File userAttributesFile) {
    Map<String, Object> json = null;
    try (BufferedReader br = Files.newBufferedReader(Paths.get(userAttributesFile.toURI()))) {
      json = GSON.fromJson(br, MAP_STRING_TO_OBJECT_TYPE);
    } catch (IOException e) {
      LOGGER.warn("Unable to read system user attribute file for hostname update.", e);
      return;
    }

    addGuestClaimsProfileAttributes(json);

    if (json.containsKey(oldHostName)) {
      Properties systemDotProperties = null;
      try {
        systemDotProperties = new Properties(systemPropertiesFile);
        json.put(systemDotProperties.get(SystemBaseUrl.INTERNAL_HOST), json.remove(oldHostName));
      } catch (IOException e) {
        LOGGER.warn("Exception while reading the system.properties file.", e);
      }
    }

    try {
      for (Map.Entry<String, Object> entry : json.entrySet()) {
        json.put(entry.getKey(), replaceLocalhost(entry.getValue()));
      }
      FileUtils.writeStringToFile(userAttributesFile, GSON.toJson(json), Charset.defaultCharset());
    } catch (IOException e) {
      LOGGER.warn("Unable to write user attribute file for system update.", e);
    }
  }

  /** Overwrite attributes with those from GuestClaimsHandlerExt so system high is set. */
  private void addGuestClaimsProfileAttributes(Map<String, Object> json) {
    Map<String, Object> selectedProfileAttributes = guestClaimsHandlerExt.getProfileSystemClaims();
    if (selectedProfileAttributes != null) {
      Map<String, Object> localhost = (Map<String, Object>) json.get(LOCAL_HOST);
      if (localhost != null) {
        localhost.putAll(selectedProfileAttributes);
      }
    }
  }

  /*
   * Replace any instances of SystemPropertiesAdmin#DEFAULT_LOCALHOST_DN with the actual
   * hostname.
   */
  @SuppressWarnings("unchecked")
  private Object replaceLocalhost(Object hostMap) throws IOException {
    Properties systemDotProperties = new Properties(systemPropertiesFile);

    if (!(hostMap instanceof Map)) {
      return hostMap;
    }

    Map<String, Object> map;
    try {
      map = (Map<String, Object>) hostMap;
    } catch (ClassCastException e) {
      return hostMap;
    }

    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (!(entry.getValue() instanceof String)) {
        continue;
      }
      String val = (String) entry.getValue();
      if (val.contains(DEFAULT_LOCALHOST_DN)) {
        map.put(
            entry.getKey(),
            val.replace(
                DEFAULT_LOCALHOST_DN,
                systemDotProperties.getProperty(SystemBaseUrl.INTERNAL_HOST)));
      }
    }
    return map;
  }

  private SystemPropertyDetails getSystemPropertyDetails(
      String key, String title, String description, List<String> options, Properties properties) {
    String property = properties.getProperty(key);
    return new SystemPropertyDetailsImpl(title, description, options, key, property);
  }

  private void configureMBean() {
    mbeanServer = ManagementFactory.getPlatformMBeanServer();

    try {
      objectName = new ObjectName(SystemPropertiesAdminMBean.OBJECT_NAME);
    } catch (MalformedObjectNameException e) {
      LOGGER.debug(
          "Exception while creating object name: " + SystemPropertiesAdminMBean.OBJECT_NAME, e);
    }

    try {
      registerSystemPropertiesAdminMBean();
    } catch (Exception e) {
      LOGGER.info("Could not register mbean.", e);
    }
  }

  private void registerSystemPropertiesAdminMBean()
      throws MBeanRegistrationException, NotCompliantMBeanException, InstanceNotFoundException,
          InstanceAlreadyExistsException {
    try {
      mbeanServer.registerMBean(
          new StandardMBean(this, SystemPropertiesAdminMBean.class), objectName);
    } catch (InstanceAlreadyExistsException e) {
      mbeanServer.unregisterMBean(objectName);
      mbeanServer.registerMBean(
          new StandardMBean(this, SystemPropertiesAdminMBean.class), objectName);
    }
  }

  public void shutdown() throws MBeanRegistrationException {
    try {
      if (objectName != null && mbeanServer != null) {
        mbeanServer.unregisterMBean(objectName);
      }
    } catch (InstanceNotFoundException | MBeanRegistrationException e) {
      throw new MBeanRegistrationException(e, "Exception unregistering mbean");
    }
  }
}
