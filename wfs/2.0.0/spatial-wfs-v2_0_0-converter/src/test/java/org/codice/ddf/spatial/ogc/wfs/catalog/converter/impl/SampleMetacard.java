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

package org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class SampleMetacard {

    private MetacardImpl metacard = new MetacardImpl();

    private MetacardTypeImpl videoMetacardType;

    public SampleMetacard() {
        metacard.setContentTypeName("I have some content type");
        metacard.setContentTypeVersion("1.0.0");
        metacard.setCreatedDate(new Date());
        metacard.setEffectiveDate(new Date());
        metacard.setId("ID");
        metacard.setLocation("POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))");
        metacard.setMetadata("metadata a whole bunch of metadata");
        metacard.setModifiedDate(new Date());
        metacard.setResourceSize("123 is the size");
        metacard.setSourceId("sourceID");
        metacard.setTitle("This is my title");

        Set<AttributeDescriptor> descriptors = new HashSet<AttributeDescriptor>();
        descriptors.add(new AttributeDescriptorImpl("id", false, false, false, false,
                BasicTypes.LONG_TYPE));
        descriptors.add(new AttributeDescriptorImpl("version", false, false, false, false,
                BasicTypes.LONG_TYPE));
        descriptors.add(new AttributeDescriptorImpl("end_date", false, false, false, false,
                BasicTypes.DATE_TYPE));
        descriptors.add(new AttributeDescriptorImpl("filename", false, false, false, false,
                BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl("height", false, false, false, false,
                BasicTypes.LONG_TYPE));
        descriptors.add(new AttributeDescriptorImpl("index_id", false, false, false, false,
                BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl("other_tags_xml", false, false, false, false,
                BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl("repository_id", false, false, false, false,
                BasicTypes.LONG_TYPE));
        descriptors.add(new AttributeDescriptorImpl("start_date", false, false, false, false,
                BasicTypes.DATE_TYPE));
        descriptors.add(new AttributeDescriptorImpl("style_id", false, false, false, false,
                BasicTypes.INTEGER_TYPE));
        descriptors.add(new AttributeDescriptorImpl("width", false, false, false, false,
                BasicTypes.LONG_TYPE));
        descriptors.add(new AttributeDescriptorImpl("ground_geom", false, false, false, false,
                BasicTypes.GEO_TYPE));

        videoMetacardType = new MetacardTypeImpl("video_data_set", descriptors);
    }

    public MetacardImpl getMetacard() {
        return metacard;
    }

    public MetacardTypeImpl getMetacardType() {
        return videoMetacardType;
    }

}
