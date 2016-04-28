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
package org.codice.ddf.ui.searchui.standard.endpoints;

import java.util.HashSet;
import java.util.Set;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class WorkspaceMetacardTypeImpl extends MetacardTypeImpl {

    public static final String WORKSPACE_TAG = "workspace";

    public static final String WORKSPACE_METACARD_TYPE_NAME = "metacard.workspace";

    public static final String WORKSPACE_METACARDS = "metacards";

    public static final String WORKSPACE_QUERIES = "queries";

    public static final String WORKSPACE_ROLES = "roles";

    public static final String WORKSPACE_UPDATED_ROLES = "updated-roles";

    private static final Set<AttributeDescriptor> DESCRIPTORS;

    static {
        DESCRIPTORS = new HashSet<>(BasicTypes.BASIC_METACARD.getAttributeDescriptors());

        DESCRIPTORS.add(new AttributeDescriptorImpl(WORKSPACE_ROLES,
                false    /* indexed */,
                true    /* stored */,
                false   /* tokenized */,
                true    /* multivalued */,
                BasicTypes.STRING_TYPE));

        DESCRIPTORS.add(new AttributeDescriptorImpl(WORKSPACE_QUERIES,
                false   /* indexed */,
                true    /* stored */,
                false   /* tokenized */,
                true    /* multivalued */,
                BasicTypes.XML_TYPE));
    }

    public WorkspaceMetacardTypeImpl() {
        this(WORKSPACE_METACARD_TYPE_NAME, DESCRIPTORS);
    }

    public WorkspaceMetacardTypeImpl(String name, Set<AttributeDescriptor> descriptors) {
        super(name, descriptors);
    }
}
