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
package ddf.catalog.federation;

import java.util.List;

import ddf.catalog.data.Result;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.Source;

/**
 * Implementations of this interface federate the specified query to all the
 * {@link Source}s in the list, process the {@link Result}s in a unique way, and then return the
 * results to the client.
 * <p>
 * For example, implementations can choose to block until all
 * {@link SourceResponse}s return then do a mass sort, or to return the results
 * back to the client as soon as they are received back from a
 * {@link FederatedSource}.
 * 
 * @author ddf.isgs@lmco.com
 */
public interface FederationStrategy {

    /**
     * Federate the given query to the {@link List} of {@link Source}s,
     * returning a {@link QueryResponse} back to the user that will include the
     * matching {@link Result}s.
     * 
     * @param sources the {@link List} of {@link Source}s to be queried. Cannot be
     *            {@code null} or empty.
     * @param query the {@link QueryRequest} to execute. Cannot be {@code null}.
     * @return {@link QueryResponse} which contains the list of {@link Result}s.
     */
    public QueryResponse federate(List<Source> sources, QueryRequest query) throws FederationException;

}
