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

import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
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
 */
public class ConfigurationContextImpl implements ConfigurationContext {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationContextImpl.class);

  // Not worth adding a dependency on felix's fileinstall module just for the constant
  static final String FELIX_FILENAME = "felix.fileinstall.filename";

  // Hidden boolean property that appears on dictionaries after the config is JUST created
  static final String FELIX_NEW_CONFIG = "_felix_.cm.newConfiguration";

  // Property of a special config that tracks all the individual configurations from a factory
  static final String SERVICE_FACTORY_PIDLIST = "factory.pidList";

  private final String servicePid;

  private final File configFile;

  private final Dictionary<String, Object> props;

  private final Object configIsNew;

  private final Object pidList;

  private final int propertyCount;

  ConfigurationContextImpl(Configuration config) {
    this(config.getPid(), config.getProperties());
  }

  ConfigurationContextImpl(String pid, Dictionary<String, Object> props) {
    Dictionary<String, Object> propsCopy = copyDictionary(props);

    propsCopy.remove(SERVICE_PID);
    propsCopy.remove(SERVICE_FACTORYPID);

    this.servicePid = pid;
    this.configFile = createFileFromFelixProp(propsCopy.remove(FELIX_FILENAME));

    this.configIsNew = propsCopy.remove(FELIX_NEW_CONFIG);
    this.pidList = propsCopy.remove(SERVICE_FACTORY_PIDLIST);

    this.propertyCount = propsCopy.size();
    this.props = propsCopy;
  }

  @Override
  public String getServicePid() {
    return servicePid;
  }

  @Override
  public File getConfigFile() {
    return configFile;
  }

  @Override
  public Dictionary<String, Object> getSanitizedProperties() {
    return copyDictionary(props);
  }

  public boolean shouldBeVisibleForProcessing() {
    return servicePid != null && configIsNew == null && pidList == null && propertyCount > 0;
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
