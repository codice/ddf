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

import java.util.Optional;
import net.minidev.json.JSONObject;
import org.codice.ddf.branding.BrandingPlugin;
import org.codice.ddf.branding.BrandingRegistry;
import org.codice.ddf.configuration.service.PlatformUiConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration class for pid=ddf.platform.ui.config.
 *
 * <p>Contains webservice method for returning the current configuration.
 */
public class PlatformUiConfiguration implements PlatformUiConfigurationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PlatformUiConfiguration.class);

  public static final String SYSTEM_USAGE_TITLE_CONFIG_KEY = "systemUsageTitle";

  public static final String SYSTEM_USAGE_MESSAGE_CONFIG_KEY = "systemUsageMessage";

  public static final String SYSTEM_USAGE_ONCE_PER_SESSION_CONFIG_KEY = "systemUsageOncePerSession";

  public static final String HEADER_CONFIG_KEY = "header";

  public static final String FOOTER_CONFIG_KEY = "footer";

  public static final String COLOR_CONFIG_KEY = "color";

  public static final String BACKGROUND_CONFIG_KEY = "background";

  public static final String TITLE_CONFIG_KEY = "title";

  public static final String VERSION_CONFIG_KEY = "version";

  public static final String PRODUCT_IMAGE_CONFIG_KEY = "productImage";

  public static final String FAV_ICON_CONFIG_KEY = "favIcon";

  public static final String VENDOR_IMAGE_CONFIG_KEY = "vendorImage";

  public static final String TIMEOUT_CONFIG_KEY = "timeout";

  private static final int DEFAULT_TIMEOUT_MINUTES = 15;

  private static final int MINIMUM_TIMEOUT_MINUTES = 2;

  private boolean systemUsageEnabled;

  private String systemUsageTitle;

  private String systemUsageMessage;

  private boolean systemUsageOncePerSession;

  private String header;

  private String footer;

  private String color;

  private String background;

  private int timeout = DEFAULT_TIMEOUT_MINUTES;

  private Optional<BrandingRegistry> branding = Optional.empty();

  @Override
  public String getConfigAsJsonString() {
    JSONObject jsonObject = new JSONObject();

    if (systemUsageEnabled) {
      jsonObject.put(SYSTEM_USAGE_TITLE_CONFIG_KEY, systemUsageTitle);
      jsonObject.put(SYSTEM_USAGE_MESSAGE_CONFIG_KEY, systemUsageMessage);
      jsonObject.put(SYSTEM_USAGE_ONCE_PER_SESSION_CONFIG_KEY, systemUsageOncePerSession);
    }

    jsonObject.put(HEADER_CONFIG_KEY, this.header);
    jsonObject.put(FOOTER_CONFIG_KEY, this.footer);
    jsonObject.put(COLOR_CONFIG_KEY, this.color);
    jsonObject.put(BACKGROUND_CONFIG_KEY, this.background);
    jsonObject.put(TITLE_CONFIG_KEY, getTitle());
    jsonObject.put(VERSION_CONFIG_KEY, getVersion());
    jsonObject.put(PRODUCT_IMAGE_CONFIG_KEY, getProductImage());
    jsonObject.put(FAV_ICON_CONFIG_KEY, getFavIcon());
    jsonObject.put(VENDOR_IMAGE_CONFIG_KEY, getVendorImage());
    jsonObject.put(TIMEOUT_CONFIG_KEY, getTimeout());

    return jsonObject.toJSONString();
  }

  private String getVersion() {
    return branding.map(BrandingRegistry::getProductVersion).orElse("");
  }

  private String getTitle() {
    return branding.map(BrandingRegistry::getProductName).orElse("");
  }

  public void setBranding(BrandingRegistry branding) {
    this.branding = Optional.ofNullable(branding);
  }

  public boolean getSystemUsageEnabled() {
    return systemUsageEnabled;
  }

  public void setSystemUsageEnabled(boolean systemUsageEnabled) {
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

  public boolean getSystemUsageOncePerSession() {
    return systemUsageOncePerSession;
  }

  public void setSystemUsageOncePerSession(boolean systemUsageOncePerSession) {
    this.systemUsageOncePerSession = systemUsageOncePerSession;
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

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    if (timeout >= MINIMUM_TIMEOUT_MINUTES) {
      this.timeout = timeout;
    } else {
      LOGGER.warn(
          "Received a timeout of {} minutes, which is less than the minimum timeout allowed of {}. Defaulting to {}.",
          timeout,
          MINIMUM_TIMEOUT_MINUTES,
          DEFAULT_TIMEOUT_MINUTES);
      this.timeout = DEFAULT_TIMEOUT_MINUTES;
    }
  }

  public String getProductImage() {
    return branding
        .map(
            brandingRegistry ->
                brandingRegistry.getAttributeFromBranding(BrandingPlugin::getBase64ProductImage))
        .orElse("");
  }

  public String getFavIcon() {
    return branding
        .map(
            brandingRegistry ->
                brandingRegistry.getAttributeFromBranding(BrandingPlugin::getBase64FavIcon))
        .orElse("");
  }

  public String getVendorImage() {
    return branding
        .map(
            brandingRegistry ->
                brandingRegistry.getAttributeFromBranding(BrandingPlugin::getBase64VendorImage))
        .orElse("");
  }
}
