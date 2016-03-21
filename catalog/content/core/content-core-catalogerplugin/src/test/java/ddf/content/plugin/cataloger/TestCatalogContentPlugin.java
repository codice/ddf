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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.activation.MimeType;

import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.util.ThreadContext;
import org.junit.Before;
import org.junit.Test;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.impl.UpdateImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.content.data.ContentItem;
import ddf.content.operation.CreateRequest;
import ddf.content.operation.CreateResponse;
import ddf.content.operation.DeleteRequest;
import ddf.content.operation.DeleteResponse;
import ddf.content.operation.UpdateRequest;
import ddf.content.operation.UpdateResponse;
import ddf.content.plugin.ContentPlugin;
import ddf.content.plugin.PluginExecutionException;
import ddf.mime.MimeTypeToTransformerMapper;
import ddf.security.SecurityConstants;
import ddf.security.Subject;

public class TestCatalogContentPlugin {

    private static final String DEFAULT_METACARD_TRANSFORMER = "geojson";

    private static final String MIME_BASE = "text";

    private static final String MIME_SUB = "plain";

    private static final String MIME_TYPE = MIME_BASE + "/" + MIME_SUB;

    private static final String TEST_USER_NAME = "TestUser";

    private static final String TEST_REALM = "testrealm";

    private static final String TEST_DATA = "Test String From InputStream";

    private static final String TEST_URI = "http://localhost/test.jpg";

    private static final String BAD_URI = "bad uri";

    private static final String TEST_ATTRIB_NAME = "Attrib1";

    private static final String TEST_ATTRIB_VALUE = "Test Value";

    private static final String TEST_EXCEPTION_TXT = "Test Exception";

    private CatalogFramework mockCatalogFramework = mock(CatalogFramework.class);

    private MimeTypeToTransformerMapper mockMimeTypeToTransformerMapper = mock(
            MimeTypeToTransformerMapper.class);

    private BinaryContent mockBinaryContent = mock(BinaryContent.class);

    private CreateRequest mockContentCreateRequest = mock(CreateRequest.class);

    private UpdateRequest mockContentUpdateRequest = mock(UpdateRequest.class);

    private DeleteRequest mockContentDeleteRequest = mock(DeleteRequest.class);

    private CreateResponse mockContentCreateResponse = mock(CreateResponse.class);

    private UpdateResponse mockContentUpdateResponse = mock(UpdateResponse.class);

    private DeleteResponse mockContentDeleteResponse = mock(DeleteResponse.class);

    private ContentItem mockContentItem = mock(ContentItem.class);

    private MimeType mockMimeType = mock(MimeType.class);

    private InputTransformer mockInputTransformer = mock(InputTransformer.class);

    private ddf.catalog.operation.CreateResponse mockCatalogCreateResponse =
            mock(ddf.catalog.operation.CreateResponse.class);

    private ddf.catalog.operation.UpdateResponse mockCatalogUpdateResponse =
            mock(ddf.catalog.operation.UpdateResponse.class);

    private ddf.catalog.operation.DeleteResponse mockCatalogDeleteResponse =
            mock(ddf.catalog.operation.DeleteResponse.class);

    private Metacard returnCard;

    private List<Metacard> metacardList = new ArrayList<>();

    private List<Update> updateList = new ArrayList<>();

    private CatalogContentPlugin catalogContentPlugin = null;

    @Before
    public void setup() throws Exception {
        catalogContentPlugin = new CatalogContentPlugin(mockCatalogFramework,
                mockMimeTypeToTransformerMapper);

        setupContent();
        setupMockContent();
    }

    @Test
    public void testProcessCreate() throws Exception {
        mockContentCreateResponse.getProperties()
                .put(SecurityConstants.SECURITY_SUBJECT, new HashMap<>());
        CreateResponse response = catalogContentPlugin.process(mockContentCreateResponse);
        assertThat(response, notNullValue());

        assertThat(response.getCreatedMetadata(),
                is(mockContentCreateResponse.getCreatedMetadata()));
        assertThat(response.getCreatedContentItem(),
                is(mockContentCreateResponse.getCreatedContentItem()));
        assertThat(response.getCreatedMetadataMimeType(),
                is(mockContentCreateResponse.getCreatedMetadataMimeType()));
        verify(mockCatalogFramework,
                times(1)).create(any(ddf.catalog.operation.CreateRequest.class));
    }

