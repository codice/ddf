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

import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.resource.Resource;

/**
 * The PreResourcePlugin executes prior to the getResource operation.  
 *  
 *  @see Resource
 */
public interface PreResourcePlugin {
    
	/**
     * Processes the {@link ResourceRequest}.
     * 
     * @param input - the {@link ResourceRequest} to process
     * @return the value of the processed {@link ResourceRequest} to pass to the next
	 *         {@link PreResourcePlugin}, or if this is the last
	 *         {@link PreResourcePlugin} to be called
	 * @throws PluginExecutionException when an error occurs during processing
     */
    public ResourceRequest process(ResourceRequest input) throws PluginExecutionException, StopProcessingException;

}
