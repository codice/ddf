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
package ddf.catalog.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.MimeType;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;

import ddf.catalog.cache.solr.impl.ValidationQueryFactory;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.impl.MockMemoryStorageProvider;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.impl.CreateStorageRequestImpl;
import ddf.catalog.content.operation.impl.UpdateStorageRequestImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceInfoRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.operation.impl.SourceInfoRequestEnterprise;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.util.impl.SourcePoller;
import ddf.catalog.util.impl.SourcePollerRunner;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeToTransformerMapper;

public class FanoutCatalogFrameworkTest {
    private static final String OLD_SOURCE_ID = "oldSourceId";

    private static final String NEW_SOURCE_ID = "newSourceId";

    private static final Double RELEVANCE_SCORE = 2.0;

    private static final Double DISTANCE_SCORE = 3.0;

    private CatalogFrameworkImpl framework;

    private FrameworkProperties frameworkProperties;

    @Before
    public void initFramework() {

        // Mock register the provider in the container
        SourcePollerRunner runner = new SourcePollerRunner();
        SourcePoller poller = new SourcePoller(runner);
        ArrayList<PostIngestPlugin> postIngestPlugins = new ArrayList<PostIngestPlugin>();
        frameworkProperties = new FrameworkProperties();
        frameworkProperties.setSourcePoller(poller);
        frameworkProperties.setFederationStrategy(new MockFederationStrategy());
        frameworkProperties.setPostIngest(postIngestPlugins);
        framework = new CatalogFrameworkImpl(frameworkProperties);
        framework.setId(NEW_SOURCE_ID);
        framework.setFanoutEnabled(true);
    }

    @Test
    public void testReplaceSourceId() {
        QueryRequest request = new QueryRequestImpl(null);

        List<Result> results = new ArrayList<Result>();

        MetacardImpl newCard1 = new MetacardImpl();
        newCard1.setSourceId(OLD_SOURCE_ID);
        ResultImpl result1 = new ResultImpl(newCard1);

        MetacardImpl newCard2 = new MetacardImpl();
        newCard2.setSourceId(OLD_SOURCE_ID);

        ResultImpl result2 = new ResultImpl(newCard2);

        results.add(result1);
        results.add(result2);

        QueryResponse response = new QueryResponseImpl(request, results, 2);

        QueryResponse newResponse = framework.replaceSourceId(response);
        assertNotNull(newResponse);

        List<Result> newResults = newResponse.getResults();
        assertNotNull(newResults);

        assertEquals(2, newResults.size());
        Metacard card = new MetacardImpl();
        // Make sure the sourceId was replaced

        for (Result newResult : newResults) {
            card = newResult.getMetacard();
            assertNotNull(card);
            assertEquals(NEW_SOURCE_ID, card.getSourceId());
        }
    }

    @Test
    public void testQueryReplacesSourceId() throws Exception {
        ConnectedSource source1 = mock(ConnectedSource.class);
        ConnectedSource source2 = mock(ConnectedSource.class);
        when(source1.getId()).thenReturn("source1");
        when(source2.getId()).thenReturn("source2");

        frameworkProperties.setConnectedSources(ImmutableList.of(source1, source2));
        frameworkProperties.setQueryResponsePostProcessor(mock(QueryResponsePostProcessor.class));

        QueryRequestImpl queryRequest = new QueryRequestImpl(mock(Query.class));

        MetacardImpl meta1 = new MetacardImpl();
        MetacardImpl meta2 = new MetacardImpl();
        meta1.setSourceId("source1");
        meta2.setSourceId("source2");
        ResultImpl result1 = new ResultImpl(meta1);
        ResultImpl result2 = new ResultImpl(meta2);
        List<Result> results = new ArrayList<>();
        results.add(result1);
        results.add(result2);

        QueryResponseImpl queryResponse = new QueryResponseImpl(queryRequest, results, 2);
        FederationStrategy strategy = mock(FederationStrategy.class);
        when(strategy.federate(anyList(), any())).thenReturn(queryResponse);

        QueryResponse response = framework.query(queryRequest, strategy);
        for (Result result : response.getResults()) {
            assertEquals(result.getMetacard()
                    .getSourceId(), NEW_SOURCE_ID);
        }
    }

    @Test
    public void testReplaceRelevance() {
        QueryRequest request = new QueryRequestImpl(null);

        List<Result> results = new ArrayList<Result>();

        MetacardImpl newCard1 = new MetacardImpl();

        ResultImpl result1 = new ResultImpl(newCard1);
        result1.setRelevanceScore(RELEVANCE_SCORE);

        MetacardImpl newCard2 = new MetacardImpl();

        ResultImpl result2 = new ResultImpl(newCard2);
        result2.setRelevanceScore(RELEVANCE_SCORE);

        results.add(result1);
        results.add(result2);

        QueryResponse response = new QueryResponseImpl(request, results, 2);

        QueryResponse newResponse = framework.replaceSourceId(response);
        assertNotNull(newResponse);

        List<Result> newResults = newResponse.getResults();
        assertNotNull(newResponse);

        assertEquals(2, newResults.size());
        Metacard card = new MetacardImpl();
        // Make sure the relevance score was copied over
        for (Result newResult : newResults) {
            card = newResult.getMetacard();
            assertNotNull(card);
            assertEquals(RELEVANCE_SCORE, newResult.getRelevanceScore());
        }
    }

