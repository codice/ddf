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
package org.codice.ddf.ui.searchui.simple.properties;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Optional;
import org.codice.ddf.branding.BrandingRegistry;
import org.codice.ddf.configuration.DictionaryMap;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Looks up the configuration for the UI and creates an object to represent it*/
public class UiConfigurationPropertiesFactory {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(UiConfigurationPropertiesFactory.class);

  private static final String CONFIG_PID = "ddf.platform.ui.config";

  private static UiConfigurationPropertiesFactory uniqueInstance =
      new UiConfigurationPropertiesFactory();

  private Optional<BrandingRegistry> branding = Optional.empty();

  private ConfigurationAdmin configurationAdmin;

  private UiConfigurationPropertiesFactory() {}

  /**
   * @return a new UiConfigurationProperties object that contains the properties to build out this
   *     UI
   */
  public UiConfigurationProperties getProperties() {
    Dictionary<String, Object> properties;
    try {
      if (configurationAdmin.listConfigurations(String.format("(service.pid=%s)", CONFIG_PID))
          != null) {
        properties = configurationAdmin.getConfiguration(CONFIG_PID, null).getProperties();
      } else {
        properties = new DictionaryMap();
      }
    } catch (IOException | InvalidSyntaxException e) {
      LOGGER.error(
          "Failed to retrieve UI configuration for Simple Search, page may not load with the proper configuration.");
      properties = new DictionaryMap();
    }
    return new UiConfigurationProperties(properties, branding);
  }

  /** @return a unique instance of {@link UiConfigurationPropertiesFactory} */
  public static UiConfigurationPropertiesFactory getInstance() {
    return uniqueInstance;
  }

  public void setBranding(BrandingRegistry branding) {
    this.branding = Optional.ofNullable(branding);
  }

  public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
    this.configurationAdmin = configurationAdmin;
  }
}
