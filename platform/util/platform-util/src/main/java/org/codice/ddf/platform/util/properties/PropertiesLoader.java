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
package org.codice.ddf.platform.util.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * Utility class that attempts several different methods for loading in properties files from the
 * classpath or file system. The strategies are attempted in the following order:
 *
 * <ol>
 *   <li>Spring (default class loader) uses utilities found in Spring-Core to load properties.
 *   <li>Spring (given class loader) does the same as above, but with respect to the given class
 *       loader.
 *   <li>Direct file system loading; useful when the properties have a fully-qualified absolute
 *       path. Relative paths are also valid, and loading attempts path resolution using (in order):
 *       <ol>
 *         <li>The relative path prepended with the {@code karaf.home} property
 *         <li>The relative path prepended with the {@code ddf.home} property
 *         <li>The relative path itself, without any modification
 *       </ol>
 *   <li>Resource loading using Java's built in resource system
 * </ol>
 *
 * Note that the first successful strategy is the one whose results are non-empty, and no further
 * strategies will be attempted thereafter. Property placeholders are always substituted after the
 * loading has finished.
 *
 * <p>If all strategies fail, then the returned Properties object will be empty.
 */
public final class PropertiesLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesLoader.class);

  private static final PropertiesLoader INSTANCE = new PropertiesLoader();

  private PropertiesLoader() {}

  public static PropertiesLoader getInstance() {
    return INSTANCE;
  }

  /**
   * Converts an instance of Properties to an instance of a Map, which is the preferred API for
   * working with key-value collections.
   *
   * @param properties the properties object whose elements should be in the resultant map
   * @param <K> the object type of the key in the returned map
   * @param <V> the object type of the value in the returned map
   * @return a new map with all elements of the given properties, or empty if the properties were
   *     null
   */
  @SuppressWarnings("unchecked")
  public <K, V> Map<K, V> toMap(Properties properties) {
    if (properties != null) {
      final Set<Map.Entry<Object, Object>> entries = properties.entrySet();
      Map<K, V> map = new HashMap<K, V>(entries.size() * 2);
      for (Map.Entry<Object, Object> entry : entries) {
        map.put((K) entry.getKey(), (V) entry.getValue());
      }

      return map;
    }
    return new HashMap<K, V>();
  }

  /**
   * Load properties from a file with no classloader specified.
   *
   * @param propertiesFile the resource name or the file path of the properties file
   * @return Properties deserialized from the specified file, or empty if the load failed
   */
  public Properties loadProperties(String propertiesFile) {
    return loadProperties(propertiesFile, null);
  }

  /**
   * Will attempt to load properties from a file using the given classloader. If that fails, several
   * other methods will be tried until the properties file is located.
   *
   * @param propertiesFile the resource name or the file path of the properties file
   * @param classLoader the class loader with access to the properties file
   * @return Properties deserialized from the specified file, or empty if the load failed
   */
  public Properties loadProperties(String propertiesFile, ClassLoader classLoader) {
    boolean error = false;
    Properties properties = new Properties();
    if (propertiesFile != null) {
      try {
        LOGGER.debug(
            "Attempting to load properties from {} with Spring PropertiesLoaderUtils.",
            propertiesFile);
        properties = PropertiesLoaderUtils.loadAllProperties(propertiesFile);
      } catch (IOException e) {
        error = true;
        LOGGER.debug("Unable to load properties using default Spring properties loader.", e);
      }
      if (error || properties.isEmpty()) {
        if (classLoader != null) {
          try {
            LOGGER.debug(
                "Attempting to load properties from {} with Spring PropertiesLoaderUtils with class loader.",
                propertiesFile);
            properties = PropertiesLoaderUtils.loadAllProperties(propertiesFile, classLoader);
            error = false;
          } catch (IOException e) {
            error = true;
            LOGGER.debug("Unable to load properties using default Spring properties loader.", e);
          }
        } else {
          try {
            LOGGER.debug(
                "Attempting to load properties from {} with Spring PropertiesLoaderUtils with class loader.",
                propertiesFile);
            properties =
                PropertiesLoaderUtils.loadAllProperties(
                    propertiesFile, PropertiesLoader.class.getClassLoader());
            error = false;
          } catch (IOException e) {
            error = true;
            LOGGER.debug("Unable to load properties using default Spring properties loader.", e);
          }
        }
      }

      if (error || properties.isEmpty()) {
        LOGGER.debug("Attempting to load properties from file system: {}", propertiesFile);
        File propFile = new File(propertiesFile);
        // If properties file has fully-qualified absolute path (which
        // the blueprint file specifies) then can load it directly.
        if (propFile.isAbsolute()) {
          LOGGER.debug("propertiesFile {} is absolute", propertiesFile);
          propFile = new File(propertiesFile);
        } else {
          String rootDirectory = System.getProperty("karaf.home");
          if (rootDirectory != null && !rootDirectory.isEmpty()) {
            propFile = new File(rootDirectory, propertiesFile);
          } else {
            rootDirectory = System.getProperty("ddf.home");
            if (rootDirectory != null && !rootDirectory.isEmpty()) {
              propFile = new File(rootDirectory, propertiesFile);
            } else {
              propFile = new File(propertiesFile);
            }
          }
        }
        properties = new Properties();

        try (InputStreamReader reader =
            new InputStreamReader(new FileInputStream(propFile), StandardCharsets.UTF_8)) {
          properties.load(reader);
        } catch (FileNotFoundException e) {
          error = true;
          LOGGER.debug("Could not find properties file: {}", propFile.getAbsolutePath(), e);
        } catch (IOException e) {
          error = true;
          LOGGER.debug("Error reading properties file: {}", propFile.getAbsolutePath(), e);
        }
      }
      if (error || properties.isEmpty()) {
        LOGGER.debug("Attempting to load properties as a resource: {}", propertiesFile);
        InputStream ins = PropertiesLoader.class.getResourceAsStream(propertiesFile);
        if (ins != null) {
          try {
            properties.load(ins);
            ins.close();
          } catch (IOException e) {
            LOGGER.debug("Unable to load properties: {}", propertiesFile, e);
          } finally {
            IOUtils.closeQuietly(ins);
          }
        }
      }

      // replace any ${prop} with system properties
      Properties filtered = new Properties();
      for (Map.Entry<?, ?> entry : properties.entrySet()) {
        filtered.put(
            StrSubstitutor.replaceSystemProperties(entry.getKey()),
            StrSubstitutor.replaceSystemProperties(entry.getValue()));
      }
      properties = filtered;

    } else {
      LOGGER.debug("Properties file must not be null.");
    }

    return properties;
  }
}