    @Test
    public void testReplaceDistance() {
        QueryRequest request = new QueryRequestImpl(null);

        List<Result> results = new ArrayList<Result>();

        MetacardImpl newCard1 = new MetacardImpl();

        ResultImpl result1 = new ResultImpl(newCard1);
        result1.setRelevanceScore(RELEVANCE_SCORE);
        result1.setDistanceInMeters(DISTANCE_SCORE);

        MetacardImpl newCard2 = new MetacardImpl();

        ResultImpl result2 = new ResultImpl(newCard2);
        result2.setRelevanceScore(RELEVANCE_SCORE);
        result2.setDistanceInMeters(DISTANCE_SCORE);

        results.add(result1);
        results.add(result2);

        QueryResponse response = new QueryResponseImpl(request, results, 2);

        QueryResponse newResponse = framework.replaceSourceId(response);
        assertNotNull(newResponse);
        List<Result> newResults = newResponse.getResults();
        assertNotNull(newResults);
        assertEquals(2, newResults.size());
        Metacard card = new MetacardImpl();
        // Make sure the relevance and distance score was copied over
        for (Result newResult : newResults) {
            card = newResult.getMetacard();
            assertNotNull(card);
            assertEquals(RELEVANCE_SCORE, newResult.getRelevanceScore());
            assertEquals(DISTANCE_SCORE, newResult.getDistanceInMeters());
        }
    }

    @Test
    public void testGetSourceIds() {
        Set<String> sourceIds = framework.getSourceIds();
        assertNotNull(sourceIds);
        assertEquals(1, sourceIds.size());
        assertEquals(NEW_SOURCE_ID,
                sourceIds.iterator()
                        .next());
    }

    /**
     * This test is to verify that an NPE will not be thrown if {@code source.getContentTypes}
     * returns null.
     *
     * @throws SourceUnavailableException
     */
    @Test
    public void testNullContentTypesInGetSourceInfo() throws SourceUnavailableException {
        SourcePollerRunner runner = new SourcePollerRunner();
        SourcePoller poller = new SourcePoller(runner);
        ArrayList<PostIngestPlugin> postIngestPlugins = new ArrayList<PostIngestPlugin>();

        SourceInfoRequest request = new SourceInfoRequestEnterprise(true);
        List<FederatedSource> fedSources = new ArrayList<FederatedSource>();

        FederatedSource mockFederatedSource = mock(FederatedSource.class);
        when(mockFederatedSource.isAvailable()).thenReturn(true);

        // Mockito would not accept Collections.emptySet() as the parameter for
        // thenReturn for mockFederatedSource.getContentTypes()
        when(mockFederatedSource.getContentTypes()).thenReturn(null);

        fedSources.add(mockFederatedSource);

        FrameworkProperties frameworkProperties = new FrameworkProperties();
        frameworkProperties.setSourcePoller(poller);
        frameworkProperties.setFederationStrategy(new MockFederationStrategy());
        frameworkProperties.setPostIngest(postIngestPlugins);
        Map<String, FederatedSource> sourceMap = new HashMap<>();
        for (FederatedSource federatedSource : fedSources) {
            sourceMap.put(federatedSource.getId(), federatedSource);
        }
        frameworkProperties.setFederatedSources(sourceMap);

        CatalogFrameworkImpl framework = new CatalogFrameworkImpl(frameworkProperties);
        framework.setId(NEW_SOURCE_ID);
        framework.setFanoutEnabled(true);

        // Assert not null simply to prove that we returned an object.
        assertNotNull(framework.getSourceInfo(request));

    }

    @Test(expected = IngestException.class)
    public void testBlacklistedTagCreateRequestFails() throws Exception {
        Metacard metacard = new MetacardImpl();
        metacard.setAttribute(new AttributeImpl(Metacard.TAGS, "blacklisted"));
        CreateRequest request = new CreateRequestImpl(metacard);
        framework.setFanoutTagBlacklist(Collections.singletonList("blacklisted"));
        framework.create(request);
    }

    @Test(expected = IngestException.class)
    public void testBlacklistedTagUpdateRequestFails() throws Exception {
        Metacard metacard = new MetacardImpl();
        metacard.setAttribute(new AttributeImpl(Metacard.ID, "metacardId"));
        metacard.setAttribute(new AttributeImpl(Metacard.TAGS, "blacklisted"));

        UpdateRequest request = new UpdateRequestImpl(metacard.getId(), metacard);
        framework.setFanoutTagBlacklist(Collections.singletonList("blacklisted"));
        framework.update(request);
    }

