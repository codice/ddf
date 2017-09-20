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
package ddf.security;

import java.util.Map;
import java.util.Properties;

/**
 * Utility class that attempts several different methods for loading in properties files from the
 * classpath or file system.
 *
 * @deprecated Use {@link org.codice.ddf.platform.util.properties.PropertiesLoader} instead.
 */
@Deprecated
public final class PropertiesLoader {
  private PropertiesLoader() {
    // static only!
  }

  /**
   * Convert properties to a Map.
   *
   * @see org.codice.ddf.platform.util.properties.PropertiesLoader#toMap(Properties)
   */
  public static <K, V> Map<K, V> toMap(Properties properties) {
    return org.codice.ddf.platform.util.properties.PropertiesLoader.getInstance().toMap(properties);
  }

  /**
   * Load properties from a file with no classloader specified.
   *
   * @see org.codice.ddf.platform.util.properties.PropertiesLoader#loadProperties(String)
   */
  public static Properties loadProperties(String propertiesFile) {
    return org.codice.ddf.platform.util.properties.PropertiesLoader.getInstance()
        .loadProperties(propertiesFile, null);
  }

  /**
   * Will attempt to load properties from a file using the given classloader. If that fails, several
   * other methods will be tried until the properties file is located.
   *
   * @see org.codice.ddf.platform.util.properties.PropertiesLoader#loadProperties(String,
   *     ClassLoader)
   */
  public static Properties loadProperties(String propertiesFile, ClassLoader classLoader) {
    return org.codice.ddf.platform.util.properties.PropertiesLoader.getInstance()
        .loadProperties(propertiesFile, classLoader);
  }
}
