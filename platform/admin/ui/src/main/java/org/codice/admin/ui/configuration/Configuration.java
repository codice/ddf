/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.admin.ui.configuration;

import java.io.ByteArrayInputStream;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.felix.webconsole.BrandingPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

/**
 * Stores external configuration properties.
 *
 * @author ddf.isgs@lmco.com
 */
@Path("/")
public class Configuration {
    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private static Configuration uniqueInstance;

    private static final String JSON_MIME_TYPE_STRING = "application/json";

    private static MimeType jsonMimeType = null;

    static {
        MimeType mime = null;
        try {
            mime = new MimeType(JSON_MIME_TYPE_STRING);
        } catch (MimeTypeParseException e) {
            LOGGER.warn("Failed to create json mimetype.");
        }
        jsonMimeType = mime;
    }

    private String header = "";

    private String footer = "";

    private String style = "";

    private String textColor = "";

    private String disabledInstallerApps = "";

    private BrandingPlugin branding;

    private Configuration() {
        header = "";
        footer = "";
        style = "";
        textColor = "";
        disabledInstallerApps = "";
    }

    /**
     * @return a unique instance of {@link Configuration}
     */
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
        configObj.put("text", header);
        configObj.put("footer", footer);
        configObj.put("style", style);
        configObj.put("textColor", textColor);
        configObj.put("disabledInstallerApps", disabledInstallerApps);
        configObj.put("branding", getProductName());

        String configString = JSONValue.toJSONString(configObj);
        response = Response
                .ok(new ByteArrayInputStream(configString.getBytes()), jsonMimeType.toString())
                .build();

        return response;
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

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public String getDisabledInstallerApps() {
        return disabledInstallerApps;
    }

    public void setDisabledInstallerApps(String disabledInstallerApps) {
        this.disabledInstallerApps = disabledInstallerApps;
    }

    public String getProductName() {
        if (branding != null) {
            // Remove the version number
            return branding.getProductName();
        } else {
            return "";
        }
    }

    public BrandingPlugin getBranding() {
        return branding;
    }

    public void setBranding(BrandingPlugin branding) {
        this.branding = branding;
    }

}
