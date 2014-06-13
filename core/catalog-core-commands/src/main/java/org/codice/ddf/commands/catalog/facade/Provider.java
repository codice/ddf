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
package org.codice.ddf.commands.catalog.facade;

import java.util.Collections;
import java.util.Set;

import ddf.catalog.federation.FederationException;
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
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

public class Provider extends CatalogFacade {

    private CatalogProvider provider;

    public Provider(CatalogProvider provider) {
        this.provider = provider;
    }

    @Override
    public String getVersion() {
        return this.provider.getVersion();
    }

    @Override
    public String getId() {
        return this.provider.getId();
    }

    @Override
    public String getTitle() {
        return this.provider.getTitle();
    }

    @Override
    public String getDescription() {
        return this.provider.getDescription();
    }

    @Override
    public String getOrganization() {
        return this.provider.getOrganization();
    }

    @Override
    public CreateResponse create(CreateRequest createRequest) throws IngestException,
        SourceUnavailableException {
        return this.provider.create(createRequest);
    }

    @Override
    public UpdateResponse update(UpdateRequest updateRequest) throws IngestException,
        SourceUnavailableException {
        return this.provider.update(updateRequest);
    }

    @Override
    public DeleteResponse delete(DeleteRequest deleteRequest) throws IngestException,
        SourceUnavailableException {
        return this.provider.delete(deleteRequest);
    }

    @Override
    public SourceResponse query(QueryRequest query) throws UnsupportedQueryException,
        SourceUnavailableException, FederationException {
        return this.provider.query(query);
    }

    @Override
    public Set<String> getSourceIds() {
        return Collections.emptySet();
    }

}
