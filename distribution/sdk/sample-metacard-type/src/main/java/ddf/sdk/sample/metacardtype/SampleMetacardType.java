/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package ddf.sdk.sample.metacardtype;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class SampleMetacardType extends MetacardTypeImpl {
    private static final Set<AttributeDescriptor> DESCRIPTORS;

    private static final String NAME = "sample";

    static {
        Set<AttributeDescriptor> descriptors = new HashSet<>();
        descriptors.add(new AttributeDescriptorImpl("sample.string",
                true /* indexed */,
                true /* stored */,
                true /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl("sample.integer",
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.INTEGER_TYPE));
        descriptors.addAll(BasicTypes.BASIC_METACARD.getAttributeDescriptors());
        DESCRIPTORS = Collections.unmodifiableSet(descriptors);
    }

    public SampleMetacardType() {
        super(NAME, DESCRIPTORS);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Set<AttributeDescriptor> getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public AttributeDescriptor getAttributeDescriptor(String name) {
        for (AttributeDescriptor attributeDescriptor : DESCRIPTORS) {
            if (attributeDescriptor.getName()
                    .equals(name)) {
                return attributeDescriptor;
            }
        }
        return null;
    }
}
