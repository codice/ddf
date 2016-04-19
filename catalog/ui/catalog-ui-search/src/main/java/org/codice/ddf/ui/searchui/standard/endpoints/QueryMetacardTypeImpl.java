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
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class QueryMetacardTypeImpl extends MetacardTypeImpl {

    public static final String QUERY_TAG = "query";

    public static final String QUERY_METACARD_TYPE_NAME = "metacard.query";

    public static final String QUERY_CQL = "cql";

    public static final String  QUERY_SOURCES = "sources";

    public static final String QUERY_ENTERPRISE = "enterprise";

    private static final Set<AttributeDescriptor> DESCRIPTORS;

    static {
        DESCRIPTORS = new HashSet<>();

        DESCRIPTORS.add(new AttributeDescriptorImpl(Metacard.TITLE,
                true    /* indexed */,
                true    /* stored */,
                false   /* tokenized */,
                false   /* multivalued */,
                BasicTypes.STRING_TYPE));

        DESCRIPTORS.add(new AttributeDescriptorImpl(QUERY_CQL,
                false   /* indexed */,
                true    /* stored */,
                false   /* tokenized */,
                false   /* multivalued */,
                BasicTypes.STRING_TYPE));

        DESCRIPTORS.add(new AttributeDescriptorImpl(QUERY_ENTERPRISE,
                false   /* indexed */,
                true    /* stored */,
                false   /* tokenized */,
                false   /* multivalued */,
                BasicTypes.BOOLEAN_TYPE));

        DESCRIPTORS.add(new AttributeDescriptorImpl(QUERY_SOURCES,
                false   /* indexed */,
                true    /* stored */,
                false   /* tokenized */,
                true    /* multivalued */,
                BasicTypes.STRING_TYPE));
    }

    public QueryMetacardTypeImpl() {
        this(QUERY_METACARD_TYPE_NAME, DESCRIPTORS);
    }

    public QueryMetacardTypeImpl(String name, Set<AttributeDescriptor> descriptors) {
        super(name, descriptors);
    }
}
