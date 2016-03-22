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
 **/
package ddf.catalog.content.data.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.data.Metacard;

public class ContentItemImpl implements ContentItem {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentItemImpl.class);

    protected String id;

    protected String uri;

    protected String filename;

    protected String mimeTypeRawData;

    protected MimeType mimeType;

    protected ByteSource byteSource;

    protected File file;

    protected long size;

    protected Metacard metacard;

    /**
     * An incoming content item since the ID will initially be
     * <code>null</code> because the {@link ddf.catalog.CatalogFramework} will assign its GUID.
     *
     * @param byteSource          the {@link ContentItem}'s input stream containing its actual data
     * @param mimeTypeRawData the {@link ContentItem}'s mime type
     */
    public ContentItemImpl(ByteSource byteSource, String mimeTypeRawData, String filename,
            Metacard metacard) {
        this(null, byteSource, mimeTypeRawData, filename, 0, metacard);
    }

    /**
     * An incoming content item where the item's GUID should be known.
     *
     * @param id              the {@link ContentItem}'s GUID
     * @param byteSource          the {@link ContentItem}'s input stream containing its actual data
     * @param mimeTypeRawData the {@link ContentItem}'s mime type
     */
    public ContentItemImpl(String id, ByteSource byteSource, String mimeTypeRawData,
            Metacard metacard) {
        this(id, byteSource, mimeTypeRawData, null, 0, metacard);
    }

    public ContentItemImpl(String id, ByteSource byteSource, String mimeTypeRawData, String filename, long size,
            Metacard metacard) {
        this.byteSource = byteSource;
        this.id = id;
        this.mimeType = null;
        this.size = size;
        if (filename != null) {
            this.filename = filename;
        } else {
            this.filename = DEFAULT_FILE_NAME;
        }
        this.metacard = metacard;
        this.mimeTypeRawData = DEFAULT_MIME_TYPE;
        if (mimeTypeRawData != null) {
            this.mimeTypeRawData = mimeTypeRawData;
        }
        try {
            this.mimeType = new MimeType(this.mimeTypeRawData);
        } catch (MimeTypeParseException e) {
            LOGGER.debug("Unable to create MimeType from raw data " + mimeTypeRawData);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public String getFilename() {
        if (filename == null && file != null) {
            filename = file.getAbsolutePath();
        }
        return filename;
    }

    @Override
    public MimeType getMimeType() {
        return mimeType;
    }

    @Override
    public String getMimeTypeRawData() {
        return mimeTypeRawData;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return byteSource.openStream();
    }

    @Override
    public File getFile() throws IOException {
        if (file == null && filename != null) {
            file = new File(filename);
        }
        return file;
    }

    @Override
    public long getSize() throws IOException {
        return size;
    }

    @Override
    public Metacard getMetacard() {
        return metacard;
    }
}
