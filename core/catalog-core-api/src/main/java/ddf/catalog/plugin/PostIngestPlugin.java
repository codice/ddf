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

import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.UpdateResponse;

/**
 * Marker interface for services that wish to be called after any
 * {@link CreateResponse}, {@link UpdateResponse}, or {@link DeleteResponse} is
 * about to be returned to an endpoint.
 * 
 * 
 * @author michael.menousek@lmco.com
 */
public interface PostIngestPlugin {
    
    /**
     * Processes the {@link CreateResponse}.
     * @param input the {@link CreateResponse} to process
     * @return the value of the processed {@link CreateResponse} to pass to the next
	 *         {@link PostIngestPlugin}, or if this is the last
	 *         {@link PostIngestPlugin} to be called
	 * @throws PluginExecutionException thrown when an error occurs during processing
     */
    public CreateResponse process(CreateResponse input) throws PluginExecutionException;
    
    /**
     * Processes the {@link UpdateResponse}.
     * @param input the {@link UpdateResponse} to process
     * @return the value of the processed {@link UpdateResponse} to pass to the next
	 *         {@link PostIngestPlugin}, or if this is the last
	 *         {@link PostIngestPlugin} to be called
	 * @throws PluginExecutionException thrown when an error occurs during processing
     */
    public UpdateResponse process(UpdateResponse input) throws PluginExecutionException;
    
    /**
     * Processes the {@link DeleteResponse}.
     * @param input the {@link DeleteResponse} to process
     * @return the value of the processed {@link DeleteResponse} to pass to the next
	 *         {@link PostIngestPlugin}, or if this is the last
	 *         {@link PostIngestPlugin} to be called
	 * @throws PluginExecutionException thrown when an error occurs during processing
     */
    public DeleteResponse process(DeleteResponse input) throws PluginExecutionException;

}
