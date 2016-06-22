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

package ddf.catalog.cache.solr.impl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ddf.catalog.data.impl.BasicTypes.VALIDATION_WARNINGS;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.delegate.ValidationQueryDelegate;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.UnsupportedQueryException;

public class ValidationQueryFactoryTest {

    private FilterBuilder filterBuilder;

    private FilterAdapter filterAdapter;

    private ValidationQueryFactory validationQueryFactory;

    private static ValidationQueryDelegate testValidationQueryDelegate;

    @Before
    public void setUp() {
        filterAdapter = new GeotoolsFilterAdapterImpl();
        filterBuilder = new GeotoolsFilterBuilder();

        validationQueryFactory = new ValidationQueryFactory(filterAdapter, filterBuilder);
        testValidationQueryDelegate = new ValidationQueryDelegate();
    }

    @Test
    public void testSearchValid()
            throws StopProcessingException, PluginExecutionException, UnsupportedQueryException {
        QueryImpl query = new QueryImpl(filterBuilder.attribute(VALIDATION_WARNINGS)
                .is()
                .empty());
        ValidationQueryDelegate delegate = new ValidationQueryDelegate();
        assertThat(filterAdapter.adapt(query, delegate), is(true));
        QueryRequest returnQuery = validationQueryFactory.getQueryRequestWithValidationFilter(new QueryRequestImpl(query));
        assertThat(filterAdapter.adapt(returnQuery.getQuery(), delegate), is(true));
    }

    @Test
    public void testSearchInvalid()
            throws StopProcessingException, PluginExecutionException, UnsupportedQueryException {
        QueryImpl query = new QueryImpl(filterBuilder.attribute(VALIDATION_WARNINGS)
                .is()
                .equalTo()
                .text("sample"));
        ValidationQueryDelegate delegate = new ValidationQueryDelegate();
        assertThat(filterAdapter.adapt(query, testValidationQueryDelegate), is(true));
        QueryRequest returnQuery = validationQueryFactory.getQueryRequestWithValidationFilter(new QueryRequestImpl(query));
        assertThat(filterAdapter.adapt(returnQuery.getQuery(), testValidationQueryDelegate),
                is(true));
    }

    @Test
    public void testSearchAnyAndShow()
            throws StopProcessingException, PluginExecutionException, UnsupportedQueryException {
        QueryImpl query = new QueryImpl(filterBuilder.attribute(Metacard.MODIFIED)
                .is()
                .equalTo()
                .text("sample"));
        assertThat(filterAdapter.adapt(query, testValidationQueryDelegate), is(false));
        QueryRequest sendQuery = new QueryRequestImpl(query);
        QueryRequest returnQuery = validationQueryFactory.getQueryRequestWithValidationFilter(sendQuery, true, true);
        assertThat(filterAdapter.adapt(returnQuery.getQuery(), testValidationQueryDelegate),
                is(false));
        assertThat(sendQuery, is(returnQuery));
    }

    @Test
    public void testSearchInvalidAndNotShow()
            throws StopProcessingException, PluginExecutionException, UnsupportedQueryException {
        QueryImpl query = new QueryImpl(filterBuilder.attribute(VALIDATION_WARNINGS)
                .is()
                .equalTo()
                .text("sample"));
        assertThat(filterAdapter.adapt(query, testValidationQueryDelegate), is(true));
        QueryRequest sendQuery = new QueryRequestImpl(query);
        QueryRequest returnQuery = validationQueryFactory.getQueryRequestWithValidationFilter(sendQuery, false, false);
        assertThat(filterAdapter.adapt(returnQuery.getQuery(), testValidationQueryDelegate),
                is(true));
        assertThat(sendQuery, is(returnQuery));
    }

    @Test
    public void testSearchBoth()
            throws StopProcessingException, PluginExecutionException, UnsupportedQueryException {
        QueryImpl query = new QueryImpl(filterBuilder.allOf(filterBuilder.attribute(
                VALIDATION_WARNINGS)
                        .is()
                        .empty(),
                filterBuilder.attribute(VALIDATION_WARNINGS)
                        .is()
                        .equalTo()
                        .text("sample")));
        QueryRequest returnQuery = validationQueryFactory.getQueryRequestWithValidationFilter(new QueryRequestImpl(query));

        assertThat(filterAdapter.adapt(returnQuery.getQuery(), testValidationQueryDelegate),
                is(true));
    }

    @Test
    public void testSearchWarningsAndNotErrors()
            throws StopProcessingException, PluginExecutionException, UnsupportedQueryException {
        QueryImpl query = new QueryImpl(filterBuilder.attribute(Metacard.MODIFIED)
                .is()
                .equalTo()
                .text("sample"));
        assertThat(filterAdapter.adapt(query, testValidationQueryDelegate), is(false));
        QueryRequest returnQuery = validationQueryFactory.getQueryRequestWithValidationFilter(new QueryRequestImpl(query));
        assertThat(filterAdapter.adapt(returnQuery.getQuery(), testValidationQueryDelegate),
                is(true));
    }

    @Test
    public void testSearchBothImplicit()
            throws StopProcessingException, PluginExecutionException, UnsupportedQueryException {
        QueryImpl query = new QueryImpl(filterBuilder.allOf(filterBuilder.attribute(
                VALIDATION_WARNINGS)
                        .is()
                        .empty(),
                filterBuilder.attribute(VALIDATION_WARNINGS)
                        .is()
                        .equalTo()
                        .text("*")));
        QueryRequest returnQuery = validationQueryFactory.getQueryRequestWithValidationFilter(new QueryRequestImpl(query));

        assertThat(filterAdapter.adapt(returnQuery.getQuery(), testValidationQueryDelegate),
                is(true));
    }

    @Test
    public void testSearchErrorsAndNotWarnings() throws UnsupportedQueryException {
        QueryImpl query = new QueryImpl(filterBuilder.attribute(Metacard.MODIFIED)
                .is()
                .equalTo()
                .text("sample"));
        QueryRequest returnQuery = validationQueryFactory.getQueryRequestWithValidationFilter(new QueryRequestImpl(query), true, false);

        assertThat(filterAdapter.adapt(returnQuery.getQuery(), testValidationQueryDelegate),
                is(true));
    }
}