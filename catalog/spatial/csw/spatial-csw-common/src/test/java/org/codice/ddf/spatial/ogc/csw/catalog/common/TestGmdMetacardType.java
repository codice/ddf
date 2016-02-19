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
 **/
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.Set;

import org.apache.cxf.common.util.CollectionUtils;
import org.junit.Test;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;

public class TestGmdMetacardType {

    @Test
    public void testConstruction() {
        GmdMetacardType gmdMetacardType = new GmdMetacardType();

        assertThat(gmdMetacardType, not(nullValue()));

        Set<AttributeDescriptor> descriptors = gmdMetacardType.getAttributeDescriptors();
        assertThat(descriptors, not(nullValue()));
        assertThat(CollectionUtils.isEmpty(descriptors), is(false));

        assertThat(gmdMetacardType.getAttributeDescriptor(Metacard.ID)
                .isMultiValued(), is(false));
    }

    @Test
    public void testGmdMetacardHasBasicMetacardDescriptorsAsIsStoredTrue() {
        MetacardType gmdMetacard = new GmdMetacardType();
        assertThat(gmdMetacard.getAttributeDescriptor(Metacard.ID)
                .isStored(), is(true));
        assertThat(gmdMetacard.getAttributeDescriptor(Metacard.TITLE)
                .isStored(), is(true));
        assertThat(gmdMetacard.getAttributeDescriptor(Metacard.METADATA)
                .isStored(), is(true));
        assertThat(gmdMetacard.getAttributeDescriptor(Metacard.EFFECTIVE)
                .isStored(), is(true));
        assertThat(gmdMetacard.getAttributeDescriptor(Metacard.MODIFIED)
                .isStored(), is(true));
        assertThat(gmdMetacard.getAttributeDescriptor(Metacard.CREATED)
                .isStored(), is(true));
        assertThat(gmdMetacard.getAttributeDescriptor(Metacard.RESOURCE_URI)
                .isStored(), is(true));
        assertThat(gmdMetacard.getAttributeDescriptor(Metacard.CONTENT_TYPE)
                .isStored(), is(true));
    }

}
