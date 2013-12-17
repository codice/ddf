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
package org.codice.ddf.spatial.kml.transformer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.codice.ddf.configuration.ConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.Options;

import ddf.catalog.data.Metacard;

/**
 * A handlebars template helper class that creates handlebar helpers which are used in the 
 * description.hbt handlebars template
 *
 */
public class DescriptionTemplateHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DescriptionTemplateHelper.class);

    private static final String CATALOG_SOURCES_REST = "/catalog/sources/";

    private static final String FORWARD_SLASH = "/";

    private String protocol = null;
    private String host = null;
    private int port = -1;
    private String serviceContext = null;
    
    public DescriptionTemplateHelper(String callingUrl,
            Map<String, String> platformConfiguration) {

        // attempt to read url context from incoming query
        if (callingUrl != null) {
            try {
                URL url = new URL(callingUrl);
                protocol = url.getProtocol();
                host = url.getHost();
                port = url.getPort();
            } catch (MalformedURLException e) {
                LOGGER.warn("Failed to parse incoming URL", e);
            }
        }
        if (platformConfiguration != null) {
            serviceContext = platformConfiguration.get(ConfigurationManager.SERVICES_CONTEXT_ROOT);

            // if no url set, use that of configurationManager
            if (host == null) {
                host = platformConfiguration.get(ConfigurationManager.HOST);
                try {
                    port = Integer.parseInt(platformConfiguration.get(ConfigurationManager.PORT));
                } catch (NumberFormatException e) {
                    LOGGER.warn("Failed to parse port from platform configuration", e);
                }
                protocol = platformConfiguration.get(ConfigurationManager.PROTOCOL);
                if (protocol.indexOf(":") != -1) {
                    protocol = protocol.split(":")[0];
                }
            }
        }
    }
    
    public String effectiveTime(Metacard context) {
        String effectiveTime = null;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        if (context.getEffectiveDate() == null) {
            effectiveTime = dateFormat.format(new Date());
        } else {
            effectiveTime = dateFormat.format(context.getEffectiveDate());
        }
        return effectiveTime;
    }

    public String resourceUrl(Metacard context) {
        StringBuilder path = new StringBuilder();
        if (serviceContext != null) {
            path.append(serviceContext);
        }
        path.append(CATALOG_SOURCES_REST).append(context.getSourceId()).append(FORWARD_SLASH).append(context.getId());
        String uri = "";
        try {
            uri = new URI(protocol, null, host, port, path.toString(), "transform=resource", null).toString();
        } catch (URISyntaxException e) {
            LOGGER.error("Failed to create resource url", e);
        }
        LOGGER.debug("HTML URI = {}", uri);
        
        return uri.toString();
    }
    
    public String metacardUrl(Metacard context) {
        StringBuilder path = new StringBuilder();
        if (serviceContext != null) {
            path.append(serviceContext);
        }
        path.append(CATALOG_SOURCES_REST).append(context.getSourceId()).append(FORWARD_SLASH).append(context.getId());
        String uri = "";
        try {
            uri = new URI(protocol, null, host, port, path.toString(), "transform=html", null).toString();
        } catch (URISyntaxException e) {
            LOGGER.error("Failed to create metacard url", e);
        }
        LOGGER.debug("HTML URI = {}", uri);
        
        return uri.toString();
    }

    public CharSequence hasThumbnail(Metacard context, Options options) {
        if ((context.getThumbnail() != null && context
                .getThumbnail().length != 0)) {
            try {
                return options.fn();
            } catch (IOException e) {                
                LOGGER.error("Failed to execute thumbnail template", e);
                return "";
            }
        } else {
            try {
                return options.inverse();
            } catch (IOException e) {
                LOGGER.error("Failed to execute noThumbnail template", e);
                return "";
            }
        }
    }

    public String base64Thumbnail(Metacard context) {
        return DatatypeConverter.printBase64Binary(context.getThumbnail());
    }

    public String resourceSizeString(Metacard context) {
        String resourceSize = context.getResourceSize();
        String sizePrefixes = " KMGTPEZYXWVU";

        if (resourceSize == null || resourceSize.trim().length() == 0
                || resourceSize.toLowerCase().indexOf("n/a") >= 0) {
            return null;
        }

        long size = 0;
        // if the size is not a number, and it isn't 'n/a', assume it is
        // already formatted, ie "10 MB"
        try {
            size = Long.parseLong(resourceSize);
        } catch (NumberFormatException nfe) {
            LOGGER.info(
                    "Failed to parse resourceSize ({}), assuming already formatted.",
                    resourceSize);
            return resourceSize;
        }

        if (size <= 0) {
            return "0";
        }
        int t2 = (int) Math
                .min(Math.floor(Math.log(size) / Math.log(1024)), 12);
        char c = sizePrefixes.charAt(t2);
        return (Math.round(size * 100 / Math.pow(1024, t2)) / 100) + " "
                + (c == ' ' ? "" : c) + "B";
    }
    
}
