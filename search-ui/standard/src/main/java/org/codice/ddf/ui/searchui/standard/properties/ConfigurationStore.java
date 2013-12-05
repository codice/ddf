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

package org.codice.ddf.ui.searchui.standard.properties;


import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.BinaryContentImpl;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;

/**
 * Stores external configuration properties.
 *
 * @author ddf.isgs@lmco.com
 */
@Path("/")
public class ConfigurationStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationStore.class);

    private static ConfigurationStore uniqueInstance;

    private String header = "";

    private String footer = "";

    private String style = "";

    private String textColor = "";

    private String wmsServer = "";

    private String layers = "";

    private String format = "";

    private String branding = "DDF";

    private static String JSON_MIME_TYPE_STRING = "application/json";

    private static MimeType JSON_MIME_TYPE = null;

    static {
        MimeType mime = null;
        try {
            mime = new MimeType(JSON_MIME_TYPE_STRING);
        } catch (MimeTypeParseException e) {
            LOGGER.warn("Failed to create json mimetype.");
        }
        JSON_MIME_TYPE = mime;
    }

    private ConfigurationStore() {
        header = "";
        footer = "";
        style = "";
        textColor = "";
        wmsServer = "";
        layers = "";
        format = "";
    }

    @GET
    @Path("/config")
    public Response getDocument(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest) {
        Response response;
        JSONObject configObj = new JSONObject();
        configObj.put("header", header);
        configObj.put("footer", footer);
        configObj.put("style", style);
        configObj.put("textColor", textColor);
        configObj.put("branding", branding);
        configObj.put("wmsServer", wmsServer);
        configObj.put("layers", layers);
        configObj.put("format", format);

        String configString = JSONValue.toJSONString(configObj);
        BinaryContent content = new BinaryContentImpl(new ByteArrayInputStream(configString.getBytes()),
                JSON_MIME_TYPE);
        response = Response.ok(content.getInputStream(), content.getMimeTypeValue()).build();

        return response;
    }

    /**
     * @return a unique instance of {@link ConfigurationStore}
     */
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

    public String getBranding() {
        return branding;
    }

    public void setBranding(String branding) {
        this.branding = branding;
    }

    public String getWmsServer() {
        return wmsServer;
    }

    public void setWmsServer(String wmsServer) {
        this.wmsServer = wmsServer;
    }

    public String getLayers() { return layers; }

    public void setLayers(String layers) {
        this.layers = layers;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}
