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
package org.codice.ddf.catalog.plugin.metacard.backup;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codice.ddf.catalog.async.data.api.internal.ProcessCreateItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessDeleteItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessRequest;
import org.codice.ddf.catalog.async.data.api.internal.ProcessResourceItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessUpdateItem;
import org.codice.ddf.catalog.plugin.metacard.backup.storage.api.MetacardBackupStorageProvider;
import org.codice.ddf.catalog.plugin.metacard.backup.storage.filestorage.MetacardBackupFileStorage;
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

public class MetacardBackupPluginTest {

    private static final String METACARD_TRANSFORMER_ID = "metadata";

    private static final List<String> METACARD_IDS = Arrays.asList("1b6482b1b8f730e343a96d61e0e089",
            "2b6482b2b8f730e343a96d61e0e089",
            "2b6482b2b8f730e343a96d61e0e082");

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

    private static final String FILE_STORAGE_PROVIDER_ID = "TestFileStorageProvider";

    private MetacardBackupPlugin metacardBackupPlugin;

    private MetacardBackupFileStorage fileStorageProvider = new MetacardBackupFileStorage();

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
        metacardBackupPlugin = new MetacardBackupPlugin();
        metacardBackupPlugin.setMetacardTransformerId(METACARD_TRANSFORMER_ID);
        metacardBackupPlugin.setMetacardTransformer(metacardTransformer);
        createRequest = generateProcessRequest(ProcessCreateItem.class);
        updateRequest = generateProcessRequest(ProcessUpdateItem.class);
        deleteRequest = generateDeleteRequest();
        fileStorageProvider.setId(FILE_STORAGE_PROVIDER_ID);
        fileStorageProvider.setOutputDirectory(OUTPUT_DIRECTORY);
        metacardBackupPlugin.setMetacardOutputProviderIds(Collections.singletonList(
                FILE_STORAGE_PROVIDER_ID));
        metacardBackupPlugin.setStorageBackupPlugins(Arrays.asList(new MetacardBackupStorageProvider[] {
                fileStorageProvider}));
    }

    @Test
    public void testDefaultKeepDeletedMetacards() {
        assertThat(metacardBackupPlugin.getKeepDeletedMetacards(), is(false));
    }

    @Test
    public void testSetKeepDeletedMetacards() {
        metacardBackupPlugin.setKeepDeletedMetacards(true);
        assertThat(metacardBackupPlugin.getKeepDeletedMetacards(), is(true));
    }

    @Test
    public void testMetacardTransformerId() {
        assertThat(metacardBackupPlugin.getMetacardTransformerId(), is(METACARD_TRANSFORMER_ID));
    }

    @Test
    public void testRefresh() {
        String newMetacardTransformer = "tika";
        Map<String, Object> properties = new HashMap<>();
        properties.put("metacardTransformerId", newMetacardTransformer);
        properties.put("keepDeletedMetacards", Boolean.TRUE);
        metacardBackupPlugin.refresh(properties);
        assertThat(metacardBackupPlugin.getKeepDeletedMetacards(), is(true));
        assertThat(metacardBackupPlugin.getMetacardTransformerId(), is(newMetacardTransformer));
    }

    @Test
    public void testRefreshBadValues() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("metacardTransformerId", 2);
        properties.put("keepDeletedMetacards", "bad");
        metacardBackupPlugin.refresh(properties);
        assertThat(metacardBackupPlugin.getKeepDeletedMetacards(), is(false));
        assertThat(metacardBackupPlugin.getMetacardTransformerId(), is(METACARD_TRANSFORMER_ID));
    }

    @Test
    public void testRefreshEmptyStrings() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("metacardTransformerId", "");
        metacardBackupPlugin.refresh(properties);
        assertThat(metacardBackupPlugin.getMetacardTransformerId(), is(METACARD_TRANSFORMER_ID));
    }

    @Test
    public void testCreateRequest() throws Exception {
        metacardBackupPlugin.setMetacardOutputProviderIds(Collections.singletonList(
                FILE_STORAGE_PROVIDER_ID));
        metacardBackupPlugin.setStorageBackupPlugins(Arrays.asList(new MetacardBackupStorageProvider[] {
                fileStorageProvider}));
        metacardBackupPlugin.processCreate(createRequest);
        assertFiles(true);
        metacardBackupPlugin.processDelete(deleteRequest);
    }

    @Test(expected = PluginExecutionException.class)
    public void testCreateRequestNullMetacardTransformer() throws Exception {
        metacardBackupPlugin.setMetacardTransformer(null);
        metacardBackupPlugin.processCreate(createRequest);
    }

    @Test(expected = PluginExecutionException.class)
    public void testCreateRequestWithFailedTransform() throws Exception {
        when(metacardTransformer.transform(any(Metacard.class), anyMap())).thenThrow(
                CatalogTransformerException.class);
        metacardBackupPlugin.processCreate(createRequest);
    }

    @Test(expected = PluginExecutionException.class)
    public void testCreateRequestWithNullContent() throws Exception {
        when(metacardTransformer.transform(any(Metacard.class), anyMap())).thenReturn(null);
        metacardBackupPlugin.processCreate(createRequest);
    }

    @Test(expected = PluginExecutionException.class)
    public void testCreateRequestWithNullContentInputStream() throws Exception {
        BinaryContent binaryContent = mock(BinaryContent.class);
        when(binaryContent.getInputStream()).thenReturn(null);
        when(metacardTransformer.transform(any(Metacard.class),
                anyMap())).thenReturn(binaryContent);
        metacardBackupPlugin.processCreate(createRequest);
    }

    @Test
    public void testCreateRequestNoStoragePlugin() throws Exception {
        cleanup();
        metacardBackupPlugin.setStorageBackupPlugins(Arrays.asList(new MetacardBackupStorageProvider[] {
                fileStorageProvider}));
        metacardBackupPlugin.setMetacardOutputProviderIds(Collections.singletonList("BogusId"));
        metacardBackupPlugin.processCreate(createRequest);
        assertFiles(false);
    }

    @Test
    public void testUpdateRequest() throws Exception {
        metacardBackupPlugin.processUpdate(updateRequest);
        assertFiles(true);
        metacardBackupPlugin.processDelete(deleteRequest);
    }

    @Test
    public void testDeleteRequest() throws Exception {
        metacardBackupPlugin.processCreate(createRequest);
        metacardBackupPlugin.processDelete(deleteRequest);
        assertFiles(false);
    }

    @Test
    public void testDeleteRequestKeepDeletedMetacard() throws Exception {
        metacardBackupPlugin.processCreate(createRequest);
        metacardBackupPlugin.setKeepDeletedMetacards(true);
        assertFiles(true);
        metacardBackupPlugin.processDelete(deleteRequest);
    }

    @Test
    public void testGetContentBytes() throws Exception {
        BinaryContent binaryContent = mock(BinaryContent.class);
        byte[] content = {'a'};
        when(binaryContent.getByteArray()).thenReturn(content);
        byte[] bytes = metacardBackupPlugin.getContentBytes(binaryContent, "metacard");
        assertThat(bytes, equalTo(content));
    }

    @Test(expected = PluginExecutionException.class)
    public void testGetContentBytesNoContent() throws Exception {
        metacardBackupPlugin.getContentBytes(null, "metacard");
    }

    @Test(expected = PluginExecutionException.class)
    public void testGetContentBytesNoContentBytes() throws Exception {
        BinaryContent binaryContent = mock(BinaryContent.class);
        when(binaryContent.getByteArray()).thenReturn(null);
        metacardBackupPlugin.getContentBytes(binaryContent, "metacard");
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

    private void assertFiles(boolean exists) {
        for (String id : METACARD_IDS) {
            Path path = fileStorageProvider.getMetacardDirectory(id);
            assertThat(path.toFile()
                    .exists(), is(exists));
        }
    }

    private void cleanup() throws PluginExecutionException {
        metacardBackupPlugin.processDelete(deleteRequest);
    }
}
