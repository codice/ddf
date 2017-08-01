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

import org.codice.ddf.migration.ExportMigrationException;
import org.codice.ddf.migration.MigrationException;
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
public class MetacardsMigratableTest {

    private static final Path EXPORT_PATH = Paths.get("etc", "exported");

    private static final String DESCRIPTION = "description";

    private static final String VERSION = "version";

    private static final String ID = "id";

    private static final String TITLE = "title";

    private static final String ORGANIZATION = "organization";

    private static final DescribableBean DESCRIBABLE_BEAN = new DescribableBean(VERSION,
            ID,
            TITLE,
            DESCRIPTION,
            ORGANIZATION);

    @Captor
    private ArgumentCaptor<QueryRequest> argQueryRequest;

    @Mock
    private CatalogFramework mockFramework;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FilterBuilder mockFilterBuilder;

    @Mock
    private MigrationFileWriter mockFileWriter;

    @Mock
    private MigrationTaskManager mockTaskManager;

    private MetacardsMigratable migratable;

    private CatalogMigratableConfig config;

    private List<Result> results;

    @Before
    public void setup() throws Exception {
        config = new CatalogMigratableConfig();
        config.setExportQueryPageSize(2);

        migratable = new MetacardsMigratable(DESCRIBABLE_BEAN,
                mockFramework,
                mockFilterBuilder,
                mockFileWriter,
                config,
                mockTaskManager);

        when(mockFilterBuilder.attribute(Metacard.ANY_TEXT)
                .is()
                .like()
                .text("*")).thenReturn(mock(Filter.class));

        results = Collections.singletonList(mock(Result.class));

        QueryResponse response = mock(QueryResponse.class);
        when(mockFramework.query(any())).thenReturn(response);
        when(response.getResults()).thenReturn(results);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullDescribable() {
        new MetacardsMigratable(null,
                mockFramework,
                mockFilterBuilder,
                mockFileWriter,
                config,
                mockTaskManager);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullFramework() {
        new MetacardsMigratable(DESCRIBABLE_BEAN,
                null,
                mockFilterBuilder,
                mockFileWriter,
                config,
                mockTaskManager);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullFilterBuilder() {
        new MetacardsMigratable(DESCRIBABLE_BEAN,
                mockFramework,
                null,
                mockFileWriter,
                config,
                mockTaskManager);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullFileWriter() {
        new MetacardsMigratable(DESCRIBABLE_BEAN,
                mockFramework,
                mockFilterBuilder,
                null,
                config,
                mockTaskManager);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullConfig() {
        new MetacardsMigratable(DESCRIBABLE_BEAN,
                mockFramework,
                mockFilterBuilder,
                mockFileWriter,
                null,
                mockTaskManager);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullTaskManager() {
        new MetacardsMigratable(DESCRIBABLE_BEAN,
                mockFramework,
                mockFilterBuilder,
                mockFileWriter,
                config,
                null);
    }

    @Test
    public void exportUsesRightQuerySettings() throws Exception {
        migratable.export(EXPORT_PATH);
        verify(mockFramework).query(argQueryRequest.capture());
        assertThat(argQueryRequest.getValue()
                .getQuery()
                .getPageSize(), is(config.getExportQueryPageSize()));
        assertThat(argQueryRequest.getValue()
                .getQuery()
                .requestsTotalResultsCount(), is(false));
    }

    @Test(expected = ExportMigrationException.class)
    public void exportWhenResponseIsNull() throws Exception {
        when(mockFramework.query(any())).thenReturn(null);
        migratable.export(EXPORT_PATH);
    }

    @Test(expected = ExportMigrationException.class)
    public void exportWhenResultSetIsNull() throws Exception {
        when(mockFramework.query(any())
                .getResults()).thenReturn(null);
        migratable.export(EXPORT_PATH);
    }

    @Test
    public void exportWhenResultSetIsEmpty() throws Exception {
        when(mockFramework.query(any())
                .getResults()).thenReturn(new ArrayList<>());
        migratable.export(EXPORT_PATH);
        verify(mockTaskManager).close();
        verifyNoMoreInteractions(mockTaskManager);
    }

    @Test
    public void exportWhenResultSetEqualToPageSize() throws Exception {
        List<Result> result = Arrays.asList(mock(Result.class), mock(Result.class));
        List<Integer> startIndices = setupProviderResponses(result, Collections.emptyList());

        migratable.export(EXPORT_PATH);

        verify(mockTaskManager).exportMetacardQuery(result, 1);
        verify(mockFramework, times(2)).query(argQueryRequest.capture());
        assertThat("Invalid start index for first query", startIndices.get(0), is(1));
        assertThat("Invalid page size",
                argQueryRequest.getAllValues()
                        .get(0)
                        .getQuery()
                        .getPageSize(),
                is(config.getExportQueryPageSize()));
        assertThat("Invalid start index for second query",
                startIndices.get(1),
                is(config.getExportQueryPageSize() + 1));
        verify(mockTaskManager).close();
        verifyNoMoreInteractions(mockTaskManager);
    }

    @Test
    public void exportWhenResultSetLargerThanExportPageSize() throws Exception {
        List<Result> result1 = Arrays.asList(mock(Result.class), mock(Result.class));
        List<Result> result2 = Collections.singletonList(mock(Result.class));
        List<Integer> startIndices = setupProviderResponses(result1, result2);

        MigrationMetadata metadata = migratable.export(EXPORT_PATH);

        verify(mockTaskManager).exportMetacardQuery(result1, 1);
        assertThat("Invalid start index for first query", startIndices.get(0), is(1));
        verify(mockTaskManager).exportMetacardQuery(result2, 2);
        assertThat("Invalid start index for second query",
                startIndices.get(1),
                is(config.getExportQueryPageSize() + 1));
        verify(mockTaskManager).close();

        assertThat(metadata.getMigrationWarnings(), is(empty()));
    }

    @Test(expected = RuntimeException.class)
    public void exportStopsWhenExportMetacardFails() throws Exception {
        List<Result> result1 = Arrays.asList(mock(Result.class), mock(Result.class));
        List<Result> result2 = Collections.singletonList(mock(Result.class));
        setupProviderResponses(result1, result2);

        doThrow(new RuntimeException()).when(mockTaskManager)
                .exportMetacardQuery(result1, 1);

        try {
            migratable.export(EXPORT_PATH);
        } finally {
            verify(mockTaskManager).exportMetacardQuery(result1, 1);
            verify(mockTaskManager).close();
            verifyNoMoreInteractions(mockTaskManager);
        }
    }

    @Test(expected = RuntimeException.class)
    public void exportWhenProviderQueryFails() throws Exception {
        when(mockFramework.query(any())).thenThrow(new UnsupportedQueryException(""));

        MetacardsMigratable migratable = new MetacardsMigratable(DESCRIBABLE_BEAN,
                mockFramework,
                mockFilterBuilder,
                mockFileWriter,
                config,
                mockTaskManager);

        migratable.export(EXPORT_PATH);
    }

    @Test(expected = RuntimeException.class)
    public void exportWhenExportMetacardFailsWithMigrationException() throws Exception {
        doThrow(new MigrationException("")).when(mockTaskManager)
                .exportMetacardQuery(results, 1);

        try {
            migratable.export(EXPORT_PATH);
        } finally {
            verify(mockTaskManager).exportMetacardQuery(results, 1);
            verify(mockTaskManager).close();
            verifyNoMoreInteractions(mockTaskManager);
        }
    }

    @Test(expected = MigrationException.class)
    public void exportWhenTaskManagerFinishFailsWithMigrationException() throws Exception {
        doThrow(new MigrationException("")).when(mockTaskManager)
                .close();

        migratable.export(EXPORT_PATH);
    }

    // Returns the list of start indices used when framework.query() was called.
    private List<Integer> setupProviderResponses(final List<Result> result1, List<Result> result2)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {
        QueryResponse response = mock(QueryResponse.class);
        when(response.getResults()).thenReturn(result1)
                .thenReturn(result2);

        List<Integer> responses = new ArrayList<>();
        when(mockFramework.query(any())).thenAnswer((invocation) -> {
            QueryRequest request = (QueryRequest) invocation.getArguments()[0];
            responses.add(request.getQuery()
                    .getStartIndex());
            return response;
        });

        return responses;
    }
}
