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
package org.codice.ddf.catalog.ui.metacard.workspace;

import java.util.HashSet;
import java.util.Set;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class SharingMetacardTypeImpl extends MetacardTypeImpl {

    public static final String SHARING_TAG = "sharing";

    public static final String SHARING_METACARD_TYPE_NAME = "metacard.sharing";

    public static final String SHARING_TYPE = "type";

    public static final String SHARING_PERMISSION = "permission";

    public static final String SHARING_VALUE = "value";

    private static final Set<AttributeDescriptor> DESCRIPTORS;

    static {
        DESCRIPTORS = new HashSet<>(BasicTypes.BASIC_METACARD.getAttributeDescriptors());

        DESCRIPTORS.add(new AttributeDescriptorImpl(SHARING_TYPE,
                false   /* indexed */,
                true    /* stored */,
                false   /* tokenized */,
                false   /* multivalued */,
                BasicTypes.STRING_TYPE));

        DESCRIPTORS.add(new AttributeDescriptorImpl(SHARING_PERMISSION,
                false   /* indexed */,
                true    /* stored */,
                false   /* tokenized */,
                false   /* multivalued */,
                BasicTypes.STRING_TYPE));

        DESCRIPTORS.add(new AttributeDescriptorImpl(SHARING_VALUE,
                false   /* indexed */,
                true    /* stored */,
                false   /* tokenized */,
                false   /* multivalued */,
                BasicTypes.STRING_TYPE));
    }

    public SharingMetacardTypeImpl() {
        this(SHARING_METACARD_TYPE_NAME, DESCRIPTORS);
    }

    public SharingMetacardTypeImpl(String name, Set<AttributeDescriptor> descriptors) {
        super(name, descriptors);
    }

}
