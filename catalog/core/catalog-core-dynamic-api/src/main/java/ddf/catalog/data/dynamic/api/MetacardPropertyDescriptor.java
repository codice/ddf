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
package ddf.catalog.data.dynamic.api;

import java.io.Serializable;

import ddf.catalog.data.AttributeType;

/**
 * The MetacardPropertyDescriptorImpl interface encompasses the methods used to describe a property
 * of a DynamicMetacard instance. The methods defined here provide the ability to interrogate the
 * property and discover operational characteristics (indexed or stored by the catalog, etc.)
 */
public interface MetacardPropertyDescriptor extends Serializable {
    /**
     * Returns whether this property should be indexed by the underlying catalog source.
     * @return whether this property should be indexed by the underlying catalog source
     */
    boolean isIndexedBySource();

    /**
     * Set the flag for this attribute indicating if this attribute should be indexed by the
     * underlying catalog source.
     * @param indexedBySource true if this attribute should be indexed
     */
    void setIndexedBySource(boolean indexedBySource);

    /**
     * Indicates that this is a tokenized representation of the data. If
     * true, the attribute should be tokenized before storing.
     * @return true if the attribute data is received in a tokenized format
     */
    boolean isTokenized();

    /**
     * Set the flag indicating that this attribute is a tokenized representation of the data. If
     * true, the attribute should be tokenized before storing.
     * @param tokenized true if this attribute data is tokenized upon receipt
     */
    void setTokenized(boolean tokenized);

    /**
     * Indicates that the catalog source should storing the value of this property.
     * @return true if the catalog source should be storing the value of this property
     */
    boolean isStored();

    /**
     * Sets the flag indicating that the catalog source should store the value of this property.
     */
    void setStored(boolean stored);

    /**
     * Returns the attribute format for this property. Especially useful for properties with the
     * same underlying class type (e.g., STRING, GEOMETRY, XML).
     * @return the {@link ddf.catalog.data.AttributeType.AttributeFormat} for this property
     */
    AttributeType.AttributeFormat getFormat();

    /**
     * Sets the attribute format for this property. This is useful when the property has a common
     * representation class but is semantically different from other types, e.g. STRING, GEOMETRY,
     * XML).
     * @param format the format of this property
     */
    void setFormat(AttributeType.AttributeFormat format);

    // Add the methods from the DynaPropery class

    Class<?> getContentType();

    String getName();

    Class<?> getType();

    boolean isIndexed();

    boolean isMapped();
}
