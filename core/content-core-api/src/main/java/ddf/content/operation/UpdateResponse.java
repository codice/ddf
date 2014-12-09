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
package ddf.content.operation;

import ddf.content.data.ContentItem;

/**
 * Defines a Update Response object which contains response information that should be returned on a
 * {@link UpdateRequest} operation.
 * 
 * @author Hugh Rodgers, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public interface UpdateResponse extends Response<UpdateRequest> {

    /**
     * Get the {@link ContentItem} returned from an update operation.
     * 
     * @return the updated {@link ContentItem}
     */
    public ContentItem getUpdatedContentItem();

    /**
     * Get the metadata returned from an update operation.
     *
     * @return the updated metadata
     */
    public byte[] getUpdatedMetadata();

    /**
     * Get the mime type returned from an update operation.
     *
     * @return the updated mime type
     */
    public String getUpdatedMetadataMimeType();
}
