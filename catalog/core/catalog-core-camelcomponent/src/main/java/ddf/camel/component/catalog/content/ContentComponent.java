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
package ddf.camel.component.catalog.content;

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;

import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.mime.MimeTypeMapper;

public class ContentComponent extends DefaultComponent {
    /**
     * The name of the scheme this custom Camel component resolves to.
     */
    public static final String NAME = "content";

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ContentComponent.class);

    private CatalogFramework catalogFramework;

    private MimeTypeMapper mimeTypeMapper;

    private UuidGenerator uuidGenerator;

    public ContentComponent() {
        super();
        LOGGER.debug("INSIDE ContentComponent constructor");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.camel.impl.DefaultComponent#createEndpoint(java.lang.String,
     * java.lang.String, java.util.Map)
     */
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters)
            throws Exception {
        LOGGER.trace("ENTERING: createEndpoint");

        LOGGER.debug("uri = {},  remaining = {}", uri, remaining);
        LOGGER.debug("parameters = {}", parameters);

        Endpoint endpoint = new ContentEndpoint(uri, this);

        try {
            setProperties(endpoint, parameters);
        } catch (Exception e) {
            throw new Exception("Failed to create content endpoint", e);
        }

        LOGGER.trace("EXITING: createEndpoint");

        return endpoint;
    }

    public CatalogFramework getCatalogFramework() {
        return catalogFramework;
    }

    public void setCatalogFramework(CatalogFramework catalogFramework) {
        this.catalogFramework = catalogFramework;
    }

    public MimeTypeMapper getMimeTypeMapper() {
        return mimeTypeMapper;
    }

    public void setMimeTypeMapper(MimeTypeMapper mimeTypeMapper) {
        this.mimeTypeMapper = mimeTypeMapper;
    }

    public void setUuidGenerator(UuidGenerator uuidGenerator) {
        this.uuidGenerator = uuidGenerator;
    }

    public UuidGenerator getUuidGenerator() {
        return uuidGenerator;
    }

}
