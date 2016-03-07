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
package org.codice.ddf.catalog.migratable.impl;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.codice.ddf.migration.ExportMigrationException;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationMetadata;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengis.filter.Filter;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

@RunWith(MockitoJUnitRunner.class)
public class CatalogMigratableImplTest {

    private static final String DESCRIPTION = "description";

    private static final Path EXPORT_PATH = Paths.get("etc", "exported");

    private static final String EXPORT_DIR_NAME = "org.codice.ddf.catalog";

    private CatalogMigratableConfig config;

    @Mock
    private CatalogFramework framework;

    @Captor
    private ArgumentCaptor<QueryRequest> queryRequest;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FilterBuilder filterBuilder;

    @Mock
    private MigrationFileWriter fileWriter;

    @Mock
    private MigrationTaskManager taskManager;

    private List<Result> results;

    private class CatalogMigratableImplUnderTest extends CatalogMigratableImpl {
        public CatalogMigratableImplUnderTest(String description, CatalogFramework framework,
                FilterBuilder filterBuilder, MigrationFileWriter fileWriter,
                CatalogMigratableConfig config) {
            super(description, framework, filterBuilder, fileWriter, config);
        }

        @Override
        MigrationTaskManager createTaskManager(CatalogMigratableConfig config,
                ExecutorService executorService) {
            return taskManager;
        }
    }

