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
package org.codice.admin.ui.configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.codice.ddf.branding.BrandingRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Stores external configuration properties. */
@Path("/")
public class Configuration {
  private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

  public static final String SYSTEM_USAGE_TITLE = "systemUsageTitle";

  public static final String SYSTEM_USAGE_MESSAGE = "systemUsageMessage";

  public static final String SYSTEM_USAGE_ONCE_PER_SESSION = "systemUsageOncePerSession";

  private static Configuration uniqueInstance;

  private static final String JSON_MIME_TYPE_STRING = "application/json";

  private static MimeType jsonMimeType = null;

  static {
    MimeType mime = null;
    try {
      mime = new MimeType(JSON_MIME_TYPE_STRING);
    } catch (MimeTypeParseException e) {
      LOGGER.info("Failed to create json mimetype.");
    }
    jsonMimeType = mime;
  }

  private boolean systemUsageEnabled;

  private String systemUsageTitle;

  private String systemUsageMessage;

  private boolean systemUsageOncePerSession;

  private String disabledInstallerApps = "";

  private Optional<BrandingRegistry> branding = Optional.empty();

  private Configuration() {
    disabledInstallerApps = "";
  }

  /** @return a unique instance of {@link Configuration} */
  public static synchronized Configuration getInstance() {

    if (uniqueInstance == null) {
      uniqueInstance = new Configuration();
    }

    return uniqueInstance;
  }

  @GET
  @Path("/config")
  public Response getDocument(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest) {
    Response response;
    JSONObject configObj = new JSONObject();

    if (systemUsageEnabled) {
      configObj.put(SYSTEM_USAGE_TITLE, systemUsageTitle);
      configObj.put(SYSTEM_USAGE_MESSAGE, systemUsageMessage);
      configObj.put(SYSTEM_USAGE_ONCE_PER_SESSION, systemUsageOncePerSession);
    }

    configObj.put("disabledInstallerApps", disabledInstallerApps);
    configObj.put("branding", getProductName());

    String configString = JSONValue.toJSONString(configObj);
    response =
        Response.ok(
                new ByteArrayInputStream(configString.getBytes(StandardCharsets.UTF_8)),
                jsonMimeType.toString())
            .build();

    return response;
  }

  public String getDisabledInstallerApps() {
    return disabledInstallerApps;
  }

  public void setDisabledInstallerApps(String disabledInstallerApps) {
    this.disabledInstallerApps = disabledInstallerApps;
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

  public String getProductName() {
    return branding.map(BrandingRegistry::getProductName).orElse("");
  }

  public BrandingRegistry getBranding() {
    return branding.orElse(null);
  }

  public void setBranding(BrandingRegistry branding) {
    this.branding = Optional.ofNullable(branding);
  }
}
