/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.util.impl;

import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * Functional interface used to abstract away methods that take in a {@link QueryRequest} and
 * return a {@link SourceResponse}. Used by classes such as {@link ResultIterable} to support
 * interfaces other than {@link ddf.catalog.CatalogFramework}.
 */
@FunctionalInterface
public interface QueryFunction {
    /**
     * Runs a query.
     *
     * @param queryRequest request to use
     * @return source response
     * @throws SourceUnavailableException if a required {@link ddf.catalog.source.Source} is unavailable
     * @throws UnsupportedQueryException  if the {@link ddf.catalog.operation.Query} can not be
     *                                    evaluated by this {@link ddf.catalog.CatalogFramework} or
     *                                    any of its {@link ddf.catalog.source.Source}s.
     * @throws FederationException        if the {@link QueryRequest} includes
     *                                    {@link ddf.catalog.source.FederatedSource}s and there is
     *                                    either a problem connecting to a
     *                                    {@link ddf.catalog.source.FederatedSource} or a
     *                                    {@link ddf.catalog.source.FederatedSource}
     *                                    cannot evaluate the {@link ddf.catalog.operation.Query}
     */
    SourceResponse query(QueryRequest queryRequest)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException;
}
