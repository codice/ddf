/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.content;

import ddf.content.operation.CreateRequest;
import ddf.content.operation.CreateResponse;
import ddf.content.operation.DeleteRequest;
import ddf.content.operation.DeleteResponse;
import ddf.content.operation.ReadRequest;
import ddf.content.operation.ReadResponse;
import ddf.content.operation.Request;
import ddf.content.operation.UpdateRequest;
import ddf.content.operation.UpdateResponse;

/**
 * The {@link ContentFramework} functions as the routing mechanism between all content components.
 * It decouples clients from service implementations and provides integration points for Content
 * Plugins.
 *
 * General, high-level flow:
 * <ul>
 * <li>An endpoint will invoke the active {@link ContentFramework}, typically via an OSGi
 * dependency injection framework such as Blueprint</li>
 * <li>For the {@link #read(ReadRequest) read}, {@link #create(CreateRequest, Request.Directive) create},
 * {@link #delete(DeleteRequest, Request.Directive) delete}, {@link #update(UpdateRequest, Request.Directive) update} methods, the
 * {@link ContentFramework} calls:
 * <ul>
 * <li>The active {@link ddf.content.storage.StorageProvider}</li>
 * <li>All "Post" Content Plugins {@link ddf.content.plugin.ContentPlugin}</li>
 * <li>The appropriate {@link ddf.content.operation.Response} is returned to the calling endpoint.</li>
 * </ul>
 * </li>
 * </ul>
 *
 */
public interface ContentFramework {
    /**
     * Creates the {@link ddf.content.data.ContentItem} in the {@link ddf.content.storage.StorageProvider}.
     *
     * <b>Implementations of this method must:</b>
     * <ol>
     * <li>Call {@link ddf.content.storage.StorageProvider#create(CreateRequest)} on the registered
     * {@link ddf.content.storage.StorageProvider}</li>
     * <li>Call {@link ddf.content.plugin.ContentPlugin#process(CreateResponse)} for each registered
     * {@link ddf.content.plugin.ContentPlugin} in order determined by the OSGi SERVICE_RANKING (Descending, highest
     * first), "daisy chaining" their responses to each other.</li>
     * </ol>
     *
     * @param createRequest the {@link CreateRequest} containing the {@link ddf.content.data.ContentItem} to be stored
     * @param directive     whether to process, or store-and-process the incoming request
     * @return the {@link CreateResponse} containing the {@link ddf.content.data.ContentItem} that was created,
     * including its auto-assigned GUID
     * @throws ContentFrameworkException if an problems encountered during the creation/storing of the {@link ddf.content.data.ContentItem}
     */
    public CreateResponse create(CreateRequest createRequest, Request.Directive directive)
            throws ContentFrameworkException;

    /**
     * Reads a {@link ddf.content.data.ContentItem} from the {@link ddf.content.storage.StorageProvider}. The {@link ddf.content.data.ContentItem} must
     * exist in the {@link ddf.content.storage.StorageProvider} for it to be successfully retrieved.
     *
     * Implementations of this method must call {@link ddf.content.storage.StorageProvider#read(ReadRequest)} on the
     * registered {@link ddf.content.storage.StorageProvider}
     *
     * @param readRequest the {@link ReadRequest} containing the GUID of the {@link ddf.content.data.ContentItem} to retrieve
     * @return the {@link ReadResponse} containing the retrieved {@link ddf.content.data.ContentItem}
     * @throws ContentFrameworkException if problems encountered while retrieving the {@link ddf.content.data.ContentItem}
     */
    public ReadResponse read(ReadRequest readRequest) throws ContentFrameworkException;

    /**
     * Updates a {@link ddf.content.data.ContentItem} in the {@link ddf.content.storage.StorageProvider}. The {@link ddf.content.data.ContentItem} must
     * exist in the {@link ddf.content.storage.StorageProvider} for it to be successfully updated. The
     * {@link ddf.content.data.ContentItem} will not be created if it does not already exist.
     *
     * <b>Implementations of this method must:</b>
     * <ol>
     * <li>Call {@link ddf.content.storage.StorageProvider#update(UpdateRequest)} on the registered
     * {@link ddf.content.storage.StorageProvider}</li>
     * <li>Call {@link ddf.content.plugin.ContentPlugin#process(UpdateResponse)} for each registered
     * {@link ddf.content.plugin.ContentPlugin} in order determined by the OSGi SERVICE_RANKING (Descending, highest
     * first), "daisy chaining" their responses to each other.</li>
     * </ol>
     *
     * @param updateRequest the {@link UpdateRequest} containing the {@link ddf.content.data.ContentItem} to be updated
     * @param directive     whether to process, or store-and-process the incoming request
     * @return the {@link UpdateResponse} containing the updated {@link ddf.content.data.ContentItem}
     * @throws ContentFrameworkException if problems encountered while updating the {@link ddf.content.data.ContentItem}
     */
    public UpdateResponse update(UpdateRequest updateRequest, Request.Directive directive)
            throws ContentFrameworkException;

    /**
     * Deletes a {@link ddf.content.data.ContentItem} from the {@link ddf.content.storage.StorageProvider}. The {@link ddf.content.data.ContentItem} must
     * exist in the {@link ddf.content.storage.StorageProvider} for it to be successfully deleted.
     *
     * <b>Implementations of this method must:</b>
     * <ol>
     * <li>Call {@link ddf.content.storage.StorageProvider#delete(DeleteRequest)} on the registered
     * {@link ddf.content.storage.StorageProvider}</li>
     * <li>Call {@link ddf.content.plugin.ContentPlugin#process(DeleteResponse)} for each registered
     * {@link ddf.content.plugin.ContentPlugin} in order determined by the OSGi SERVICE_RANKING (Descending, highest
     * first), "daisy chaining" their responses to each other.</li>
     * </ol>
     *
     * @param deleteRequest the {@link DeleteRequest} containing the GUID of {@link ddf.content.data.ContentItem} to be deleted
     * @param directive     whether to process, or store-and-process the incoming request
     * @return the {@link DeleteResponse} containing the status of the {@link ddf.content.data.ContentItem} deletion
     * @throws ContentFrameworkException if problems encountered while deleting the {@link ddf.content.data.ContentItem}
     */
    public DeleteResponse delete(DeleteRequest deleteRequest, Request.Directive directive)
            throws ContentFrameworkException;
}
