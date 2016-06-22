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
package org.codice.ddf.catalog.transformer.zip;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;

public class TestZipDecompression {

    private static final String ZIP_FILE_NAME = "signed.zip";

    private static final String ZIP_FILE_PATH = TestZipCompression.class.getResource(
            File.separator + ZIP_FILE_NAME)
            .getPath();

    private ZipDecompression zipDecompression;

    private InputStream zipInputStream;

    private Map<String, Serializable> arguments;

    private List<String> zipContentList = Arrays.asList("id1",
            "id2",
            "id3",
            "id3-localresource.txt");

    private ZipValidator zipValidator;

    @Before
    public void setUp() throws Exception {
        zipDecompression = new ZipDecompression();
        zipInputStream = new ZipInputStream(new FileInputStream(ZIP_FILE_PATH));
        arguments = new HashMap<>();
        arguments.put(ZipDecompression.FILE_PATH, "target/");
        arguments.put(ZipDecompression.FILE_NAME, ZIP_FILE_NAME);
        zipValidator = mock(ZipValidator.class);
        when(zipValidator.validateZipFile(anyString())).thenReturn(true);
        zipDecompression.setZipValidator(zipValidator);
    }

    @Test(expected = CatalogTransformerException.class)
    public void testTransformWithNullArguments() throws Exception {
        List<Metacard> result = zipDecompression.transform(zipInputStream, null);
        assertThat(result, nullValue());
    }

    @Test(expected = CatalogTransformerException.class)
    public void testTransformWithEmptyArguments() throws Exception {
        List<Metacard> result = zipDecompression.transform(zipInputStream, new HashMap<>());
        assertThat(result, nullValue());
    }

    @Test(expected = CatalogTransformerException.class)
    public void testTransformWithBadArguments() throws Exception {
        Map<String, Serializable> badMap = new HashMap<>();
        badMap.put("bad", "arg");
        List<Metacard> result = zipDecompression.transform(zipInputStream, badMap);
        assertThat(result, nullValue());

    }

    @Test(expected = CatalogTransformerException.class)
    public void testTransformWithNullStream() throws Exception {
        List<Metacard> result = zipDecompression.transform(null, arguments);
        assertThat(result, nullValue());
    }

    @Test
    public void testTransform() throws Exception {
        List<Metacard> result = zipDecompression.transform(zipInputStream, arguments);
        assertThat(result, notNullValue());
        assertMetacardList(result);
    }

    public void assertMetacardList(List<Metacard> metacardList) {
        for (Metacard metacard : metacardList) {
            assertThat(zipContentList, hasItem(metacard.getId()));
        }
    }
}
