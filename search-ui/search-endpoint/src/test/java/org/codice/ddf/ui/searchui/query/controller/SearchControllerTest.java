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
package org.codice.ddf.ui.searchui.query.controller;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.codice.ddf.ui.searchui.query.actions.ActionRegistryImpl;
import org.codice.ddf.ui.searchui.query.model.Search;
import org.codice.ddf.ui.searchui.query.model.SearchRequest;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerMessage.Mutable;
import org.cometd.bayeux.server.ServerSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.action.ActionProvider;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * Test cases for {@link org.codice.ddf.ui.searchui.query.controller.SearchController}
 */
public class SearchControllerTest {
    private SearchController searchController;
    
    private CatalogFramework framework;

    // NOTE: The ServerSession ID == The ClientSession ID
    private static final String MOCK_SESSION_ID = "1234-5678-9012-3456";

    private ServerSession mockServerSession;

    private static final Date TIMESTAMP = new Date();

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchControllerTest.class);

    @Before
    public void setUp() throws Exception {
        framework = createFramework();
        
        searchController = new SearchController(framework, new ActionRegistryImpl(
                Collections.<ActionProvider> emptyList())) {
            @Override
            ExecutorService getExecutorService() {
                return new SequentialExectorService();
            }
        };

        mockServerSession = mock(ServerSession.class);

        when(mockServerSession.getId()).thenReturn(MOCK_SESSION_ID);
    }

    @Test
    public void testMetacardTypeValuesCacheDisabled() {

        final String ID = "id";
        Set<String> srcIds = new HashSet<String>();
        srcIds.add(ID);

        BayeuxServer bayeuxServer = mock(BayeuxServer.class);
        ServerChannel channel = mock(ServerChannel.class);
        ArgumentCaptor<ServerMessage.Mutable> reply =
                ArgumentCaptor.forClass(ServerMessage.Mutable.class);

        when(bayeuxServer.getChannel(any(String.class))).thenReturn(channel);

        SearchRequest request = new SearchRequest(srcIds, mock(Query.class), ID);

        searchController.setBayeuxServer(bayeuxServer);
        // Disable Cache
        searchController.setCacheDisabled(true);
        searchController.executeQuery(request, mockServerSession, null);

        verify(channel, timeout(1000).only()).publish(any(ServerSession.class),
                reply.capture(), anyString());
        List<Mutable> replies = reply.getAllValues();
        assertReplies(replies);   
    }
    
    @Test
    public void testMetacardTypeValuesCacheEnabled() {

        final String ID = "id";
        Set<String> srcIds = new HashSet<String>();
        srcIds.add(ID);

        BayeuxServer bayeuxServer = mock(BayeuxServer.class);
        ServerChannel channel = mock(ServerChannel.class);
        ArgumentCaptor<ServerMessage.Mutable> reply =
                ArgumentCaptor.forClass(ServerMessage.Mutable.class);

        when(bayeuxServer.getChannel(any(String.class))).thenReturn(channel);

        SearchRequest request = new SearchRequest(srcIds, mock(Query.class), ID);

        searchController.setBayeuxServer(bayeuxServer);
        // Disable Cache
        searchController.setCacheDisabled(false);
        searchController.executeQuery(request, mockServerSession, null);

        verify(channel, timeout(1000).times(2)).publish(any(ServerSession.class),
                reply.capture(), anyString());
        List<Mutable> replies = reply.getAllValues();
        assertReplies(replies);
    }
    
    /**
     * Verify that the CatalogFramework does not use the cache (i.e. the CatalogFramework 
     * is called WITH the query request property mode=cache).
     */
    @Test
    public void testExecuteQueryCacheEnabled() throws Exception {
        // Setup
        final String ID = "id";
        Set<String> srcIds = new HashSet<>(1);
        srcIds.add(ID);
        SearchRequest request = new SearchRequest(srcIds, mock(Query.class), ID);
        BayeuxServer bayeuxServer = mock(BayeuxServer.class);
        ServerChannel channel = mock(ServerChannel.class);
        when(bayeuxServer.getChannel(any(String.class))).thenReturn(channel);
        ArgumentCaptor<QueryRequest> queryRequestCaptor = ArgumentCaptor
                .forClass(QueryRequest.class);
        // Disable Cache
        searchController.setCacheDisabled(false);
        searchController.setBayeuxServer(bayeuxServer);

        // Perform Test
        searchController.executeQuery(request, mockServerSession, null);

        // Verify
        verify(framework, times(3)).query(queryRequestCaptor.capture());
        List<QueryRequest> capturedQueryRequests = queryRequestCaptor.getAllValues();

        List<String> modes = new ArrayList<>(3);
        for (QueryRequest queryRequest : capturedQueryRequests) {
            for (String key : queryRequest.getProperties().keySet()) {
                modes.add((String) queryRequest.getPropertyValue(key));
            }
        }

        assertThat(modes, hasItems("cache", "index", "cache"));
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
        SearchRequest request = new SearchRequest(srcIds, mock(Query.class), ID);
        BayeuxServer bayeuxServer = mock(BayeuxServer.class);
        ServerChannel channel = mock(ServerChannel.class);
        when(bayeuxServer.getChannel(any(String.class))).thenReturn(channel);
        ArgumentCaptor<QueryRequest> queryRequestCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        // Enable Cache
        searchController.setCacheDisabled(true);
        searchController.setBayeuxServer(bayeuxServer);

        // Perform Test
        searchController.executeQuery(request, mockServerSession, null);
        
        // Verify
        verify(framework).query(queryRequestCaptor.capture());
        assertThat(queryRequestCaptor.getValue().getProperties().size(), is(0));
    }
    
    private void assertReplies(List<Mutable> replies) {
        for (Mutable reply : replies) {
            assertThat(reply, is(not(nullValue())));
            assertThat(reply.get(Search.METACARD_TYPES), is(not(nullValue())));
            assertThat(reply.get(Search.METACARD_TYPES), instanceOf(Map.class));

            @SuppressWarnings("unchecked")
            Map<String, Object> types = (Map<String, Object>) reply.get(Search.METACARD_TYPES);

            assertThat(types.get("ddf.metacard"), is(not(nullValue())));
            assertThat(types.get("ddf.metacard"), instanceOf(Map.class));

            @SuppressWarnings("unchecked")
            Map<String, String> typeInfo = (Map<String, String>) types.get("ddf.metacard");

            assertThat(typeInfo.get("effective"), is("DATE"));
            assertThat(typeInfo.get("modified"), is("DATE"));
            assertThat(typeInfo.get("created"), is("DATE"));
            assertThat(typeInfo.get("expiration"), is("DATE"));
            assertThat(typeInfo.get("id"), is("STRING"));
            assertThat(typeInfo.get("title"), is("STRING"));
            assertThat(typeInfo.get("metadata-content-type"), is("STRING"));
            assertThat(typeInfo.get("metadata-content-type-version"), is("STRING"));
            assertThat(typeInfo.get("metadata-target-namespace"), is("STRING"));
            assertThat(typeInfo.get("resource-uri"), is("STRING"));
            // since resource-size is not indexed, it should be filtered out
            assertNull(typeInfo.get("resource-size"));
            assertThat(typeInfo.get("metadata"), is("XML"));
            assertThat(typeInfo.get("location"), is("GEOMETRY"));
        }
    }
    
    private CatalogFramework createFramework() {
        final long COUNT = 2;

        CatalogFramework framework = mock(CatalogFramework.class);
        List<Result> results = new ArrayList<Result>();

        for(int i = 0; i < COUNT; i++) {
            Result result = mock(Result.class);

            MetacardImpl metacard = new MetacardImpl();
            metacard.setId("Metacard_" + i);
            metacard.setTitle("Metacard " + i);
            metacard.setLocation("POINT(" + i + " " + i + ")");
            metacard.setType(BasicTypes.BASIC_METACARD);
            metacard.setCreatedDate(TIMESTAMP);
            metacard.setEffectiveDate(TIMESTAMP);
            metacard.setExpirationDate(TIMESTAMP);
            metacard.setModifiedDate(TIMESTAMP);
            metacard.setContentTypeName("TEST");
            metacard.setContentTypeVersion("1.0");
            metacard.setTargetNamespace(URI.create(getClass().getPackage().getName()));

            when(result.getDistanceInMeters()).thenReturn(100.0 * i);
            when(result.getRelevanceScore()).thenReturn(100.0 * (COUNT - i) / COUNT);
            when(result.getMetacard()).thenReturn(metacard);

            results.add(result);
        }

        QueryResponse response = new QueryResponseImpl(mock(QueryRequest.class),
                new ArrayList<Result>(), COUNT);
        response.getResults().addAll(results);

        try {
            when(framework.query(any(QueryRequest.class))).thenReturn(response);
        } catch (UnsupportedQueryException e) {
            LOGGER.debug("Error querying framework", e);
        } catch (SourceUnavailableException e) {
            LOGGER.debug("Error querying framework", e);
        } catch (FederationException e) {
            LOGGER.debug("Error querying framework", e);
        }
        return framework;
    }
    
    /**
     * The SearchController spawns off threads to complete tasks. We cannot reliably test multi-threaded code, so
     * we use this Mock ExecutorService to ensure that all operations for a test happen in the same thread as
     * each JUnit test case.
     */
    private class SequentialExectorService implements ExecutorService {

        @Override
        public void execute(Runnable command) {      
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
        public <T> Future<T> submit(Callable<T> task) {
            return null;
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return null;
        }

        @Override
        public Future<?> submit(Runnable task) {
            task.run();
            return null;
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
