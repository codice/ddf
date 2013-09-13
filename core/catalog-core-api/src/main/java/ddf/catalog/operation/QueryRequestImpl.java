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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;

public class QueryRequestImpl extends OperationImpl implements QueryRequest {

    protected Query query;

    protected Set<String> sourceIds;

    protected boolean isEnterprise = false;

    /**
     * Instantiates a new QueryRequestImpl with a $(@link Query) query,
     * 
     * @param query
     *            the query
     */
    public QueryRequestImpl(Query query) {
        this(query, false, null, null);
    }

    /**
     * Instantiates a new QueryRequestImpl with a $(@link Query) and a ${@link Map} of properties
     * 
     * @param query
     *            the query
     * @param properties
     *            a Map of properties
     */
    public QueryRequestImpl(Query query, Map<String, Serializable> properties) {
        this(query, false, null, properties);
    }

    /**
     * Instantiates a new QueryRequestImpl with a $(@link Query) and a ${@link Collection} of
     * sourceIDs
     * 
     * @param query
     *            the query
     * @param sourceIds
     *            a Collection of sourceIDs
     */
    public QueryRequestImpl(Query query, Collection<String> sourceIds) {
        this(query, false, sourceIds, null);
    }

    /**
     * Instantiates a new QueryRequestImpl with a $(@link Query) and a boolean indicating if it is
     * an Enterprise QueryRequest.
     * 
     * @param query
     *            the query
     * @param isEnterprise
     *            the enterprise indicator
     */
    public QueryRequestImpl(Query query, boolean isEnterprise) {
        this(query, isEnterprise, null, null);
    }

    /**
     * Instantiates a new QueryRequestImpl with a $(@link Query), a boolean indicating if it is an
     * Enterprise QueryRequest, a ${@link Collection} of siteNames, and a ${@link Map} of properties
     * 
     * @param query
     *            the query
     * @param isEnterprise
     *            the enterprise indicator
     * @param siteNames
     *            a Collection of siteNames
     * @param properties
     *            the properties
     */
    public QueryRequestImpl(Query query, boolean isEnterprise, Collection<String> siteNames,
            Map<String, Serializable> properties) {
        super(properties);
        this.query = query;
        this.isEnterprise = isEnterprise;
        if (siteNames != null) {
            if (siteNames instanceof Set) {
                this.sourceIds = (Set<String>) siteNames;
            } else {
                this.sourceIds = new HashSet<String>();
                this.sourceIds.addAll(siteNames);
            }
        }
    }

    @Override
    public Query getQuery() {
        return query;
    }

    @Override
    public Set<String> getSourceIds() {
        return sourceIds;
    }

    @Override
    public boolean isEnterprise() {
        return isEnterprise;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
