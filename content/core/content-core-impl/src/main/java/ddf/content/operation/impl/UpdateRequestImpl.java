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
package ddf.content.operation.impl;

import java.io.Serializable;
import java.util.Map;

import ddf.content.data.ContentItem;
import ddf.content.operation.UpdateRequest;

/**
 * UpdateRequestImpl represents a {@link UpdateRequest} and supports passing a {@link Map} of
 * properties for update operations.
 */
public class UpdateRequestImpl extends OperationImpl implements UpdateRequest {
    private ContentItem contentItem;

    /**
     * Instantiates a new UpdateRequestImpl with the {@link ContentItem} to be updated.
     * 
     * @param contentItem
     *            the {@link ContentItem}
     */
    public UpdateRequestImpl(ContentItem contentItem) {
        this(contentItem, null);
    }

    /**
     * Instantiates a new UpdateRequestImpl with the {@link ContentItem} to be updated and a
     * {@link Map} of properties.
     * 
     * @param contentItem
     *            the {@link ContentItem}
     * @param properties
     *            the properties of the operation
     */
    public UpdateRequestImpl(ContentItem contentItem, Map<String, Serializable> properties) {
        super(properties);

        this.contentItem = contentItem;
    }

    @Override
    public ContentItem getContentItem() {
        return contentItem;
    }
}
