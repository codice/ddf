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

import java.util.Dictionary;
import java.util.Optional;
import org.codice.ddf.branding.BrandingRegistry;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/* Contains getters and setters for the UI configuration for building Simple Search UI  */
public class UiConfigurationProperties {

  private Dictionary<String, Object> properties;

  private Optional<BrandingRegistry> branding = Optional.empty();

  UiConfigurationProperties(
      Dictionary<String, Object> properties, Optional<BrandingRegistry> branding) {
    this.properties = properties;
    this.branding = branding;
  }

  public String getProductName() {
    return branding.map(BrandingRegistry::getProductName).orElse("");
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
    return getBooleanProperty("systemUsageEnabled");
  }

  public Boolean getSystemUsageOncePerSession() {
    return getBooleanProperty("systemUsageOncePerSession");
  }

  private String getStringProperty(String key) {
    String val = "";
    if (properties.get(key) instanceof String) {
      val = (String) properties.get(key);
    }
    return Jsoup.clean(val, Safelist.relaxed());
  }

  private Boolean getBooleanProperty(String key) {
    Boolean val = false;
    if (properties.get(key) instanceof Boolean) {
      val = (Boolean) properties.get(key);
    }
    return val;
  }
}