    @Test
    public void testProcessUpdate() throws Exception {
        mockContentUpdateResponse.getProperties()
                .put(SecurityConstants.SECURITY_SUBJECT, new HashMap<>());
        UpdateResponse response = catalogContentPlugin.process(mockContentUpdateResponse);
        assertThat(response, notNullValue());

        assertThat(response.getUpdatedMetadata(),
                is(mockContentUpdateResponse.getUpdatedMetadata()));
        assertThat(response.getUpdatedContentItem(),
                is(mockContentUpdateResponse.getUpdatedContentItem()));
        assertThat(response.getUpdatedMetadataMimeType(),
                is(mockContentUpdateResponse.getUpdatedMetadataMimeType()));
        verify(mockCatalogFramework,
                times(1)).update(any(ddf.catalog.operation.UpdateRequest.class));
    }

    @Test
    public void testProcessDelete() throws Exception {
        mockContentDeleteResponse.getProperties()
                .put(SecurityConstants.SECURITY_SUBJECT, new HashMap<>());
        DeleteResponse response = catalogContentPlugin.process(mockContentDeleteResponse);
        assertThat(response, notNullValue());

        assertThat(response.getContentItem(), is(mockContentDeleteResponse.getContentItem()));
        verify(mockCatalogFramework,
                times(1)).delete(any(ddf.catalog.operation.DeleteRequest.class));
    }

    @Test
    public void subjectHasNoName() throws Exception {
        Subject mockSubject = mock(Subject.class);
        when(mockSubject.getPrincipals()).thenReturn(null);

        mockContentCreateResponse.getProperties()
                .put(SecurityConstants.SECURITY_SUBJECT, new HashMap<>());
        CreateResponse response = catalogContentPlugin.process(mockContentCreateResponse);
        assertThat(response, notNullValue());

        assertThat(response.getCreatedMetadata(),
                is(mockContentCreateResponse.getCreatedMetadata()));
        assertThat(response.getCreatedContentItem(),
                is(mockContentCreateResponse.getCreatedContentItem()));
        assertThat(response.getCreatedMetadataMimeType(),
                is(mockContentCreateResponse.getCreatedMetadataMimeType()));
    }

    @Test
    public void testAddAttributesFromStoragePlugin() throws Exception {
        Map<String, Serializable> propMap = new HashMap<>();
        HashMap<String, Serializable> properties = new HashMap<>();
        properties.put("Test", "Abc123");
        propMap.put(ContentPlugin.STORAGE_PLUGIN_METACARD_ATTRIBUTES, properties);
        when(mockContentCreateResponse.getProperties()).thenReturn(propMap);

        CreateResponse response = catalogContentPlugin.process(mockContentCreateResponse);
        assertThat(response, notNullValue());
        assertThat(response.getCreatedMetadata(),
                is(mockContentCreateResponse.getCreatedMetadata()));
        assertThat(response.getCreatedContentItem(),
                is(mockContentCreateResponse.getCreatedContentItem()));
        assertThat(response.getCreatedMetadataMimeType(),
                is(mockContentCreateResponse.getCreatedMetadataMimeType()));
    }

    @Test
    public void testMetacardNoTitle() throws Exception {
        Metacard noTitleCard = new MetacardImpl();
        ((MetacardImpl) noTitleCard).setId(UUID.randomUUID()
                .toString());

        List<Metacard> noTitleList = new ArrayList<>();
        noTitleList.add(noTitleCard);

        when(mockContentItem.getUri()).thenReturn(null);
        when(mockCatalogCreateResponse.getCreatedMetacards()).thenReturn(noTitleList);
        when(mockInputTransformer.transform(any(InputStream.class))).thenReturn(noTitleCard);
        when(mockInputTransformer.transform(any(InputStream.class), any(String.class))).thenReturn(
                noTitleCard);

        CreateResponse response = catalogContentPlugin.process(mockContentCreateResponse);
        assertThat(response, notNullValue());
        assertThat(response.getCreatedMetadata(),
                is(mockContentCreateResponse.getCreatedMetadata()));
        assertThat(response.getCreatedContentItem(),
                is(mockContentCreateResponse.getCreatedContentItem()));
        assertThat(response.getCreatedMetadataMimeType(),
                is(mockContentCreateResponse.getCreatedMetadataMimeType()));
    }

