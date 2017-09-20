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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Useful for scanning a directory for properties files and aggregating the data into maps and
 * lists.
 */
public class PropertiesFileReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesFileReader.class);

  private static final String PROPERTIES_EXTENSION = ".properties";

  /**
   * Deserializes all on-disk properties files in the given directory into a list of maps.
   *
   * @param directoryPath the path to the directory of properties files
   * @return a list of maps holding the values of the properties files in the directory the given
   *     path pointed at, which can be empty if no properties were found.
   */
  public List<Map<String, String>> loadPropertiesFilesInDirectory(String directoryPath) {
    List<Map<String, String>> loadedPropertiesFiles = new ArrayList<>();
    if (!StringUtils.isEmpty(directoryPath)) {
      File directory = new File(directoryPath);
      if (directory.exists()) {
        File[] propertyFiles =
            directory.listFiles((dir, name) -> name.endsWith(PROPERTIES_EXTENSION));
        if (propertyFiles != null) {
          for (File propertyFile : propertyFiles) {
            loadedPropertiesFiles.add(loadPropertiesFile(propertyFile));
          }
        } else {
          LOGGER.debug("Property load target {} was not a directory", directoryPath);
        }
      }
    }
    return loadedPropertiesFiles;
  }

  /**
   * Deserializes an on-disk properties file into a map.
   *
   * @param filePath the path to the file of properties
   * @return a map of the values in the properties file the provided path pointed at, which can be
   *     empty if no properties were found.
   */
  public Map<String, String> loadSinglePropertiesFile(String filePath) {
    Map<String, String> propertyMap = new HashMap<>();
    if (!StringUtils.isEmpty(filePath)) {
      File propertyFile = new File(filePath);
      if (propertyFile.exists()) {
        if (!propertyFile.isDirectory()) {
          propertyMap.putAll(loadPropertiesFile(propertyFile));
        } else {
          LOGGER.debug("Property load target {} was a directory", filePath);
        }
      } else {
        LOGGER.debug("Property load target {} does not exist", filePath);
      }
    }
    return propertyMap;
  }

  /** Helper method for working with the {@link PropertiesLoader}. */
  private Map<String, String> loadPropertiesFile(File propertiesFile) {
    Map<String, String> propertyMap = new HashMap<>();
    if (propertiesFile != null && propertiesFile.exists()) {
      PropertiesLoader loader = PropertiesLoader.getInstance();
      Properties properties = loader.loadProperties(propertiesFile.getAbsolutePath());
      propertyMap = loader.toMap(properties);
    }
    return propertyMap;
  }
}
