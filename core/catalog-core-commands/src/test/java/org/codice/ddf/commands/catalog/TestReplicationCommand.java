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
package org.codice.ddf.commands.catalog;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.felix.service.command.CommandSession;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.commands.catalog.facade.Framework;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

public class TestReplicationCommand {

    private CatalogFramework catalogFramework;

    private QueryResponse mockQueryResponse = mock(QueryResponse.class);

    private CreateResponse mockCreateResponse = mock(CreateResponse.class);

    private CommandSession mockSession = mock(CommandSession.class);

    private InputStream mockIS = IOUtils.toInputStream("sourceId1");

    private static final Set<String> sourceIds = new HashSet<String>(Arrays.asList("sourceId1",
            "sourceId2"));

    private ReplicationCommand replicationCmd;

    private int pageSize;

    private static final int HITS = 1000;

    private static ConsoleOutput consoleOutput;

    @Before
    public void setUp() throws UnsupportedQueryException, SourceUnavailableException,
        FederationException, IngestException {
        
        consoleOutput = new ConsoleOutput();
        consoleOutput.interceptSystemOut();

        catalogFramework = mock(CatalogFramework.class);
        replicationCmd = new ReplicationCommand() {
            @Override
            public String getInput(String message) throws IOException {
                return "sourceId1";
            }

            @Override
            protected CatalogFacade getCatalog() throws InterruptedException {
                return new Framework(catalogFramework);
            }

            @Override
            protected FilterBuilder getFilterBuilder() throws InterruptedException {
                return new GeotoolsFilterBuilder();
            }

            @Override
            protected <T> T getService(Class<T> classObject) {
                T service = null;
                if (classObject.equals(CatalogFramework.class)) {
                    return (T) catalogFramework;
                }
                return service;
            }
        };

        when(mockSession.getKeyboard()).thenReturn(mockIS);

        when(catalogFramework.getSourceIds()).thenReturn(sourceIds);
        when(catalogFramework.query(isA(QueryRequest.class))).thenAnswer(
                new Answer<QueryResponse>() {
                    @Override
                    public QueryResponse answer(InvocationOnMock invocation) throws Throwable {
                        Object[] args = invocation.getArguments();
                        QueryRequest request = (QueryRequest) args[0];
                        pageSize = request.getQuery().getPageSize();
                        return mockQueryResponse;
                    }
                });

        when(mockQueryResponse.getHits()).thenReturn(Long.valueOf(HITS));
        when(mockQueryResponse.getResults()).thenAnswer(new Answer<List<Result>>() {
            @Override
            public List<Result> answer(InvocationOnMock invocation) throws Throwable {
                return getResultList(pageSize);
            }
        });
        when(catalogFramework.create(isA(CreateRequest.class))).thenAnswer(
                new Answer<CreateResponse>() {
                    @Override
                    public CreateResponse answer(InvocationOnMock invocation) throws Throwable {
                        Object[] args = invocation.getArguments();
                        CreateRequest request = (CreateRequest) args[0];
                        when(mockCreateResponse.getCreatedMetacards()).thenReturn(
                                request.getMetacards());
                        return mockCreateResponse;
                    }
                });

    }

    @AfterClass
    public static void cleanUp() throws IOException {
        consoleOutput.resetSystemOut();
        consoleOutput.closeBuffer();
    }

    @Test
    public void testBadBatchSize() throws Exception {
        replicationCmd.isProvider = true;
        replicationCmd.isUseTemporal = false;
        replicationCmd.sourceId = "sourceId1";

        replicationCmd.batchSize = -1;

        replicationCmd.doExecute();

        assertThat(consoleOutput.getOutput(),
                containsString("Batch Size must be between 1 and 1000."));
        consoleOutput.reset();
    }

    @Test
    public void testPrintSourceIds() throws Exception {
        replicationCmd.isProvider = true;
        replicationCmd.isUseTemporal = false;
        replicationCmd.sourceId = "";

        replicationCmd.doExecute();

        assertThat(consoleOutput.getOutput(),
                containsString("Please enter the Source ID you would like to replicate:"));
        assertThat(consoleOutput.getOutput(), containsString("sourceId1"));
        assertThat(consoleOutput.getOutput(), containsString("sourceId2"));
        consoleOutput.reset();
    }

