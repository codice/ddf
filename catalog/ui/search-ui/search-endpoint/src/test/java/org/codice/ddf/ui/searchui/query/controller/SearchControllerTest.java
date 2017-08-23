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
 **/
package org.codice.ddf.ui.searchui.query.controller;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.codice.ddf.ui.searchui.query.actions.ActionRegistryImpl;
import org.codice.ddf.ui.searchui.query.controller.search.SourceQueryRunnable;
import org.codice.ddf.ui.searchui.query.model.Search;
import org.codice.ddf.ui.searchui.query.model.SearchRequest;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerMessage.Mutable;
import org.cometd.bayeux.server.ServerSession;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.Subject;

/**
 * Test cases for {@link org.codice.ddf.ui.searchui.query.controller.SearchController}
 */
@RunWith(MockitoJUnitRunner.class)
public class SearchControllerTest {
    private static final String REASON_UNSUPPORTED_QUERY = "Query was invalid";

    private static final String REASON_SOURCE_UNAVAILABLE =
            "Query hit a source that was unavailable";

    private static final String REASON_INTERRUPTED = "Query was interrupted";

    private static final String REASON_TIMEOUT = "Query timed out";

    private static final String REASON_INTERNAL = "Internal error";

    private static final int MAX_EXCEPTION_SCAN_DEPTH = 10;

    // NOTE: The ServerSession ID == The ClientSession ID
    private static final String MOCK_SESSION_ID = "1234-5678-9012-3456";

