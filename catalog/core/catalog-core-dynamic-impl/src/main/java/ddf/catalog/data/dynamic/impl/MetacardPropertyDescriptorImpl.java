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
package ddf.catalog.data.dynamic.impl;

import java.util.Date;
import java.util.HashMap;

import org.apache.commons.beanutils.DynaProperty;

import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.dynamic.api.MetacardPropertyDescriptor;

/**
 * Extends the DynaProperty to include fields used by the catalog: indexedBySource, tokenized,
 * and stored. This class mirrors the DynaProperty constructors with the addition of constructors taking
 * values for the added fields. Attributes of the dynamic metacards are described by instances
 * of this class.
 */
public class MetacardPropertyDescriptorImpl extends DynaProperty implements MetacardPropertyDescriptor {

    private boolean indexedBySource = true;
    private boolean tokenized = false;
    private boolean stored = true;
    private AttributeFormat format;

    /*
     * Mapping between the logical attribute formats for metacard attributes and the underlying
     * classes used to represent them.
     */
    private static final HashMap<AttributeFormat, Class<?>> TYPE_MAP = new HashMap<>();
    static {
        TYPE_MAP.put(AttributeFormat.BINARY, Byte[].class);
        TYPE_MAP.put(AttributeFormat.BOOLEAN, Boolean.class);
        TYPE_MAP.put(AttributeFormat.DATE, Date.class);
        TYPE_MAP.put(AttributeFormat.DOUBLE, Double.class);
        TYPE_MAP.put(AttributeFormat.FLOAT, Float.class);
        TYPE_MAP.put(AttributeFormat.GEOMETRY, String.class);
        TYPE_MAP.put(AttributeFormat.INTEGER, Integer.class);
        TYPE_MAP.put(AttributeFormat.LONG, Long.class);
        TYPE_MAP.put(AttributeFormat.OBJECT, Object.class);
        TYPE_MAP.put(AttributeFormat.SHORT, Short.class);
        TYPE_MAP.put(AttributeFormat.STRING, String.class);
        TYPE_MAP.put(AttributeFormat.XML, String.class);
    }

    /**
     * Creates a property descriptor for the given name using default values for indexedBySource,
     * tokenized, and stored.
     * This works for the majority of cases. In cases where the underlying class representing this
     * property is the same as other property formats, {@link #setFormat(AttributeFormat)} should be
     * called after creation to set the correct format.     * @param name name of this attribute
     */
    public MetacardPropertyDescriptorImpl(String name) {
        super(name);
        format = AttributeFormat.OBJECT;
    }

    /**
     * Creates a property descriptor for the given name and type using default values for indexedBySource,
     * tokenized, and stored.
     * This works for the majority of cases. In cases where the underlying class representing this
     * property is the same as other property formats, {@link #setFormat(AttributeFormat)} should be
     * called after creation to set the correct format.     * @param name name of this attribute
     * @param type the base data type for this attribute
     */
    public MetacardPropertyDescriptorImpl(String name, Class<?> type) {
        super(name, type);
        format = getFormatFromClass(type);
    }

    /**
     * Creates a property descriptor for the given name, index type, and content type
     * using default values for indexedBySource, tokenized, and stored.
     * This works for the majority of cases. In cases where the underlying class representing this
     * property is the same as other property formats, {@link #setFormat(AttributeFormat)} should be
     * called after creation to set the correct format.     * @param name name of this attribute
     * @param type the type of collection for this attribute
     * @param contentType the base data type for this attribute
     */
    public MetacardPropertyDescriptorImpl(String name, Class<?> type, Class<?> contentType) {
        super(name, type, contentType);
        format = getFormatFromClass(contentType);
    }

    /**
     * Creates a property descriptor for the given name, type, and specified values for
     * indexedBySource, tokenized, and stored.
     * This works for the majority of cases. In cases where the underlying class representing this
     * property is the same as other property formats, {@link #setFormat(AttributeFormat)} should be
     * called after creation to set the correct format.     * @param name name of this attribute
     * @param type the base data type for this attribute
     * @param indexedBySource indicates whether the source should index this attribute for search
     * @param stored indicates whether the source should store this value
     * @param tokenized indicates whether the value for this attribute should be parsed
     */
    public MetacardPropertyDescriptorImpl(String name, Class<?> type, boolean indexedBySource,
            boolean stored, boolean tokenized) {
        super(name, type);
        this.indexedBySource = indexedBySource;
        this.stored = stored;
        this.tokenized = tokenized;
        format = getFormatFromClass(type);
    }