    @Test
    public void testDefaultQuery() throws Exception {
        replicationCmd.isProvider = true;
        replicationCmd.isUseTemporal = false;
        replicationCmd.sourceId = "sourceId1";

        replicationCmd.doExecute();

        ArgumentCaptor<QueryRequest> argument = ArgumentCaptor.forClass(QueryRequest.class);
        verify(catalogFramework, times(HITS / replicationCmd.batchSize + 1)).query(
                argument.capture());
        QueryRequest request = argument.getValue();
        assertThat(request, notNullValue());
        Query query = request.getQuery();
        assertThat(query, notNullValue());
        assertThat(query.getPageSize(), is(ReplicationCommand.MAX_BATCH_SIZE));
        assertThat(query.getStartIndex(), is(1));
    }

    @Test
    public void testSmallBatchSize() throws Exception {
        replicationCmd.isProvider = true;
        replicationCmd.isUseTemporal = false;
        replicationCmd.sourceId = "sourceId1";
        replicationCmd.batchSize = 10;

        replicationCmd.doExecute();

        ArgumentCaptor<QueryRequest> argument = ArgumentCaptor.forClass(QueryRequest.class);
        verify(catalogFramework, times(HITS / replicationCmd.batchSize + 1)).query(
                argument.capture());
        QueryRequest request = argument.getValue();
        assertThat(request, notNullValue());
        Query query = request.getQuery();
        assertThat(query, notNullValue());
        assertThat(query.getPageSize(), is(replicationCmd.batchSize));
        assertThat(query.getStartIndex(), is(991));
    }

    @Test
    public void testMultithreaded() throws Exception {
        replicationCmd.isProvider = true;
        replicationCmd.isUseTemporal = false;
        replicationCmd.sourceId = "sourceId1";
        replicationCmd.batchSize = 10;
        replicationCmd.multithreaded = 4;

        replicationCmd.doExecute();

        ArgumentCaptor<QueryRequest> argument = ArgumentCaptor.forClass(QueryRequest.class);
        verify(catalogFramework, times(HITS / replicationCmd.batchSize + 1)).query(
                argument.capture());
        QueryRequest request = argument.getValue();
        assertThat(request, notNullValue());
        Query query = request.getQuery();
        assertThat(query, notNullValue());
        assertThat(query.getPageSize(), is(replicationCmd.batchSize));
        assertThat(consoleOutput.getOutput(),
                containsString("1000 record(s) replicated; 0 record(s) failed"));
        consoleOutput.reset();
    }

    @Test
    public void testTemporalFlag() throws Exception {
        replicationCmd.isProvider = true;
        replicationCmd.isUseTemporal = true;
        replicationCmd.sourceId = "sourceId1";

        replicationCmd.doExecute();

        ArgumentCaptor<QueryRequest> argument = ArgumentCaptor.forClass(QueryRequest.class);
        verify(catalogFramework, times(HITS / replicationCmd.batchSize + 1)).query(
                argument.capture());
        QueryRequest request = argument.getValue();
        assertThat(request, notNullValue());
        Query query = request.getQuery();
        assertThat(query, notNullValue());
        assertThat(query.getPageSize(), is(replicationCmd.batchSize));
        assertThat(query.getStartIndex(), is(1));
        assertThat(query.getSortBy().getPropertyName().getPropertyName(), is(Metacard.EFFECTIVE));
        // TODO - How do I validate the acutal filter
    }

    @Test
    public void testFailedtoIngestHalf() throws Exception {

        when(catalogFramework.create(isA(CreateRequest.class))).thenAnswer(
                new Answer<CreateResponse>() {
                    @Override
                    public CreateResponse answer(InvocationOnMock invocation) throws Throwable {
                        Object[] args = invocation.getArguments();
                        CreateRequest request = (CreateRequest) args[0];
                        when(mockCreateResponse.getCreatedMetacards()).thenReturn(
                                request.getMetacards()
                                        .subList(0, request.getMetacards().size() / 2));
                        return mockCreateResponse;
                    }
                });

        replicationCmd.isProvider = true;
        replicationCmd.isUseTemporal = false;
        replicationCmd.sourceId = "sourceId1";

        replicationCmd.doExecute();

        assertThat(consoleOutput.getOutput(), containsString("500 record(s) failed"));
        consoleOutput.reset();
    }

    private List<Result> getResultList(int size) {
        List<Result> results = new ArrayList<Result>();
        for (int i = 0; i < size; i++) {
            MetacardImpl metacard = new MetacardImpl();
            metacard.setId(UUID.randomUUID().toString());
            Result result = new ResultImpl(metacard);
            results.add(result);
        }
        return results;
    }
}
