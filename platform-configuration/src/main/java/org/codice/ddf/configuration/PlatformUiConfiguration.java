/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.configuration;

import net.minidev.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Configuration class for pid=ddf.platform.ui.config.
 *
 * Contains webservice method for returning the current configuration.
 *
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

    private boolean systemUsageEnabled;
    private String systemUsageTitle;
    private String systemUsageMessage;
    private boolean systemUsageOncePerSession;
    private String header;
    private String footer;
    private String color;
    private String background;

    @GET
    @Path("/config/ui")
    @Produces("application/json")
    public String getConfig() {
        JSONObject jsonObject = new JSONObject();

        if(systemUsageEnabled) {
            jsonObject.put(SYSTEM_USAGE_TITLE, systemUsageTitle);
            jsonObject.put(SYSTEM_USAGE_MESSAGE, systemUsageMessage);
            jsonObject.put(SYSTEM_USAGE_ONCE_PER_SESSION, systemUsageOncePerSession);
        }

        jsonObject.put(HEADER, this.header);
        jsonObject.put(FOOTER, this.footer);
        jsonObject.put(COLOR, this.color);
        jsonObject.put(BACKGROUND, this.background);
        return jsonObject.toJSONString();
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

}
