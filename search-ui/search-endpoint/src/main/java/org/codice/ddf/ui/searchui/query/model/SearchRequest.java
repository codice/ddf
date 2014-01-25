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
package org.codice.ddf.ui.searchui.query.model;

import java.util.List;

import org.codice.ddf.opensearch.query.OpenSearchQuery;

/**
 * This class holds all of the search requests processed for a particular query. The GUID represents
 * the unique id for this query and must not be null or empty string.
 */
public class SearchRequest {

    private final String guid;

    private final String queryFormat;

    private List<OpenSearchQuery> queryRequests;

    /**
     * Creates a SearchRequest
     * 
     * @param queryRequests
     *            - List of query requests
     * @param queryFormat
     *            - Format to return results in
     * @param guid
     *            - Unique id for this query
     */
    public SearchRequest(List<OpenSearchQuery> queryRequests, String queryFormat, String guid) {
        this.queryRequests = queryRequests;
        this.queryFormat = queryFormat;
        this.guid = guid;
    }

    public String getGuid() {
        return guid;
    }

    public String getQueryFormat() {
        return queryFormat;
    }

    public List<OpenSearchQuery> getQueryRequests() {
        return queryRequests;
    }

    public void setQueryRequests(List<OpenSearchQuery> queryRequests) {
        this.queryRequests = queryRequests;
    }

    public String toString() {
        return getGuid();
    }
}
