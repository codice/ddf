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

import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.resource.impl.ResourceImpl;
import ddf.catalog.resource.impl.URLResourceReader;
import ddf.mime.MimeTypeMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Overloaded implementation of {@link URLResourceReader} to provide redirection when a product is text/html.
 */
public class OgcUrlResourceReader extends URLResourceReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(OgcUrlResourceReader.class);

    private static final List<String> UNKNOWN_MIME_TYPES = Collections.unmodifiableList(
            Arrays.asList("application/unknown", "application/octet-stream"));

    public OgcUrlResourceReader(MimeTypeMapper mimeTypeMapper) {
        super(mimeTypeMapper);

    }

    /**
     * Calls {@link URLResourceReader}, if the mime-type is "text/html" it will inject a simple
     * script to redirect to the resourceURI instead of attempting to download it.
     *
     * @param resourceURI A {@link URI} that defines what {@link Resource} to retrieve and how to do it.
     * @param properties  Any additional arguments that should be passed to the {@link ResourceReader}.
     * @return A {@link ResourceResponse} containing the retrieved {@link Resource}.
     */
    @Override
    public ResourceResponse retrieveResource(URI resourceURI, Map<String, Serializable> properties)
            throws IOException, ResourceNotFoundException {
        ResourceResponse response = super.retrieveResource(resourceURI, properties);
        Resource resource = response.getResource();
        MimeType mimeType = resource.getMimeType();
        if (mimeType != null) {
            String mimeTypeStr = mimeType.toString();
            String detectedMimeType = "";
            if (UNKNOWN_MIME_TYPES.contains(mimeTypeStr)) {
                Tika tika = new Tika();
                detectedMimeType = tika.detect(resourceURI.toURL());
            }
            if (StringUtils.contains(detectedMimeType, MediaType.TEXT_HTML) || StringUtils
                    .contains(mimeTypeStr, MediaType.TEXT_HTML)) {
                LOGGER.debug("Detected \"text\\html\". Building redirect script");
                StringBuilder strBuilder = new StringBuilder();
                strBuilder
                        .append("<html><script type=\"text/javascript\">window.location.replace(\"");
                strBuilder.append(resourceURI);
                strBuilder.append("\");</script></html>");
                return new ResourceResponseImpl(
                        new ResourceImpl(new ByteArrayInputStream(strBuilder.toString().getBytes()),
                                detectedMimeType, resource.getName()));
            }
        }
        return response;
    }

}
