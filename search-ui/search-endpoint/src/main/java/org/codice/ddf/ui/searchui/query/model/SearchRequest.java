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

import ddf.catalog.operation.Query;
import org.apache.commons.lang.Validate;

import java.util.Collections;
import java.util.Set;

/**
 * This class holds all of the search requests processed for a particular query.
 */
public class SearchRequest {

    private final String id;

    private final Set<String> sourceIds;

    private final Query query;

    /**
     * Creates a SearchRequest
     *
     * @param sourceIds
     *            - Source IDs to query
     * @param query
     *            - Query requests
     * @param id
     *            - ID for this query
     */
    public SearchRequest(Set<String> sourceIds, Query query, String id) {
        Validate.notEmpty(id, "Valid ID required.");

        this.sourceIds = Collections.unmodifiableSet(sourceIds);
        this.query = query;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Query getQuery() {
        return query;
    }

    public Set<String> getSourceIds() {
        return sourceIds;
    }

    public String toString() {
        return getId();
    }
}