    @Test(expected = PluginExecutionException.class)
    public void testCreateNoTransformers() throws Exception {
        when(mockMimeTypeToTransformerMapper.findMatches(eq(InputTransformer.class),
                eq(mockMimeType))).thenReturn(new ArrayList<>());
        catalogContentPlugin.process(mockContentCreateResponse);
    }

    @Test(expected = PluginExecutionException.class)
    public void testUpdateNoTransformers() throws Exception {
        when(mockMimeTypeToTransformerMapper.findMatches(eq(InputTransformer.class),
                eq(mockMimeType))).thenReturn(new ArrayList<>());
        catalogContentPlugin.process(mockContentUpdateResponse);
    }

    @Test
    public void testDeleteNoTransformers() throws Exception {
        when(mockMimeTypeToTransformerMapper.findMatches(eq(InputTransformer.class),
                eq(mockMimeType))).thenReturn(new ArrayList<>());
        DeleteResponse response = catalogContentPlugin.process(mockContentDeleteResponse);
        assertThat(response, notNullValue());
    }

    @Test(expected = PluginExecutionException.class)
    public void testMetacardCreateTransformerException() throws Exception {
        when(mockInputTransformer.transform(any(InputStream.class))).thenThrow(new CatalogTransformerException(
                TEST_EXCEPTION_TXT));
        when(mockInputTransformer.transform(any(InputStream.class), any(String.class))).thenThrow(
                new CatalogTransformerException(TEST_EXCEPTION_TXT));

        catalogContentPlugin.process(mockContentCreateResponse);
    }

    @Test(expected = PluginExecutionException.class)
    public void testMetacardUpdateTransformerException() throws Exception {
        when(mockInputTransformer.transform(any(InputStream.class))).thenThrow(new CatalogTransformerException(
                TEST_EXCEPTION_TXT));
        when(mockInputTransformer.transform(any(InputStream.class), any(String.class))).thenThrow(
                new CatalogTransformerException(TEST_EXCEPTION_TXT));

        catalogContentPlugin.process(mockContentUpdateResponse);
    }

    @Test
    public void testMetacardDeleteTransformerException() throws Exception {
        when(mockInputTransformer.transform(any(InputStream.class))).thenThrow(new CatalogTransformerException(
                TEST_EXCEPTION_TXT));
        when(mockInputTransformer.transform(any(InputStream.class), any(String.class))).thenThrow(
                new CatalogTransformerException(TEST_EXCEPTION_TXT));

        DeleteResponse response = catalogContentPlugin.process(mockContentDeleteResponse);
        assertThat(response, notNullValue());
    }

    @Test(expected = PluginExecutionException.class)
    public void testMetacardCreateIOException() throws Exception {
        when(mockInputTransformer.transform(any(InputStream.class))).thenThrow(new IOException(
                TEST_EXCEPTION_TXT));
        when(mockInputTransformer.transform(any(InputStream.class), any(String.class))).thenThrow(
                new IOException(TEST_EXCEPTION_TXT));

        catalogContentPlugin.process(mockContentCreateResponse);
    }

    @Test(expected = PluginExecutionException.class)
    public void testMetacardUpdateIOException() throws Exception {
        when(mockInputTransformer.transform(any(InputStream.class))).thenThrow(new IOException(
                TEST_EXCEPTION_TXT));
        when(mockInputTransformer.transform(any(InputStream.class), any(String.class))).thenThrow(
                new IOException(TEST_EXCEPTION_TXT));

        catalogContentPlugin.process(mockContentUpdateResponse);
    }

    @Test
    public void testMetacardDeleteIOException() throws Exception {
        when(mockInputTransformer.transform(any(InputStream.class))).thenThrow(new IOException(
                TEST_EXCEPTION_TXT));
        when(mockInputTransformer.transform(any(InputStream.class), any(String.class))).thenThrow(
                new IOException(TEST_EXCEPTION_TXT));

        DeleteResponse response = catalogContentPlugin.process(mockContentDeleteResponse);
        assertThat(response, notNullValue());
    }

    @Test
    public void testMetacardCreateTransformException() throws Exception {
        when(mockCatalogFramework.transform(any(Metacard.class),
                eq(DEFAULT_METACARD_TRANSFORMER),
                isNull(Map.class))).thenThrow(new CatalogTransformerException(TEST_EXCEPTION_TXT));

        CreateResponse createResponse = catalogContentPlugin.process(mockContentCreateResponse);
        assertThat(createResponse, notNullValue());
        UpdateResponse updateResponse = catalogContentPlugin.process(mockContentUpdateResponse);
        assertThat(updateResponse, notNullValue());
        DeleteResponse deleteResponse = catalogContentPlugin.process(mockContentDeleteResponse);
        assertThat(deleteResponse, notNullValue());
    }

