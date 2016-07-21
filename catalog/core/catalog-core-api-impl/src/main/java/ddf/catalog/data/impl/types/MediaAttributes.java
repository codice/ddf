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
package ddf.catalog.data.impl.types;

import java.util.HashSet;
import java.util.Set;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.types.Media;

/**
 * This class provides attributes that reflect metadata about media resources associated with a
 * metacard.  This includes bit rates, encoding and compression.
 */
public class MediaAttributes implements Media, MetacardType {

    private static final Set<AttributeDescriptor> DESCRIPTORS = new HashSet<>();

    private static final String NAME = "media";

    static {
        DESCRIPTORS.add(new AttributeDescriptorImpl(FORMAT,
                true /* indexed */,
                true /* stored */,
                true /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));
        DESCRIPTORS.add(new AttributeDescriptorImpl(FORMAT_VERSION,
                true /* indexed */,
                true /* stored */,
                true /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));
        DESCRIPTORS.add(new AttributeDescriptorImpl(BITS_PER_SECOND,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.INTEGER_TYPE));
        DESCRIPTORS.add(new AttributeDescriptorImpl(BITS_PER_SAMPLE,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.INTEGER_TYPE));
        DESCRIPTORS.add(new AttributeDescriptorImpl(COMPRESSION,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.INTEGER_TYPE));
        DESCRIPTORS.add(new AttributeDescriptorImpl(ENCODING,
                true /* indexed */,
                true /* stored */,
                true /* tokenized */,
                true /* multivalued */,
                BasicTypes.STRING_TYPE));
        DESCRIPTORS.add(new AttributeDescriptorImpl(FRAME_CENTER,
                true /* indexed */,
                true /* stored */,
                true /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));
        DESCRIPTORS.add(new AttributeDescriptorImpl(FRAMES_PER_SECOND,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.INTEGER_TYPE));
        DESCRIPTORS.add(new AttributeDescriptorImpl(HEIGHT,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.INTEGER_TYPE));
        DESCRIPTORS.add(new AttributeDescriptorImpl(NUMBER_OF_BANDS,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.INTEGER_TYPE));
        DESCRIPTORS.add(new AttributeDescriptorImpl(SCANNING_MODE,
                true /* indexed */,
                true /* stored */,
                true /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));
        DESCRIPTORS.add(new AttributeDescriptorImpl(TYPE,
                true /* indexed */,
                true /* stored */,
                true /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));
        DESCRIPTORS.add(new AttributeDescriptorImpl(WIDTH,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.INTEGER_TYPE));
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

    @Override
    public String getName() {
        return NAME;
    }
}
