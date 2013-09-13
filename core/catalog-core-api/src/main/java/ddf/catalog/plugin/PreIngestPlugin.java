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
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.source.CatalogProvider;

/**
 * Executes business logic prior to an ingest operation executing. For example if {@link Metacard}s
 * need to be validated prior to {@link CatalogProvider#create(CreateRequest)} a
 * {@link PreIngestPlugin} can do the validation.
 * 
 * @see Metacard
 */
public interface PreIngestPlugin {

    /**
     * Process a {@link CreateRequest} prior to {@link CatalogProvider#create(CreateRequest)}.
     * 
     * @param input
     *            the {@link CreateRequest} to process
     * @return the value of the processed {@link CreateRequest} to pass to the next
     *         {@link PreIngestPlugin}, or to the {@link CatalogProvider} if this is the last
     *         {@link PreIngestPlugin} to be called
     * @throws PluginExecutionException
     *             if an error in processing occurs
     */
    public CreateRequest process(CreateRequest input) throws PluginExecutionException,
        StopProcessingException;

    /**
     * Process a {@link UpdateRequest} prior to {@link CatalogProvider#update(UpdateRequest)}.
     * 
     * @param input
     *            the {@link UpdateRequest} to process
     * @return the value of the processed {@link UpdateRequest} to pass to the next
     *         {@link PreIngestPlugin}, or to the {@link CatalogProvider} if this is the last
     *         {@link PreIngestPlugin} to be called
     * @throws PluginExecutionException
     *             if an error in processing occurs
     */
    public UpdateRequest process(UpdateRequest input) throws PluginExecutionException,
        StopProcessingException;

    /**
     * Processes the {@link DeleteRequest} prior to the execution of the update operation.
     * 
     * @param input
     *            the {@link DeleteRequest} to process
     * @return the value of the processed {@link DeleteRequest} to pass to the next
     *         {@link PreIngestPlugin}, or if this is the last {@link PreIngestPlugin} to be called
     * @throws PluginExecutionException
     *             thrown when an error in processing occurs
     */
    public DeleteRequest process(DeleteRequest input) throws PluginExecutionException,
        StopProcessingException;

}
