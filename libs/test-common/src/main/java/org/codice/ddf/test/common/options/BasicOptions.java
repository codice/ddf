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
package org.codice.ddf.test.common.options;

import static org.codice.ddf.test.common.options.TestResourcesOptions.getTestResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import org.codice.ddf.test.common.configurators.PortFinder;

/** Base class for common test options logic. */
public abstract class BasicOptions {

  private static final PortFinder PORT_FINDER = new PortFinder();

  public static PortFinder getPortFinder() {
    return PORT_FINDER;
  }

  public static void recordConfiguration(String key, String value) {
    try (OutputStream os =
        Files.newOutputStream(getConfigurationPath(), StandardOpenOption.CREATE)) {
      Properties props = getProperties();
      props.setProperty(key, value);
      props.store(os, null);
    } catch (IOException e) {
      throw new RuntimeException("Failed to save properties to configuration file.", e);
    }
  }

  public static void appendConfiguration(String key, String value) {
    recordConfiguration(key, getProperties().getProperty(key) + "," + value);
  }

  public static String getConfiguration(String key) {
    return getProperties().getProperty(key);
  }

  protected static Properties getProperties() {
    if (!getConfigurationPath().toFile().exists()) {
      return new Properties();
    }

    try (FileInputStream is = new FileInputStream(getConfigurationPath().toFile())) {
      Properties props = new Properties();
      props.load(is);
      return props;

    } catch (Exception e) {
      throw new RuntimeException("Failed to load properties from configuration file.", e);
    }
  }

  protected static Path getConfigurationPath() {
    return Paths.get(getTestResource("/exam.config"));
  }
}
