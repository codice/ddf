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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.felix.service.command.CommandSession;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.commands.catalog.facade.Framework;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.data.types.Core;
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

public class ReplicateCommandTest {

    private static final Set<String> SOURCE_IDS = new HashSet<>(Arrays.asList("sourceId1",
            "sourceId2"));

    private static final int HITS = 1000;

    private static ConsoleOutput consoleOutput;

    private CatalogFramework catalogFramework;

    private CreateResponse mockCreateResponse = mock(CreateResponse.class);

    private CommandSession mockSession = mock(CommandSession.class);

    private InputStream mockIS = IOUtils.toInputStream("sourceId1");

    private ReplicateCommand replicationCmd;

    @AfterClass
    public static void cleanUp() throws IOException {
        consoleOutput.resetSystemOut();
        consoleOutput.closeBuffer();
    }

    @Before
    public void setUp()
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            IngestException {

        consoleOutput = new ConsoleOutput();
        consoleOutput.interceptSystemOut();

        catalogFramework = mock(CatalogFramework.class);
        replicationCmd = new ReplicateCommand() {
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

            @Override
            protected Object doExecute() throws Exception {
                return executeWithSubject();
            }
        };
        replicationCmd.isProvider = true;

        when(mockSession.getKeyboard()).thenReturn(mockIS);

        when(catalogFramework.getSourceIds()).thenReturn(SOURCE_IDS);
        when(catalogFramework.query(isA(QueryRequest.class))).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            QueryRequest request = (QueryRequest) args[0];
            QueryResponse mockQueryResponse = mock(QueryResponse.class);
            when(mockQueryResponse.getHits()).thenReturn(Long.valueOf(HITS));
            when(mockQueryResponse.getResults()).thenReturn(getResultList(Math.min(replicationCmd.batchSize,
                    HITS - request.getQuery()
                            .getStartIndex() + 1)));

            return mockQueryResponse;
        });

        when(catalogFramework.create(isA(CreateRequest.class))).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            CreateRequest request = (CreateRequest) args[0];
            when(mockCreateResponse.getCreatedMetacards()).thenReturn(request.getMetacards());
            return mockCreateResponse;
        });
    }

    @Test
    public void testBadBatchSize() throws Exception {
        replicationCmd.isUseTemporal = false;
        replicationCmd.sourceId = "sourceId1";
        replicationCmd.temporalProperty = Metacard.EFFECTIVE;
        replicationCmd.batchSize = -1;

        replicationCmd.doExecute();
        verifyConsoleOutput("Batch Size must be between 1 and 1000.");
    }

    @Test
    public void testInvalidTemporalProperty() {
        replicationCmd.temporalProperty = "invalidTemporalProperty";

        assertThat(replicationCmd.getTemporalProperty(), is(Core.CREATED));
    }

    @Test
    public void testPrintSourceIds() throws Exception {
        replicationCmd.isUseTemporal = false;
        replicationCmd.sourceId = "";
        replicationCmd.temporalProperty = Metacard.EFFECTIVE;

        replicationCmd.doExecute();
        verifyConsoleOutput("Please enter the Source ID you would like to replicate:",
                "sourceId1",
                "sourceId2");
    }

    @Test
    public void testDefaultQuery() throws Exception {
        replicationCmd.isUseTemporal = false;
        replicationCmd.sourceId = "sourceId1";
        replicationCmd.temporalProperty = Metacard.EFFECTIVE;

        replicationCmd.doExecute();
        verifyReplicate(HITS, Metacard.EFFECTIVE);
        verifyConsoleOutput(HITS + " record(s) replicated; " + 0 + " record(s) failed;");
    }

    @Test
    public void testSmallBatchSize() throws Exception {
        replicationCmd.isUseTemporal = false;
        replicationCmd.sourceId = "sourceId1";
        replicationCmd.batchSize = 10;
        replicationCmd.temporalProperty = Metacard.EFFECTIVE;

        replicationCmd.doExecute();
        verifyReplicate(HITS, Metacard.EFFECTIVE);
        verifyConsoleOutput(HITS + " record(s) replicated; " + 0 + " record(s) failed;");
    }

    @Test
    public void testMaxMetacard() throws Exception {
        replicationCmd.isUseTemporal = false;
        replicationCmd.sourceId = "sourceId1";
        replicationCmd.batchSize = 10;
        replicationCmd.maxMetacards = 20;
        replicationCmd.temporalProperty = Metacard.EFFECTIVE;

        replicationCmd.doExecute();
        verifyReplicate(Math.min(HITS, replicationCmd.maxMetacards), Metacard.EFFECTIVE);
        verifyConsoleOutput(
                Math.min(HITS, replicationCmd.maxMetacards) + " record(s) replicated; " + 0
                        + " record(s) failed;");
    }

    @Test
    public void testMultithreaded() throws Exception {
        replicationCmd.isUseTemporal = false;
        replicationCmd.sourceId = "sourceId1";
        replicationCmd.batchSize = 10;
        replicationCmd.multithreaded = 4;
        replicationCmd.temporalProperty = Metacard.EFFECTIVE;

        replicationCmd.doExecute();

        verifyReplicate(HITS, Metacard.EFFECTIVE);
        verifyConsoleOutput(HITS + " record(s) replicated; " + 0 + " record(s) failed;");
    }

    @Test
    public void testTemporalFlag() throws Exception {
        replicationCmd.isUseTemporal = true;
        replicationCmd.sourceId = "sourceId1";
        replicationCmd.temporalProperty = Core.CREATED;
        replicationCmd.lastMinutes = 30;

        replicationCmd.doExecute();
        verifyReplicate(HITS, Metacard.EFFECTIVE);
        verifyConsoleOutput(HITS + " record(s) replicated; " + 0 + " record(s) failed;");
        // TODO - How do I validate the actual filter
    }

    @Test
    public void testFailedtoIngestHalf() throws Exception {
        when(catalogFramework.create(isA(CreateRequest.class))).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            CreateRequest request = (CreateRequest) args[0];
            when(mockCreateResponse.getCreatedMetacards()).thenReturn(request.getMetacards()
                    .subList(0,
                            request.getMetacards()
                                    .size() / 2));
            return mockCreateResponse;
        });

        replicationCmd.isUseTemporal = false;
        replicationCmd.sourceId = "sourceId1";
        replicationCmd.temporalProperty = Metacard.EFFECTIVE;

        replicationCmd.doExecute();
        verifyReplicate(HITS, Metacard.EFFECTIVE);
        verifyConsoleOutput(
                (int) Math.floor(HITS / 2) + " record(s) replicated; " + (int) (HITS - Math.floor(
                        HITS / 2)) + " record(s) failed;");
    }

    private List<Result> getResultList(int size) {
        return Stream.generate(() -> {
            MetacardImpl metacard = new MetacardImpl();
            metacard.setId(UUID.randomUUID()
                    .toString());
            return new ResultImpl(metacard);
        })
                .limit(size)
                .collect(Collectors.toList());
    }

    private void verifyReplicate(int actualMaxMetacards, String sortBy) throws Exception {
        ArgumentCaptor<QueryRequest> argument = ArgumentCaptor.forClass(QueryRequest.class);
        verify(catalogFramework,
                times((int) Math.ceil(
                        (double) actualMaxMetacards / (double) replicationCmd.batchSize))).query(
                argument.capture());
        QueryRequest request = argument.getValue();
        assertThat(request, notNullValue());
        Query query = request.getQuery();
        assertThat(query, notNullValue());
        assertThat(query.getPageSize(), is(replicationCmd.batchSize));
        if (replicationCmd.multithreaded == 1) {
            assertThat(query.getStartIndex(),
                    is((((int) ((double) (actualMaxMetacards - 1) / (double) replicationCmd.batchSize))
                            * replicationCmd.batchSize + 1)));
        }
        assertThat(query.getSortBy()
                .getPropertyName()
                .getPropertyName(), is(sortBy));
    }

    private void verifyConsoleOutput(String... message) {
        Arrays.stream(message)
                .forEach(s -> assertThat(consoleOutput.getOutput(), containsString(s)));
        consoleOutput.reset();
    }
}
