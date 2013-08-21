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
package ddf.catalog.plugin;

import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.source.Source;

/**
 * A PreFederatedQueryPlugin is used to execute functionality before a federated query has been executed.
 * For example if a query needed to be altered, a PreFederatedQueryPlugin could be implemented
 * to alter the query prior to execution.
 * 
 * @author rodgersh
 */
public interface PreFederatedQueryPlugin {
	
	/**
     * Processes a {@link QueryRequest} prior to execution of the Federated {@link Query}.
     * 
     * @param source the {@link Source} the query will be sent to
     * @param input the {@link QueryRequest} to process
     * @return the value of the processed {@link QueryRequest} to pass to the next
	 *         {@link PreFederatedQueryPlugin}, or if this is the last
	 *         {@link PreFederatedQueryPlugin} to be called     
	 * @throws PluginExecutionException thrown when an error occurs while processing the {@link QueryRequest}
	 * @throws StopProcessingException thrown to halt processing when a critical issue occurs during processing.
	 * This is intended to prevent other plugins from processing as well. 
	 */
    public QueryRequest process(Source source, QueryRequest input) throws PluginExecutionException, StopProcessingException;

}
