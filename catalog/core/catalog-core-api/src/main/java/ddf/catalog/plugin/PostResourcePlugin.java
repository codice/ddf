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

import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;

/**
 * The PostResourcePlugin is executed after a getResource operation has completed.
 * 
 * @see Resource
 */
public interface PostResourcePlugin {
    /**
     * Processes a {@link ResourceResponse} after the execution of a getResource operation.
     * 
     * @param input
     *            the {@link ResourceResponse} to process
     * @return the value of the processed {@link ResourceResponse} to pass to the next
     *         {@link PostResourcePlugin}, or if this is the last {@link PostResourcePlugin} to be
     *         called
     * @throws PluginExecutionException
     *             thrown when an error occurs while processing the {@link ResourceResponse}
     * @throws StopProcessingException
     *             thrown to halt processing when a critical issue occurs during processing. This is
     *             intended to prevent other plugins from processing as well.
     */
    public ResourceResponse process(ResourceResponse input) throws PluginExecutionException,
        StopProcessingException;

}