    private static final Date TIMESTAMP = new Date();

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchControllerTest.class);

    private static final String[] SOURCE_ID_LIST = {"any_source_id"};

    private ArgumentCaptor<QueryResponse> queryResponseArgumentCaptor;

    private ArgumentCaptor<QueryRequest> queryRequestArgumentCaptor;

    private SearchController searchController;

    private CatalogFramework framework;

    private ServerSession mockServerSession;

    @Mock
    private ExecutorService mockExecutor;

    @Mock
    private BayeuxServer mockBayeuxServer;

    @Mock
    private ServerChannel mockServerChannel;

    @Mock
    private Future mockFuture;

    @Mock
    private SearchRequest mockSearchRequest;

    @Mock
    private Query mockQuery;

    @Mock
    private Search search;

    @Before
    public void setUp() throws Exception {
        framework = createFramework();
        queryResponseArgumentCaptor = ArgumentCaptor.forClass(QueryResponse.class);
        queryRequestArgumentCaptor = ArgumentCaptor.forClass(QueryRequest.class);

        searchController = new SearchController(framework,
                new ActionRegistryImpl(Collections.emptyList(), Collections.emptyList()),
                new GeotoolsFilterAdapterImpl(),
                new SequentialExecutorService());

        mockServerSession = mock(ServerSession.class);

        when(mockServerSession.getId()).thenReturn(MOCK_SESSION_ID);
        when(mockSearchRequest.getId()).thenReturn("any_id");
        when(mockSearchRequest.getSourceIds()).thenReturn(new HashSet<>(Arrays.asList(SOURCE_ID_LIST)));
        when(mockSearchRequest.getQuery()).thenReturn(mockQuery);
        when(mockQuery.getSortBy()).thenReturn(SortBy.NATURAL_ORDER);
        when(mockBayeuxServer.getChannel(anyString())).thenReturn(mockServerChannel);
        when(mockExecutor.submit(any(Runnable.class))).thenAnswer((args) -> {
            Runnable runnable = (Runnable) args.getArguments()[0];
            runnable.run();
            return mockFuture;
        });
    }

    @Test
    public void testExceptionPassedOnBadQuery() throws Exception {
        runTestForReasonMessages(new ExecutionException(new UnsupportedQueryException()),
                REASON_UNSUPPORTED_QUERY);
    }

    @Test
    public void testExceptionPassedOnSourceError() throws Exception {
        runTestForReasonMessages(new ExecutionException(new SourceUnavailableException()),
                REASON_SOURCE_UNAVAILABLE);
    }

    @Test
    public void testExceptionPassedOnInterrupt() throws Exception {
        runTestForReasonMessages(new InterruptedException(), REASON_INTERRUPTED);
    }

    @Test
    public void testExceptionPassedOnTimeout() throws Exception {
        runTestForReasonMessages(new TimeoutException(), REASON_TIMEOUT);
    }

    @Test
    public void testExceptionPassedOnExecutionException() throws Exception {
        ExecutionException executionException =
                new ExecutionException(new ExecutionException(new Exception()));
        runTestForReasonMessages(executionException, REASON_INTERNAL);
    }

    @Test
    public void testExceptionScanDepthValid() throws Exception {
        runTestForExceptionScanDepth(2, REASON_TIMEOUT);
    }

    @Test
    public void testExceptionScanDepthTooDeep() throws Exception {
        runTestForExceptionScanDepth(1, REASON_INTERNAL);
    }

    @Test
    public void testMetacardTypeValuesCacheDisabled() throws Exception {

        final String ID = "id";
        Set<String> srcIds = new HashSet<String>();
        srcIds.add(ID);

        BayeuxServer bayeuxServer = mock(BayeuxServer.class);
        ServerChannel channel = mock(ServerChannel.class);
        ArgumentCaptor<ServerMessage.Mutable> reply =
                ArgumentCaptor.forClass(ServerMessage.Mutable.class);

        when(bayeuxServer.getChannel(any(String.class))).thenReturn(channel);

        SearchRequest request = new SearchRequest(srcIds,
                getQueryRequest("title LIKE 'Meta*'"),
                ID);

        searchController.setBayeuxServer(bayeuxServer);
        // Disable Cache
        searchController.setCacheDisabled(true);
        searchController.executeQuery(request, mockServerSession, null);

        verify(channel, timeout(1000).only()).publish(any(ServerSession.class), reply.capture());
        List<Mutable> replies = reply.getAllValues();
        assertReplies(replies);
    }

    @Test
    public void testMetacardTypeValuesCacheEnabled() throws Exception {

        final String ID = "id";
        Set<String> srcIds = new HashSet<>();
        srcIds.add(ID);

        setupModifiedFramework(2L, 2, 2, 0);
        BayeuxServer bayeuxServer = mock(BayeuxServer.class);
        ServerChannel channel = mock(ServerChannel.class);
        ArgumentCaptor<ServerMessage.Mutable> reply =
                ArgumentCaptor.forClass(ServerMessage.Mutable.class);

        when(bayeuxServer.getChannel(any(String.class))).thenReturn(channel);

        SearchRequest request = new SearchRequest(srcIds,
                getQueryRequest("title LIKE 'Meta*'"),
                ID);

        searchController.setBayeuxServer(bayeuxServer);
        searchController.setCacheDisabled(false);
        searchController.executeQuery(request, mockServerSession, null);

        verify(channel, timeout(1000).times(2)).publish(any(ServerSession.class), reply.capture());
        List<Mutable> replies = reply.getAllValues();
        assertReplies(replies);
    }

    private Query getQueryRequest(String cql) throws CQLException {
        Filter filter = ECQL.toFilter(cql);

        return new QueryImpl(filter,
                1,
                200,
                new SortByImpl(Result.TEMPORAL, SortOrder.DESCENDING),
                true,
                1000);
    }

    @Test
    public void testExecuteQueryCacheEnabledWithSingleSource() throws Exception {
        Set<String> srcIds = new HashSet<>(1);
        srcIds.add("id");
        List<String> modes = cacheQuery(srcIds, 2);

        assertThat(modes.size(), is(2));
        assertThat(modes, hasItems("cache", "update"));
    }

    @Test
    public void testExecuteQueryCacheEnabledWithMultipleSources() throws Exception {
        Set<String> srcIds = new HashSet<>(2);
        srcIds.add("id1");
        srcIds.add("id2");
        List<String> modes = cacheQuery(srcIds, 3);

        assertThat(modes.size(), is(3));
        assertThat(modes, hasItems("cache", "update", "update"));
    }

    @Test
    public void testFailingQuery() throws Exception {
        // Setup
        framework = mock(CatalogFramework.class);

        QueryResponse response = mock(QueryResponse.class);
        when(response.getResults()).thenThrow(new RuntimeException("Getting results failed"));

        when(framework.query(any(QueryRequest.class))).thenReturn(response);

        searchController = new SearchController(framework,
                new ActionRegistryImpl(Collections.emptyList(), Collections.emptyList()),
                new GeotoolsFilterAdapterImpl(),
                new SequentialExecutorService());

        final String ID = "id";
        Set<String> srcIds = new HashSet<>();
        srcIds.add(ID);

        SearchRequest request = new SearchRequest(srcIds,
                getQueryRequest("anyText LIKE '*'"),
                "queryId");
        BayeuxServer bayeuxServer = mock(BayeuxServer.class);
        ServerChannel channel = mock(ServerChannel.class);
        when(bayeuxServer.getChannel(any(String.class))).thenReturn(channel);
        searchController.setBayeuxServer(bayeuxServer);
        searchController.setCacheDisabled(true);

        // Perform Test
        searchController.executeQuery(request, mockServerSession, null);

        // Verify
        verify(channel, times(1)).publish(any(), any());
    }

    private List<String> cacheQuery(Set<String> srcIds, int queryRequestCount) throws Exception {

        setupModifiedFramework(2L, 0);
        SearchRequest request = new SearchRequest(srcIds,
                getQueryRequest("anyText LIKE '*'"),
                "queryId");
        BayeuxServer bayeuxServer = mock(BayeuxServer.class);
        ServerChannel channel = mock(ServerChannel.class);
        when(bayeuxServer.getChannel(any(String.class))).thenReturn(channel);
        ArgumentCaptor<QueryRequest> queryRequestCaptor =
                ArgumentCaptor.forClass(QueryRequest.class);
        searchController.setCacheDisabled(false);
        searchController.setNormalizationDisabled(false);
        searchController.setBayeuxServer(bayeuxServer);

        // Perform Test
        searchController.executeQuery(request, mockServerSession, null);

        // Verify
        verify(framework, times(queryRequestCount)).query(queryRequestCaptor.capture());
        List<QueryRequest> capturedQueryRequests = queryRequestCaptor.getAllValues();

        List<String> modes = new ArrayList<>(queryRequestCount);
        for (QueryRequest queryRequest : capturedQueryRequests) {
            for (String key : queryRequest.getProperties()
                    .keySet()) {
                modes.add((String) queryRequest.getPropertyValue(key));
            }
        }

        return modes;
    }

    /**
     * Verify that the CatalogFramework does not use the cache (i.e. the CatalogFramework
     * is called WITHOUT the query request property mode=cache).
     */
    @Test
    public void testExecuteQueryCacheDisabled() throws Exception {
        // Setup
        final String ID = "id";
        Set<String> srcIds = new HashSet<>(1);
        srcIds.add(ID);
        SearchRequest request = new SearchRequest(srcIds,
                getQueryRequest("title LIKE 'Meta*'"),
                ID);
        BayeuxServer bayeuxServer = mock(BayeuxServer.class);
        ServerChannel channel = mock(ServerChannel.class);
        when(bayeuxServer.getChannel(any(String.class))).thenReturn(channel);
        ArgumentCaptor<QueryRequest> queryRequestCaptor =
                ArgumentCaptor.forClass(QueryRequest.class);
        // Enable Cache
        searchController.setCacheDisabled(true);
        searchController.setBayeuxServer(bayeuxServer);

        // Perform Test
        searchController.executeQuery(request, mockServerSession, null);

        // Verify
        verify(framework, times(2)).query(queryRequestCaptor.capture());
        assertThat(queryRequestCaptor.getValue()
                .getProperties()
                .size(), is(0));
    }

    @Test
    public void testOffsetStartIndexMultiPage() throws Exception {
        final int START_INDEX = 13;
        final long TOTAL_CATALOG_HITS = 20L;
        final int EXPECTED_RESULT = 8;
        final int FIRST_PAGE_RESULT = 5;
        final int SECOND_PAGE_RESULT = 3;
        final int EMPTY_PAGE_RESULT = 0;
        final String EXPECTED_HITS_MESSAGE =
                "The framework should have only return " + TOTAL_CATALOG_HITS + " hits";
        final String EXPECTED_RESULTS_MESSAGE =
                "The max query results was set to expect " + EXPECTED_RESULT + " results";

        when(mockQuery.getStartIndex()).thenReturn(START_INDEX);
        setupModifiedFramework(TOTAL_CATALOG_HITS,
                FIRST_PAGE_RESULT,
                SECOND_PAGE_RESULT,
                EMPTY_PAGE_RESULT);
        SourceQueryRunnable sourceQueryRunnable = setupSourceQueryRunnable(search, EXPECTED_RESULT);
        sourceQueryRunnable.run();

        verify(framework, times(3)).query(queryRequestArgumentCaptor.capture());
        verify(search).update(any(), queryResponseArgumentCaptor.capture());
        List<Integer> startIndices = queryRequestArgumentCaptor.getAllValues()
                .stream()
                .map(qR -> qR.getQuery()
                        .getStartIndex())
                .collect(Collectors.toList());
        List<Integer> expectedIndices = Arrays.asList(13, 18, 21);
        assertThat(startIndices, is(expectedIndices));
        assertThat(EXPECTED_HITS_MESSAGE,
                queryResponseArgumentCaptor.getValue()
                        .getHits(),
                is(TOTAL_CATALOG_HITS));
        assertThat(EXPECTED_RESULTS_MESSAGE,
                queryResponseArgumentCaptor.getValue()
                        .getResults()
                        .size(),
                is(EXPECTED_RESULT));
    }

    @Test
    public void testMatchingPageSize() throws Exception {
        final long TOTAL_CATALOG_HITS = 60L;
        final int EXPECTED_RESULT = 60;
        final int FIRST_PAGE_RESULT = 30;
        final int SECOND_PAGE_RESULT = 30;
        final int EMPTY_PAGE_RESULT = 0;
        final String EXPECTED_HITS_MESSAGE =
                "The framework should have only return " + TOTAL_CATALOG_HITS + " hits";
        final String EXPECTED_RESULTS_MESSAGE =
                "The max query results was set to expect " + EXPECTED_RESULT + " results";

        setupModifiedFramework(TOTAL_CATALOG_HITS,
                FIRST_PAGE_RESULT,
                SECOND_PAGE_RESULT,
                EMPTY_PAGE_RESULT);
        SourceQueryRunnable sourceQueryRunnable = setupSourceQueryRunnable(search, EXPECTED_RESULT);
        sourceQueryRunnable.run();

        verify(framework, times(3)).query(queryRequestArgumentCaptor.capture());
        verify(search).update(any(), queryResponseArgumentCaptor.capture());
        List<Integer> startIndices = queryRequestArgumentCaptor.getAllValues()
                .stream()
                .map(qR -> qR.getQuery()
                        .getStartIndex())
                .collect(Collectors.toList());
        List<Integer> expectedIndices = Arrays.asList(0, 30, 60);
        assertThat(startIndices, is(expectedIndices));
        assertThat(EXPECTED_HITS_MESSAGE,
                queryResponseArgumentCaptor.getValue()
                        .getHits(),
                is(TOTAL_CATALOG_HITS));
        assertThat(EXPECTED_RESULTS_MESSAGE,
                queryResponseArgumentCaptor.getValue()
                        .getResults()
                        .size(),
                is(EXPECTED_RESULT));
    }

    @Test
    public void testMaxQueryPaging() throws Exception {
        final long TOTAL_CATALOG_HITS = 1100L;
        final int EXPECTED_MAX_RESULT = 1000;
        final int FIRST_PAGE_RESULT = 400;
        final int SECOND_PAGE_RESULT = 400;
        final int THIRD_PAGE_RESULT = 300;
        final int EMPTY_PAGE_RESULT = 0;
        final String EXPECTED_HITS_MESSAGE =
                "The framework should have only return " + TOTAL_CATALOG_HITS + " hits";
        final String EXPECTED_RESULTS_MESSAGE =
                "The max query results was set to expect " + EXPECTED_MAX_RESULT + " results";

        setupModifiedFramework(TOTAL_CATALOG_HITS,
                FIRST_PAGE_RESULT,
                SECOND_PAGE_RESULT,
                THIRD_PAGE_RESULT,
                EMPTY_PAGE_RESULT);
        SourceQueryRunnable sourceQueryRunnable = setupSourceQueryRunnable(search,
                EXPECTED_MAX_RESULT);
        sourceQueryRunnable.run();

        verify(framework, times(4)).query(queryRequestArgumentCaptor.capture());
        verify(search).update(any(), queryResponseArgumentCaptor.capture());
        List<Integer> startIndices = queryRequestArgumentCaptor.getAllValues()
                .stream()
                .map(qR -> qR.getQuery()
                        .getStartIndex())
                .collect(Collectors.toList());
        List<Integer> expectedIndices = Arrays.asList(0, 400, 800, 1100);
        assertThat(startIndices, is(expectedIndices));
        assertThat(EXPECTED_HITS_MESSAGE,
                queryResponseArgumentCaptor.getValue()
                        .getHits(),
                is(TOTAL_CATALOG_HITS));
        assertThat(EXPECTED_RESULTS_MESSAGE,
                queryResponseArgumentCaptor.getValue()
                        .getResults()
                        .size(),
                is(EXPECTED_MAX_RESULT));
    }

    private SourceQueryRunnable setupSourceQueryRunnable(Search search, int pageSize) {
        when(mockQuery.getPageSize()).thenReturn(pageSize);
        Subject subject = mock(Subject.class);
        BayeuxServer bayeuxServer = mock(BayeuxServer.class);
        ServerChannel channel = mock(ServerChannel.class);
        when(bayeuxServer.getChannel(any(String.class))).thenReturn(channel);
        searchController.setBayeuxServer(bayeuxServer);
        when(mockSearchRequest.getQuery()).thenReturn(mockQuery);

        return new SourceQueryRunnable(searchController,
                "sampleSourceId",
                mockSearchRequest,
                subject,
                new HashMap<>(),
                search,
                mockServerSession,
                mockFuture,
                mockFuture);
    }

    private void setupModifiedFramework(long totalHits, int... catalogResults) throws Exception {
        framework = createFramework(totalHits, catalogResults);
        searchController = new SearchController(framework,
                new ActionRegistryImpl(Collections.emptyList(), Collections.emptyList()),
                new GeotoolsFilterAdapterImpl(),
                new SequentialExecutorService());
    }

    private void assertReplies(List<Mutable> replies) {
        for (Mutable reply : replies) {
            assertThat(reply, is(not(nullValue())));
            assertThat(reply.getDataAsMap()
                    .get(Search.METACARD_TYPES), is(not(nullValue())));
            assertThat(reply.getDataAsMap()
                    .get(Search.METACARD_TYPES), instanceOf(Map.class));

            @SuppressWarnings("unchecked")
            Map<String, Object> types = (Map<String, Object>) reply.getDataAsMap()
                    .get(Search.METACARD_TYPES);

            assertThat(types.get("ddf.metacard"), is(not(nullValue())));
            assertThat(types.get("ddf.metacard"), instanceOf(Map.class));

            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> typeInfo =
                    (Map<String, Map<String, Object>>) types.get("ddf.metacard");

            assertThat((String) typeInfo.get("effective")
                    .get("format"), is("DATE"));
            assertThat((String) typeInfo.get("modified")
                    .get("format"), is("DATE"));
            assertThat((String) typeInfo.get("created")
                    .get("format"), is("DATE"));
            assertThat((String) typeInfo.get("expiration")
                    .get("format"), is("DATE"));
            assertThat((String) typeInfo.get("id")
                    .get("format"), is("STRING"));
            assertThat((String) typeInfo.get("title")
                    .get("format"), is("STRING"));
            assertThat((String) typeInfo.get("metadata-content-type")
                    .get("format"), is("STRING"));
            assertThat((String) typeInfo.get("metadata-content-type-version")
                    .get("format"), is("STRING"));
            assertThat((String) typeInfo.get("metadata-target-namespace")
                    .get("format"), is("STRING"));
            assertThat((String) typeInfo.get("resource-uri")
                    .get("format"), is("STRING"));
            assertThat((Boolean) typeInfo.get("resource-uri")
                    .get("indexed"), is(true));
            // since resource-size is not indexed, it should be filtered out
            assertThat((Boolean) typeInfo.get("resource-size")
                    .get("indexed"), is(false));
            assertThat((String) typeInfo.get("metadata")
                    .get("format"), is("XML"));
            assertThat((String) typeInfo.get("location")
                    .get("format"), is("GEOMETRY"));
        }
    }

    private CatalogFramework createFramework() throws Exception {
        return createFramework(2L, 2, 0);
    }

    private CatalogFramework createFramework(long totalHits, int... pagingResults)
            throws Exception {

        CatalogFramework framework = mock(CatalogFramework.class);

        List<QueryResponse> responseList = new ArrayList<>();
        for (int resultsPerPage : pagingResults) {
            List<Result> results = generateResults(resultsPerPage);
            QueryResponse response = new QueryResponseImpl(mock(QueryRequest.class),
                    results,
                    totalHits);
            responseList.add(response);
        }

        when(framework.query(any(QueryRequest.class))).thenReturn(responseList.get(0),
                popNextResponse(responseList));
        return framework;
    }

    private QueryResponse[] popNextResponse(List<QueryResponse> responseList) {
        QueryResponse[] responseArr = new QueryResponse[responseList.size()];
        responseList.toArray(responseArr);
        return Arrays.copyOfRange(responseArr, 1, responseList.size());
    }

    private List<Result> generateResults(int resultsPerPage) {
        return Stream.generate(() -> generateResult(UUID.randomUUID()
                .toString()))
                .limit(resultsPerPage)
                .collect(Collectors.toList());
    }

    private Result generateResult(String id) {
        Result result = mock(Result.class);
        MetacardImpl metacard = new MetacardImpl();
        metacard.setId(id);
        when(result.getMetacard()).thenReturn(metacard);
        return result;
    }

    private Exception generateExceptionStack(int targetSize, int currentSize,
            @Nullable Exception cause) {
        Exception e;
        if (cause == null) {
            e = new Exception();
        } else {
            e = new Exception(cause);
        }
        if (currentSize < targetSize) {
            return generateExceptionStack(targetSize, currentSize + 1, e);
        } else {
            return e;
        }
    }

    /**
     * Template method for testing exception depth and cause calculations
     */
    private void runTestForExceptionScanDepth(int offset, String expectedReason) throws Exception {
        Exception e = generateExceptionStack(MAX_EXCEPTION_SCAN_DEPTH - offset,
                0,
                new TimeoutException());
        runTestForReasonMessages(new ExecutionException(e), expectedReason);
    }

    /**
     * Template method for testing the JSON responses for reasons
     */
    private void runTestForReasonMessages(Exception exception, String expectedReasonMessage)
            throws Exception {
        searchController = new SearchController(framework,
                new ActionRegistryImpl(Collections.emptyList(), Collections.emptyList()),
                new GeotoolsFilterAdapterImpl(),
                mockExecutor);
        searchController.setBayeuxServer(mockBayeuxServer);
        ArgumentCaptor<ServerMessage.Mutable> serverMessageCaptor = ArgumentCaptor.forClass(
                ServerMessage.Mutable.class);
        when(mockFuture.get(anyLong(), any(TimeUnit.class))).thenThrow(exception);
        searchController.executeQuery(mockSearchRequest, mockServerSession, null);
        verify(mockServerChannel, times(3)).publish(any(ServerSession.class),
                serverMessageCaptor.capture());
        validateReasonMessageInJson(serverMessageCaptor.getValue(), expectedReasonMessage);
    }

    private void validateReasonMessageInJson(ServerMessage.Mutable response,
            String expectedReasonMessage) {
        Map<String, Object> jsonDocument = response.getDataAsMap();
        List<Map<String, Object>> statuses = (List<Map<String, Object>>) jsonDocument.get("status");

        if (statuses.isEmpty()) {
            fail("Status list was empty");
        }

        List<String> reasons = (List<String>) statuses.get(0)
                .get("reasons");

        if (reasons.isEmpty()) {
            fail("Reasons list was empty");
        }
        assertThat(String.format("Unexpected reason: %s", reasons.get(0)),
                reasons.get(0)
                        .equals(expectedReasonMessage));

    }

    /**
     * The SearchController spawns off threads to complete tasks. We cannot reliably test
     * multi-threaded code, so we use this Mock ExecutorService to ensure that all operations
     * for a test happen in the same thread as each JUnit test case.
     */
    private class SequentialExecutorService implements ExecutorService {

        @Override
        public void execute(Runnable command) {
            submit(command);
        }

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return null;
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public <T> Future<T> submit(final Callable<T> task) {
            try {
                return new Future<T>() {
                    @Override
                    public boolean cancel(boolean mayInterruptIfRunning) {
                        return false;
                    }

                    @Override
                    public boolean isCancelled() {
                        return false;
                    }

                    @Override
                    public boolean isDone() {
                        return false;
                    }

                    @Override
                    public T get() throws InterruptedException, ExecutionException {
                        return null;
                    }

                    @Override
                    public T get(long timeout, TimeUnit unit)
                            throws InterruptedException, ExecutionException, TimeoutException {
                        try {
                            return task.call();
                        } catch (Exception e) {
                            fail(e.getMessage());
                        }
                        return null;
                    }
                };
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            try {
                task.run();
            } catch (Throwable t) {
                return Futures.immediateFailedFuture(t);
            }
            return Futures.immediateFuture(result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            try {
                task.run();
            } catch (Throwable t) {
                return Futures.immediateFailedFuture(t);
            }
            return new Future<Object>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return false;
                }

                @Override
                public Object get() throws InterruptedException, ExecutionException {
                    return null;
                }

                @Override
                public Object get(long timeout, TimeUnit unit)
                        throws InterruptedException, ExecutionException, TimeoutException {
                    return null;
                }
            };
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
                throws InterruptedException {
            return null;
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
                TimeUnit unit) throws InterruptedException {
            return null;
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
                throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }

}
