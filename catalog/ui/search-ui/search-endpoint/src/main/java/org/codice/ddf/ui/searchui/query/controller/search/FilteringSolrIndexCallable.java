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
package org.codice.ddf.ui.searchui.query.controller.search;

import java.util.concurrent.Callable;

import org.codice.ddf.ui.searchui.query.model.SearchRequest;
import org.codice.ddf.ui.searchui.query.solr.FilteringSolrIndex;

import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.impl.QueryRequestImpl;

public class FilteringSolrIndexCallable implements Callable<FilteringSolrIndex> {
    private final SearchRequest request;

    private final FilterAdapter filterAdapter;

    public FilteringSolrIndexCallable(SearchRequest request, FilterAdapter filterAdapter) {
        this.request = request;
        this.filterAdapter = filterAdapter;
    }

    @Override
    public FilteringSolrIndex call() throws Exception {
        return new FilteringSolrIndex(filterAdapter, new QueryRequestImpl(request.getQuery()));
    }
}
