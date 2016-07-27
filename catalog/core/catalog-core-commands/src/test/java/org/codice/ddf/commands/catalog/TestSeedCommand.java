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
package org.codice.ddf.commands.catalog;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.geotools.filter.text.cql2.CQL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;

public class TestSeedCommand extends TestAbstractCommand {
    private SeedCommand seedCommand;

    private ConsoleOutput consoleOutput;

    private CatalogFramework framework;

    @Before
    public void setUp() {
        consoleOutput = new ConsoleOutput();
        consoleOutput.interceptSystemOut();

        seedCommand = new SeedCommand();

        framework = mock(CatalogFramework.class);
        seedCommand.framework = framework;

        seedCommand.filterBuilder = new GeotoolsFilterBuilder();
    }

    @After
    public void tearDown() throws IOException {
        consoleOutput.resetSystemOut();
        consoleOutput.closeBuffer();
    }

    @Test
    public void testBadProductLimit() throws Exception {
        seedCommand.productLimit = 0;
        seedCommand.executeWithSubject();
        assertThat(consoleOutput.getOutput(), containsString("The limit must be greater than 0."));
    }

    @Test
    public void testEnterprise() throws Exception {
        mockQueryResponse(1, new String[0], new boolean[0]);
        runCommandAndVerifyQueryRequest(request -> assertThat(request.isEnterprise(), is(true)));
    }

    @Test
    public void testSources() throws Exception {
        final String source1 = "source1";
        final String source2 = "source2";
        seedCommand.sources = newArrayList(source1, source2);

        mockQueryResponse(1, new String[0], new boolean[0]);

        runCommandAndVerifyQueryRequest(request -> {
            assertThat(request.isEnterprise(), is(false));
            assertThat(request.getSourceIds(), is(newHashSet(source1, source2)));
        });
    }

    @Test
    public void testCql() throws Exception {
        final String cql = "modified AFTER 2016-07-21T00:00:00Z";
        seedCommand.cql = cql;

        mockQueryResponse(1, new String[0], new boolean[0]);

        runCommandAndVerifyQueryRequest(request -> {
            Query query = request.getQuery();
            assertThat(CQL.toCQL(query), is(cql));
        });
    }

    @Test
    public void testNoCql() throws Exception {
        mockQueryResponse(1, new String[0], new boolean[0]);

        runCommandAndVerifyQueryRequest(request -> {
            Query query = request.getQuery();
            assertThat(CQL.toCQL(query), is("anyText ILIKE '*'"));
        });
    }

    @Test
    public void testCacheMetacards() throws Exception {
        mockQueryResponse(1, new String[0], new boolean[0]);

        runCommandAndVerifyQueryRequest(request -> assertThat(request.getProperties(),
                hasEntry("mode", "update")));
    }

    private void runCommandAndVerifyQueryRequest(Consumer<QueryRequest> queryRequestAssertions)
            throws Exception {
        seedCommand.executeWithSubject();

        ArgumentCaptor<QueryRequest> queryCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        verify(framework).query(queryCaptor.capture());

        QueryRequest request = queryCaptor.getValue();
        queryRequestAssertions.accept(request);
    }

    @Test
    public void testProductLimit() throws Exception {
        final int limit = 2;
        seedCommand.productLimit = limit;

        final String id1 = "1";
        final String id2 = "2";
        mockQueryResponse(limit * 2 + 1, new String[] {id1, id2}, new boolean[] {false, false});

        runCommandAndVerifyResourceRequests(limit, resourceRequests -> {
            assertThat(resourceRequests, hasSize(limit));
            verifyResourceRequest(resourceRequests.get(0), id1);
            verifyResourceRequest(resourceRequests.get(1), id2);
        }, siteNames -> assertThat(siteNames, is(newArrayList(id1, id2))));

        verify(framework, times(1)).query(any(QueryRequest.class));
    }

