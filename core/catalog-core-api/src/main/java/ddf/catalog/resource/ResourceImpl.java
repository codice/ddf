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
package ddf.catalog.resource;

import java.io.InputStream;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.data.BinaryContentImpl;

/**
 * This class is a generic implementation of the {@link Resource} interface
 * 
 * @deprecated As of release 2.3.0, replaced by
 *             ddf.catalog.resource.impl.ResourceImpl
 * 
 */
@Deprecated
public class ResourceImpl extends BinaryContentImpl implements Resource {

    private static final XLogger logger = new XLogger(LoggerFactory.getLogger(ResourceImpl.class));

    private String name;

    /**
     * Instantiates a new product {@link Resource}.
     * 
     * @param inputStream
     *            the {@link InputStream} of the {@link Resource}
     * @param name
     *            the name of the {@link Resource}
     */
    public ResourceImpl(InputStream inputStream, String name) {
        super(inputStream);
        this.name = name;
    }

    /**
     * Instantiates a new product {@link Resource}.
     * 
     * @param inputStream
     *            the {@link InputStream} of the {@link Resource}
     * @param mimeType
     *            the {@link MimeType} of the {@link Resource}
     * @param name
     *            the name of the {@link Resource}
     */
    public ResourceImpl(InputStream inputStream, MimeType mimeType, String name) {
        super(inputStream, mimeType);
        this.name = name;
    }

    /**
     * Instantiates a new product {@link Resource}.
     * 
     * @param inputStream
     *            the {@link InputStream} of the {@link Resource}
     * @param mimeTypeString
     *            the Mime Type value of the {@link Resource}
     * @param name
     *            the name of the {@link Resource}
     */
    public ResourceImpl(InputStream inputStream, String mimeTypeString, String name) {
        this(inputStream, toMimeType(mimeTypeString, name), name);
        this.name = name;
    }

    /**
     * Converts the mimeType string value to a {@link MimeType}
     * 
     * @param mimeTypeString
     *            the Mime Type string value
     * @param name
     *            the name of the {@link Resource}
     * @return the converted {@link MimeType}, null if it cannot be converted
     */
    private static MimeType toMimeType(String mimeTypeString, String name) {
        MimeType mimeType = null;
        try {
            mimeType = mimeTypeString == null ? null : new MimeType(mimeTypeString);
        } catch (MimeTypeParseException e) {
            logger.warn("Could not assign the MimeType to the Resource named '" + name
                    + "' because the following " + "MimeType could not be parsed properly: "
                    + mimeTypeString);
        }
        return mimeType;
    }

    @Override
    public String getName() {
        return name;
    }

}
