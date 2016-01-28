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

package ddf.catalog.registry.metacard.filter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.UnsupportedQueryException;

public class RegistryMetacardFilterPluginTest {
    private FilterBuilder filterBuilder;

    private FilterAdapter filterAdapter;

    private RegistryMetacardFilterPlugin registryMetacardFilterPlugin;

    private static RegistryQueryDelegate registryQueryDelegate;

    private String customContentType = "customType";

    @Before
    public void setUp() {
        this.filterBuilder = new GeotoolsFilterBuilder();
        this.filterAdapter = new GeotoolsFilterAdapterImpl();
        registryMetacardFilterPlugin = new RegistryMetacardFilterPlugin(filterBuilder,
                filterAdapter);
        registryQueryDelegate = new RegistryQueryDelegate();
    }

    @Test
    public void testQueryAllFiltersOutRegistryMetacards()
            throws StopProcessingException, PluginExecutionException, UnsupportedQueryException {
        QueryImpl query = new QueryImpl(filterBuilder.attribute(Metacard.ANY_TEXT)
                .is()
                .like()
                .text(FilterDelegate.WILDCARD_CHAR));
        //Assert that this is not a query for registry metacards
        assertThat(filterAdapter.adapt(query, registryQueryDelegate), is(false));
        QueryRequest returnQuery =
                registryMetacardFilterPlugin.process(new QueryRequestImpl(query));
        //Assert that this query now contains the registry content type by going through the filter
        assertThat(filterAdapter.adapt(returnQuery.getQuery(), registryQueryDelegate), is(true));
    }

    @Test
    public void testQueryRegistryMetacardsDoesNotModifyQuery()
            throws UnsupportedQueryException, StopProcessingException, PluginExecutionException {
        QueryImpl query = new QueryImpl(filterBuilder.attribute(Metacard.CONTENT_TYPE)
                .is()
                .like()
                .text(RegistryMetacardFilterPlugin.REGISTRY_CONTENT_TYPE
                        + RegistryQueryDelegate.WILDCARD_CHAR));
        //Assert that this is a query for registry metacards
        assertThat(filterAdapter.adapt(query, registryQueryDelegate), is(true));
        QueryRequest returnQuery =
                registryMetacardFilterPlugin.process(new QueryRequestImpl(query));
        //Assert that it is still a query for registry metacards
        assertThat(filterAdapter.adapt(returnQuery.getQuery(), registryQueryDelegate), is(true));
    }

    @Test
    public void testQueryNonRegistryTypeMetacardsFiltersOutRegistryMetacards()
            throws UnsupportedQueryException, StopProcessingException, PluginExecutionException {
        QueryImpl query = new QueryImpl(filterBuilder.attribute(Metacard.CONTENT_TYPE)
                .is()
                .like()
                .text(customContentType));
        //Assert that this is not a query for registry metacards
        assertThat(filterAdapter.adapt(query, registryQueryDelegate), is(false));
        QueryRequest returnQuery =
                registryMetacardFilterPlugin.process(new QueryRequestImpl(query));
        //Assert that this query now contains the registry content type by going through the filter
        assertThat(filterAdapter.adapt(returnQuery.getQuery(), registryQueryDelegate), is(true));
    }

    @Test
    public void testQueryBothRegistryAndNonRegistryMetacards()
            throws UnsupportedQueryException, StopProcessingException, PluginExecutionException {
        Filter customTypeFilter = filterBuilder.attribute(Metacard.CONTENT_TYPE)
                .is()
                .like()
                .text(customContentType);
        Filter registryTypeFilter = filterBuilder.attribute(Metacard.CONTENT_TYPE)
                .is()
                .like()
                .text(RegistryMetacardFilterPlugin.REGISTRY_CONTENT_TYPE
                        + RegistryQueryDelegate.WILDCARD_CHAR);
        Filter combinedFilter = filterBuilder.allOf(customTypeFilter, registryTypeFilter);
        QueryImpl query = new QueryImpl(combinedFilter);
        //Assert that this is a query for registry metacards
        assertThat(filterAdapter.adapt(query, registryQueryDelegate), is(true));
        QueryRequest returnQuery =
                registryMetacardFilterPlugin.process(new QueryRequestImpl(query));
        //Assert that this is still a query for registry metacards
        assertThat(filterAdapter.adapt(returnQuery.getQuery(), registryQueryDelegate), is(true));
    }

}
