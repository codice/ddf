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
package ddf.catalog.data.dynamic;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import org.apache.commons.beanutils.DynaProperty;

/**
 * Implements the {@link AttributeDescriptor} and {@link AttributeType} for dynamic
 * metacards. Adds the properties that the catalog provider use - indexed, stored, multiValued,
 * and tokenized.
 */
public class MetacardAttributeDescriptor implements AttributeDescriptor, AttributeType {
    protected MetacardPropertyDescriptor propertyDescriptor;


    /**
     * Creates a MetacardAttributeDescriptor given a {@link DynaProperty} descriptor.
     * @param descriptor {@link DynaProperty} descriptor for this attribute
     */
    public MetacardAttributeDescriptor(DynaProperty descriptor) {
        if (descriptor instanceof MetacardPropertyDescriptor) {
            propertyDescriptor = (MetacardPropertyDescriptor) descriptor;
        } else {
            propertyDescriptor = new MetacardPropertyDescriptor(descriptor, true, false, true);
        }
    }

    @Override
    public String getName() {
        return propertyDescriptor.getName();
    }

    @Override
    public boolean isMultiValued() {
        return false;
    }

    @Override
    public AttributeType<?> getType() {
        return this;
    }

    @Override
    public boolean isIndexed() {
        return propertyDescriptor.isIndexedBySource();
    }

    @Override
    public boolean isTokenized() {
        return propertyDescriptor.isTokenized();
    }

    @Override
    public boolean isStored() {
        return false;
    }

    @Override
    public Class getBinding() {
        return propertyDescriptor.getType();
    }

    /**
     * Converts from regular Java class types to the enumerated types in
     * {@link ddf.catalog.data.AttributeType.AttributeFormat}.
     * @return the AttributeFormat enumeration for this attribute's class type.
     */
    @Override
    public AttributeFormat getAttributeFormat() {
        AttributeFormat attributeFormat;
        Class<?> classType = propertyDescriptor.getType();
        switch (classType.getSimpleName()) {
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
        case "Byte[]":
            attributeFormat = AttributeFormat.BINARY;
            break;
        case "Xml":
            attributeFormat = AttributeFormat.XML;
            break;
        default:
            attributeFormat = AttributeFormat.OBJECT;
        }
        return attributeFormat;
    }
}
