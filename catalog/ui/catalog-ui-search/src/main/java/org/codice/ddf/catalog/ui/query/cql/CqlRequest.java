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
package org.codice.ddf.catalog.ui.query.cql;

import static spark.Spark.halt;

import java.util.Collections;

import org.apache.commons.lang.StringUtils;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;

public class CqlRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CqlRequest.class);

    private static final String DEFAULT_SORT_ORDER = "desc";

    private static final String LOCAL_SOURCE = "local";

    private static final String CACHE_SOURCE = "cache";

    private String id;

    private String src;

    private Long timeout = 300000L;

    private Long start = 1L;

    private Long count = 10L;

    private String cql;

    private String sort;

    public void setId(String id) {
        this.id = id;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public void setCql(String cql) {
        this.cql = cql;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public QueryRequest getQueryRequest(String localSourceId, FilterBuilder filterBuilder) {

        Query query = new QueryImpl(createFitler(filterBuilder),
                start.intValue(),
                count.intValue(), parseSort(sort),
                true, timeout);

        String source = parseSrc(localSourceId);

        QueryRequest queryRequest;
        if (CACHE_SOURCE.equals(source)) {
            queryRequest = new QueryRequestImpl(query, true);
            queryRequest.getProperties()
                    .put("mode", "cache");
        } else {
            queryRequest = new QueryRequestImpl(query, Collections.singleton(source));
            queryRequest.getProperties()
                    .put("mode", "update");
        }

        return queryRequest;
    }

    private String parseSrc(String localSourceId) {
        if (StringUtils.equalsIgnoreCase(src, LOCAL_SOURCE) || StringUtils.isBlank(src)) {
            src = localSourceId;
        }

        return src;
    }

    private Filter createFitler(FilterBuilder filterBuilder) {
        Filter filter = null;
        try {
            filter = ECQL.toFilter(cql);
        } catch (CQLException e) {
            halt(400, "Unable to parse CQL filter");
        }

        if (filter == null) {
            LOGGER.debug("Received an empty filter. Using a wildcard contextual filter instead.");
            filter = filterBuilder.attribute(Metacard.ANY_TEXT)
                    .is()
                    .like()
                    .text(FilterDelegate.WILDCARD_CHAR);
        }
    }

    private SortBy parseSort(String sortStr) {
        // default values
        String sortField = Result.TEMPORAL;
        String sortOrder = DEFAULT_SORT_ORDER;

        // Updated to use the passed in index if valid (=> 1)
        // and to use the default if no value, or an invalid value (< 1)
        // is specified
        if (!(StringUtils.isEmpty(sortStr))) {
            String[] sortAry = sortStr.split(":");
            if (sortAry.length > 1) {
                sortField = sortAry[0];
                sortOrder = sortAry[1];
            }
        }

        // Query must specify a valid sort order if a sort field was specified, i.e., query
        // cannot specify just "date:", must specify "date:asc"
        SortBy sort;
        if ("asc".equalsIgnoreCase(sortOrder)) {
            sort = new SortByImpl(sortField, SortOrder.ASCENDING);
        } else if ("desc".equalsIgnoreCase(sortOrder)) {
            sort = new SortByImpl(sortField, SortOrder.DESCENDING);
        } else {
            throw new IllegalArgumentException(
                    "Incorrect sort order received, must be 'asc' or 'desc'");
        }

        LOGGER.debug("Retrieved query settings: \n sortField: {} \nsortOrder: {}",
                sortField,
                sortOrder);

        return sort;
    }

    public String getSource() {
        return src;
    }

    public String getId() {
        return id;
    }
}
