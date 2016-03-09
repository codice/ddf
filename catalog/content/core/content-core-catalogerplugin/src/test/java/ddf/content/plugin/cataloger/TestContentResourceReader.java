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
package ddf.content.plugin.cataloger;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.content.ContentFramework;
import ddf.content.ContentFrameworkException;
import ddf.content.data.ContentItem;
import ddf.content.operation.ReadRequest;
import ddf.content.operation.ReadResponse;
import ddf.content.operation.impl.ReadResponseImpl;
import ddf.content.resource.impl.ContentResourceReader;

public class TestContentResourceReader {

    private static final String URL_CONTENT_SCHEME = "content";

    private static final String VERSION = "1.0";

    private static final String ID = "ContentResourceReader";

    private static final String TITLE = "Content Resource Reader";

    private static final String DESCRIPTION = "Retrieves a file from the DDF Content Repository.";

    private static final String ORGANIZATION = "DDF";

    private static final String FILE_NAME = "/tmp/test.jpg";

    private ContentFramework mockContentFramework = mock(ContentFramework.class);

    private ContentItem mockContentItem = mock(ContentItem.class);

    private ReadResponse readResponse;

    private ContentResourceReader resourceReader = null;

    @Before
    public void setUp() throws Exception {
        resourceReader = new ContentResourceReader(mockContentFramework);

        setupMocks();
    }

    @Test
    public void testSimpleMethods() {
        assertThat(DESCRIPTION, is(resourceReader.getDescription()));
        assertThat(ID, is(resourceReader.getId()));
        assertThat(ORGANIZATION, is(resourceReader.getOrganization()));
        assertThat(TITLE, is(resourceReader.getTitle()));
        assertThat(VERSION, is(resourceReader.getVersion()));
        assertThat(resourceReader, notNullValue());
        assertThat(resourceReader.getSupportedSchemes(), hasItems(URL_CONTENT_SCHEME));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testNullResourceURI() throws Exception {
        resourceReader.retrieveResource(null, null);
    }

    @Test
    public void testNonContentURI() throws Exception {
        ResourceResponse response = resourceReader.retrieveResource(new URI(
                "http://localhost/index.html"), null);
        assertThat(response, nullValue());
    }

    @Test
    public void testContentURI() throws Exception {
        ResourceResponse response = resourceReader.retrieveResource(new URI(
                "content://localhost/test.jpg"), null);
        assertThat(response, notNullValue());
        assertThat(response.getResource(), notNullValue());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testContentFrameworkThrows() throws Exception {
        when(mockContentFramework.read(any(ReadRequest.class))).thenThrow(new ContentFrameworkException(
                "Test not found."));
        resourceReader.retrieveResource(new URI("content://localhost/test.jpg"), null);
    }

    private void setupMocks() throws Exception {
        readResponse = new ReadResponseImpl(null, mockContentItem);
        when(mockContentItem.getFile()).thenReturn(new File(FILE_NAME));
        when(mockContentItem.getInputStream()).thenReturn(null);
        when(mockContentFramework.read(any(ReadRequest.class))).thenReturn(readResponse);
    }
}
