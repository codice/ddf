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
import org.osgi.service.cm.ConfigurationAdmin;

/** Stores external configuration properties. */
public class UiConfigurationPropertiesFactory {

  private static final String CONFIG_PID = "ddf.platform.ui.config";

  private static UiConfigurationPropertiesFactory uniqueInstance;

  private Optional<BrandingRegistry> branding = Optional.empty();

  private ConfigurationAdmin configurationAdmin;

  private UiConfigurationPropertiesFactory() {}

  /**
   * @return a new UiConfigurationProperties object that contains the properties to build out this
   *     UI
   */
  public UiConfigurationProperties getProperties() {
    return new UiConfigurationProperties(configurationAdmin, CONFIG_PID, branding);
  }

  /** @return a unique instance of {@link UiConfigurationPropertiesFactory} */
  public static synchronized UiConfigurationPropertiesFactory getInstance() {

    if (uniqueInstance == null) {
      uniqueInstance = new UiConfigurationPropertiesFactory();
    }

    return uniqueInstance;
  }

  public Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }

  public void setBranding(BrandingRegistry branding) {
    this.branding = Optional.ofNullable(branding);
  }

  public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
    this.configurationAdmin = configurationAdmin;
  }
}
