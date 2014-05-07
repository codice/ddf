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
package ddf.catalog.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.FileDataSource;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.activation.MimetypesFileTypeMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.internal.runners.statements.Fail;
import org.junit.runner.notification.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.event.DeliveryException;
import ddf.catalog.event.EventException;
import ddf.catalog.event.InvalidSubscriptionException;
import ddf.catalog.event.SubscriptionExistsException;
import ddf.catalog.event.SubscriptionNotFoundException;
import ddf.catalog.federation.FederationException;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.impl.ResourceImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;

public class ResourceImplTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceImplTest.class);

    private static final String testName = ResourceImplTest.class.getSimpleName();

    private File content;

    private MimeType mimeType;

    @Before
    public void setUp() {
        content = new File("src/test/resources/data/i4ce.png");
        MimetypesFileTypeMap mimeMapper = new MimetypesFileTypeMap();
        try {
            mimeType = new MimeType(mimeMapper.getContentType(content));
        } catch (MimeTypeParseException e) {
            LOGGER.error("Mime parser Failure", e);
            new Failure(null, e);
        }
    }

    @Test
    public void testResourceImplConstructors() {
        InputStream is;
        try {
            is = new FileInputStream(content);
            ResourceImpl ri = new ResourceImpl(is, testName);
            ri.setSize(content.length());
            assertEquals(is, ri.getInputStream());
            assertEquals(null, ri.getMimeType());
            assertEquals(content.length(), ri.getSize());
            assertNotNull(ri.toString());

            ri = new ResourceImpl(is, mimeType, testName);
            ri.setSize(content.length());
            assertEquals(is, ri.getInputStream());
            assertEquals(mimeType.toString(), ri.getMimeType().toString());
            assertEquals(content.length(), ri.getSize());
            assertEquals(mimeType.toString(), ri.getMimeTypeValue());
            assertNotNull(ri.toString());

            ri = new ResourceImpl(is, mimeType.toString(), testName);
            ri.setSize(content.length());
            assertEquals(is, ri.getInputStream());
            assertEquals(mimeType.toString(), ri.getMimeType().toString());
            assertEquals(content.length(), ri.getSize());
            assertNotNull(ri.toString());

        } catch (IOException e) {
            LOGGER.error("IO Failure", e);
            new Failure(null, e);
        }
    }

    @Test
    public void testResourceImplNullInputStream() {
        InputStream is = null;
        ResourceImpl ri = new ResourceImpl(is, mimeType, testName);
        ri.setSize(content.length());
        assertEquals(is, ri.getInputStream());
        assertEquals(mimeType.toString(), ri.getMimeType().toString());
        assertEquals(content.length(), ri.getSize());
    }

    @Test
    public void testResourceImplNullMimeType() {
        InputStream is = null;
        try {
            is = new FileInputStream(content);
        } catch (IOException e) {
            LOGGER.error("IO Failure", e);
            new Failure(null, e);
        }
        ResourceImpl ri = new ResourceImpl(is, (MimeType) null, testName);
        assertEquals(null, ri.getMimeType());

        ri = new ResourceImpl(is, (String) null, testName);
        assertEquals(null, ri.getMimeType());
    }

    @Test
    public void testResourceImplSizeNotSet() {
        InputStream is;
        try {
            is = new FileInputStream(content);
            ResourceImpl ri = new ResourceImpl(is, mimeType, testName);
            assertEquals(-1, ri.getSize());
        } catch (IOException e) {
            LOGGER.error("IO Failure", e);
            new Failure(null, e);
        }
    }

    @Test
    public void testResourceImplNegativeSize() {
        InputStream is;
        try {
            is = new FileInputStream(content);
            ResourceImpl ri = new ResourceImpl(is, mimeType, testName);
            ri.setSize(-20l);
            assertEquals(-1, ri.getSize());
        } catch (IOException e) {
            LOGGER.error("IO Failure", e);
            new Failure(null, e);
        }
    }

    @Test
    public void testResourceNameNotSet() {
        InputStream is;
        try {
            is = new FileInputStream(content);
            ResourceImpl ri = new ResourceImpl(is, mimeType, null);
            assertEquals(null, ri.getName());
        } catch (IOException e) {
            LOGGER.error("IO Failure", e);
            new Failure(null, e);
        }
    }

    @Test
    public void testInvalidMimeType() {
        InputStream is;
        try {
            is = new FileInputStream(content);
            ResourceImpl ri = new ResourceImpl(is, "123467890", null);
            assertEquals(null, ri.getMimeType());
        } catch (IOException e) {
            LOGGER.error("IO Failure", e);
            new Failure(null, e);
        }
    }

}