    @Test(expected = IngestException.class)
    public void testBlacklistedTagDeleteRequestFails() throws Exception {
        Metacard metacard = new MetacardImpl();
        metacard.setAttribute(new AttributeImpl(Metacard.ID, "metacardId"));
        metacard.setAttribute(new AttributeImpl(Metacard.TAGS, "blacklisted"));

        CatalogProvider catalogProvider = mock(CatalogProvider.class);
        doReturn(true).when(catalogProvider)
                .isAvailable();
        StorageProvider storageProvider = new MockMemoryStorageProvider();

        FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
        FilterAdapter filterAdapter = mock(FilterAdapter.class);

        ValidationQueryFactory validationQueryFactory = new ValidationQueryFactory(filterAdapter,
                filterBuilder);

        QueryRequestImpl queryRequest = new QueryRequestImpl(mock(Query.class));
        ResultImpl result = new ResultImpl(metacard);
        List<Result> results = new ArrayList<>();
        results.add(result);

        QueryResponseImpl queryResponse = new QueryResponseImpl(queryRequest, results, 1);
        FederationStrategy strategy = mock(FederationStrategy.class);
        when(strategy.federate(anyList(), any())).thenReturn(queryResponse);

        QueryResponsePostProcessor queryResponsePostProcessor =
                mock(QueryResponsePostProcessor.class);
        doNothing().when(queryResponsePostProcessor)
                .processResponse(any());

        frameworkProperties.setCatalogProviders(Collections.singletonList(catalogProvider));
        frameworkProperties.setStorageProviders(Collections.singletonList(storageProvider));
        frameworkProperties.setFilterBuilder(filterBuilder);
        frameworkProperties.setValidationQueryFactory(validationQueryFactory);
        frameworkProperties.setFederationStrategy(strategy);
        frameworkProperties.setQueryResponsePostProcessor(queryResponsePostProcessor);

        CatalogFrameworkImpl framework = new CatalogFrameworkImpl(frameworkProperties);
        framework.bind(catalogProvider);
        framework.bind(storageProvider);

        framework.setId(NEW_SOURCE_ID);
        framework.setFanoutEnabled(true);
        framework.setFanoutTagBlacklist(Collections.singletonList("blacklisted"));

        DeleteRequest request = new DeleteRequestImpl(metacard.getId());
        framework.delete(request);
    }

    @Test(expected = IngestException.class)
    public void testBlacklistedTagCreateStorageRequestFails() throws Exception {
        Metacard metacard = new MetacardImpl();
        metacard.setAttribute(new AttributeImpl(Metacard.TAGS, "blacklisted"));

        CatalogProvider catalogProvider = mock(CatalogProvider.class);
        doReturn(true).when(catalogProvider)
                .isAvailable();
        StorageProvider storageProvider = new MockMemoryStorageProvider();
        MimeTypeMapper mimeTypeMapper = mock(MimeTypeMapper.class);
        doReturn("extension").when(mimeTypeMapper)
                .getFileExtensionForMimeType(anyString());
        InputTransformer transformer = mock(InputTransformer.class);
        doReturn(metacard).when(transformer)
                .transform(any(InputStream.class));

        MimeTypeToTransformerMapper mimeTypeToTransformerMapper =
                mock(MimeTypeToTransformerMapper.class);
        doReturn(Collections.singletonList(transformer)).when(mimeTypeToTransformerMapper)
                .findMatches(any(Class.class), any(MimeType.class));

        frameworkProperties.setCatalogProviders(Collections.singletonList(catalogProvider));
        frameworkProperties.setStorageProviders(Collections.singletonList(storageProvider));
        frameworkProperties.setMimeTypeMapper(mimeTypeMapper);
        frameworkProperties.setMimeTypeToTransformerMapper(mimeTypeToTransformerMapper);

        // Need to set these for InputValidation to work
        System.setProperty("bad.files", "none");
        System.setProperty("bad.file.extensions", "none");
        System.setProperty("bad.mime.types", "none");

        CatalogFrameworkImpl framework = new CatalogFrameworkImpl(frameworkProperties);
        framework.bind(catalogProvider);
        framework.bind(storageProvider);
        framework.setId(NEW_SOURCE_ID);
        framework.setFanoutEnabled(true);
        framework.setFanoutTagBlacklist(Collections.singletonList("blacklisted"));

        ContentItem item = new ContentItemImpl(ByteSource.empty(),
                "text/xml",
                "filename.xml",
                metacard);
        CreateStorageRequest request = new CreateStorageRequestImpl(Collections.singletonList(item),
                new HashMap<>());

        framework.create(request);
    }

    @Test(expected = IngestException.class)
    public void testBlacklistedTagUpdateStorageRequestFails() throws Exception {
        Metacard metacard = new MetacardImpl();
        metacard.setAttribute(new AttributeImpl(Metacard.ID, "metacardId"));
        metacard.setAttribute(new AttributeImpl(Metacard.TAGS, "blacklisted"));

        ContentItem item = new ContentItemImpl(ByteSource.empty(),
                "text/xml",
                "filename.xml",
                metacard);
        UpdateStorageRequest request = new UpdateStorageRequestImpl(Collections.singletonList(item),
                new HashMap<>());
        framework.setFanoutTagBlacklist(Collections.singletonList("blacklisted"));
        framework.update(request);
    }

}
