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

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.QueryRequest;

/**
 * A PreQueryPlugin is used to execute functionality after a query has been executed. For example if
 * a query needed to be altered, a PreQueryPlugin could be implemented to alter the query prior to
 * execution.
 */
public interface PreQueryPlugin {

    /**
     * Processes a {@link QueryRequest} prior to execution of the {@link Query}.
     * 
     * @param input
     *            the {@link QueryRequest} to process
     * @return the value of the processed {@link QueryRequest} to pass to the next
     *         {@link PreQueryPlugin}, or if this is the last {@link PreQueryPlugin} to be called
     * @throws PluginExecutionException
     *             thrown when an error occurs while processing the {@link QueryRequest}
     * @throws StopProcessingException
     *             thrown to halt processing when a critical issue occurs during processing. This is
     *             intended to prevent other plugins from processing as well.
     */
    public QueryRequest process(QueryRequest input) throws PluginExecutionException,
        StopProcessingException;

}
