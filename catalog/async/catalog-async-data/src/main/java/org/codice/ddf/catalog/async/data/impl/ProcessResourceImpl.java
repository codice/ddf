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
package org.codice.ddf.catalog.async.data.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.catalog.async.data.api.internal.ProcessResource;

public class ProcessResourceImpl implements ProcessResource {

    public static final String CONTENT_SCHEME = "content";

    public static final String DEFAULT_NAME = "content_store_file.bin";

    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private URI uri;

    private String name;

    private String mimeType;

    private long size;

    private InputStream inputStream;

    private String qualifier;

    private boolean isModified;

    public ProcessResourceImpl(String metacardId, @Nullable InputStream inputStream,
            String mimeType, String name) {
        this(metacardId, inputStream, mimeType, name, UNKNOWN_SIZE, "", true);
    }

    public ProcessResourceImpl(String metacardId, @Nullable InputStream inputStream,
            String mimeType, String name, long size) {
        this(metacardId, inputStream, mimeType, name, size, "", true);
    }

    public ProcessResourceImpl(String metacardId, @Nullable InputStream inputStream,
            String mimeType, String name, long size, boolean isModified) {
        this(metacardId, inputStream, mimeType, name, size, "", isModified);
    }

    public ProcessResourceImpl(String metacardId, @Nullable InputStream inputStream,
            String mimeType, String name, long size, String qualifier) {
        this(metacardId, inputStream, mimeType, name, size, qualifier, true);
    }

    /**
     * Creates a new {@link ProcessResource}.
     *
     * @param metacardId    schema specific part of {@link URI}, throws {@link IllegalArgumentException} if blank or null
     * @param inputStream   {@link InputStream} of the {@link ProcessResource}, can be null
     * @param mimeType      mime type of the {@link ProcessResource}, defaults to {@link #DEFAULT_MIME_TYPE}
     * @param name          name of the {@link ProcessResource}, defaults to {@link #DEFAULT_NAME}
     * @param size          size of the {@link ProcessResource}'s {@param inputStream}, throws {@link IllegalArgumentException}
     *                      if less than -1
     * @param qualifier     fragment of the {@link ProcessResource}'s {@link URI}, defaults to empty string
     * @param isModified    flags whether {@link ddf.catalog.content.operation.UpdateStorageRequest}s are sent back to the {@link ddf.catalog.CatalogFramework}
     *                      for this {@link ProcessResource}
     */
    public ProcessResourceImpl(String metacardId, @Nullable InputStream inputStream,
            String mimeType, String name, long size, String qualifier, boolean isModified) {
        if (size < -1) {
            throw new IllegalArgumentException("ProcessResourceImpl size may not be less than -1");
        }

        this.qualifier = qualifier;
        this.isModified = isModified;
        this.inputStream = inputStream;
        this.size = size;

        if (StringUtils.isNotBlank(name)) {
            this.name = name;
        } else {
            this.name = DEFAULT_NAME;
        }
        this.mimeType = DEFAULT_MIME_TYPE;
        if (StringUtils.isNotBlank(mimeType)) {
            this.mimeType = mimeType;
        }
        try {
            if (StringUtils.isNotBlank(this.qualifier)) {
                uri = new URI(CONTENT_SCHEME, metacardId, this.qualifier);
            } else {
                uri = new URI(CONTENT_SCHEME, metacardId, null);
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Unable to create content URI.", e);
        }
    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public String getQualifier() {
        return qualifier;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    @Nullable public InputStream getInputStream() throws IOException {
        return inputStream;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public boolean isModified() {
        return isModified;
    }

    public void markAsModified() {
        isModified = true;
    }
}