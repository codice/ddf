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
package ddf.catalog.data.impl;

import java.util.Set;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardType;

/**
 * Used for test purposes, this is an empty {@link MetacardType} that returns null for all its
 * required fields.
 * 
 */
public class EmptyMetacardType implements MetacardType {

    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<AttributeDescriptor> getAttributeDescriptors() {
        return null;
    }

    @Override
    public AttributeDescriptor getAttributeDescriptor(String attributeName) {
        return null;
    }

}
