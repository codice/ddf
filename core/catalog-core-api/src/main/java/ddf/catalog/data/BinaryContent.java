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
package ddf.catalog.data;

import java.io.IOException;
import java.io.InputStream;

import javax.activation.MimeType;

import ddf.catalog.transform.QueryResponseTransformer;

/**
 * {@link BinaryContent} is used to return a format that has been transformed by a Transformer.
 * 
 * @see InputTransformer
 * @see QueryResponseTransformer
 */
public interface BinaryContent {

    /**
     * Gets the input stream.
     * <p>
     * Note that the binary from the <code>InputStream</code> can only be accessed once. Thus, when
     * you call {@link #getInputStream()} and retrieve the <code>InputStream</code> object, it can
     * only be used once to extract the content. If it is necessary that the bytes of the
     * <code>InputStream</code> object are needed again, use the {@link #getByteArray()} method
     * instead.
     * </p>
     * 
     * @return an input stream
     */
    public InputStream getInputStream();

    /**
     * @return the mime type of the information in the <code>InputStream</code>
     */
    public MimeType getMimeType();

    /**
     * Gets the {@link String} format {@link MimeType}
     * 
     * @return the mime type as a {@link String}
     */
    public String getMimeTypeValue();

    /**
     * Get the size if known.
     * 
     * @return the amount of bytes as a <code>long</code>, -1 if unknown
     */
    public long getSize();

    /**
     * Convenience method to attempt to read the contents of the <code>InputStream</code> into a
     * <code>byte</code> array.
     * <p>
     * Note that {@link #getByteArray()} should be idempotent if {@link #getInputStream()} has not
     * been initially invoked.
     * </p>
     * 
     * @return byte[]
     * @throws IOException
     *             if the stream can not be read into the array
     */
    public byte[] getByteArray() throws IOException;

}