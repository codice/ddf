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
package org.codice.felix.cm.file;

import static java.lang.String.format;
import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Set;
import org.codice.felix.cm.internal.ConfigurationContext;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@inheritDoc}
 *
 * <p>Helper class for enabling functional constructs with {@link Configuration}s and exposes the
 * felix.fileinstall.filename property, if it exists, as a {@link File}.
 *
 * <p>Reasons a {@link #getConfigFile()} call would return null:
 *
 * <ul>
 *   <li>There was no dictionary of properties associated with the config
 *   <li>There was no felix.fileinstall.filename property
 *   <li>The felix.fileinstall.filename property was invalid
 * </ul>
 *
 * <b>See FELIX-4005 & FELIX-4556. This class cannot utilize Java 8 language constructs due to maven
 * bundle plugin 2.3.7</b>
 */
public class ConfigurationContextImpl implements ConfigurationContext {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationContextImpl.class);

  // Not worth adding a dependency on felix's fileinstall module just for the constant
  static final String FELIX_FILENAME = "felix.fileinstall.filename";

  // Hidden boolean property that appears on dictionaries after the config is JUST created
  static final String FELIX_NEW_CONFIG = "_felix_.cm.newConfiguration";

  // Property of a special config that tracks all the individual configurations from a factory
  static final String SERVICE_FACTORY_PIDLIST = "factory.pidList";

  // Property that keeps track of the revision of the configuration
  static final String PROPERTY_REVISION = ":org.apache.felix.configadmin.revision:";

  private static final Set<String> SPECIAL_PROPERTIES =
      new HashSet<>(
          Arrays.asList(
              SERVICE_PID,
              SERVICE_FACTORYPID,
              FELIX_FILENAME,
              FELIX_NEW_CONFIG,
              SERVICE_FACTORY_PIDLIST,
              PROPERTY_REVISION));

  private final String servicePid;

  private final String factoryPid;

  private final File configFile;

  private final Dictionary<String, Object> originalProperties;

  private final Dictionary<String, Object> sanitizedProperties;

  private final Object configIsNew;

  private final Object pidList;

  private final int propertyCount;

  ConfigurationContextImpl(Configuration config) {
    this(config.getPid(), config.getProperties());
  }

  ConfigurationContextImpl(String pid, Dictionary<String, Object> props) {
    this.originalProperties = props;

    Dictionary<String, Object> propsCopy = copyDictionary(props);

    // No guarantee these are in the props dictionary so do not assign from the removal
    propsCopy.remove(SERVICE_PID);
    propsCopy.remove(SERVICE_FACTORYPID);
    propsCopy.remove(PROPERTY_REVISION);

    this.servicePid = pid;
    this.factoryPid = parseFactoryPid(pid);
    this.configFile = createFileFromFelixProp(propsCopy.remove(FELIX_FILENAME));

    this.configIsNew = propsCopy.remove(FELIX_NEW_CONFIG);
    this.pidList = propsCopy.remove(SERVICE_FACTORY_PIDLIST);

    this.propertyCount = propsCopy.size();
    this.sanitizedProperties = propsCopy;
  }

  @Override
  public String getServicePid() {
    return servicePid;
  }

  @Override
  public String getFactoryPid() {
    return factoryPid;
  }

  @Override
  public File getConfigFile() {
    return configFile;
  }

  @Override
  public Dictionary<String, Object> getSanitizedProperties() {
    return copyDictionary(sanitizedProperties);
  }

  @Override
  public void setProperty(String key, Object value) {
    if (key == null || value == null || key.isEmpty()) {
      throw new IllegalArgumentException(
          format("Property parameters were not valid, key = [%s] and value = [%s]", key, value));
    }
    originalProperties.put(key, value);
    if (!SPECIAL_PROPERTIES.contains(key)) {
      sanitizedProperties.put(key, value);
    }
  }

  public boolean shouldBeVisibleToPlugins() {
    return servicePid != null && configIsNew == null && pidList == null && propertyCount > 0;
  }

  // Contexts can be used in a set
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ConfigurationContextImpl context = (ConfigurationContextImpl) o;

    return servicePid != null ? servicePid.equals(context.servicePid) : context.servicePid == null;
  }

  // Contexts can be used in a set
  @Override
  public int hashCode() {
    return Objects.hash(servicePid);
  }

  // Config files in etc may delimit on the '-' but in memory it's always last '.'
  private static String parseFactoryPid(String pid) {
    if (pid != null && pid.contains("-")) {
      return pid.substring(0, pid.lastIndexOf('.'));
    }
    return null;
  }

  /**
   * Code adopted from Karaf's Config Repository Impl:
   * https://github.com/apache/karaf/blob/karaf-4.1.2/config/src/main/java/org/apache/karaf/
   * config/core/impl/ConfigRepositoryImpl.java#L100-L109
   */
  private static File createFileFromFelixProp(Object felixConfigFileName) {
    if (felixConfigFileName == null) {
      return null;
    }
    try {
      if (felixConfigFileName instanceof URL) {
        return new File(((URL) felixConfigFileName).toURI());
      }
      if (felixConfigFileName instanceof URI) {
        return new File((URI) felixConfigFileName);
      }
      if (felixConfigFileName instanceof String) {
        return new File(new URL((String) felixConfigFileName).toURI());
      }
    } catch (URISyntaxException | MalformedURLException e) {
      LOGGER.debug(
          "Was expecting a correctly formatted URL or URI for felix file name [{}], but got: {}",
          felixConfigFileName,
          e);
      return null;
    }
    LOGGER.debug("Unexpected type for felix file name: {}", felixConfigFileName.getClass());
    return null;
  }

  @SuppressWarnings("squid:S1149" /* we are API bound on Dictionary - Felix uses them everywhere */)
  private static Dictionary<String, Object> copyDictionary(Dictionary<String, Object> original) {
    Dictionary<String, Object> copy = new Hashtable<>();
    Enumeration<String> keys = original.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      copy.put(key, original.get(key));
    }
    return copy;
  }
}
