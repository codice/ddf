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
package ddf.catalog.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ddf.catalog.source.CatalogProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.impl.MockFederationStrategy;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceInfoRequest;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.operation.impl.SourceInfoRequestEnterprise;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.PostResourcePlugin;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.PreResourcePlugin;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.util.impl.SourcePoller;
import ddf.catalog.util.impl.SourcePollerRunner;

public class FanoutCatalogFrameworkTest {
    private static final String OLD_SOURCE_ID = "oldSourceId";

    private static final String NEW_SOURCE_ID = "newSourceId";

    private static final Double RELEVANCE_SCORE = 2.0;

    private static final Double DISTANCE_SCORE = 3.0;

    private CatalogFrameworkImpl framework;

    @Before
    public void initFramework() {

        // Mock register the provider in the container
        SourcePollerRunner runner = new SourcePollerRunner();
        SourcePoller poller = new SourcePoller(runner);
        ArrayList<PostIngestPlugin> postIngestPlugins = new ArrayList<PostIngestPlugin>();
        framework = new CatalogFrameworkImpl(new ArrayList<CatalogProvider>(),
                null, new ArrayList<PreIngestPlugin>(), postIngestPlugins,
                new ArrayList<PreQueryPlugin>(), new ArrayList<PostQueryPlugin>(),
                new ArrayList<PreResourcePlugin>(), new ArrayList<PostResourcePlugin>(),
                new ArrayList<ConnectedSource>(), new ArrayList<FederatedSource>(),
                new ArrayList<ResourceReader>(), new MockFederationStrategy(), null, null,
                poller, null, null, null);
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
        assertEquals(NEW_SOURCE_ID, sourceIds.iterator().next());
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

        FederatedSource mockFederatedSource = Mockito.mock(FederatedSource.class);
        Mockito.when(mockFederatedSource.isAvailable()).thenReturn(true);

        // Mockito would not accept Collections.emptySet() as the parameter for
        // thenReturn for mockFederatedSource.getContentTypes()
        Mockito.when(mockFederatedSource.getContentTypes()).thenReturn(null);

        fedSources.add(mockFederatedSource);

        CatalogFrameworkImpl framework = new CatalogFrameworkImpl(new ArrayList<CatalogProvider>(),
                null, new ArrayList<PreIngestPlugin>(), postIngestPlugins,
                new ArrayList<PreQueryPlugin>(), new ArrayList<PostQueryPlugin>(),
                new ArrayList<PreResourcePlugin>(), new ArrayList<PostResourcePlugin>(),
                new ArrayList<ConnectedSource>(), fedSources, new ArrayList<ResourceReader>(),
                new MockFederationStrategy(), null, null, poller, null, null, null);
        framework.setId(NEW_SOURCE_ID);
        framework.setFanoutEnabled(true);

        // Assert not null simply to prove that we returned an object.
        assertNotNull(framework.getSourceInfo(request));

    }

}