    @Before
    public void setup() throws Exception {
        config = new CatalogMigratableConfig();
        config.setExportQueryPageSize(2);

        when(filterBuilder.attribute(Metacard.ANY_TEXT)
                .is()
                .like()
                .text("*")).thenReturn(mock(Filter.class));

        results = Collections.singletonList(mock(Result.class));

        QueryResponse response = mock(QueryResponse.class);
        when(framework.query(any())).thenReturn(response);
        when(response.getResults()).thenReturn(results);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullDescription() {
        new CatalogMigratableImpl(null, framework, filterBuilder, fileWriter, config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullProvider() {
        new CatalogMigratableImpl(DESCRIPTION, null, filterBuilder, fileWriter, config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullFilterBuilder() {
        new CatalogMigratableImpl(DESCRIPTION, framework, null, fileWriter, config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullFileWriter() {
        new CatalogMigratableImpl(DESCRIPTION, framework, filterBuilder, null, config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullConfig() {
        new CatalogMigratableImpl(DESCRIPTION, framework, filterBuilder, fileWriter, null);
    }

    @Test
    public void exportSetsSubDirectory() {
        CatalogMigratableImpl migratable = new CatalogMigratableImplUnderTest(DESCRIPTION,
                framework,
                filterBuilder,
                fileWriter,
                config);

        migratable.export(EXPORT_PATH);

        assertThat(config.getExportPath(), is(EXPORT_PATH.resolve(EXPORT_DIR_NAME)));
    }

    @Test
    public void exportUsesRightQuerySettings() throws Exception {
        CatalogMigratableImpl migratable = new CatalogMigratableImplUnderTest(DESCRIPTION,
                framework,
                filterBuilder,
                fileWriter,
                config);

        migratable.export(EXPORT_PATH);

        verify(framework).query(queryRequest.capture());
        assertThat(queryRequest.getValue()
                .getQuery()
                .getPageSize(), is(config.getExportQueryPageSize()));
        assertThat(queryRequest.getValue()
                .getQuery()
                .requestsTotalResultsCount(), is(false));
    }

    @Test
    public void exportFileWriterCreatesExportDirectory() {
        CatalogMigratableImpl migratable = new CatalogMigratableImplUnderTest(DESCRIPTION,
                framework,
                filterBuilder,
                fileWriter,
                config);

        migratable.export(EXPORT_PATH);
        verify(fileWriter).createExportDirectory(any(Path.class));
    }

    @Test(expected = ExportMigrationException.class)
    public void exportWhenResponseIsNull() throws Exception {
        when(framework.query(any())).thenReturn(null);

        CatalogMigratableImpl migratable = new CatalogMigratableImplUnderTest(DESCRIPTION,
                framework,
                filterBuilder,
                fileWriter,
                config);
        migratable.export(EXPORT_PATH);
    }

    @Test(expected = ExportMigrationException.class)
    public void exportWhenResultSetIsNull() throws Exception {
        when(framework.query(any())
                .getResults()).thenReturn(null);

        CatalogMigratableImpl migratable = new CatalogMigratableImplUnderTest(DESCRIPTION,
                framework,
                filterBuilder,
                fileWriter,
                config);

        migratable.export(EXPORT_PATH);
    }

    @Test
    public void exportWhenResultSetIsEmpty() throws Exception {
        when(framework.query(any())
                .getResults()).thenReturn(new ArrayList<>());

        CatalogMigratableImpl migratable = new CatalogMigratableImplUnderTest(DESCRIPTION,
                framework,
                filterBuilder,
                fileWriter,
                config);
        migratable.export(EXPORT_PATH);

        verify(taskManager).exportFinish();
        verifyNoMoreInteractions(taskManager);
    }

    @Test
    public void exportWhenResultSetEqualToPageSize() throws Exception {
        List<Result> result = Arrays.asList(mock(Result.class), mock(Result.class));
        List<Integer> startIndices = setupProviderResponses(result, Collections.emptyList());

        CatalogMigratableImpl migratable = new CatalogMigratableImplUnderTest(DESCRIPTION,
                framework,
                filterBuilder,
                fileWriter,
                config);

        migratable.export(EXPORT_PATH);

        verify(taskManager).exportMetacardQuery(result, 1);
        verify(framework, times(2)).query(queryRequest.capture());
        assertThat("Invalid start index for first query", startIndices.get(0), is(1));
        assertThat("Invalid page size",
                queryRequest.getAllValues()
                        .get(0)
                        .getQuery()
                        .getPageSize(),
                is(config.getExportQueryPageSize()));
        assertThat("Invalid start index for second query",
                startIndices.get(1),
                is(config.getExportQueryPageSize() + 1));
        verify(taskManager).exportFinish();
        verifyNoMoreInteractions(taskManager);
    }

    @Test
    public void exportWhenResultSetLargerThanExportPageSize() throws Exception {
        List<Result> result1 = Arrays.asList(mock(Result.class), mock(Result.class));
        List<Result> result2 = Collections.singletonList(mock(Result.class));
        List<Integer> startIndices = setupProviderResponses(result1, result2);

        CatalogMigratableImpl migratable = new CatalogMigratableImplUnderTest(DESCRIPTION,
                framework,
                filterBuilder,
                fileWriter,
                config);

        MigrationMetadata metadata = migratable.export(EXPORT_PATH);

        verify(taskManager).exportMetacardQuery(result1, 1);
        assertThat("Invalid start index for first query", startIndices.get(0), is(1));
        verify(taskManager).exportMetacardQuery(result2, 2);
        assertThat("Invalid start index for second query",
                startIndices.get(1),
                is(config.getExportQueryPageSize() + 1));
        verify(taskManager).exportFinish();

        assertThat(metadata.getMigrationWarnings(), is(empty()));
    }

    @Test(expected = RuntimeException.class)
    public void exportStopsWhenExportMetacardFails() throws Exception {
        List<Result> result1 = Arrays.asList(mock(Result.class), mock(Result.class));
        List<Result> result2 = Collections.singletonList(mock(Result.class));
        setupProviderResponses(result1, result2);

        doThrow(new RuntimeException()).when(taskManager)
                .exportMetacardQuery(result1, 1);

        CatalogMigratableImpl migratable = new CatalogMigratableImplUnderTest(DESCRIPTION,
                framework,
                filterBuilder,
                fileWriter,
                config);

        try {
            migratable.export(EXPORT_PATH);
        } finally {
            verify(taskManager).exportMetacardQuery(result1, 1);
            verify(taskManager).exportFinish();
            verifyNoMoreInteractions(taskManager);
        }
    }

    @Test(expected = RuntimeException.class)
    public void exportWhenProviderQueryFails() throws Exception {
        when(framework.query(any())).thenThrow(new UnsupportedQueryException(""));

        CatalogMigratableImpl migratable = new CatalogMigratableImpl(DESCRIPTION,
                framework,
                filterBuilder,
                fileWriter,
                config);

        migratable.export(EXPORT_PATH);
    }

    @Test(expected = RuntimeException.class)
    public void exportWhenExportMetacardFailsWithMigrationException() throws Exception {
        doThrow(new MigrationException("")).when(taskManager)
                .exportMetacardQuery(results, 1);

        CatalogMigratableImpl migratable = new CatalogMigratableImplUnderTest(DESCRIPTION,
                framework,
                filterBuilder,
                fileWriter,
                config);

        try {
            migratable.export(EXPORT_PATH);
        } finally {
            verify(taskManager).exportMetacardQuery(results, 1);
            verify(taskManager).exportFinish();
            verifyNoMoreInteractions(taskManager);
        }
    }

    @Test(expected = MigrationException.class)
    public void exportWhenTaskManagerFinishFailsWithMigrationException() {
        doThrow(new MigrationException("")).when(taskManager)
                .exportFinish();

        CatalogMigratableImpl migratable = new CatalogMigratableImplUnderTest(DESCRIPTION,
                framework,
                filterBuilder,
                fileWriter,
                config);

        migratable.export(EXPORT_PATH);
    }

    // Returns the list of start indices used when framework.query() was called.
    private List<Integer> setupProviderResponses(final List<Result> result1, List<Result> result2)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {
        QueryResponse response = mock(QueryResponse.class);
        when(response.getResults()).thenReturn(result1)
                .thenReturn(result2);

        List<Integer> responses = new ArrayList<>();
        when(framework.query(any())).thenAnswer((invocation) -> {
            QueryRequest request = (QueryRequest) invocation.getArguments()[0];
            responses.add(request.getQuery()
                    .getStartIndex());
            return response;
        });

        return responses;
    }
}
