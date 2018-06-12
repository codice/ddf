/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.commands.catalog.facade;

import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.Describable;
import java.util.Set;

/**
 * A Catalog facade which unifies at least the {@link ddf.catalog.CatalogFramework} and the {@link
 * ddf.catalog.source.CatalogProvider} interfaces.
 *
 * @author Ashraf Barakat
 */
public abstract class CatalogFacade implements Describable {

  public abstract CreateResponse create(CreateRequest createRequest)
      throws IngestException, SourceUnavailableException;

  public abstract UpdateResponse update(UpdateRequest updateRequest)
      throws IngestException, SourceUnavailableException;

  public abstract DeleteResponse delete(DeleteRequest deleteRequest)
      throws IngestException, SourceUnavailableException;

  public abstract SourceResponse query(QueryRequest query)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException;

  public abstract Set<String> getSourceIds();
}
