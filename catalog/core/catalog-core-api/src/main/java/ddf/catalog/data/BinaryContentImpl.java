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
package ddf.catalog.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.MimeType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * This class is a common implementation of the {@link BinaryContent} interface.
 *
 * @deprecated As of release 2.3.0, replaced by
 *             ddf.catalog.data.impl.BinaryContentImpl
 *
 */
@Deprecated
public class BinaryContentImpl implements BinaryContent {

    static final long UNKNOWN_SIZE = -1;

    private InputStream inputStream;

    private MimeType mimeType;

    private long size = UNKNOWN_SIZE;

    private byte[] byteArray = null;

    /**
     * Instantiates a new product resource.
     *
     * @param inputStream
     *            the input stream
     * @param mimeType
     *            the mime type for the resource
     */
    public BinaryContentImpl(InputStream inputStream, MimeType mimeType) {
        this.inputStream = inputStream;
        this.mimeType = mimeType;
    }

    /**
     * Instantiates a new product resource.
     *
     * @param inputStream
     *            the input stream
     */
    public BinaryContentImpl(InputStream inputStream) {
        this.inputStream = inputStream;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.Resource#getInputStream()
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.Resource#getMimeType()
     */
    @Override
    public MimeType getMimeType() {
        return mimeType;
    }

    @Override
    public String getMimeTypeValue() {
        return mimeType != null ? mimeType.getBaseType() : null;
    }

    /**
     * String representation of this {@code BinaryContentImpl}.
     *
     * @return the String representation of this {@code BinaryContentImpl}
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public long getSize() {
        return size;
    }

    /**
     * Sets the size in bytes of the binary content. If the size is unknown, then a -1 is returned.
     *
     * @param size
     */
    public void setSize(long size) {
        if (size < 0) {
            size = UNKNOWN_SIZE;
        }
        this.size = size;
    }

    @Override
    public byte[] getByteArray() throws IOException {
        if (byteArray == null) {
            if (inputStream != null) {
                byteArray = IOUtils.toByteArray(inputStream);
                ByteArrayInputStream bInputStream = new ByteArrayInputStream(byteArray);
                inputStream = bInputStream;
            }
        }
        return byteArray;
    }

}
