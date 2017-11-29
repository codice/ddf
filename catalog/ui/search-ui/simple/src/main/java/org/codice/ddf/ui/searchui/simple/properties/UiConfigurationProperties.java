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
import java.util.Hashtable;
import java.util.Optional;
import org.codice.ddf.branding.BrandingRegistry;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UiConfigurationProperties {

  private static final Logger LOGGER = LoggerFactory.getLogger(UiConfigurationProperties.class);

  private Dictionary<String, Object> properties;

  private Optional<BrandingRegistry> branding = Optional.empty();

  UiConfigurationProperties(
      ConfigurationAdmin configurationAdmin,
      String configPid,
      Optional<BrandingRegistry> brandingRegistry) {
    this.branding = brandingRegistry;
    try {
      if (configurationAdmin.listConfigurations(String.format("(service.pid=%s)", configPid))
          != null) {
        properties = configurationAdmin.getConfiguration(configPid, null).getProperties();
      } else {
        properties = new Hashtable<>();
      }
    } catch (IOException | InvalidSyntaxException e) {
      LOGGER.error(
          "Failed to retrieve UI configuration for Simple Search, page may not load with the proper configuration.");
      properties = new Hashtable<>();
    }
  }

  private String getStringProperty(String string) {
    String val = (String) properties.get(string);
    return (val != null) ? val : "";
  }

  private Boolean getBoolProperty(String string) {
    Boolean val = (Boolean) properties.get(string);
    return (val != null) ? val : false;
  }

  public String getProductName() {
    return branding.map(BrandingRegistry::getProductName).orElse("");
  }

  public BrandingRegistry getBranding() {
    return branding.orElse(null);
  }

  public String getHeader() {
    return getStringProperty("header");
  }

  public String getFooter() {
    return getStringProperty("footer");
  }

  public String getColor() {
    return getStringProperty("color");
  }

  public String getBackground() {
    return getStringProperty("background");
  }

  public String getSystemUsageTitle() {
    return getStringProperty("systemUsageTitle");
  }

  public String getSystemUsageMessage() {
    return getStringProperty("systemUsageMessage");
  }

  public Boolean getSystemUsageEnabled() {
    return getBoolProperty("systemUsageEnabled");
  }

  public Boolean getSystemUsageOncePerSession() {
    return getBoolProperty("systemUsageOncePerSession");
  }
}
