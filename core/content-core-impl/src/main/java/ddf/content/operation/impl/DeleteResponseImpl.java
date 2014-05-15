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
import ddf.content.operation.DeleteRequest;
import ddf.content.operation.DeleteResponse;

/**
 * DeleteResponseImpl contains the {@link DeleteResponse} information (boolean status of the file
 * deletion) from a {@link DeleteRequest} operation.
 */
public class DeleteResponseImpl extends ResponseImpl<DeleteRequest> implements DeleteResponse {
    private boolean fileDeleted;

    private ContentItem contentItem;

    /**
     * Instantiates a DeleteResponseImpl object with {@link DeleteResponse} object. This is useful
     * for daisy-chaining plugins together because it preserves the response properties and
     * operation properties throughout the sequence of daisy-chained components.
     * 
     * @param response
     *            the {@link DeleteResponse} to instantiate a new {@link DeleteResponse} from
     */
    public DeleteResponseImpl(DeleteResponse response) {
        this(response.getRequest(), response.getContentItem(), response.isFileDeleted(), response
                .getResponseProperties(), response.getProperties());
    }

    /**
     * Instantiates a DeleteResponseImpl object with the status of the file deleted.
     * 
     * @param request
     *            the original {@link DeleteRequest} that initiated this response
     * @param contentItem
     *            the content item to delete
     * @param fileDeleted
     *            <code>true</code> if file deleted, <code>false</code> otherwise
     */
    public DeleteResponseImpl(DeleteRequest request, ContentItem contentItem, boolean fileDeleted) {
        this(request, contentItem, fileDeleted, null, null);
    }

    /**
     * Instantiates a DeleteResponseImpl object with the status of the file deleted and a
     * {@link Map} of properties.
     * 
     * @param request
     *            the original {@link DeleteRequest} that initiated this response
     * @param contentItem
     *            the content item to delete
     * @param fileDeleted
     *            <code>true</code> if file deleted, <code>false</code> otherwise
     * @param responseProperties
     *            the properties associated with this response
     */
    public DeleteResponseImpl(DeleteRequest request, ContentItem contentItem, boolean fileDeleted,
            Map<String, String> responseProperties) {
        this(request, contentItem, fileDeleted, responseProperties, null);
    }

    /**
     * Instantiates a DeleteResponseImpl object with the status of the file deleted and a
     * {@link Map} of properties.
     * 
     * @param request
     *            the original {@link DeleteRequest} that initiated this response
     * @param contentItem
     *            the content item to delete
     * @param fileDeleted
     *            <code>true</code> if file deleted, <code>false</code> otherwise
     * @param responseProperties
     *            the properties associated with this response
     * @param properties
     *            the properties associated with the operation
     */
    public DeleteResponseImpl(DeleteRequest request, ContentItem contentItem, boolean fileDeleted,
            Map<String, String> responseProperties, Map<String, Serializable> properties) {
        super(request, responseProperties, properties);

        this.contentItem = contentItem;
        this.fileDeleted = fileDeleted;
    }

    @Override
    public ContentItem getContentItem() {
        return contentItem;
    }

    public void setContentItem(ContentItem contentItem) {
        this.contentItem = contentItem;
    }

    @Override
    public boolean isFileDeleted() {
        return fileDeleted;
    }

}
