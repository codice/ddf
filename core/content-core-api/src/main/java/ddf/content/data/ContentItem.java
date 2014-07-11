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
package ddf.content.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.MimeType;

import ddf.content.storage.StorageProvider;

/**
 * 
 * ContentItem is the POJO representing the information about the content to be stored in the
 * {@link StorageProvider}.
 * 
 * A ContentItem encapsulates the content's globally unique ID, mime type, and input stream (i.e.,
 * the actual content).
 * 
 * @author Hugh Rodgers, Lockheed Martin
 * @author ddf.isgs@lmco.com
 * 
 */
public interface ContentItem {
    /**
     * Return the globally unique ID for the content item.
     * 
     * @return the content item's ID
     */
    public String getId();

    /**
     * Sets the URI of the content item.
     * 
     * This is used by the {@link StorageProvider} when the content item is stored in the content
     * repository and will be of the form <code>content:&lt;GUID&gt;</code>.
     * 
     * Or this is used by the endpoint when the client specifies the content item's location which
     * means the content is stored remote/external to DDF.
     *
     * @param uri the URI for the content item
     */
    public void setUri(String uri);

    /**
     * Return the URI of the content item.
     * 
     * @return the URI of the content item
     */
    public String getUri();

    public String getFilename(); // DDF-1856

    /**
     * Return the mime type for the content item, e.g., image/nitf or application/pdf
     * 
     * @return the mime type for the content item
     */
    public MimeType getMimeType();

    /**
     * Return the mime type raw data for the content item, e.g., image/nitf or text/xml;id=xml
     * 
     * @return the mime type raw data for the content item
     */
    public String getMimeTypeRawData();

    /**
     * Return the input stream containing the item's actual data content.
     * 
     * @return the item's input stream
     * @throws IOException
     *             if the input stream is not available
     */
    public InputStream getInputStream() throws IOException;

    /**
     * Return the file associated with the content item.
     * 
     * @return the content item's associated file
     * @throws IOException
     *             if the content cannot be resolved to be an actual file
     */
    public File getFile() throws IOException;

    /**
     * Return the total number of bytes in the item's input stream.
     * 
     * @return returns the total number of bytes that can be read from the input stream
     * @throws IOException
     *             if the total number of bytes is unknown or not applicable (e.g., in the case of a
     *             live stream)
     */
    public long getSize() throws IOException;

}
