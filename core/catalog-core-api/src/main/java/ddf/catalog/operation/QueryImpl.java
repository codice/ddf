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
package ddf.catalog.operation;

import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.sort.SortBy;

import ddf.catalog.Constants;

/**
 * 
 * @deprecated As of release 2.3.0, replaced by
 *             ddf.catalog.operation.impl.QueryImpl
 * 
 */
@Deprecated
public class QueryImpl implements Query {

    protected Filter filter = null;

    protected int startIndex = 1;

    protected int pageSize = Constants.DEFAULT_PAGE_SIZE;

    protected boolean requestsTotalResultsCount;

    protected long timeoutMillis = 0 /* no timeout */;

    protected SortBy sortBy = null;

    protected Map<String, Object> properties;

    /**
     * Instantiates a new QueryImpl with a {@link Filter} Throws IllegalArgumentException if a null
     * object is passed in.
     * 
     * @param filter
     *            the filter
     */
    public QueryImpl(Filter filter) {

        if (null == filter) {
            throw new IllegalArgumentException("Null filter used in creation of QueryImpl object.");
        }

        this.filter = filter;
    }

    /**
     * Instantiates a new QueryImpl with a {@link Filter}, startIndex, pageSize, sortPolicy,
     * requestsTotalResultsCount, and timeoutMillis
     * 
     * @param rootFilter
     *            the filter
     * @param startIndex
     *            the starting index
     * @param pageSize
     *            the page size
     * @param sortPolicy
     *            the sort policy
     * @param requestsTotalResultsCount
     *            if the Total Results Count is requested or not
     * @param timeoutMillis
     *            the timeout in milliseconds
     */
    public QueryImpl(Filter rootFilter, int startIndex, int pageSize, SortBy sortPolicy,
            boolean requestsTotalResultsCount, long timeoutMillis) {
        this(rootFilter);
        this.startIndex = startIndex;
        this.pageSize = pageSize;
        this.sortBy = sortPolicy;
        this.requestsTotalResultsCount = requestsTotalResultsCount;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public Object accept(FilterVisitor visitor, Object obj) {
        return filter.accept(visitor, obj);
    }

    @Override
    public boolean evaluate(Object obj) {
        return filter.evaluate(obj);
    }

    /**
     * @return filter
     */
    public Filter getFilter() {
        return filter;
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public SortBy getSortBy() {
        return this.sortBy;
    }

    /**
     * 
     * @param sortBy
     */
    public void setSortBy(SortBy sortBy) {
        this.sortBy = sortBy;
    }

    @Override
    public int getStartIndex() {
        return startIndex;
    }

    @Override
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    @Override
    public boolean requestsTotalResultsCount() {
        return requestsTotalResultsCount;
    }

    /**
     * Throws IllegalArgumentException if a null object is passed in.
     * 
     * @param rootFilter
     */
    public void setFilter(Filter rootFilter) {

        if (null == rootFilter) {
            throw new IllegalArgumentException("Null filter used in creation of QueryImpl object.");
        }
        this.filter = rootFilter;
    }

    /**
     * 
     * @param pageSize
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * 
     * @param properties
     */
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    /**
     * 
     * @param requestsTotalResultsCount
     */
    public void setRequestsTotalResultsCount(boolean requestsTotalResultsCount) {
        this.requestsTotalResultsCount = requestsTotalResultsCount;
    }

    /**
     * 
     * @param startIndex
     */
    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    /**
     * 
     * @param timeoutMillis
     */
    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
