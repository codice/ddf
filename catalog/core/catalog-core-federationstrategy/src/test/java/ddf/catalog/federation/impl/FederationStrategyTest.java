/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.federation.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.geotools.filter.FilterFactoryImpl;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortOrder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.federation.base.AbstractFederationStrategy;
import ddf.catalog.impl.CatalogFrameworkImpl;
import ddf.catalog.impl.MockDelayProvider;
import ddf.catalog.impl.QueryResponsePostProcessor;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.plugin.PostFederatedQueryPlugin;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.PostResourcePlugin;
import ddf.catalog.plugin.PreFederatedQueryPlugin;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.PreResourcePlugin;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.SourcePoller;
import ddf.catalog.util.impl.SourcePollerRunner;

@PrepareForTest(AbstractFederationStrategy.class)
public class FederationStrategyTest {

    private static final long SHORT_TIMEOUT = 25;

    private static final long LONG_TIMEOUT = 100;

    private static final FilterFactory FILTER_FACTORY = new FilterFactoryImpl();

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);

    private static final Logger LOGGER = LoggerFactory
            .getLogger(FederationStrategyTest.class.getName());

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    /**
     * Tests that the framework properly times out using the default federation strategy.
     */
    @Test
    public void testQueryTimeout() {
        long queryDelay = 100;

        MockDelayProvider provider = new MockDelayProvider("Provider", "Provider", "v1.0", "DDF",
                new HashSet<ContentType>(), true, new Date());
        provider.setQueryDelayMillis(queryDelay);

        // Mock register the provider in the container
        SourcePollerRunner runner = new SourcePollerRunner();
        SourcePoller poller = new SourcePoller(runner);
        runner.bind(provider);

        // Must have more than one thread or sleeps will block the monitor
        SortedFederationStrategy fedStrategy = new SortedFederationStrategy(EXECUTOR,
                new ArrayList<PreFederatedQueryPlugin>(),
                new ArrayList<PostFederatedQueryPlugin>());

        CatalogFrameworkImpl framework = new CatalogFrameworkImpl(
                Collections.singletonList((CatalogProvider) provider), null,
                new ArrayList<PreIngestPlugin>(), new ArrayList<PostIngestPlugin>(),
                new ArrayList<PreQueryPlugin>(), new ArrayList<PostQueryPlugin>(),
                new ArrayList<PreResourcePlugin>(), new ArrayList<PostResourcePlugin>(),
                new ArrayList<ConnectedSource>(), new ArrayList<FederatedSource>(),
                new ArrayList<ResourceReader>(), fedStrategy,
                mock(QueryResponsePostProcessor.class), null, poller, null, null, null);
        framework.bind(provider);

        List<Metacard> metacards = new ArrayList<Metacard>();

        MetacardImpl newCard = new MetacardImpl();
        newCard.setId(null);
        metacards.add(newCard);

        CreateResponse createResponse = null;
        try {
            createResponse = framework.create(new CreateRequestImpl(metacards, null));
        } catch (IngestException e1) {
            fail();
        } catch (SourceUnavailableException e1) {
            fail();
        }
        assertEquals(createResponse.getCreatedMetacards().size(), provider.size());
        for (Metacard curCard : createResponse.getCreatedMetacards()) {
            assertNotNull(curCard.getId());
        }

        QueryImpl query = new QueryImpl(FILTER_FACTORY.equals(FILTER_FACTORY.property(Metacard.ID),
                FILTER_FACTORY.literal(createResponse.getCreatedMetacards().get(0).getId())));
        query.setTimeoutMillis(SHORT_TIMEOUT);
        query.setSortBy(new FilterFactoryImpl().sort(Result.RELEVANCE, SortOrder.ASCENDING));

        QueryRequest fedQueryRequest = new QueryRequestImpl(query);

        try {
            QueryResponse response = framework.query(fedQueryRequest);
            assertEquals("Timeout should happen before results return", 0, response.getHits());
        } catch (UnsupportedQueryException e) {
            fail();
        } catch (FederationException e) {
            LOGGER.error("Unexpected federation exception during test", e);
            fail();
        }
    }

    @Test
    public void testNegativePageSizeQuery() throws Exception {
        Query query = mock(Query.class);
        when(query.getPageSize()).thenReturn(-1);
        when(query.getTimeoutMillis()).thenReturn(LONG_TIMEOUT);

        QueryRequest fedQueryRequest = mock(QueryRequest.class);
        when(fedQueryRequest.getQuery()).thenReturn(query);

        Result mockResult = mock(Result.class);

        SourceResponse mockResponse = mock(SourceResponse.class);
        List<Result> results = Arrays.asList(mockResult);
        when(mockResponse.getHits()).thenReturn((long) results.size());
        when(mockResponse.getResults()).thenReturn(results);

        CatalogProvider mockProvider = mock(CatalogProvider.class);
        when(mockProvider.query(any(QueryRequest.class))).thenReturn(mockResponse);
        when(mockProvider.getId()).thenReturn("mock provider");

        List<Source> sources = new ArrayList<Source>();
        sources.add(mockProvider);

        SortedFederationStrategy sortedStrategy = new SortedFederationStrategy(EXECUTOR,
                new ArrayList<PreFederatedQueryPlugin>(),
                new ArrayList<PostFederatedQueryPlugin>());

        QueryResponse fedResponse = sortedStrategy.federate(sources, fedQueryRequest);
        assertEquals(1, fedResponse.getResults().size());

        FifoFederationStrategy fifoStrategy = new FifoFederationStrategy(EXECUTOR,
                new ArrayList<PreFederatedQueryPlugin>(),
                new ArrayList<PostFederatedQueryPlugin>());
        fedResponse = fifoStrategy.federate(sources, fedQueryRequest);
        assertEquals(1, fedResponse.getResults().size());
    }

    /**
     * Verify that a modified version of the query passed into {@link
     * ddf.catalog.federation.AbstractFederationStrategy#federate(List<Source>, QueryRequest)} is
     * used by the sources.
     *
     * Special results handling done by OffsetResultsHandler.
     *
     */
    @Test
    public void testFederateTwoSourcesOffsetTwoPageSizeThree() throws Exception {
        LOGGER.debug("testFederate_TwoSources_OffsetTwo_PageSizeThree()");
        // Test Setup
        Query mockQuery = mock(QueryImpl.class);
        // Offset of 2
        when(mockQuery.getStartIndex()).thenReturn(2);
        // Page size of 3
        when(mockQuery.getPageSize()).thenReturn(3);

        QueryRequest queryRequest = mock(QueryRequest.class);
        when(queryRequest.getQuery()).thenReturn(mockQuery);
        ArgumentCaptor<QueryRequest> argument1 = ArgumentCaptor.forClass(QueryRequest.class);
        ArgumentCaptor<QueryRequest> argument2 = ArgumentCaptor.forClass(QueryRequest.class);

        /**
         * When using a modified query to query the sources, the desired offset and page size are
         * NOT used. So, the results returned by each source start at index 1 and end at (offset +
         * pageSize - 1).
         *
         * Number of results returned by each source = offset + pageSize - 1 4 = 2 + 3 - 1
         */
        Result mockSource1Result1 = mock(Result.class);
        Result mockSource1Result2 = mock(Result.class);
        Result mockSource1Result3 = mock(Result.class);
        Result mockSource1Result4 = mock(Result.class);

        SourceResponse mockSource1Response = mock(SourceResponse.class);
        List<Result> mockSource1Results = Arrays
                .asList(mockSource1Result1, mockSource1Result2, mockSource1Result3,
                        mockSource1Result4);
        when(mockSource1Response.getResults()).thenReturn(mockSource1Results);

        Source mockSource1 = mock(Source.class);
        when(mockSource1.query(any(QueryRequest.class))).thenReturn(mockSource1Response);
        when(mockSource1.getId()).thenReturn("####### MOCK SOURCE 1.3 #######");

        /**
         * When using a modified query to query the sources, the desired offset and page size are
         * NOT used. So, the results returned by each source start at index 1 and end at (offset +
         * pageSize - 1).
         *
         * Number of results returned by each source = offset + pageSize - 1 4 = 2 + 3 - 1
         */
        Result mockSource2Result1 = mock(Result.class);
        Result mockSource2Result2 = mock(Result.class);
        Result mockSource2Result3 = mock(Result.class);
        Result mockSource2Result4 = mock(Result.class);

        SourceResponse mockSource2Response = mock(SourceResponse.class);
        List<Result> mockSource2Results = Arrays
                .asList(mockSource2Result1, mockSource2Result2, mockSource2Result3,
                        mockSource2Result4);
        when(mockSource2Response.getResults()).thenReturn(mockSource2Results);

        Source mockSource2 = mock(Source.class);
        when(mockSource2.query(any(QueryRequest.class))).thenReturn(mockSource2Response);
        when(mockSource2.getId()).thenReturn("####### MOCK SOURCE 2.3 #######");

        // Two sources
        List<Source> sources = new ArrayList<Source>(2);
        sources.add(mockSource1);
        sources.add(mockSource2);

        Result mockSortedResult1 = mock(Result.class);
        Result mockSortedResult2 = mock(Result.class);
        Result mockSortedResult3 = mock(Result.class);
        Result mockSortedResult4 = mock(Result.class);
        Result mockSortedResult5 = mock(Result.class);
        Result mockSortedResult6 = mock(Result.class);
        Result mockSortedResult7 = mock(Result.class);
        Result mockSortedResult8 = mock(Result.class);

        List<Result> mockSortedResults = Arrays
                .asList(mockSortedResult1, mockSortedResult2, mockSortedResult3, mockSortedResult4,
                        mockSortedResult5, mockSortedResult6, mockSortedResult7, mockSortedResult8);

        QueryResponseImpl mockOriginalResults = Mockito.mock(QueryResponseImpl.class);
        // Return true for the number of mockSortedResults
        Mockito.when(mockOriginalResults.hasMoreResults())
                .thenReturn(true, true, true, true, true, true, true, true, false);
        Mockito.when(mockOriginalResults.getResults()).thenReturn(mockSortedResults);
        // Returns the sorted results from both sources (4 + 4 = 8)
        Mockito.when(mockOriginalResults.take())
                .thenReturn(mockSortedResult1, mockSortedResult2, mockSortedResult3,
                        mockSortedResult4, mockSortedResult5, mockSortedResult6, mockSortedResult7,
                        mockSortedResult8);
        QueryResponseImpl offsetResultQueue = new QueryResponseImpl(queryRequest, null);
        PowerMockito.whenNew(QueryResponseImpl.class)
                .withArguments(queryRequest, (Map<String, Serializable>) null)
                .thenReturn(mockOriginalResults, offsetResultQueue);

        SortedFederationStrategy strategy = new SortedFederationStrategy(EXECUTOR,
                new ArrayList<PreFederatedQueryPlugin>(),
                new ArrayList<PostFederatedQueryPlugin>());

        // Run Test
        QueryResponse federatedResponse = strategy.federate(sources, queryRequest);

        // Verification
        assertNotNull(federatedResponse);
        verify(mockSource1).query(argument1.capture());

        // The modified query should have a start index of 1 and an end index of offset + pageSize -
        // 1
        assertEquals(1, argument1.getValue().getQuery().getStartIndex());
        assertEquals(4, argument1.getValue().getQuery().getPageSize());

        verify(mockSource2).query(argument2.capture());
        assertThat(mockQuery, not(argument2.getValue().getQuery()));
        // The modified query should have a start index of 1 and an end index of offset + pageSize -
        // 1
        assertEquals(1, argument2.getValue().getQuery().getStartIndex());
        assertEquals(4, argument2.getValue().getQuery().getPageSize());

        /**
         * Verify three results (page size) are returned. The sorted results returned by the sources
         * do NOT have the offset and page size taken into account, so the offset and page size are
         * applied to the sorted results in the OffsetResultHandler.
         *
         * Offset of 2 (start at result 2) and page size of 3 (end at result 4).
         */
        LOGGER.debug("mockSortedResult1: " + mockSortedResult1);
        LOGGER.debug("mockSortedResult2: " + mockSortedResult2);
        LOGGER.debug("mockSortedResult3: " + mockSortedResult3);
        LOGGER.debug("mockSortedResult4: " + mockSortedResult4);

        assertEquals(3, federatedResponse.getResults().size());
        assertEquals(mockSortedResult2, federatedResponse.getResults().get(0));
        assertEquals(mockSortedResult3, federatedResponse.getResults().get(1));
        assertEquals(mockSortedResult4, federatedResponse.getResults().get(2));

        for (Result result : federatedResponse.getResults()) {
            LOGGER.debug("federated response result: " + result);
        }
    }

    /**
     * Verify that the original query passed into {@link
     * ddf.catalog.federation.AbstractFederationStrategy#federate(List<Source>, QueryRequest)} is
     * used by the source.
     *
     * No special results handling done by OffsetResultsHandler.
     */
    @Test
    public void testFederateOneSourceOffsetTwoPageSizeTwo() throws Exception {
        LOGGER.debug("testFederate_OneSource_OffsetTwo_PageSizeTwo()");
        // Test Setup
        Query mockQuery = mock(QueryImpl.class);
        // Offset of 2
        when(mockQuery.getStartIndex()).thenReturn(2);
        // Page size of 2
        when(mockQuery.getPageSize()).thenReturn(2);

        QueryRequest queryRequest = mock(QueryRequest.class);
        when(queryRequest.getQuery()).thenReturn(mockQuery);
        // ArgumentCaptor<QueryRequest> argument = ArgumentCaptor.forClass(QueryRequest.class);

        /**
         * When using the original query to query the source, the desired offset and page size are
         * used. So, the results returned by the source already have the offset and page size taken
         * into account.
         */
        Result mockResult1 = mock(Result.class);
        Result mockResult2 = mock(Result.class);

        SourceResponse mockSourceResponse = mock(SourceResponse.class);
        List<Result> results = Arrays.asList(mockResult1, mockResult2);
        when(mockSourceResponse.getResults()).thenReturn(results);

        Source mockSource1 = mock(Source.class);
        when(mockSource1.query(any(QueryRequest.class))).thenReturn(mockSourceResponse);
        when(mockSource1.getId()).thenReturn("####### MOCK SOURCE 1.1 #######");

        // Only one source
        List<Source> sources = new ArrayList<Source>(1);
        sources.add(mockSource1);

        SortedFederationStrategy strategy = new SortedFederationStrategy(EXECUTOR,
                new ArrayList<PreFederatedQueryPlugin>(),
                new ArrayList<PostFederatedQueryPlugin>());

        // Run Test
        QueryResponse federatedResponse = strategy.federate(sources, queryRequest);

        // Verification
        assertNotNull(federatedResponse);

        LOGGER.debug("Federated response result size: " + federatedResponse.getResults().size());

        /**
         * Verify two results (page size) are returned. The results returned by the source already
         * have the offset and page size taken into account, so we can verify that the lists match.
         */
        assertEquals(2, federatedResponse.getResults().size());
        assertEquals(mockResult1, federatedResponse.getResults().get(0));
        assertEquals(mockResult2, federatedResponse.getResults().get(1));

        LOGGER.debug("mockResult1: " + mockResult1);
        LOGGER.debug("mockResult2: " + mockResult2);

        for (Result result : federatedResponse.getResults()) {
            LOGGER.debug("result: " + result);
        }
    }

    /**
     * Verify that the original query passed into {@link
     * ddf.catalog.federation.AbstractFederationStrategy#federate(List<Source>, QueryRequest)} is
     * used by the sources.
     *
     * No special results handling done by OffsetResultsHandler.
     */
    @Test
    public void testFederateTwoSourcesOffsetOnePageSizeThree() throws Exception {
        LOGGER.debug("testFederate_TwoSources_OffsetOne_PageSizeThree()");
        // Test Setup
        Query mockQuery = mock(QueryImpl.class);
        // Offset of 1
        when(mockQuery.getStartIndex()).thenReturn(1);
        // Page size of 3
        when(mockQuery.getPageSize()).thenReturn(3);

        QueryRequest queryRequest = mock(QueryRequest.class);
        when(queryRequest.getQuery()).thenReturn(mockQuery);

        /**
         * When using the original query to query the sources, the desired offset and page size are
         * used. So, the results returned by the sources already have the offset and page size taken
         * into account.
         */
        Result mockSource1Result1 = mock(Result.class);
        Mockito.when(mockSource1Result1.getRelevanceScore()).thenReturn(0.7);
        Result mockSource1Result2 = mock(Result.class);
        Mockito.when(mockSource1Result2.getRelevanceScore()).thenReturn(0.5);
        Result mockSource1Result3 = mock(Result.class);
        Mockito.when(mockSource1Result3.getRelevanceScore()).thenReturn(0.3);
        Result mockSource1Result4 = mock(Result.class);
        Mockito.when(mockSource1Result4.getRelevanceScore()).thenReturn(0.1);

        SourceResponse mockSource1Response = mock(SourceResponse.class);
        List<Result> mockSource1Results = Arrays
                .asList(mockSource1Result1, mockSource1Result2, mockSource1Result3,
                        mockSource1Result4);
        when(mockSource1Response.getResults()).thenReturn(mockSource1Results);

        Source mockSource1 = mock(Source.class);
        when(mockSource1.query(any(QueryRequest.class))).thenReturn(mockSource1Response);
        when(mockSource1.getId()).thenReturn("####### MOCK SOURCE 1.4 #######");

        Result mockSource2Result1 = mock(Result.class);
        Mockito.when(mockSource2Result1.getRelevanceScore()).thenReturn(0.8);
        Result mockSource2Result2 = mock(Result.class);
        Mockito.when(mockSource2Result2.getRelevanceScore()).thenReturn(0.6);
        Result mockSource2Result3 = mock(Result.class);
        Mockito.when(mockSource2Result3.getRelevanceScore()).thenReturn(0.4);
        Result mockSource2Result4 = mock(Result.class);
        Mockito.when(mockSource2Result4.getRelevanceScore()).thenReturn(0.2);

        SourceResponse mockSource2Response = mock(SourceResponse.class);
        List<Result> mockSource2Results = Arrays
                .asList(mockSource2Result1, mockSource2Result2, mockSource2Result3,
                        mockSource2Result4);
        when(mockSource2Response.getResults()).thenReturn(mockSource2Results);

        Source mockSource2 = mock(Source.class);
        when(mockSource2.query(any(QueryRequest.class))).thenReturn(mockSource2Response);
        when(mockSource2.getId()).thenReturn("####### MOCK SOURCE 2.4 #######");

        // Two sources
        List<Source> sources = new ArrayList<Source>(2);
        sources.add(mockSource1);
        sources.add(mockSource2);

        SortedFederationStrategy strategy = new SortedFederationStrategy(EXECUTOR,
                new ArrayList<PreFederatedQueryPlugin>(),
                new ArrayList<PostFederatedQueryPlugin>());

        // Run Test
        QueryResponse federatedResponse = strategy.federate(sources, queryRequest);

        // Verification
        assertNotNull(federatedResponse);

        LOGGER.debug("Federated response result size: " + federatedResponse.getResults().size());

        /**
         * Verify three results (page size) are returned. Since we are using mock Results, the
         * relevance score is 0.0, and the merged and sorted results of both sources is
         * mockSource2Result1, mockSource1Result1, mockSource2Result2, mockSource1Result2,
         * mockSource2Result3, mockSource1Result3, mockSource2Result4, mockSource1Result4. So, the
         * results are mockSource2Result1, mockSource1Result1, mockSource2Result2. No need to use
         * OffsetResultHander in this case.
         */
        assertEquals(3, federatedResponse.getResults().size());
        assertEquals(mockSource2Result1, federatedResponse.getResults().get(0));
        assertEquals(mockSource1Result1, federatedResponse.getResults().get(1));
        assertEquals(mockSource2Result2, federatedResponse.getResults().get(2));

        LOGGER.debug("mockSource2Result1: " + mockSource2Result1);
        LOGGER.debug("mockSource1Result1: " + mockSource1Result1);
        LOGGER.debug("mockSource2Result2: " + mockSource2Result2);

        for (Result result : federatedResponse.getResults()) {
            LOGGER.debug("federated response result: " + result);
        }

        // Check the responseProperties
        List<String> siteList = (List) federatedResponse.getPropertyValue(QueryResponse.SITE_LIST);

        assertTrue(siteList.contains("####### MOCK SOURCE 2.4 #######"));

        Map<String, Serializable> siteProperties = (Map) federatedResponse
                .getPropertyValue("####### MOCK SOURCE 2.4 #######");
        assertNotNull(siteProperties.get(QueryResponse.TOTAL_HITS));
        assertNotNull(siteProperties.get(QueryResponse.TOTAL_RESULTS_RETURNED));

        assertTrue(siteList.contains("####### MOCK SOURCE 2.4 #######"));

        siteProperties = (Map) federatedResponse
                .getPropertyValue("####### MOCK SOURCE 1.4 #######");
        assertNotNull(siteProperties.get(QueryResponse.TOTAL_HITS));
        assertNotNull(siteProperties.get(QueryResponse.TOTAL_RESULTS_RETURNED));
    }

    /**
     * Verify that the original query passed into {@link
     * ddf.catalog.federation.AbstractFederationStrategy#federate(List<Source>, QueryRequest)} is
     * used by the source.
     *
     * No special results handling done by OffsetResultsHandler.
     */
    @Test
    public void testFederateOneSourceOffsetOnePageSizeTwo() throws Exception {
        LOGGER.debug("testFederate_OneSource_OffsetOne_PageSizeTwo()");
        // Test Setup
        Query mockQuery = mock(QueryImpl.class);
        // Offset of 1
        when(mockQuery.getStartIndex()).thenReturn(1);
        // Page size of 2
        when(mockQuery.getPageSize()).thenReturn(2);

        QueryRequest queryRequest = mock(QueryRequest.class);
        when(queryRequest.getQuery()).thenReturn(mockQuery);

        /**
         * When using the original query to query the source, the desired offset and page size are
         * used. So, the results returned by the source already have the offset and page size taken
         * into account.
         */
        Result mockResult1 = mock(Result.class);
        Result mockResult2 = mock(Result.class);

        SourceResponse mockSourceResponse = mock(SourceResponse.class);
        List<Result> results = Arrays.asList(mockResult1, mockResult2);
        when(mockSourceResponse.getResults()).thenReturn(results);

        Source mockSource1 = mock(Source.class);
        when(mockSource1.query(any(QueryRequest.class))).thenReturn(mockSourceResponse);
        when(mockSource1.getId()).thenReturn("####### MOCK SOURCE 1.2 #######");

        // Only one source
        List<Source> sources = new ArrayList<Source>(1);
        sources.add(mockSource1);

        SortedFederationStrategy strategy = new SortedFederationStrategy(EXECUTOR,
                new ArrayList<PreFederatedQueryPlugin>(),
                new ArrayList<PostFederatedQueryPlugin>());

        // Run Test
        QueryResponse federatedResponse = strategy.federate(sources, queryRequest);

        // Verification
        assertNotNull(federatedResponse);

        LOGGER.debug("Federated response result size: " + federatedResponse.getResults().size());

        /**
         * Verify two results (page size) are returned. The results returned by the source already
         * have the offset and page size taken into account, so we can verify that the lists match.
         */
        assertEquals(2, federatedResponse.getResults().size());
        assertEquals(mockResult1, federatedResponse.getResults().get(0));
        assertEquals(mockResult2, federatedResponse.getResults().get(1));

        LOGGER.debug("mockResult1: " + mockResult1);
        LOGGER.debug("mockResult2: " + mockResult2);

        for (Result result : federatedResponse.getResults()) {
            LOGGER.debug("result: " + result);
        }

        // Check the responseProperties
        assertEquals("####### MOCK SOURCE 1.2 #######",
                ((List) federatedResponse.getPropertyValue(QueryResponse.SITE_LIST)).get(0));

        Map<String, Serializable> siteProperties = (Map) federatedResponse
                .getPropertyValue("####### MOCK SOURCE 1.2 #######");
        assertNotNull(siteProperties.get(QueryResponse.TOTAL_HITS));
        assertNotNull(siteProperties.get(QueryResponse.TOTAL_RESULTS_RETURNED));
    }

}
