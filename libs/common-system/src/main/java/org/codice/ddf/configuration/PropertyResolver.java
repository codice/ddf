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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class holds a string potentially containing variables of the format ${system.prop} and handles
 * resolving those variables by trying to replace them with system properties
 */
public class PropertyResolver {
  private static final Logger LOGGER = LoggerFactory.getLogger(PropertyResolver.class);

  private String propertyString;

  public PropertyResolver(String propStr) {
    propertyString = propStr;
  }

  /**
   * Returns the raw string passed in the ctor with any variables it might contain
   *
   * @return
   */
  public String getRawString() {
    return propertyString;
  }

  /**
   * Returns a string with variables replaced by system property values if they exist
   *
   * @return
   */
  public String getResolvedString() {
    return resolveProperties(propertyString);
  }

  /**
   * Returns a string with variables replaced by system property values if they exist
   *
   * @return
   */
  public static String resolveProperties(String str) {
    return StrSubstitutor.replaceSystemProperties(str);
  }

  /**
   * Returns a List with variables replaced by system property values if they exist
   *
   * @return
   */
  public static List<String> resolveProperties(List<String> list) {
    for (final ListIterator<String> i = list.listIterator(); i.hasNext(); ) {
      final String item = StrSubstitutor.replaceSystemProperties(i.next());
      i.set(item);
    }
    return list;
  }

  /**
   * Reads the properties from the given properties locations into a {@link Properties} object and
   * replaces system property values if they exist
   *
   * @param propertiesLocation location of the properties file
   * @return {@link Properties}
   */
  public static Properties resolvePropertiesFromLocation(String propertiesLocation)
      throws IOException {
    Properties properties = new Properties();

    try (InputStream input = new FileInputStream(propertiesLocation)) {
      properties.load(input);
    }
    properties.forEach(
        (key, value) ->
            properties.setProperty(
                (String) key, PropertyResolver.resolveProperties((String) value)));

    return properties;
  }

  /**
   * Returns the resolved string
   *
   * @return
   */
  public String toString() {
    return getResolvedString();
  }
}
