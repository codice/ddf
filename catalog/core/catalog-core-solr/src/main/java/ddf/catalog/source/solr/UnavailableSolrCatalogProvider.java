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
package ddf.catalog.source.solr;

import java.util.Set;

import ddf.catalog.data.ContentType;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * This class is used to signify an unavailable CatalogProvider instance. If a user tries to
 * unsuccessfully connect to Solr, then a message will be displayed to check the
 * connection.
 */
public class UnavailableSolrCatalogProvider implements CatalogProvider {

    private static final String CLIENT_DISCONNECTED_MESSAGE =
            "Solr client is not connected. Please verify Solr is available and retry.";

    @Override
    public Set<ContentType> getContentTypes() {
        throw new IllegalArgumentException(CLIENT_DISCONNECTED_MESSAGE);
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public boolean isAvailable(SourceMonitor callback) {
        return false;
    }

    @Override
    public SourceResponse query(QueryRequest queryRequest) throws UnsupportedQueryException {
        throw new IllegalArgumentException(CLIENT_DISCONNECTED_MESSAGE);
    }

    @Override
    public String getDescription() {
        throw new IllegalArgumentException(CLIENT_DISCONNECTED_MESSAGE);
    }

    @Override
    public String getId() {
        throw new IllegalArgumentException(CLIENT_DISCONNECTED_MESSAGE);
    }

    @Override
    public String getOrganization() {
        throw new IllegalArgumentException(CLIENT_DISCONNECTED_MESSAGE);
    }

    @Override
    public String getTitle() {
        throw new IllegalArgumentException(CLIENT_DISCONNECTED_MESSAGE);
    }

    @Override
    public String getVersion() {
        throw new IllegalArgumentException(CLIENT_DISCONNECTED_MESSAGE);
    }

    @Override
    public void maskId(String sourceId) {
        // no op
    }

    @Override
    public CreateResponse create(CreateRequest createRequest) throws IngestException {
        throw new IllegalArgumentException(CLIENT_DISCONNECTED_MESSAGE);
    }

    @Override
    public DeleteResponse delete(DeleteRequest deleteRequest) throws IngestException {
        throw new IllegalArgumentException(CLIENT_DISCONNECTED_MESSAGE);
    }

    @Override
    public UpdateResponse update(UpdateRequest updateRequest) throws IngestException {
        throw new IllegalArgumentException(CLIENT_DISCONNECTED_MESSAGE);
    }

}