    @Test(expected = PluginExecutionException.class)
    public void testMetacardCreateNullInputStream() throws Exception {
        when(mockContentItem.getInputStream()).thenReturn(null);
        catalogContentPlugin.process(mockContentCreateResponse);
    }

    @Test(expected = PluginExecutionException.class)
    public void testMetacardUpdateNullInputStream() throws Exception {
        when(mockContentItem.getInputStream()).thenReturn(null);
        catalogContentPlugin.process(mockContentUpdateResponse);
    }

    @Test
    public void testMetacardDeleteNullInputStream() throws Exception {
        when(mockContentItem.getInputStream()).thenReturn(null);
        catalogContentPlugin.process(mockContentDeleteResponse);
    }

    @Test(expected = PluginExecutionException.class)
    public void testMetacardCreateInputStreamThrows() throws Exception {
        when(mockContentItem.getInputStream()).thenThrow(new IOException(TEST_EXCEPTION_TXT));
        catalogContentPlugin.process(mockContentCreateResponse);
    }

    @Test(expected = PluginExecutionException.class)
    public void testMetacardUpdateNullInputStreamThrows() throws Exception {
        when(mockContentItem.getInputStream()).thenThrow(new IOException(TEST_EXCEPTION_TXT));
        catalogContentPlugin.process(mockContentUpdateResponse);
    }

    @Test
    public void testMetacardDeleteNullInputStreamThrows() throws Exception {
        when(mockContentItem.getInputStream()).thenThrow(new IOException(TEST_EXCEPTION_TXT));
        DeleteResponse response = catalogContentPlugin.process(mockContentDeleteResponse);
        assertThat(response, notNullValue());
    }

    @Test(expected = PluginExecutionException.class)
    public void testCatalogFramworkCreateThrows() throws Exception {
        when(mockCatalogFramework.create(any(ddf.catalog.operation.CreateRequest.class))).thenThrow(
                new IngestException(TEST_EXCEPTION_TXT));
        catalogContentPlugin.process(mockContentCreateResponse);
    }

    @Test(expected = PluginExecutionException.class)
    public void testCatalogFramworkUpdateThrows() throws Exception {
        when(mockCatalogFramework.update(any(ddf.catalog.operation.UpdateRequest.class))).thenThrow(
                new IngestException(TEST_EXCEPTION_TXT));
        catalogContentPlugin.process(mockContentUpdateResponse);
    }

    @Test(expected = PluginExecutionException.class)
    public void testCatalogFramworkDeleteThrows() throws Exception {
        when(mockCatalogFramework.delete(any(ddf.catalog.operation.DeleteRequest.class))).thenThrow(
                new IngestException(TEST_EXCEPTION_TXT));
        catalogContentPlugin.process(mockContentDeleteResponse);
    }

    @Test
    public void testCatalogFrameworkResponseNull() throws Exception {
        when(mockCatalogCreateResponse.getCreatedMetacards()).thenReturn(null);
        CreateResponse response = catalogContentPlugin.process(mockContentCreateResponse);
        assertThat(response, notNullValue());
    }

    @Test
    public void testCreateBadContentItemURI() throws Exception {
        when(mockContentItem.getUri()).thenReturn(BAD_URI);
        CreateResponse response = catalogContentPlugin.process(mockContentCreateResponse);
        assertThat(response, notNullValue());
    }

    @Test(expected = PluginExecutionException.class)
    public void testUpdateBadContentItemURI() throws Exception {
        when(mockContentItem.getUri()).thenReturn(BAD_URI);
        catalogContentPlugin.process(mockContentUpdateResponse);
    }

    @Test(expected = PluginExecutionException.class)
    public void testDeleteBadContentItemURI() throws Exception {
        when(mockContentItem.getUri()).thenReturn(BAD_URI);
        catalogContentPlugin.process(mockContentDeleteResponse);
    }

    private void setupContent() throws Exception {
        returnCard = new MetacardImpl();
        ((MetacardImpl) returnCard).setTitle("Test Metacard");
        ((MetacardImpl) returnCard).setResourceURI(new URI("http://localhost/card"));
        ((MetacardImpl) returnCard).setId(UUID.randomUUID()
                .toString());

        metacardList.add(returnCard);

        Update update = new UpdateImpl(returnCard, returnCard);
        updateList.add(update);
    }