    /**
     * Creates a property descriptor for the given name, type, and specified values for
     * indexedBySource, tokenized, and stored.
     * This works for the majority of cases. In cases where the underlying class representing this
     * property is the same as other property formats, {@link #setFormat(AttributeFormat)} should be
     * called after creation to set the correct format.     * @param name name of this attribute
     * @param type the type of collection for this attribute
     * @param contentType the base data type for this attribute
     * @param indexedBySource indicates whether the source should index this attribute for search
     * @param stored indicates whether the source should store this value
     * @param tokenized indicates whether the value for this attribute should be parsed
     */
    public MetacardPropertyDescriptorImpl(String name, Class<?> type, Class<?> contentType,
            boolean indexedBySource, boolean stored, boolean tokenized) {
        super(name, type, contentType);
        this.indexedBySource = indexedBySource;
        this.stored = stored;
        this.tokenized = tokenized;
        format = getFormatFromClass(contentType);
    }

    /**
     * Creates a property desriptor based on the given DynaProperty object and the specified values for
     * indexedBySource, tokenized, and stored.
     * This works for the majority of cases. In cases where the underlying class representing this
     * property is the same as other property formats, {@link #setFormat(AttributeFormat)} should be
     * called after creation to set the correct format.
     * @param dynaProperty descriptor of the attribute (name, value, etc.)
     * @param indexedBySource indicates whether the source should index this attribute for search
     * @param stored indicates whether the source should store this value
     * @param tokenized indicates whether the value for this attribute should be parsed
     */
    public MetacardPropertyDescriptorImpl(DynaProperty dynaProperty, boolean indexedBySource,
            boolean stored, boolean tokenized) {
        super(dynaProperty.getName(), dynaProperty.getType(), dynaProperty.getContentType());
        this.indexedBySource = indexedBySource;
        this.stored = stored;
        this.tokenized = tokenized;
        format = getFormatFromClass(dynaProperty.getContentType() == null ? dynaProperty.getType() : dynaProperty.getContentType());
    }

    /**
     * Creates a property descriptor based on the specified attribute format. This method is useful
     * for creating distinct descriptors built on the same underlying base class, e.g. XML and
     * GEOMETRY types. Created as a static method since processing must be done before invoking
     * the underlying base class constructors.
     * @param name the name of this attribute
     * @param format the format of this attribute - one of {@link AttributeFormat}
     * @param indexedBySource indicates whether the source should index this attribute for search
     * @param stored indicates whether the source should store this value
     * @param tokenized indicates whether the value for this attribute should be parsed
     * @return an instance of MetacardPropertyDescriptor created from the passed values
     */
    public static MetacardPropertyDescriptor createByFormat(String name, AttributeFormat format,
            boolean indexedBySource, boolean stored, boolean tokenized) {
        MetacardPropertyDescriptor propertyDescriptor = null;

        Class<?> type = MetacardPropertyDescriptorImpl.TYPE_MAP.get(format);

        propertyDescriptor = new MetacardPropertyDescriptorImpl(name, type, indexedBySource, stored, tokenized);
        propertyDescriptor.setFormat(format);

        return propertyDescriptor;
    }

    @Override
    public boolean isIndexedBySource() {
        return indexedBySource;
    }

    @Override
    public void setIndexedBySource(boolean indexedBySource) {
        this.indexedBySource = indexedBySource;
    }

    @Override
    public boolean isTokenized() {
        return tokenized;
    }

    @Override
    public void setTokenized(boolean tokenized) {
        this.tokenized = tokenized;
    }

    @Override
    public boolean isStored() {
        return stored;
    }

    @Override
    public void setStored(boolean stored) {
        this.stored = stored;
    }

    @Override
    public AttributeFormat getFormat() {
        return this.format;
    }

    @Override
    public void setFormat(AttributeFormat format) {
        this.format = format;
    }

    /**
     * Returns the logical attribute format for the specified based on the specified class
     * used to represent the attribute. There is ambiguity for several formats (String, XML,
     * Geometry) which all use String for their representation. This will only return the
     * STRING format as it has no other details to select any other.
     * @param type the class used to represent this property
     * @return the corresponding {@link AttributeFormat} for the specified type
     */
    private AttributeFormat getFormatFromClass(Class<?> type) {
        AttributeFormat attributeFormat = AttributeFormat.OBJECT;
        if (type == null) {
            return attributeFormat;
        }
        String typeName = type.getSimpleName();

        switch (typeName) {
        case "String":
            attributeFormat = AttributeFormat.STRING;
            break;
        case "Boolean":
            attributeFormat = AttributeFormat.BOOLEAN;
            break;
        case "Date":
            attributeFormat = AttributeFormat.DATE;
            break;
        case "Short":
            attributeFormat = AttributeFormat.SHORT;
            break;
        case "Integer":
            attributeFormat = AttributeFormat.INTEGER;
            break;
        case "Long":
            attributeFormat = AttributeFormat.LONG;
            break;
        case "Float":
            attributeFormat = AttributeFormat.FLOAT;
            break;
        case "Double":
            attributeFormat = AttributeFormat.DOUBLE;
            break;
        case "Byte":
        case "Byte[]":
            attributeFormat = AttributeFormat.BINARY;
            break;
        default:
            attributeFormat = AttributeFormat.OBJECT;
        }

        return attributeFormat;
    }
}
