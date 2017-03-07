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
package org.codice.ddf.catalog.plugin.metadata.backup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.codice.ddf.catalog.async.data.api.internal.ProcessCreateItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessDeleteItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessRequest;
import org.codice.ddf.catalog.async.data.api.internal.ProcessResourceItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessUpdateItem;
import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;

public class MetadataBackupPluginTest {

    private static final String METACARD_TRANSFORMER_ID = "metadata";

    private static final List<String> METACARD_IDS = Arrays.asList("1b6482b1b8f730e343a96d61e0e089",
            "2b6482b2b8f730e343a96d61e0e089");

    private static final String XML_METADATA =
            "        <csw:Record\n" + "            xmlns:ows=\"http://www.opengis.net/ows\"\n"
                    + "            xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
                    + "            xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
                    + "            xmlns:dct=\"http://purl.org/dc/terms/\"\n"
                    + "            xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n"
                    + "            <dc:identifier>$id$</dc:identifier>\n"
                    + "            <dc:title>Aliquam fermentum purus quis arcu</dc:title>\n"
                    + "            <dc:type>http://purl.org/dc/dcmitype/Text</dc:type>\n"
                    + "            <dc:subject>Hydrography--Dictionaries</dc:subject>\n"
                    + "            <dc:format>application/pdf</dc:format>\n"
                    + "            <dc:date>2006-05-12</dc:date>\n"
                    + "            <dct:abstract>Vestibulum quis ipsum sit amet metus imperdiet vehicula. Nulla scelerisque cursus mi.</dct:abstract>\n"
                    + "            <ows:BoundingBox crs=\"urn:x-ogc:def:crs:EPSG:6.11:4326\">\n"
                    + "                <ows:LowerCorner>44.792 -6.171</ows:LowerCorner>\n"
                    + "                <ows:UpperCorner>51.126 -2.228</ows:UpperCorner>\n"
                    + "            </ows:BoundingBox>\n" + "        </csw:Record>";

    private static final String OUTPUT_DIRECTORY = "target/";

    private static final int DEPTH = 4;

    private MetadataBackupPlugin metadataBackupPlugin;

    private ProcessRequest<ProcessCreateItem> createRequest;

    private ProcessRequest<ProcessUpdateItem> updateRequest;

    private ProcessRequest<ProcessDeleteItem> deleteRequest;

