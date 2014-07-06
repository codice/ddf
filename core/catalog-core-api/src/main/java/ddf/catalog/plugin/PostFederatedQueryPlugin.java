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
package ddf.catalog.plugin;

import ddf.catalog.CatalogFramework;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryResponse;

/**
 * A PostFederatedQueryPlugin is used to execute functionality after a federated query has been
 * executed. For example if metrics need to be recorded on the query, a PostFederatedQueryPlugin
 * could be implemented to do this prior to the query response being returned to the
 * {@link CatalogFramework}.
 * 
 * @author rodgersh
 */
public interface PostFederatedQueryPlugin {

    /**
     * Processes a {@link QueryResponse} after the execution of the Federated {@link Query}.
     * 
     * @param input
     *            the Federated {@link QueryResponse} to process
     * @return the value of the processed {@link QueryResponse} to pass to the next
     *         {@link PostFederatedQueryPlugin}, or if this is the last
     *         {@link PostFederatedQueryPlugin} to be called
     * @throws PluginExecutionException
     *             thrown when an error occurs while processing the {@link QueryResponse}
     * @throws StopProcessingException
     *             thrown to halt processing when a critical issue occurs during processing. This is
     *             intended to prevent other plugins from processing as well.
     */
    public QueryResponse process(QueryResponse input) throws PluginExecutionException,
        StopProcessingException;

}
