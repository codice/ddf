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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.transform.CatalogTransformerException;

public class TestZipCompression {

    private ZipCompression zipCompression;

    private SourceResponse sourceResponse;

    private Map<String, Serializable> filePathArgument;

    private CatalogFramework catalogFramework;

    private static final String LOCAL_RESOURCE_FILENAME = "localresource.txt";

    private static final String LOCAL_RESOURCE_PATH = TestZipCompression.class.getResource(
            File.separator + LOCAL_RESOURCE_FILENAME)
            .getPath();

    private static final String ZIP_FILE_PATH = "target/signed.zip";

    File file = new File(ZIP_FILE_PATH);

    private InputStream resourceFileStream;

    @Before
    public void setUp() throws Exception {
        zipCompression = new ZipCompression();
        sourceResponse = generateMetacardList("ddf.distribution", false, Arrays.asList("id1",
                "id2"));
        filePathArgument = new HashMap<>();
        filePathArgument.put("filePath", ZIP_FILE_PATH);
        catalogFramework = mock(CatalogFramework.class);

        Resource resource = mock(Resource.class);
        resourceFileStream = new FileInputStream(new File(LOCAL_RESOURCE_PATH));
        when(resource.getName()).thenReturn(LOCAL_RESOURCE_FILENAME);
        when(resource.getInputStream()).thenReturn(resourceFileStream);
        ResourceResponse resourceResponse = new ResourceResponseImpl(resource);
        when(catalogFramework.getLocalResource(any(ResourceRequest.class))).thenReturn(
                resourceResponse);
        zipCompression.setCatalogFramework(catalogFramework);

    }

    @Test(expected = CatalogTransformerException.class)
    public void testCompressionNullArguments() throws Exception {
        BinaryContent binaryContent = zipCompression.transform(sourceResponse, null);
        assertThat(binaryContent, nullValue());
        Files.deleteIfExists(file.toPath());
    }

    @Test(expected = CatalogTransformerException.class)
    public void testCompressionNullSourceResponse() throws Exception {
        BinaryContent binaryContent = zipCompression.transform(null, filePathArgument);
        assertThat(binaryContent, nullValue());
        Files.deleteIfExists(file.toPath());
    }

    @Test(expected = CatalogTransformerException.class)
    public void testCompressioNullListInSourceResponse() throws Exception {
        BinaryContent binaryContent = zipCompression.transform(new SourceResponseImpl(null, null),
                filePathArgument);
        assertThat(binaryContent, nullValue());
        Files.deleteIfExists(file.toPath());
    }

    @Test(expected = CatalogTransformerException.class)
    public void testCompressionEmptyListInSourceResponse() throws Exception {
        BinaryContent binaryContent = zipCompression.transform(new SourceResponseImpl(null,
                new ArrayList<>()), filePathArgument);
        assertThat(binaryContent, nullValue());
        Files.deleteIfExists(file.toPath());
    }

    @Test(expected = CatalogTransformerException.class)
    public void testCompressionEmptyArguments() throws Exception {
        BinaryContent binaryContent = zipCompression.transform(sourceResponse, new HashMap<>());
        assertThat(binaryContent, nullValue());
        Files.deleteIfExists(file.toPath());
    }

    @Test(expected = CatalogTransformerException.class)
    public void testCompressionMissingArguments() throws Exception {
        HashMap<String, Serializable> arguments = new HashMap<>();
        arguments.put("bad", "argument");
        BinaryContent binaryContent = zipCompression.transform(sourceResponse, null);
        assertThat(binaryContent, nullValue());
        Files.deleteIfExists(file.toPath());
    }

    @Test
    public void testCompressionWithFilePath() throws Exception {
        BinaryContent binaryContent = zipCompression.transform(sourceResponse, filePathArgument);
        assertThat(binaryContent, notNullValue());
        assertZipContents(binaryContent, Arrays.asList(ZipCompression.METACARD_PATH + "id1",
                ZipCompression.METACARD_PATH + "id2"));
        Files.deleteIfExists(file.toPath());
    }

    @Test
    public void testCompressionWithContent() throws Exception {
        List<String> idList = Arrays.asList("id1", "id2", "id3");
        SourceResponse sourceResponse = generateMetacardList("", true, idList);
        BinaryContent binaryContent = zipCompression.transform(sourceResponse, filePathArgument);
        assertThat(binaryContent, notNullValue());

        List<String> assertionList = Arrays.asList(
                ZipCompression.METACARD_PATH + "id1",
                ZipCompression.METACARD_PATH + "id2",
                ZipCompression.METACARD_PATH + "id3",
                "content/id3-localresource.txt");
        assertZipContents(binaryContent, assertionList);

        Files.deleteIfExists(file.toPath());

    }

    private void assertZipContents(BinaryContent binaryContent, List<String> ids)
            throws IOException {
        ZipInputStream zipInputStream = (ZipInputStream) binaryContent.getInputStream();
        List<String> entryNames = new ArrayList<>();

        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            entryNames.add(zipEntry.getName());
            zipEntry = zipInputStream.getNextEntry();
        }

        assertThat(entryNames.size(), is(ids.size()));

        for (String id : ids) {
            assertThat(entryNames, hasItem(id));
        }
    }

    private SourceResponse generateMetacardList(String sourceId, boolean setResourceUri,
            List<String> ids) {
        List<Result> resultList = new ArrayList<>();

        for (String string : ids) {
            MetacardImpl metacard = new MetacardImpl();
            metacard.setId(string);
            metacard.setSourceId(sourceId);

            if (setResourceUri) {
                try {
                    metacard.setResourceURI(new URI("content:" + metacard.getId()));
                } catch (URISyntaxException e) {
                    // ignore
                }
            }

            Result result = new ResultImpl(metacard);

            resultList.add(result);
        }
        SourceResponse sourceResponse = new SourceResponseImpl(null, resultList);
        return sourceResponse;
    }
}
