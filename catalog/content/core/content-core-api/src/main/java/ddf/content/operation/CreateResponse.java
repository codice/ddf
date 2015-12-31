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
package ddf.content.operation;

import ddf.content.data.ContentItem;

/**
 * Defines a Create Response object which contains response information that should be returned on a
 * {@link CreateRequest} operation.
 *
 * @author Hugh Rodgers, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public interface CreateResponse extends Response<CreateRequest> {

    /**
     * Get the {@link ContentItem} returned from a create operation.
     *
     * @return the created {@link ContentItem}
     */
    public ContentItem getCreatedContentItem();

    /**
     * Get the metadata returned from an create operation.
     *
     * @return the updated metadata
     */
    public byte[] getCreatedMetadata();

    /**
     * Get the mime type returned from an create operation.
     *
     * @return the updated mime type
     */
    public String getCreatedMetadataMimeType();
}
