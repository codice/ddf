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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import net.minidev.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.branding.BrandingPlugin;
import org.codice.ddf.branding.BrandingRegistry;
import sun.misc.BASE64Decoder;

/**
 * Configuration class for pid=ddf.platform.ui.config.
 *
 * <p>Contains webservice method for returning the current configuration.
 */
@Path("/")
public class PlatformUiConfiguration {

  public static final String SYSTEM_USAGE_TITLE = "systemUsageTitle";

  public static final String SYSTEM_USAGE_MESSAGE = "systemUsageMessage";

  public static final String SYSTEM_USAGE_ONCE_PER_SESSION = "systemUsageOncePerSession";

  public static final String HEADER = "header";

  public static final String FOOTER = "footer";

  public static final String COLOR = "color";

  public static final String BACKGROUND = "background";

  public static final String TITLE = "title";

  public static final String VERSION = "version";

  public static final String PRODUCT_IMAGE = "productImage";

  public static final String FAV_ICON = "favIcon";

  public static final String VENDOR_IMAGE = "vendorImage";

  public static final String VENDOR_IMAGE_ASPECT_RATIO = "vendorImageAspectRatio";

  private boolean systemUsageEnabled;

  private String systemUsageTitle;

  private String systemUsageMessage;

  private boolean systemUsageOncePerSession;

  private String header;

  private String footer;

  private String color;

  private String background;

  private Optional<BrandingRegistry> branding = Optional.empty();

  @GET
  @Path("/config/ui")
  @Produces("application/json")
  public String getConfig() {
    JSONObject jsonObject = new JSONObject();

    if (systemUsageEnabled) {
      jsonObject.put(SYSTEM_USAGE_TITLE, systemUsageTitle);
      jsonObject.put(SYSTEM_USAGE_MESSAGE, systemUsageMessage);
      jsonObject.put(SYSTEM_USAGE_ONCE_PER_SESSION, systemUsageOncePerSession);
    }

    String vendorImage = getVendorImage();

    jsonObject.put(HEADER, this.header);
    jsonObject.put(FOOTER, this.footer);
    jsonObject.put(COLOR, this.color);
    jsonObject.put(BACKGROUND, this.background);
    jsonObject.put(TITLE, getTitle());
    jsonObject.put(VERSION, getVersion());
    jsonObject.put(PRODUCT_IMAGE, getProductImage());
    jsonObject.put(FAV_ICON, getFavIcon());
    jsonObject.put(VENDOR_IMAGE, vendorImage);
    jsonObject.put(VENDOR_IMAGE_ASPECT_RATIO, getAspectRatio(vendorImage));

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

  public String getProductImage() {
    return branding
        .map(branding -> branding.getAttributeFromBranding(BrandingPlugin::getBase64ProductImage))
        .orElse("");
  }

  public String getFavIcon() {
    return branding
        .map(branding -> branding.getAttributeFromBranding(BrandingPlugin::getBase64FavIcon))
        .orElse("");
  }

  public String getVendorImage() {
    return branding
        .map(branding -> branding.getAttributeFromBranding(BrandingPlugin::getBase64VendorImage))
        .orElse("");
  }

  public float getAspectRatio(String base64Image) {
    if (StringUtils.isBlank(base64Image)) {
      return 0;
    }
    BASE64Decoder b64Decoder = new BASE64Decoder();
    try {
      byte[] imageBytes = b64Decoder.decodeBuffer(base64Image);
      ByteArrayInputStream stream = new ByteArrayInputStream(imageBytes);
      BufferedImage image = ImageIO.read(stream);
      stream.close();
      return ((float) image.getWidth()) / image.getHeight();
    } catch (IOException e) {
      return 0;
    }
  }
}
