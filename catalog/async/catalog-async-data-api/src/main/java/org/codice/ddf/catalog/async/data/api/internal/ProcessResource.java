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
package org.codice.ddf.catalog.async.data.api.internal;

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 *
 * <p>
 * A {@code ProcessResource} represents a local or remote resource that will be used during processing
 * by the {@link ProcessingFramework}.
 */
public interface ProcessResource {

    String DEFAULT_MIME_TYPE = "application/octet-stream";

    String DEFAULT_FILE_NAME = "content_store_file.bin";

    /**
     * An optional field to provide additional information about a {@link ProcessResource}
     *
     * @return the qualifier of the {@link ProcessResource}
     */
    String getQualifier();

    /**
     * The filename of the {@link ProcessResource}. If not set it will default to {@link #DEFAULT_FILE_NAME}
     *
     * @return the filename of the {@link ProcessResource}
     */
    String getFilename();

    /**
     * Return the mime type raw data for the {@link ProcessResource}, e.g., image/nitf or application/json;id=geojson
     *
     * @return the mime type raw data for the {@link ProcessResource}
     */
    String getMimeTypeRawData();

    /**
     * Return the input stream containing the {@link ProcessResource}'s actual data content.
     *
     * @return the {@link ProcessResource}'s input stream
     * @throws IOException if the input stream is not available
     */
    InputStream getInputStream() throws IOException;

    /**
     * Return the total number of bytes in the {@link ProcessResource}'s input stream.
     *
     * @return returns the total number of bytes that can be read from the input stream, or -1 if the
     * total number of bytes is unknown or not applicable (e.g., in the case of a live stream)
     */
    long getSize();

    /**
     * Determines if the {@code ProcessResource} has been modified during processing.
     *
     * @return {@code true} if modified, {@code false} otherwise
     */
    boolean isModified();
}
