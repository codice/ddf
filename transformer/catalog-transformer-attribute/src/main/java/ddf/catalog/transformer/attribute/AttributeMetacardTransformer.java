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
package ddf.catalog.transformer.attribute;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.Map;

import javax.activation.MimeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;

/**
 * Given a {@link Metacard}, this class can be used to return the contents of the Metacard
 * attributes. Supported formats are String and byte[]
 * 
 * @see MetacardTransformer
 * 
 */
public class AttributeMetacardTransformer implements MetacardTransformer {

    private String id;

    private MimeType mimeType;

    private String attributeName;

    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeMetacardTransformer.class);

    /**
     * Constructor for this transformer
     * 
     * @param attributeName
     *            The name of the attribute that will be used to retrieve the value within the
     *            {@link Metacard}
     * @param id
     *            the {@link MetacardTransformer} id
     * @param mimeType
     *            the MIME type that should be returned
     */
    public AttributeMetacardTransformer(String attributeName, String id, MimeType mimeType) {
        this.attributeName = attributeName;
        this.id = id;
        this.mimeType = mimeType;

    }

    @Override
    public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
        throws CatalogTransformerException {

        if (metacard == null) {
            throw new CatalogTransformerException("No attribute [" + attributeName
                    + "] found in Metacard.");
        }

        LOGGER.debug("Attempting transformation of [{}] with transformer [{}]", metacard, this);

        Attribute attribute = metacard.getAttribute(attributeName);

        if (attribute != null && attribute.getValue() != null) {

            if (byte[].class.isAssignableFrom(attribute.getValue().getClass())) {
                return new BinaryContentImpl(
                        new ByteArrayInputStream((byte[]) attribute.getValue()), mimeType);
            }
            if (String.class.isAssignableFrom(attribute.getValue().getClass())) {
                return new BinaryContentImpl(new ByteArrayInputStream(attribute.getValue()
                        .toString().getBytes()), mimeType);
            }

        }
        throw new CatalogTransformerException("No attribute [" + attributeName
                + "] found in Metacard.");
    }

    @Override
    public String toString() {
        return MetacardTransformer.class.getName() + " {Impl=" + this.getClass().getName()
                + ", attributeName=" + attributeName + ", id=" + id + ", MIME Type=" + mimeType
                + "}";
    }
}
