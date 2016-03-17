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
package ddf.catalog.content.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.MimeType;

import ddf.catalog.data.Metacard;

/**
 * ContentItem is the POJO representing the information about the content to be stored in the
 * {@link ddf.catalog.content.StorageProvider}.
 *
 * A ContentItem encapsulates the content's globally unique ID, mime type, and input stream (i.e.,
 * the actual content).
 *
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 *
 */
public interface ContentItem {

    String DEFAULT_MIME_TYPE = "application/octet-stream";

    String DEFAULT_FILE_NAME = "content_store_file.bin";

    /**
     * Return the globally unique ID for the content item.
     *
     * @return the content item's ID
     */
    String getId();

    /**
     * Return the URI of the content item.
     *
     * @return the URI of the content item
     */
    String getUri();

    /**
     * Sets the URI of the content item.
     *
     * This is used by the {@link ddf.catalog.content.StorageProvider} when the content item is stored in the content
     * repository and will be of the form <code>content:&lt;GUID&gt;</code>.
     *
     * Or this is used by the endpoint when the client specifies the content item's location which
     * means the content is stored remote/external to DDF.
     *
     * @param uri the URI for the content item
     */
    void setUri(String uri);

    String getFilename(); // DDF-1856

    /**
     * Return the mime type for the content item, e.g., image/nitf or application/pdf
     *
     * @return the mime type for the content item
     */
    MimeType getMimeType();

    /**
     * Return the mime type raw data for the content item, e.g., image/nitf or application/json;id=geojson
     *
     * @return the mime type raw data for the content item
     */
    String getMimeTypeRawData();

    /**
     * Return the input stream containing the item's actual data content.
     *
     * @return the item's input stream
     * @throws IOException if the input stream is not available
     */
    InputStream getInputStream() throws IOException;

    /**
     * Return the file associated with the content item.
     *
     * @return the content item's associated file
     * @throws IOException if the content cannot be resolved to be an actual file
     */
    File getFile() throws IOException;

    /**
     * Return the total number of bytes in the item's input stream.
     *
     * @return returns the total number of bytes that can be read from the input stream
     * @throws IOException if the total number of bytes is unknown or not applicable (e.g., in the case of a
     *                     live stream)
     */
    long getSize() throws IOException;

    /**
     * Returns the metacard associated with this product.
     * @return Metacard
     */
    Metacard getMetacard();

}
