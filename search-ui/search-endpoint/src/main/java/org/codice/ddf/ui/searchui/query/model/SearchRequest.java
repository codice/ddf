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
import java.util.UUID;

import javax.ws.rs.core.UriInfo;

import org.codice.ddf.opensearch.query.OpenSearchQuery;

import ddf.security.Subject;

/**
 * Created by tustisos on 12/13/13.
 */
public class SearchRequest {
    private final String guid;
    private final String queryFormat;
    private OpenSearchQuery localQueryRequest;
    private List<OpenSearchQuery> remoteQueryRequests;

    public SearchRequest(OpenSearchQuery localQueryRequest, List<OpenSearchQuery> remoteQueryRequests, String queryFormat, String guid) {
        this.localQueryRequest = localQueryRequest;
        this.remoteQueryRequests = remoteQueryRequests;
        this.queryFormat = queryFormat;
        this.guid = guid;
    }

    public String getGuid() {
        return guid;
    }

    public String getQueryFormat() {
        return queryFormat;
    }

    public OpenSearchQuery getLocalQueryRequest() {
        return localQueryRequest;
    }

    public void setLocalQueryRequest(OpenSearchQuery localQueryRequest) {
        this.localQueryRequest = localQueryRequest;
    }

    public List<OpenSearchQuery> getRemoteQueryRequests() {
        return remoteQueryRequests;
    }

    public void setRemoteQueryRequests(List<OpenSearchQuery> remoteQueryRequests) {
        this.remoteQueryRequests = remoteQueryRequests;
    }
}
