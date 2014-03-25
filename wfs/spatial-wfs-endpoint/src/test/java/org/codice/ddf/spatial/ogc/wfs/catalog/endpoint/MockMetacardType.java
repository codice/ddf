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
package org.codice.ddf.spatial.ogc.wfs.catalog.endpoint;

import java.util.HashMap;
import java.util.Map;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class MockMetacardType extends MetacardTypeImpl {

    private static final long serialVersionUID = 1L;

    public static final String NAME = "mockMetacard";

    public static final String IMAGE = "image";

    public static final String VIDEO = "video";

    public static final String[] TYPES = {IMAGE, VIDEO};

    public static final Map<String, Object> PROPERTIES;

    static {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Metacard.CONTENT_TYPE, TYPES);
        PROPERTIES = props;
    }

    public MockMetacardType() {
        super(NAME, null);

        descriptors.addAll(BasicTypes.BASIC_METACARD.getAttributeDescriptors());

        descriptors.add(new AttributeDescriptorImpl("binary", true, true, false, true,
                BasicTypes.BINARY_TYPE));
        descriptors.add(new AttributeDescriptorImpl("boolean", true, true, false, true,
                BasicTypes.BOOLEAN_TYPE));
        descriptors.add(new AttributeDescriptorImpl("date", true, true, false, true,
                BasicTypes.DATE_TYPE));
        descriptors.add(new AttributeDescriptorImpl("double", true, true, false, true,
                BasicTypes.DOUBLE_TYPE));
        descriptors.add(new AttributeDescriptorImpl("float", true, true, false, true,
                BasicTypes.FLOAT_TYPE));
        descriptors.add(new AttributeDescriptorImpl("geo", true, true, false, true,
                BasicTypes.GEO_TYPE));
        descriptors.add(new AttributeDescriptorImpl("integer", true, true, false, true,
                BasicTypes.INTEGER_TYPE));
        descriptors.add(new AttributeDescriptorImpl("long", true, true, false, true,
                BasicTypes.LONG_TYPE));
        descriptors.add(new AttributeDescriptorImpl("object", true, true, false, true,
                BasicTypes.OBJECT_TYPE));
        descriptors.add(new AttributeDescriptorImpl("short", true, true, false, true,
                BasicTypes.SHORT_TYPE));
        descriptors.add(new AttributeDescriptorImpl("string", true, true, false, true,
                BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl("xml", true, true, false, true,
                BasicTypes.XML_TYPE));
        descriptors.add(new AttributeDescriptorImpl("mock.effective", true, true, false, true,
                BasicTypes.DATE_TYPE));

    }

}
