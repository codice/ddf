/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.configuration;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.branding.BrandingPlugin;

import net.minidev.json.JSONObject;

/**
 * Configuration class for pid=ddf.platform.ui.config.
 * <p>
 * Contains webservice method for returning the current configuration.
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

    private boolean systemUsageEnabled;

    private String systemUsageTitle;

    private String systemUsageMessage;

    private boolean systemUsageOncePerSession;

    private String header;

    private String footer;

    private String color;

    private String background;

    private BrandingPlugin branding;

    private String title;

    private String version;

    private String productImage;

    private String favicon;

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

        jsonObject.put(HEADER, this.header);
        jsonObject.put(FOOTER, this.footer);
        jsonObject.put(COLOR, this.color);
        jsonObject.put(BACKGROUND, this.background);
        jsonObject.put(TITLE, this.title);
        jsonObject.put(VERSION, this.version);
        jsonObject.put(PRODUCT_IMAGE, this.productImage);
        jsonObject.put(FAV_ICON, this.favicon);
        return jsonObject.toJSONString();
    }

    private void setVersion() {
        if (branding != null) {
            version = branding.getProductName();
        }
    }

    private void setTitle() {
        if (StringUtils.isNotBlank(version)) {
            title = StringUtils.substringBeforeLast(version, " ");
        } else {
            title = "DDF";
        }
    }

    public void setBranding(BrandingPlugin branding) throws IOException {
        this.branding = branding;
        setInfo();
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

    public void setProvider() throws IOException {
        setInfo();
    }

    private void setInfo() throws IOException {
        if (branding != null) {
            setVersion();
            setTitle();
            this.productImage = branding.getBase64ProductImage();
            this.favicon = branding.getBase64FavIcon();
        }
    }
}
