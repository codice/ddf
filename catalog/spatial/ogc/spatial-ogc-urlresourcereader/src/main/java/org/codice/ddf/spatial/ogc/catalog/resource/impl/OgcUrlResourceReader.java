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
package org.codice.ddf.spatial.ogc.catalog.resource.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.MimeType;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.resource.impl.ResourceImpl;

/**
 * Resource Reader which provide redirection when a product has a mime type of text/html
 */
public class OgcUrlResourceReader implements ResourceReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(OgcUrlResourceReader.class);

    public static final String VERSION = "1.0";

    public static final String SHORTNAME = OgcUrlResourceReader.class.getSimpleName();

    public static final String TITLE = "OGC URL ddf.catalog.resource.Resource Reader";

    public static final String DESCRIPTION = "Resource Reader which provide redirection when a product has a mime type of text/html";

    public static final String ORGANIZATION = "OGC";

    private static final List<String> UNKNOWN_MIME_TYPES = Collections.unmodifiableList(Arrays
            .asList("application/unknown", "application/octet-stream"));

    private static final String URL_HTTP_SCHEME = "http";

    private static final String URL_HTTPS_SCHEME = "https";

    private static final Set<String> QUALIFIER_SET = ImmutableSet.of(URL_HTTP_SCHEME,
            URL_HTTPS_SCHEME);

    private Tika tika;

    private ResourceReader urlResourceReader;

    public OgcUrlResourceReader(ResourceReader urlResourceReader, Tika tika) {
        this.tika = tika;
        this.urlResourceReader = urlResourceReader;
        LOGGER.debug("Supported Schemes for {}: {}", OgcUrlResourceReader.class.getSimpleName(),
                QUALIFIER_SET.toString());
    }

    public Set<String> getSupportedSchemes() {
        return QUALIFIER_SET;
    }

    /**
     * Retrieves a {@link ddf.catalog.resource.Resource} based on a {@link URI} and provided
     * arguments. A connection is made to the {@link URI} to obtain the
     * {@link ddf.catalog.resource.Resource}'s {@link InputStream} and build a
     * {@link ResourceResponse} from that. The {@link ddf.catalog.resource.Resource}'s name gets set
     * to the {@link URI} passed in. Calls {@link URLResourceReader}, if the mime-type is
     * "text/html" it will inject a simple script to redirect to the resourceURI instead of
     * attempting to download it.
     * 
     * @param resourceURI
     *            A {@link URI} that defines what {@link Resource} to retrieve and how to do it.
     * @param properties
     *            Any additional arguments that should be passed to the
     *            {@link ddf.catalog.resource.ResourceReader}.
     * @return A {@link ResourceResponse} containing the retrieved {@link Resource}.
     * @throws ResourceNotSupportedException
     */
    public ResourceResponse retrieveResource(URI resourceURI, Map<String, Serializable> properties)
        throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
        LOGGER.debug("Calling URLResourceReader.retrieveResource()");
        ResourceResponse response = urlResourceReader.retrieveResource(resourceURI, properties);
        Resource resource = response.getResource();
        MimeType mimeType = resource.getMimeType();
        LOGGER.debug("mimeType: {}", mimeType);
        if (mimeType != null) {
            String mimeTypeStr = mimeType.toString();
            String detectedMimeType = "";
            if (UNKNOWN_MIME_TYPES.contains(mimeTypeStr)) {
                detectedMimeType = tika.detect(resourceURI.toURL());
            }
            if (StringUtils.contains(detectedMimeType, MediaType.TEXT_HTML)
                    || StringUtils.contains(mimeTypeStr, MediaType.TEXT_HTML)) {
                LOGGER.debug("Detected \"text\\html\". Building redirect script");
                StringBuilder strBuilder = new StringBuilder();
                strBuilder
                        .append("<html><script type=\"text/javascript\">window.location.replace(\"");
                strBuilder.append(resourceURI);
                strBuilder.append("\");</script></html>");
                return new ResourceResponseImpl(new ResourceImpl(new ByteArrayInputStream(
                        strBuilder.toString().getBytes(StandardCharsets.UTF_8)), detectedMimeType, resource.getName()));
            }
        }
        return response;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getId() {
        return SHORTNAME;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getOrganization() {
        return ORGANIZATION;
    }

    @Override
    public Set<String> getOptions(Metacard metacard) {
        LOGGER.debug("OgcUrlResourceReader getOptions doesn't support options, returning empty set.");
        return Collections.emptySet();
    }
}
