/**
 * Copyright (c) Codice Foundation
<<<<<<< HEAD
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
=======
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
>>>>>>> master
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package ddf.catalog.cache.solr.impl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
<<<<<<< HEAD
import static ddf.catalog.data.impl.BasicTypes.VALIDATION_WARNINGS;
=======
>>>>>>> master

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Metacard;
<<<<<<< HEAD
=======
import ddf.catalog.data.types.Validation;
>>>>>>> master
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

<<<<<<< HEAD
=======
    private static ValidationQueryDelegate testValidationQueryDelegate;

>>>>>>> master
    private FilterBuilder filterBuilder;

    private FilterAdapter filterAdapter;

    private ValidationQueryFactory validationQueryFactory;

<<<<<<< HEAD
    private static ValidationQueryDelegate testValidationQueryDelegate;

=======
>>>>>>> master
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
<<<<<<< HEAD
        QueryImpl query = new QueryImpl(filterBuilder.attribute(VALIDATION_WARNINGS)
=======
        QueryImpl query = new QueryImpl(filterBuilder.attribute(Validation.VALIDATION_WARNINGS)
>>>>>>> master
                .is()
                .empty());
        ValidationQueryDelegate delegate = new ValidationQueryDelegate();
        assertThat(filterAdapter.adapt(query, delegate), is(true));
<<<<<<< HEAD
        QueryRequest returnQuery = validationQueryFactory.getQueryRequestWithValidationFilter(new QueryRequestImpl(query));
=======
        QueryRequest returnQuery =
                validationQueryFactory.getQueryRequestWithValidationFilter(new QueryRequestImpl(
                        query));
>>>>>>> master
        assertThat(filterAdapter.adapt(returnQuery.getQuery(), delegate), is(true));
    }

    @Test
    public void testSearchInvalid()
            throws StopProcessingException, PluginExecutionException, UnsupportedQueryException {
<<<<<<< HEAD
        QueryImpl query = new QueryImpl(filterBuilder.attribute(VALIDATION_WARNINGS)
=======
        QueryImpl query = new QueryImpl(filterBuilder.attribute(Validation.VALIDATION_WARNINGS)
>>>>>>> master
                .is()
                .equalTo()
                .text("sample"));
        ValidationQueryDelegate delegate = new ValidationQueryDelegate();
        assertThat(filterAdapter.adapt(query, testValidationQueryDelegate), is(true));
<<<<<<< HEAD
        QueryRequest returnQuery = validationQueryFactory.getQueryRequestWithValidationFilter(new QueryRequestImpl(query));
=======
        QueryRequest returnQuery =
                validationQueryFactory.getQueryRequestWithValidationFilter(new QueryRequestImpl(
                        query));
>>>>>>> master
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
<<<<<<< HEAD
        QueryRequest returnQuery = validationQueryFactory.getQueryRequestWithValidationFilter(sendQuery, true, true);
=======
        QueryRequest returnQuery = validationQueryFactory.getQueryRequestWithValidationFilter(
                sendQuery,
                true,
                true);
>>>>>>> master
        assertThat(filterAdapter.adapt(returnQuery.getQuery(), testValidationQueryDelegate),
                is(false));
        assertThat(sendQuery, is(returnQuery));
    }

    @Test
    public void testSearchInvalidAndNotShow()
            throws StopProcessingException, PluginExecutionException, UnsupportedQueryException {
<<<<<<< HEAD
        QueryImpl query = new QueryImpl(filterBuilder.attribute(VALIDATION_WARNINGS)
=======
        QueryImpl query = new QueryImpl(filterBuilder.attribute(Validation.VALIDATION_WARNINGS)
>>>>>>> master
                .is()
                .equalTo()
                .text("sample"));
        assertThat(filterAdapter.adapt(query, testValidationQueryDelegate), is(true));
        QueryRequest sendQuery = new QueryRequestImpl(query);
<<<<<<< HEAD
        QueryRequest returnQuery = validationQueryFactory.getQueryRequestWithValidationFilter(sendQuery, false, false);
=======
        QueryRequest returnQuery = validationQueryFactory.getQueryRequestWithValidationFilter(
                sendQuery,
                false,
                false);
>>>>>>> master
        assertThat(filterAdapter.adapt(returnQuery.getQuery(), testValidationQueryDelegate),
                is(true));
        assertThat(sendQuery, is(returnQuery));
    }

    @Test
    public void testSearchBoth()
            throws StopProcessingException, PluginExecutionException, UnsupportedQueryException {
<<<<<<< HEAD
        QueryImpl query = new QueryImpl(filterBuilder.allOf(filterBuilder.attribute(
                VALIDATION_WARNINGS)
                        .is()
                        .empty(),
                filterBuilder.attribute(VALIDATION_WARNINGS)
                        .is()
                        .equalTo()
                        .text("sample")));
        QueryRequest returnQuery = validationQueryFactory.getQueryRequestWithValidationFilter(new QueryRequestImpl(query));
=======
        QueryImpl query =
                new QueryImpl(filterBuilder.allOf(filterBuilder.attribute(Validation.VALIDATION_WARNINGS)
                                .is()
                                .empty(),
                        filterBuilder.attribute(Validation.VALIDATION_WARNINGS)
                                .is()
                                .equalTo()
                                .text("sample")));
        QueryRequest returnQuery =
                validationQueryFactory.getQueryRequestWithValidationFilter(new QueryRequestImpl(
                        query));
>>>>>>> master

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
<<<<<<< HEAD
        QueryRequest returnQuery = validationQueryFactory.getQueryRequestWithValidationFilter(new QueryRequestImpl(query));
=======
        QueryRequest returnQuery =
                validationQueryFactory.getQueryRequestWithValidationFilter(new QueryRequestImpl(
                        query));
>>>>>>> master
        assertThat(filterAdapter.adapt(returnQuery.getQuery(), testValidationQueryDelegate),
                is(true));
    }

    @Test
    public void testSearchBothImplicit()
            throws StopProcessingException, PluginExecutionException, UnsupportedQueryException {
<<<<<<< HEAD
        QueryImpl query = new QueryImpl(filterBuilder.allOf(filterBuilder.attribute(
                VALIDATION_WARNINGS)
                        .is()
                        .empty(),
                filterBuilder.attribute(VALIDATION_WARNINGS)
                        .is()
                        .equalTo()
                        .text("*")));
        QueryRequest returnQuery = validationQueryFactory.getQueryRequestWithValidationFilter(new QueryRequestImpl(query));
=======
        QueryImpl query =
                new QueryImpl(filterBuilder.allOf(filterBuilder.attribute(Validation.VALIDATION_WARNINGS)
                                .is()
                                .empty(),
                        filterBuilder.attribute(Validation.VALIDATION_WARNINGS)
                                .is()
                                .equalTo()
                                .text("*")));
        QueryRequest returnQuery =
                validationQueryFactory.getQueryRequestWithValidationFilter(new QueryRequestImpl(
                        query));
>>>>>>> master

        assertThat(filterAdapter.adapt(returnQuery.getQuery(), testValidationQueryDelegate),
                is(true));
    }

    @Test
    public void testSearchErrorsAndNotWarnings() throws UnsupportedQueryException {
        QueryImpl query = new QueryImpl(filterBuilder.attribute(Metacard.MODIFIED)
                .is()
                .equalTo()
                .text("sample"));
<<<<<<< HEAD
        QueryRequest returnQuery = validationQueryFactory.getQueryRequestWithValidationFilter(new QueryRequestImpl(query), true, false);
=======
        QueryRequest returnQuery =
                validationQueryFactory.getQueryRequestWithValidationFilter(new QueryRequestImpl(
                        query), true, false);
>>>>>>> master

        assertThat(filterAdapter.adapt(returnQuery.getQuery(), testValidationQueryDelegate),
                is(true));
    }
}