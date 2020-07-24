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
package org.codice.ddf.branding.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.branding.BrandingPlugin;
import org.codice.ddf.branding.BrandingRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrandingRegistryImpl implements BrandingRegistry {
  private static final Logger LOGGER = LoggerFactory.getLogger(BrandingRegistryImpl.class);

  private List<BrandingPlugin> brandingPlugins = Collections.emptyList();

  @Override
  public String getProductName() {
    return getBrandingPlugins().stream()
        .map(plugin -> StringUtils.substringBeforeLast(plugin.getProductName(), " "))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse("DDF");
  }

  @Override
  public String getAttributeFromBranding(BrandingMethod supplier) {
    return getBrandingPlugins().stream()
        .map(
            plugin -> {
              try {
                return supplier.apply(plugin);
              } catch (IOException e) {
                LOGGER.warn("Could not get the requested attribute from the Branding Plugin", e);
                return null;
              }
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse("");
  }

  @Override
  public String getProductVersion() {
    return getBrandingPlugins().stream()
        .map(plugin -> StringUtils.substringAfterLast(plugin.getProductName(), " "))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse("");
  }

  @Override
  public List<BrandingPlugin> getBrandingPlugins() {
    return brandingPlugins;
  }

  @Override
  public void setBrandingPlugins(List<BrandingPlugin> brandingPlugins) {
    this.brandingPlugins = brandingPlugins;
  }
}
