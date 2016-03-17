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
package org.codice.ddf.catalog.content.resource.reader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.content.StorageException;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.ReadStorageRequest;
import ddf.catalog.content.operation.ReadStorageResponse;
import ddf.catalog.content.operation.impl.ReadStorageRequestImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.resource.impl.ResourceImpl;

public class ContentResourceReader implements ResourceReader {
    private static final String URL_CONTENT_SCHEME = "content";

    private static final String VERSION = "1.0";

    private static final String ID = "ContentResourceReader";

    private static final String TITLE = "Content Resource Reader";

    private static final String DESCRIPTION = "Retrieves a file from the DDF Content Repository.";

    private static final String ORGANIZATION = "DDF";

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentResourceReader.class);

    private static Set<String> qualifierSet;

    static {
        qualifierSet = new HashSet<>(1);
        qualifierSet.add(URL_CONTENT_SCHEME);
    }

    private List<StorageProvider> storageProviders;

    private StorageProvider storage;

    public ContentResourceReader(List<StorageProvider> storageProviders) {
        this.storageProviders = storageProviders;
        if (storageProviders.size() > 0) {
            this.storage = storageProviders.get(0);
        }
    }

    public void bind(StorageProvider storageProvider) {
        this.storage = storageProviders.get(0);
    }

    public void unbind(StorageProvider storageProvider) {
        if (this.storageProviders.size() > 0) {
            this.storage = storageProviders.get(0);
        } else {
            this.storage = null;
        }
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getOrganization() {
        return ORGANIZATION;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public Set<String> getOptions(Metacard metacard) {
        LOGGER.trace("ENTERING/EXITING: getOptions");
        LOGGER.debug(
                "ContentResourceReader getOptions doesn't support options, returning empty set.");

        return Collections.emptySet();
    }

    @Override
    public Set<String> getSupportedSchemes() {
        return qualifierSet;
    }

    @Override
    public ResourceResponse retrieveResource(URI resourceUri, Map<String, Serializable> arguments)
            throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
        LOGGER.trace("ENTERING: retrieveResource");

        ResourceResponse response = null;

        if (resourceUri == null) {
            LOGGER.warn("Resource URI was null");
            throw new ResourceNotFoundException("Unable to find resource - resource URI was null");
        }

        if (resourceUri.getScheme()
                .equals(URL_CONTENT_SCHEME)) {
            LOGGER.debug("Resource URI is content scheme");
            String contentId = resourceUri.getSchemeSpecificPart();
            if (contentId != null && !contentId.isEmpty()) {
                ReadStorageRequest readRequest = new ReadStorageRequestImpl(resourceUri, arguments);
                try {
                    ReadStorageResponse readResponse = storage.read(readRequest);
                    ContentItem contentItem = readResponse.getContentItem();
                    File filePathName = contentItem.getFile();
                    String fileName = filePathName.getName();
                    LOGGER.debug("resource name: " + fileName);
                    InputStream is = contentItem.getInputStream();
                    response = new ResourceResponseImpl(
                            new ResourceImpl(new BufferedInputStream(is), contentItem.getMimeType(),
                                    fileName));
                } catch (StorageException e) {
                    throw new ResourceNotFoundException(e);
                }
            }
        }

        LOGGER.trace("EXITING: retrieveResource");

        return response;
    }

}
