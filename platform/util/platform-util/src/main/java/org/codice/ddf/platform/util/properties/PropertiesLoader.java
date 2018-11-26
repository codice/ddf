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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiFunction;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.codice.ddf.configuration.AbsolutePathResolver;
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

  private static final List<BiFunction<String, ClassLoader, Properties>>
      PROPERTY_LOADING_STRATEGIES =
          ImmutableList.of(
              PropertiesLoader::attemptLoadWithSpring,
              PropertiesLoader::attemptLoadWithSpringAndClassLoader,
              PropertiesLoader::attemptLoadWithFileSystem,
              PropertiesLoader::attemptLoadAsResource);

  private PropertiesLoader() {
    // Perform operations using the singleton instance.
  }

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
      Map<K, V> map = new HashMap<>(entries.size() * 2);
      for (Map.Entry<Object, Object> entry : entries) {
        map.put((K) entry.getKey(), (V) entry.getValue());
      }

      return map;
    }
    return Collections.emptyMap();
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
    Properties properties = new Properties();
    if (propertiesFile == null) {
      LOGGER.debug("Properties file must not be null.");
      return properties;
    }

    Iterator<BiFunction<String, ClassLoader, Properties>> strategiesIterator =
        PROPERTY_LOADING_STRATEGIES.iterator();
    do {
      properties = strategiesIterator.next().apply(propertiesFile, classLoader);
    } while (properties.isEmpty() && strategiesIterator.hasNext());

    properties = substituteSystemPropertyPlaceholders(properties);
    return properties;
  }

  /**
   * Will attempt to load properties from a file using the given classloader without replacing the
   * system properties with their value. If that fails, several other methods will be tried until
   * the properties file is located.
   *
   * @param propertiesFile the resource name or the file path of the properties file
   * @param classLoader the class loader with access to the properties file
   * @return Properties deserialized from the specified file, or empty if the load failed
   */
  public Properties loadPropertiesWithoutSystemPropertySubstitution(
      String propertiesFile, ClassLoader classLoader) {

    Properties properties = new Properties();
    if (propertiesFile == null) {
      LOGGER.debug("Properties file must not be null.");
      return properties;
    }
    Iterator<BiFunction<String, ClassLoader, Properties>> strategiesIterator =
        PROPERTY_LOADING_STRATEGIES.iterator();
    do {
      properties = strategiesIterator.next().apply(propertiesFile, classLoader);
    } while (properties.isEmpty() && strategiesIterator.hasNext());
    return properties;
  }

  /** Default property loading strategy. */
  @SuppressWarnings("squid:S1172" /* Used in bi-function */)
  @VisibleForTesting
  static Properties attemptLoadWithSpring(String propertiesFile, ClassLoader classLoader) {
    Properties properties = new Properties();
    try {
      LOGGER.debug(
          "Attempting to load properties from {} with Spring PropertiesLoaderUtils.",
          propertiesFile);
      properties = PropertiesLoaderUtils.loadAllProperties(propertiesFile);
    } catch (IOException e) {
      LOGGER.debug("Unable to load properties using default Spring properties loader.", e);
    }
    return properties;
  }

  /** Try loading properties using Spring and a provided class loader. */
  @VisibleForTesting
  static Properties attemptLoadWithSpringAndClassLoader(
      String propertiesFile, ClassLoader classLoader) {
    Properties properties = new Properties();
    try {
      LOGGER.debug(
          "Attempting to load properties from {} with Spring PropertiesLoaderUtils with class loader.",
          propertiesFile);
      if (classLoader != null) {
        properties = PropertiesLoaderUtils.loadAllProperties(propertiesFile, classLoader);
      } else {
        properties =
            PropertiesLoaderUtils.loadAllProperties(
                propertiesFile, PropertiesLoader.class.getClassLoader());
      }
    } catch (IOException e) {
      LOGGER.debug("Unable to load properties using default Spring properties loader.", e);
    }
    return properties;
  }

  /**
   * Try loading the properties directly from the file system. If the properties file has a
   * fully-qualified absolute path (which is what the blueprint file should specify) then it can be
   * loaded directly, using this method. Otherwise the path will be considered relative and attempts
   * will be made with {@code karaf.home} and {@code ddf.home} property values prepended to the
   * original path.
   */
  @SuppressWarnings("squid:S1172" /* Used in bi-function */)
  @VisibleForTesting
  static Properties attemptLoadWithFileSystem(String propertiesFile, ClassLoader classLoader) {
    LOGGER.debug("Attempting to load properties from file system: {}", propertiesFile);
    Properties properties = new Properties();

    String karafHome = System.getProperty("karaf.home");
    String ddfHome = System.getProperty("ddf.home");

    File propFile;
    AbsolutePathResolver absPath = new AbsolutePathResolver(propertiesFile);
    if (StringUtils.isNotBlank(karafHome)) {
      propFile = new File(absPath.getPath(karafHome));
    } else if (StringUtils.isNotBlank(ddfHome)) {
      propFile = new File(absPath.getPath(ddfHome));
    } else {
      propFile = new File(propertiesFile);
    }

    if (propFile.exists()) {
      try (InputStreamReader reader =
          new InputStreamReader(new FileInputStream(propFile), StandardCharsets.UTF_8)) {
        properties.load(reader);
      } catch (FileNotFoundException e) {
        LOGGER.debug("Could not find properties file: {}", propFile.getAbsolutePath(), e);
      } catch (IOException e) {
        LOGGER.debug("Error reading properties file: {}", propFile.getAbsolutePath(), e);
        properties.clear();
      }
    } else {
      LOGGER.debug("Could not find properties file: {}", propFile.getAbsolutePath());
    }

    return properties;
  }

  /** Try loading the properties using Java's resource loading facilities. */
  @SuppressWarnings("squid:S1172" /* Used in bi-function */)
  @VisibleForTesting
  static Properties attemptLoadAsResource(String propertiesFile, ClassLoader classLoader) {
    LOGGER.debug("Attempting to load properties as a resource: {}", propertiesFile);
    InputStream ins = PropertiesLoader.class.getResourceAsStream(propertiesFile);
    Properties properties = new Properties();
    if (ins != null) {
      try {
        properties.load(ins);
      } catch (IOException e) {
        LOGGER.debug("Unable to load properties: {}", propertiesFile, e);
      } finally {
        IOUtils.closeQuietly(ins);
      }
    }
    return properties;
  }

  /**
   * Replace any ${prop} with system properties.
   *
   * @param props current state of properties that contain placeholders of the form ${property}.
   * @return the given property object with system property placeholders switched to their actual
   *     values.
   */
  @VisibleForTesting
  static Properties substituteSystemPropertyPlaceholders(Properties props) {
    Properties filtered = new Properties();
    for (Map.Entry<?, ?> entry : props.entrySet()) {
      filtered.put(
          StrSubstitutor.replaceSystemProperties(entry.getKey()),
          StrSubstitutor.replaceSystemProperties(entry.getValue()));
      StrSubstitutor.replaceSystemProperties(new Object());
    }
    return filtered;
  }
}
