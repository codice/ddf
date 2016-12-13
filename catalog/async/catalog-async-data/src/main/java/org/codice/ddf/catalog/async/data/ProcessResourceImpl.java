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
package org.codice.ddf.catalog.async.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.catalog.async.data.api.internal.ProcessResource;

public class ProcessResourceImpl implements ProcessResource {

    private static final String CONTENT_SCHEME = "content";

    private static final int UNKNOWN_SIZE = -1;

    private URI uri;

    private String filename;

    private String mimeTypeRawData;

    private long size;

    private InputStream inputStream;

    private String qualifier;

    private boolean isModified;

    public ProcessResourceImpl(String metacardId, InputStream inputStream, String mimeTypeRawData,
            String filename) {
        this(metacardId, inputStream, mimeTypeRawData, filename, UNKNOWN_SIZE, "", true);
    }

    public ProcessResourceImpl(String metacardId, InputStream inputStream, String mimeTypeRawData,
            String filename, long size) {
        this(metacardId, inputStream, mimeTypeRawData, filename, size, "", true);
    }

    public ProcessResourceImpl(String metacardId, InputStream inputStream, String mimeTypeRawData,
            String filename, long size, boolean isModified) {
        this(metacardId, inputStream, mimeTypeRawData, filename, size, "", isModified);
    }

    public ProcessResourceImpl(String metacardId, InputStream inputStream, String mimeTypeRawData,
            String filename, long size, String qualifier) {
        this(metacardId, inputStream, mimeTypeRawData, filename, size, qualifier, true);
    }

    public ProcessResourceImpl(String metacardId, InputStream inputStream, String mimeTypeRawData,
            String filename, long size, String qualifier, boolean isModified) {
        this.isModified = isModified;
        this.qualifier = qualifier;
        this.inputStream = inputStream;
        this.size = size;
        if (StringUtils.isNotBlank(filename)) {
            this.filename = filename;
        } else {
            this.filename = DEFAULT_FILE_NAME;
        }
        this.mimeTypeRawData = DEFAULT_MIME_TYPE;
        if (StringUtils.isNotBlank(mimeTypeRawData)) {
            this.mimeTypeRawData = mimeTypeRawData;
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
    public String getUri() {
        return uri.toString();
    }

    @Override
    public String getQualifier() {
        return qualifier;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public String getMimeType() {
        return mimeTypeRawData;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
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