    private MetacardTransformer metacardTransformer;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        metacardTransformer = mock(MetacardTransformer.class);
        BinaryContent binaryContent =
                new BinaryContentImpl(new ByteArrayInputStream(XML_METADATA.getBytes(
                        StandardCharsets.UTF_8)));
        when(metacardTransformer.transform(any(Metacard.class),
                anyMap())).thenReturn(binaryContent);
        metadataBackupPlugin = new MetadataBackupPlugin();
        metadataBackupPlugin.setMetacardTransformerId(METACARD_TRANSFORMER_ID);
        metadataBackupPlugin.setMetacardTransformer(metacardTransformer);
        createRequest = generateProcessRequest(ProcessCreateItem.class);
        updateRequest = generateProcessRequest(ProcessUpdateItem.class);
        deleteRequest = generateDeleteRequest();
        metadataBackupPlugin.setOutputDirectory(OUTPUT_DIRECTORY);
        metadataBackupPlugin.setFolderDepth(DEPTH);
    }

    @Test
    public void testOutputDirectory() {
        assertThat(metadataBackupPlugin.getOutputDirectory(), is(OUTPUT_DIRECTORY));
    }

    @Test
    public void testFolderDepth() {
        assertThat(metadataBackupPlugin.getFolderDepth(), is(DEPTH));
    }

    @Test
    public void testDefaultKeepDeletedMetacards() {
        assertThat(metadataBackupPlugin.getKeepDeletedMetacards(), is(false));
    }

    @Test
    public void testMetacardTransformerId() {
        assertThat(metadataBackupPlugin.getMetacardTransformerId(), is(METACARD_TRANSFORMER_ID));
    }

    @Test
    public void testFolderDepthMax() {
        metadataBackupPlugin.setFolderDepth(5);
        assertThat(metadataBackupPlugin.getFolderDepth(), is(DEPTH));
    }

    @Test
    public void testFolderDepthMin() {
        metadataBackupPlugin.setFolderDepth(0);
        assertThat(metadataBackupPlugin.getFolderDepth(), is(0));
    }

    @Test
    public void testRefresh() {
        String newBackupDir = "target/temp";
        String newMetacardTransformer = "tika";
        Map<String, Object> properties = new HashMap<>();
        properties.put("outputDirectory", newBackupDir);
        properties.put("metacardTransformerId", newMetacardTransformer);
        properties.put("folderDepth", 2);
        properties.put("keepDeletedMetacards", Boolean.TRUE);
        metadataBackupPlugin.refresh(properties);
        assertThat(metadataBackupPlugin.getKeepDeletedMetacards(), is(true));
        assertThat(metadataBackupPlugin.getOutputDirectory(), is(newBackupDir));
        assertThat(metadataBackupPlugin.getFolderDepth(), is(2));
        assertThat(metadataBackupPlugin.getMetacardTransformerId(), is(newMetacardTransformer));
    }

    @Test
    public void testRefreshBadValues() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("outputDirectory", 2);
        properties.put("metacardTransformerId", 2);
        properties.put("folderDepth", "bad");
        properties.put("keepDeletedMetacards", "bad");
        metadataBackupPlugin.refresh(properties);
        assertThat(metadataBackupPlugin.getKeepDeletedMetacards(), is(false));
        assertThat(metadataBackupPlugin.getOutputDirectory(), is(OUTPUT_DIRECTORY));
        assertThat(metadataBackupPlugin.getFolderDepth(), is(DEPTH));
        assertThat(metadataBackupPlugin.getMetacardTransformerId(), is(METACARD_TRANSFORMER_ID));
    }

    @Test
    public void testRefreshEmptyStrings() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("outputDirectory", "");
        properties.put("metacardTransformerId", "");
        metadataBackupPlugin.refresh(properties);
        assertThat(metadataBackupPlugin.getOutputDirectory(), is(OUTPUT_DIRECTORY));
        assertThat(metadataBackupPlugin.getMetacardTransformerId(), is(METACARD_TRANSFORMER_ID));
    }

    @Test(expected = PluginExecutionException.class)
    public void testCreateRequestEmptyOutputDirectory() throws Exception {
        metadataBackupPlugin.setOutputDirectory("");
        metadataBackupPlugin.processCreate(createRequest);
    }

    @Test(expected = PluginExecutionException.class)
    public void testUpdateRequestEmptyOutputDirectory() throws Exception {
        metadataBackupPlugin.setOutputDirectory("");
        metadataBackupPlugin.processUpdate(updateRequest);
    }

    @Test(expected = PluginExecutionException.class)
    public void testDeleteRequestEmptyOutputDirectory() throws Exception {
        metadataBackupPlugin.setOutputDirectory("");
        metadataBackupPlugin.processDelete(deleteRequest);
    }

    @Test(expected = PluginExecutionException.class)
    public void testCreateRequestNullMetacardTransformer() throws Exception {
        metadataBackupPlugin.setMetacardTransformer(null);
        metadataBackupPlugin.processCreate(createRequest);
    }

    @Test
    public void testCreateRequestNoDepth() throws Exception {
        int depth = 0;
        metadataBackupPlugin.setFolderDepth(depth);
        ProcessRequest<ProcessCreateItem> newCreateRequest = metadataBackupPlugin.processCreate(
                createRequest);
        assertFiles(newCreateRequest.getProcessItems(), depth);
    }

    @Test
    public void testCreateRequestVaryingDepths() throws Exception {
        for (int depth = 0; depth < 5; depth++) {
            metadataBackupPlugin.setFolderDepth(depth);
            ProcessRequest<ProcessCreateItem> newCreateRequest = metadataBackupPlugin.processCreate(
                    createRequest);
            assertFiles(newCreateRequest.getProcessItems(), depth);
        }
    }

    @Test
    public void testCreateRequestFailedTransform() throws Exception {
        when(metacardTransformer.transform(any(Metacard.class), anyMap())).thenThrow(
                CatalogTransformerException.class);
        int depth = 0;
        metadataBackupPlugin.setFolderDepth(depth);
        metadataBackupPlugin.processCreate(createRequest);
        assertFileDoesNotExist(depth);
    }

    @Test
    public void testCreateRequestNullBinaryContent() throws Exception {
        when(metacardTransformer.transform(any(Metacard.class), anyMap())).thenReturn(null);
        int depth = 0;
        metadataBackupPlugin.setFolderDepth(depth);
        metadataBackupPlugin.processCreate(createRequest);
        assertFileDoesNotExist(depth);
    }

    @Test
    public void testCreateRequestNullBinaryContentInputStream() throws Exception {
        BinaryContent binaryContent = mock(BinaryContent.class);
        when(binaryContent.getInputStream()).thenReturn(null);
        when(metacardTransformer.transform(any(Metacard.class),
                anyMap())).thenReturn(binaryContent);
        int depth = 0;
        metadataBackupPlugin.setFolderDepth(depth);
        metadataBackupPlugin.processCreate(createRequest);
        assertFileDoesNotExist(depth);
    }

    @Test
    public void testCreateRequestDepthGreaterThanIdLength() throws Exception {
        int depth = 32;
        metadataBackupPlugin.setFolderDepth(depth);
        ProcessRequest<ProcessCreateItem> newCreateRequest = metadataBackupPlugin.processCreate(
                createRequest);
        assertFiles(newCreateRequest.getProcessItems(), depth);
    }

    @Test
    public void testUpdateRequestNoDepth() throws Exception {
        int depth = 0;
        metadataBackupPlugin.setFolderDepth(0);
        ProcessRequest<ProcessUpdateItem> newUpdateRequest = metadataBackupPlugin.processUpdate(
                updateRequest);
        assertFiles(newUpdateRequest.getProcessItems(), depth);
    }

    @Test
    public void testUpdateRequestVaryingDepths() throws Exception {
        for (int depth = 0; depth < 5; depth++) {
            metadataBackupPlugin.setFolderDepth(depth);
            ProcessRequest<ProcessUpdateItem> newUpdateRequest = metadataBackupPlugin.processUpdate(
                    updateRequest);
            assertFiles(newUpdateRequest.getProcessItems(), depth);
        }
    }

    @Test
    public void testDeleteRequestVaryingDepths() throws Exception {
        for (int i = 0; i < 5; i++) {
            metadataBackupPlugin.setFolderDepth(i);

            List<Path> paths = new ArrayList<>();
            for (String id : METACARD_IDS) {
                paths.add(generateSampleFile(i, id));
            }

            metadataBackupPlugin.processDelete(deleteRequest);

            /* Verify All Backups Deleted */
            for (Path path : paths) {
                assertThat(Files.exists(path), is(false));
            }
        }
    }

    @Test
    public void testKeepDeletedMetacards() throws Exception {
        metadataBackupPlugin.setFolderDepth(0);
        metadataBackupPlugin.setKeepDeletedMetacards(true);
        List<Path> paths = new ArrayList<>();
        for (String id : METACARD_IDS) {
            paths.add(generateSampleFile(0, id));
        }

        metadataBackupPlugin.processDelete(deleteRequest);

        /* Verify No Backups Deleted */
        for (Path path : paths) {
            assertThat(Files.exists(path), is(true));
        }

        clearRemainingBackups(0);
    }

    private ProcessRequest<ProcessDeleteItem> generateDeleteRequest() {
        List<ProcessDeleteItem> processDeleteItems = new ArrayList<>();

        for (String id : METACARD_IDS) {
            Metacard metacard = new MetacardImpl();
            metacard.setAttribute(new AttributeImpl(Core.ID, id));
            metacard.setAttribute(new AttributeImpl(Core.METADATA, XML_METADATA));
            ProcessDeleteItem processDeleteItem = mock(ProcessDeleteItem.class);
            when(processDeleteItem.getMetacard()).thenReturn(metacard);
            processDeleteItems.add(processDeleteItem);
        }

        ProcessRequest<ProcessDeleteItem> processRequest = mock(ProcessRequest.class);
        when(processRequest.getProcessItems()).thenReturn(processDeleteItems);
        return processRequest;
    }

    private ProcessRequest generateProcessRequest(Class<? extends ProcessResourceItem> clazz) {
        List<ProcessResourceItem> processCreateItems = new ArrayList<>();

        for (String id : METACARD_IDS) {
            Metacard metacard = new MetacardImpl();
            metacard.setAttribute(new AttributeImpl(Core.ID, id));
            metacard.setAttribute(new AttributeImpl(Core.METADATA, XML_METADATA));
            ProcessResourceItem processResourceItem;
            if (clazz.getName()
                    .contains("ProcessCreateItem")) {
                processResourceItem = mock(ProcessCreateItem.class);
            } else {
                processResourceItem = mock(ProcessUpdateItem.class);
            }
            when(processResourceItem.getMetacard()).thenReturn(metacard);
            processCreateItems.add(processResourceItem);
        }

        ProcessRequest localCreateRequest = mock(ProcessRequest.class);
        when(localCreateRequest.getProcessItems()).thenReturn(processCreateItems);
        return localCreateRequest;
    }

    private void assertFiles(List<? extends ProcessResourceItem> processResourceItems, int depth)
            throws Exception {
        for (ProcessResourceItem processResourceItem : processResourceItems) {
            Metacard metacard = processResourceItem.getMetacard();
            String filename = metacard.getId();
            File file = new File(getCompleteDirectory(OUTPUT_DIRECTORY, depth, metacard.getId())
                    + File.separator + filename);
            Path path = file.toPath();
            assertThat(Files.exists(path), is(true));
            assertFileContents(file, metacard);
            Files.deleteIfExists(path);
        }
        metadataBackupPlugin.processDelete(deleteRequest);
    }

    private void assertFileDoesNotExist(int depth) throws IOException {
        Metacard metacard = createRequest.getProcessItems()
                .get(0)
                .getMetacard();
        String filename = metacard.getId();
        File file = new File(getCompleteDirectory(OUTPUT_DIRECTORY, depth, metacard.getId())
                + File.separator + filename);
        Path path = file.toPath();
        assertThat(Files.exists(path), is(false));
    }

    private void assertFileContents(File file, Metacard metacard) throws IOException {
        String metadata = metacard.getMetadata();
        String fileContents = FileUtils.readFileToString(file);
        assertThat(metadata, is(fileContents));
    }

    private Path generateSampleFile(int depth, String id) throws Exception {
        File file = getCompleteDirectory(OUTPUT_DIRECTORY, depth, id);
        File targetFile = new File(file, id);
        FileUtils.writeStringToFile(targetFile, XML_METADATA);
        return targetFile.toPath();
    }

    private File getCompleteDirectory(String outputDirectory, int depth, String id)
            throws IOException {
        File parent = new File(outputDirectory);
        for (int i = 0; i < Math.min(depth, DEPTH) && id.length() > (i * 2 + 2); i++) {
            parent = new File(parent, id.substring(i * 2, i * 2 + 2));
        }
        return parent;
    }

    private void clearRemainingBackups(int depth) {
        for (String id : METACARD_IDS) {
            try {
                File file = getCompleteDirectory(OUTPUT_DIRECTORY, depth, id);
                File fileToDelete = new File(file, id);
                Files.deleteIfExists(fileToDelete.toPath());
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}
