/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.spatial.ogc.catalog.common;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.hasItems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.ContentTypeImpl;
import ddf.catalog.data.Metacard;

public class TestContentTypeFilterDelegate {

    private static final String MOCK_PROPERTY = "mockProperty";

    private static final String MOCK_TYPE_IMAGE = "image";

    private static final String MOCK_TYPE_VIDEO = "video";

    private final List<ContentType> contentTypeImage = Arrays
            .asList((ContentType) new ContentTypeImpl(MOCK_TYPE_IMAGE, ""));

    private final List<ContentType> contentTypeVideo = Arrays
            .asList((ContentType) new ContentTypeImpl(MOCK_TYPE_VIDEO, ""));

    @Before
    public void setUp() {
    }

    @Test
    public void testContentTypeFilterDelegate() {
        ContentTypeFilterDelegate delegate = new ContentTypeFilterDelegate();
        assertNotNull(delegate);
    }

    @Test
    public void testPropertyIsEqualToStringStringBooleanContentType() {

        ContentTypeFilterDelegate delegate = new ContentTypeFilterDelegate();
        List<ContentType> types = delegate.propertyIsEqualTo(Metacard.CONTENT_TYPE,
                MOCK_TYPE_IMAGE, true);
        // Ensure that the content type was returned correctly
        assertNotNull(types);
        assertTrue(types.size() == 1);
        assertTrue((types.get(0).getName()).equals(MOCK_TYPE_IMAGE));
    }

    @Test
    public void testPropertyIsEqualToStringStringBoolean() {

        ContentTypeFilterDelegate delegate = new ContentTypeFilterDelegate();
        List<ContentType> types = delegate.propertyIsEqualTo(MOCK_PROPERTY, MOCK_TYPE_IMAGE, true);
        // Ensure this is an empty ContentType list
        assertNotNull(types);
        assertTrue(types.isEmpty());
    }

    @Test
    public void testPropertyIsLikeStringStringBooleanContentType() {
        ContentTypeFilterDelegate delegate = new ContentTypeFilterDelegate();
        List<ContentType> types = delegate.propertyIsLike(Metacard.CONTENT_TYPE, MOCK_TYPE_IMAGE,
                true);
        // Ensure that the content type was returned correctly
        assertNotNull(types);
        assertTrue(types.size() == 1);
        assertTrue((types.get(0).getName()).equals(MOCK_TYPE_IMAGE));
    }

    @Test
    public void testPropertyIsLikeStringStringBoolean() {

        ContentTypeFilterDelegate delegate = new ContentTypeFilterDelegate();
        List<ContentType> types = delegate.propertyIsLike(MOCK_PROPERTY, MOCK_TYPE_IMAGE, true);
        // Ensure this is an empty ContentType list
        assertNotNull(types);
        assertTrue(types.isEmpty());
    }

    @Test
    public void testAnd() {

        ContentTypeFilterDelegate delegate = new ContentTypeFilterDelegate();
        @SuppressWarnings("unchecked")
        List<ContentType> types = delegate.and(Arrays.asList(this.contentTypeImage,
                this.contentTypeVideo));

        // Ensure the lists were combined into a single list
        assertNotNull(types);
        assertTrue(!types.isEmpty());
        assertThat(types, hasItems(contentTypeImage.get(0), contentTypeVideo.get(0)));

    }

    @Test
    public void testAndEmptyList() {

        ContentTypeFilterDelegate delegate = new ContentTypeFilterDelegate();
        @SuppressWarnings("unchecked")
        List<ContentType> types = delegate.and(new ArrayList<List<ContentType>>());

        // Ensure the list returned is empty
        assertNotNull(types);
        assertTrue(types.isEmpty());

    }

    @Test
    public void testOr() {

        ContentTypeFilterDelegate delegate = new ContentTypeFilterDelegate();
        @SuppressWarnings("unchecked")
        List<ContentType> types = delegate.or(Arrays.asList(this.contentTypeImage,
                this.contentTypeVideo));

        // Ensure the lists were combined into a single list
        assertNotNull(types);
        assertTrue(!types.isEmpty());
        assertThat(types, hasItems(contentTypeImage.get(0), contentTypeVideo.get(0)));

    }

    @Test
    public void testOrEmptyList() {

        ContentTypeFilterDelegate delegate = new ContentTypeFilterDelegate();
        @SuppressWarnings("unchecked")
        List<ContentType> types = delegate.or(new ArrayList<List<ContentType>>());

        // Ensure the list returned is empty
        assertNotNull(types);
        assertTrue(types.isEmpty());

    }

}
