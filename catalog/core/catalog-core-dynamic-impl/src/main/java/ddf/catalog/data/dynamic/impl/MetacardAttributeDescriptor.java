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

import org.apache.commons.beanutils.DynaProperty;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;

/**
 * Implements the {@link AttributeDescriptor} and {@link AttributeType} for dynamic
 * metacards. Adds the properties that the catalog provider use - indexed, stored, multiValued,
 * and tokenized.
 */
public class MetacardAttributeDescriptor implements AttributeDescriptor, AttributeType {
    protected MetacardPropertyDescriptorImpl propertyDescriptor;


    /**
     * Creates a MetacardAttributeDescriptor given a {@link DynaProperty} descriptor.
     * @param descriptor {@link DynaProperty} descriptor for this attribute
     */
    public MetacardAttributeDescriptor(DynaProperty descriptor) {
        if (descriptor instanceof MetacardPropertyDescriptorImpl) {
            propertyDescriptor = (MetacardPropertyDescriptorImpl) descriptor;
        } else {
            propertyDescriptor = new MetacardPropertyDescriptorImpl(descriptor, true, false, true);
        }
    }

    @Override
    public String getName() {
        return propertyDescriptor.getName();
    }

    @Override
    public boolean isMultiValued() {
        return propertyDescriptor.isIndexed();
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
        return propertyDescriptor.getFormat();

    }
}
