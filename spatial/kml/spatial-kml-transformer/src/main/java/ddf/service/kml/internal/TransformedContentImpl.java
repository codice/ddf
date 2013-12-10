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
package ddf.service.kml.internal;

import java.io.IOException;
import java.io.InputStream;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import ddf.catalog.data.BinaryContent;

public class TransformedContentImpl implements BinaryContent {
    private static final Logger LOGGER = Logger.getLogger(TransformedContentImpl.class.getName());

    private String mimetype;

    private InputStream inputStream;

    private byte[] bytes;

    public TransformedContentImpl(InputStream kmlInputStream, String kmlMimetype) {
        this.inputStream = kmlInputStream;
        this.mimetype = kmlMimetype;

    }

    @Override
    public InputStream getInputStream() {

        return this.inputStream;
    }

    @Override
    public MimeType getMimeType() {
        MimeType type = null;
        try {
            type = new MimeType(mimetype);
        } catch (MimeTypeParseException e) {

        }
        return type;
    }

    @Override
    public String getMimeTypeValue() {
        return this.mimetype;
    }

    @Override
    public long getSize() {
        if (bytes == null) {
            try {
                bytes = IOUtils.toByteArray(inputStream);
            } catch (IOException e) {
                LOGGER.warn("Error getting content size", e);
                bytes = new byte[0];
            }
        }
        return bytes.length;
    }

    @Override
    public byte[] getByteArray() throws IOException {
        if (bytes == null) {
            try {
                bytes = IOUtils.toByteArray(inputStream);
            } catch (IOException e) {
                LOGGER.warn("Error getting content size", e);
            }
        }
        return bytes;
    }

}
