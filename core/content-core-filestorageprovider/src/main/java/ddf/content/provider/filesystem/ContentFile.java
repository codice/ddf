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
package ddf.content.provider.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.content.data.ContentItem;

public class ContentFile implements ContentItem {
    private static XLogger LOGGER = new XLogger(LoggerFactory.getLogger(ContentFile.class));

    private File file;

    private String mimeTypeRawData;

    private MimeType mimeType;

    private String id;

    private String uri;

    private String filename;

    protected ContentFile(File file, String id, String mimeTypeRawData) {
        this(file, id, mimeTypeRawData, null);
    }

    protected ContentFile(File file, String id, String mimeTypeRawData, String filename) {
        this.file = file;
        this.mimeTypeRawData = mimeTypeRawData;
        this.id = id;
        this.mimeType = null;
        this.filename = filename; // DDF-1856

        if (filename == null && file != null) {
            LOGGER.debug("Input filename is NULL, setting to file.getName()");
            this.filename = file.getName();
        }

        // Determine mime type only if incoming raw data is non-null.
        // The mime type raw data is null for content items to be deleted since
        // deletion operation only needs the id.
        if (mimeTypeRawData != null) {
            try {
                this.mimeType = new MimeType(mimeTypeRawData);
            } catch (MimeTypeParseException e) {
                LOGGER.debug("Unable to create MimeType from raw data " + mimeTypeRawData);
                this.mimeType = null;
            }
        }
    }

    @Override
    public File getFile() throws IOException {
        return file;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            LOGGER.error("Cannot get content as stream: ", e);
            throw new IOException(e);
        }
    }

    @Override
    public MimeType getMimeType() {
        return mimeType;
    }

    @Override
    public long getSize() throws IOException {
        if (file != null) {
            return file.length();
        } else {
            throw new IOException("File is null - cannot get size");
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
    public String getMimeTypeRawData() {
        return mimeTypeRawData;
    }

}
