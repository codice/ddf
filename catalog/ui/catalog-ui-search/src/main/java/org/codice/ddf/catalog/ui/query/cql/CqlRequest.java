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

    private long timeout = 300000L;

    private int start = 1;

    private int count = 10;

    private String cql;

    private String sort;

    private boolean normalize = false;

    public void setId(String id) {
        this.id = id;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setCql(String cql) {
        this.cql = cql;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getSrc() {
        return src;
    }

    public long getTimeout() {
        return timeout;
    }

    public int getStart() {
        return start;
    }

    public int getCount() {
        return count;
    }

    public String getCql() {
        return cql;
    }

    public String getSort() {
        return sort;
    }

    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }

    public boolean isNormalize() {
        return normalize;
    }

    public QueryRequest createQueryRequest(String localSource, FilterBuilder filterBuilder) {
        Query query = new QueryImpl(createFilter(filterBuilder),
                start,
                count,
                parseSort(sort),
                true,
                timeout);

        String source = parseSrc(localSource);

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

    private String parseSrc(String localSource) {
        if (StringUtils.equalsIgnoreCase(src, LOCAL_SOURCE) || StringUtils.isBlank(src)) {
            src = localSource;
        }

        return src;
    }

    private Filter createFilter(FilterBuilder filterBuilder) {
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

        return filter;
    }

    private SortBy parseSort(String sortStr) {
        String sortField = Result.TEMPORAL;
        String sortOrder = DEFAULT_SORT_ORDER;

        if (StringUtils.isNotBlank(sortStr)) {
            String[] sortAry = StringUtils.split(sortStr, ":", 2);
            if (sortAry.length == 2) {
                sortField = sortAry[0];
                sortOrder = sortAry[1];
            }
        }

        SortBy sort;
        switch (sortOrder) {
        case "asc":
            sort = new SortByImpl(sortField, SortOrder.ASCENDING);
            break;
        case "desc":
            sort = new SortByImpl(sortField, SortOrder.DESCENDING);
            break;
        default:
            throw new IllegalArgumentException(
                    "Incorrect sort order received, must be 'asc' or 'desc'");
        }

        return sort;
    }

    public String getSource() {
        return src;
    }

    public String getId() {
        return id;
    }
}
