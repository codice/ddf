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
package ddf.content;

import ddf.content.data.ContentItem;
import ddf.content.operation.CreateRequest;
import ddf.content.operation.CreateResponse;
import ddf.content.operation.DeleteRequest;
import ddf.content.operation.DeleteResponse;
import ddf.content.operation.ReadRequest;
import ddf.content.operation.ReadResponse;
import ddf.content.operation.Request;
import ddf.content.operation.Response;
import ddf.content.operation.UpdateRequest;
import ddf.content.operation.UpdateResponse;
import ddf.content.plugin.ContentPlugin;
import ddf.content.storage.StorageProvider;

/**
 * The {@link ContentFramework} functions as the routing mechanism between all content components.
 * It decouples clients from service implementations and provides integration points for Content
 * Plugins.
 * <p>
 * General, high-level flow:
 * <ul>
 * <li/>An endpoint will invoke the active {@link ContentFramework}, typically via an OSGi
 * dependency injection framework such as Blueprint
 * <li/>For the {@link #read(ReadRequest) read}, {@link #create(CreateRequest) create},
 * {@link #delete(DeleteRequest) delete}, {@link #update(UpdateRequest) update} methods, the
 * {@link ContentFramework} calls:
 * <ul>
 * <li/>The active {@link StorageProvider}
 * <li/>All "Post" Content Plugins {@link ContentPlugin}
 * <li/>The appropriate {@link Response} is returned to the calling endpoint.
 * </ul>
 * </ul>
 * </p>
 * 
 * @author Hugh Rodgers, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public interface ContentFramework {
    /**
     * Creates the {@link ContentItem} in the {@link StorageProvider}.
     * 
     * <p>
     * <b>Implementations of this method must:</b>
     * <ol>
     * <li/>Call {@link StorageProvider#create(CreateRequest)} on the registered
     * {@link StorageProvider}
     * <li/>Call {@link ContentPlugin#process(CreateResponse)} for each registered
     * {@link ContentPlugin} in order determined by the OSGi SERVICE_RANKING (Descending, highest
     * first), "daisy chaining" their responses to each other.
     * </ol>
     * </p>
     * 
     * @param createRequest
     *            the {@link CreateRequest} containing the {@link ContentItem} to be stored
     * @return the {@link CreateResponse} containing the {@link ContentItem} that was created,
     *         including its auto-assigned GUID
     * @throws ContentFrameworkException
     *             if an problems encountered during the creation/storing of the {@link ContentItem}
     */
    public CreateResponse create(CreateRequest createRequest, Request.Directive directive)
        throws ContentFrameworkException;

    /**
     * Reads a {@link ContentItem} from the {@link StorageProvider}. The {@link ContentItem} must
     * exist in the {@link StorageProvider} for it to be successfully retrieved.
     * 
     * *
     * <p>
     * <b>Implementations of this method must:</b>
     * <ol>
     * <li/>Call {@link StorageProvider#read(ReadRequest)} on the registered {@link StorageProvider}
     * <li/>Call {@link ContentPlugin#process(ReadResponse)} for each registered
     * {@link ContentPlugin} in order determined by the OSGi SERVICE_RANKING (Descending, highest
     * first), "daisy chaining" their responses to each other.
     * </ol>
     * </p>
     * 
     * @param readRequest
     *            the {@link ReadRequest} containing the GUID of the {@link ContentItem} to retrieve
     * @return the {@link ReadResponse} containing the retrieved {@link ContentItem}
     * @throws ContentFrameworkException
     *             if problems encountered while retrieving the {@link ContentItem}
     */
    public ReadResponse read(ReadRequest readRequest) throws ContentFrameworkException;

    /**
     * Updates a {@link ContentItem} in the {@link StorageProvider}. The {@link ContentItem} must
     * exist in the {@link StorageProvider} for it to be successfully updated. The
     * {@link ContentItem} will not be created if it does not already exist.
     * 
     * <p>
     * <b>Implementations of this method must:</b>
     * <ol>
     * <li/>Call {@link StorageProvider#update(UpdateRequest)} on the registered
     * {@link StorageProvider}
     * <li/>Call {@link ContentPlugin#process(UpdateResponse)} for each registered
     * {@link ContentPlugin} in order determined by the OSGi SERVICE_RANKING (Descending, highest
     * first), "daisy chaining" their responses to each other.
     * </ol>
     * </p>
     * 
     * @param updateRequest
     *            the {@link UpdateRequest} containing the {@link ContentItem} to be updated
     * @return the {@link UpdateResponse} containing the updated {@link ContentItem}
     * @throws ContentFrameworkException
     *             if problems encountered while updating the {@link ContentItem}
     */
    public UpdateResponse update(UpdateRequest updateRequest, Request.Directive directive)
        throws ContentFrameworkException;

    /**
     * Deletes a {@link ContentItem} from the {@link StorageProvider}. The {@link ContentItem} must
     * exist in the {@link StorageProvider} for it to be successfully deleted.
     * 
     * <p>
     * <b>Implementations of this method must:</b>
     * <ol>
     * <li/>Call {@link StorageProvider#delete(DeleteRequest)} on the registered
     * {@link StorageProvider}
     * <li/>Call {@link ContentPlugin#process(DeleteResponse)} for each registered
     * {@link ContentPlugin} in order determined by the OSGi SERVICE_RANKING (Descending, highest
     * first), "daisy chaining" their responses to each other.
     * </ol>
     * </p>
     * 
     * @param deleteRequest
     *            the {@link DeleteRequest} containing the GUID of {@link ContentItem} to be deleted
     * @return the {@link DeleteResponse} containing the status of the {@link ContentItem} deletion
     * @throws ContentFrameworkException
     *             if problems encountered while deleting the {@link ContentItem}
     */
    public DeleteResponse delete(DeleteRequest deleteRequest, Request.Directive directive)
        throws ContentFrameworkException;
}
