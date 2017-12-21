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

import java.util.Optional;
import org.codice.ddf.branding.BrandingRegistry;

/** Stores external configuration properties. */
public class ConfigurationStore {

  private static ConfigurationStore uniqueInstance;

  private String header = "";

  private String footer = "";

  private String color = "";

  private String background = "";

  private Boolean systemUsageEnabled = false;

  private String systemUsageTitle = "";

  private String systemUsageMessage = "";

  private Optional<BrandingRegistry> branding = Optional.empty();

  private ConfigurationStore() {
    header = "";
    footer = "";
    color = "";
    background = "";
    systemUsageEnabled = false;
    systemUsageTitle = "";
    systemUsageMessage = "";
  }

  /** @return a unique instance of {@link ConfigurationStore} */
  public static synchronized ConfigurationStore getInstance() {

    if (uniqueInstance == null) {
      uniqueInstance = new ConfigurationStore();
    }

    return uniqueInstance;
  }

  public String getHeader() {
    return header;
  }

  public void setHeader(String header) {
    this.header = header;
  }

  public String getFooter() {
    return footer;
  }

  public void setFooter(String footer) {
    this.footer = footer;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public String getBackground() {
    return background;
  }

  public void setBackground(String background) {
    this.background = background;
  }

  public Boolean getSystemUsageEnabled() {
    return systemUsageEnabled;
  }

  public void setSystemUsageEnabled(Boolean systemUsageEnabled) {
    this.systemUsageEnabled = systemUsageEnabled;
  }

  public String getSystemUsageTitle() {
    return systemUsageTitle;
  }

  public void setSystemUsageTitle(String systemUsageTitle) {
    this.systemUsageTitle = systemUsageTitle;
  }

  public String getSystemUsageMessage() {
    return systemUsageMessage;
  }

  public void setSystemUsageMessage(String systemUsageMessage) {
    this.systemUsageMessage = systemUsageMessage;
  }

  public String getProductName() {
    return branding.map(BrandingRegistry::getProductName).orElse("");
  }

  public BrandingRegistry getBranding() {
    return branding.orElse(null);
  }

  public void setBranding(BrandingRegistry branding) {
    this.branding = Optional.ofNullable(branding);
  }

  public Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }
}