    @Test
    public void testFewerProductsThanLimit() throws Exception {
        final int limit = 10;
        seedCommand.productLimit = limit;

        final String id1 = "1";
        final String id2 = "2";
        final String id3 = "3";
        mockQueryResponse(limit + 1,
                new String[] {id1, id2, id3},
                new boolean[] {false, false, false});

        final int expectedResourceRequests = 3;
        runCommandAndVerifyResourceRequests(expectedResourceRequests, resourceRequests -> {
            assertThat(resourceRequests, hasSize(expectedResourceRequests));
            verifyResourceRequest(resourceRequests.get(0), id1);
            verifyResourceRequest(resourceRequests.get(1), id2);
            verifyResourceRequest(resourceRequests.get(2), id3);
        }, siteNames -> assertThat(siteNames, is(newArrayList(id1, id2, id3))));

        verify(framework, times(2)).query(any(QueryRequest.class));
    }

    @Test
    public void testDoesNotDownloadCachedProduct() throws Exception {
        final int limit = 3;
        seedCommand.productLimit = limit;

        final String id1 = "1";
        final String id2 = "2";
        mockQueryResponse(limit * 2 + 1,
                new String[] {id1, id2, "3"},
                new boolean[] {false, false, true});
        final int expectedResourceRequests = 3;

        runCommandAndVerifyResourceRequests(expectedResourceRequests, resourceRequests -> {
            assertThat(resourceRequests, hasSize(expectedResourceRequests));
            verifyResourceRequest(resourceRequests.get(0), id1);
            verifyResourceRequest(resourceRequests.get(1), id2);
            verifyResourceRequest(resourceRequests.get(2), id1);
        }, siteNames -> assertThat(siteNames, is(newArrayList(id1, id2, id1))));

        verify(framework, times(2)).query(any(QueryRequest.class));
    }

    private void runCommandAndVerifyResourceRequests(int expectedResourceRequests,
            Consumer<List<ResourceRequest>> requestAssertions,
            Consumer<List<String>> siteNameAssertions) throws Exception {
        seedCommand.executeWithSubject();

        ArgumentCaptor<ResourceRequest> resourceRequestCaptor = ArgumentCaptor.forClass(
                ResourceRequest.class);
        ArgumentCaptor<String> siteNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(framework,
                times(expectedResourceRequests)).getResource(resourceRequestCaptor.capture(),
                siteNameCaptor.capture());

        List<ResourceRequest> resourceRequests = resourceRequestCaptor.getAllValues();
        requestAssertions.accept(resourceRequests);

        List<String> siteNames = siteNameCaptor.getAllValues();
        siteNameAssertions.accept(siteNames);
    }

    private void mockQueryResponse(int stopReturningResultsAtIndex, String[] ids, boolean[] cached)
            throws Exception {
        List<Result> results = new ArrayList<>();
        for (int i = 0; i < ids.length; ++i) {
            String id = ids[i];
            Result mockResult = mock(Result.class);
            MetacardImpl metacard = new MetacardImpl();
            metacard.setId(id);
            metacard.setSourceId(id);
            metacard.setAttribute("internal.local-resource", cached[i]);
            when(mockResult.getMetacard()).thenReturn(metacard);
            results.add(mockResult);
        }

        QueryResponse response = mock(QueryResponse.class);
        when(response.getResults()).thenReturn(results);
        doReturn(response).when(framework)
                .query(argThat(isQueryWithStartIndex(request -> request.getQuery()
                        .getStartIndex() < stopReturningResultsAtIndex)));

        QueryResponse noResults = mock(QueryResponse.class);
        when(noResults.getResults()).thenReturn(Collections.emptyList());
        doReturn(noResults).when(framework)
                .query(argThat(isQueryWithStartIndex(request -> request.getQuery()
                        .getStartIndex() >= stopReturningResultsAtIndex)));
    }

    private IsQueryWithStartIndex isQueryWithStartIndex(Predicate<QueryRequest> test) {
        return new IsQueryWithStartIndex(test);
    }

    private class IsQueryWithStartIndex extends ArgumentMatcher<QueryRequest> {
        private final Predicate<QueryRequest> test;

        private IsQueryWithStartIndex(Predicate<QueryRequest> test) {
            this.test = test;
        }

        @Override
        public boolean matches(Object o) {
            return test.test((QueryRequest) o);
        }
    }

    private void verifyResourceRequest(ResourceRequest request, String expectedAttributeValue) {
        assertThat(request.getAttributeName(), is(Metacard.ID));
        assertThat(request.getAttributeValue(), is(expectedAttributeValue));
    }
}