    private void setupMockContent() throws Exception {
        mockSecurity();

        byte[] data = TEST_DATA.getBytes("UTF-8");
        InputStream is = new ByteArrayInputStream(data);
        when(mockBinaryContent.getInputStream()).thenReturn(is);
        when(mockBinaryContent.getMimeType()).thenReturn(mockMimeType);
        when(mockBinaryContent.getMimeTypeValue()).thenReturn(MIME_TYPE);
        when(mockBinaryContent.getByteArray()).thenReturn(data);

        when(mockMimeType.getBaseType()).thenReturn(MIME_TYPE);
        when(mockMimeType.getSubType()).thenReturn(MIME_SUB);
        when(mockMimeType.toString()).thenReturn(MIME_TYPE);

        when(mockCatalogCreateResponse.getCreatedMetacards()).thenReturn(metacardList);
        when(mockCatalogUpdateResponse.getUpdatedMetacards()).thenReturn(updateList);
        when(mockCatalogDeleteResponse.getDeletedMetacards()).thenReturn(metacardList);

        List<InputTransformer> inputTransformers = new ArrayList<>();
        inputTransformers.add(mockInputTransformer);
        when(mockInputTransformer.transform(any(InputStream.class))).thenReturn(returnCard);
        when(mockInputTransformer.transform(any(InputStream.class), any(String.class))).thenReturn(
                returnCard);

        when(mockMimeTypeToTransformerMapper.findMatches(eq(InputTransformer.class),
                eq(mockMimeType))).thenReturn(inputTransformers);

        when(mockContentItem.getMimeType()).thenReturn(mockMimeType);
        when(mockContentItem.getInputStream()).thenReturn(is);

        when(mockContentCreateResponse.getCreatedContentItem()).thenReturn(mockContentItem);
        when(mockContentCreateResponse.getRequest()).thenReturn(mockContentCreateRequest);
        when(mockContentCreateRequest.getProperties()).thenReturn(new HashMap<>());
        when(mockContentCreateResponse.getCreatedMetadata()).thenReturn(data);
        when(mockContentCreateResponse.getCreatedMetadataMimeType()).thenReturn(MIME_TYPE);

        when(mockCatalogFramework.transform(any(Metacard.class), eq(DEFAULT_METACARD_TRANSFORMER),
                isNull(Map.class))).thenReturn(mockBinaryContent);
        when(mockCatalogFramework.create(any(ddf.catalog.operation.CreateRequest.class))).thenReturn(
                mockCatalogCreateResponse);
        when(mockCatalogFramework.update(any(ddf.catalog.operation.UpdateRequest.class))).thenReturn(
                mockCatalogUpdateResponse);
        when(mockCatalogFramework.delete(any(ddf.catalog.operation.DeleteRequest.class))).thenReturn(
                mockCatalogDeleteResponse);

        when(mockContentItem.getUri()).thenReturn(TEST_URI);
        when(mockContentUpdateResponse.getUpdatedContentItem()).thenReturn(mockContentItem);
        when(mockContentUpdateResponse.getRequest()).thenReturn(mockContentUpdateRequest);
        when(mockContentUpdateRequest.getProperties()).thenReturn(new HashMap<>());
        when(mockContentUpdateResponse.getUpdatedMetadata()).thenReturn(data);
        when(mockContentUpdateResponse.getUpdatedMetadataMimeType()).thenReturn(MIME_TYPE);

        when(mockContentDeleteResponse.getContentItem()).thenReturn(mockContentItem);
        when(mockContentDeleteResponse.getRequest()).thenReturn(mockContentDeleteRequest);
        when(mockContentDeleteRequest.getProperties()).thenReturn(new HashMap<>());
    }

    private void mockSecurity() {
        org.apache.shiro.mgt.SecurityManager secManager = new DefaultSecurityManager();
        PrincipalCollection principals = new SimplePrincipalCollection(TEST_USER_NAME, TEST_REALM);
        org.apache.shiro.subject.Subject subject = new org.apache.shiro.subject.Subject.Builder(
                secManager).principals(principals)
                .session(new SimpleSession())
                .authenticated(true)
                .buildSubject();
        ThreadContext.bind(secManager);
        ThreadContext.bind(subject);
    }
}
