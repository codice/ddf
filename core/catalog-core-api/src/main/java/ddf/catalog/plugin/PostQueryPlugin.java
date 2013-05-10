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
import ddf.catalog.operation.QueryResponse;

/**
 * A PostQueryPlugin is used to execute functionality after a query has been
 * executed. For example if query results needed to be filtered, a
 * PostQueryPlugin could be implemented and executed after a query to do
 * filtering.
 */
public interface PostQueryPlugin {
	/**
     * Processes a {@link QueryResponse} after the execution of the {@link Query}.
     * 
     * @param input the {@link QueryResponse} to process
     * @return the value of the processed {@link QueryResponse} to pass to the next
	 *         {@link PostQueryPlugin}, or if this is the last
	 *         {@link PostQueryPlugin} to be called
	 * @throws PluginExecutionException thrown when an error occurs while processing the {@link QueryResponse}
	 * @throws StopProcessingException thrown to halt processing when a critical issue occurs during processing.
	 * This is intended to prevent other plugins from processing as well. 
	 */
    public QueryResponse process(QueryResponse input) throws PluginExecutionException, StopProcessingException;

}
