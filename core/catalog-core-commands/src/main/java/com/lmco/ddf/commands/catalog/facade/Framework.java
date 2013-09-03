/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package com.lmco.ddf.commands.catalog.facade;


import ddf.catalog.CatalogFramework;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

public class Framework extends CatalogFacade {

	private CatalogFramework framework;

	public Framework(CatalogFramework framework) {
		this.framework = framework;
	}

	@Override
	public String getVersion() {
		return this.framework.getVersion();
	}

	@Override
	public String getId() {
		return this.framework.getId();
	}

	@Override
	public String getTitle() {
		return this.framework.getTitle();
	}

	@Override
	public String getDescription() {
		return this.framework.getDescription();
	}

	@Override
	public String getOrganization() {
		return this.framework.getOrganization();
	}

	@Override
	public CreateResponse create(CreateRequest createRequest) throws IngestException, SourceUnavailableException {
		return this.framework.create(createRequest);
	}

	@Override
	public UpdateResponse update(UpdateRequest updateRequest) throws IngestException, SourceUnavailableException {
		return this.framework.update(updateRequest);
	}

	@Override
	public DeleteResponse delete(DeleteRequest deleteRequest) throws IngestException, SourceUnavailableException {
		return this.framework.delete(deleteRequest);
	}

	@Override
	public QueryResponse query(QueryRequest query) throws UnsupportedQueryException, SourceUnavailableException,
			FederationException {
		return this.framework.query(query);
	}

}
