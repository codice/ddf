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

import ddf.content.data.ContentItem;
import ddf.content.operation.UpdateRequest;
import ddf.content.operation.UpdateResponse;

import java.io.Serializable;
import java.util.Map;

/**
 * UpdateResponseImpl contains the {@link UpdateResponse} information (updated {@link ContentItem})
 * from an {@link UpdateRequest} operation.
 */
public class UpdateResponseImpl extends ResponseImpl<UpdateRequest> implements UpdateResponse {
    private ContentItem contentItem;
    private byte[] metadata;
    private String mimeType;

    /**
     * Instantiates a UpdateResponseImpl object with {@link UpdateResponse} object. This is useful
     * for daisy-chaining plugins together because it preserves the response properties and
     * operation properties throughout the sequence of daisy-chained components.
     * 
     * @param response
     *            the {@link UpdateResponse} to instantiate a new {@link UpdateResponse} from
     */
    public UpdateResponseImpl(UpdateResponse response) {
        this(response.getRequest(), response.getUpdatedContentItem(), response
                .getResponseProperties(), response.getProperties());
    }

    /**
     * Instantiates an UpdateResponseImpl object with {@link ContentItem} updated.
     * 
     * @param request
     *            the original {@link UpdateRequest} that initiated this response
     * @param contentItem
     *            the {@link ContentItem} updated
     */
    public UpdateResponseImpl(UpdateRequest request, ContentItem contentItem) {
        this(request, contentItem, null, null);
    }

    /**
     * Instantiates an UpdateResponseImpl object with {@link ContentItem} updated and a {@link Map}
     * of properties.
     * 
     * @param request
     *            the original {@link UpdateRequest} that initiated this response
     * @param contentItem
     *            the {@link ContentItem} updated
     * @param responseProperties
     *            the properties associated with this response
     */
    public UpdateResponseImpl(UpdateRequest request, ContentItem contentItem,
            Map<String, String> responseProperties) {
        this(request, contentItem, responseProperties, null);

        this.contentItem = contentItem;
    }

    /**
     * Instantiates an UpdateResponseImpl object with {@link ContentItem} updated and a {@link Map}
     * of properties.
     * 
     * @param request
     *            the original {@link UpdateRequest} that initiated this response
     * @param contentItem
     *            the {@link ContentItem} updated
     * @param responseProperties
     *            the properties associated with this response
     * @param properties
     *            the properties associated with the operation
     */
    public UpdateResponseImpl(UpdateRequest request, ContentItem contentItem,
            Map<String, String> responseProperties, Map<String, Serializable> properties) {
        super(request, responseProperties, properties);

        this.contentItem = contentItem;
    }

    @Override
    public ContentItem getUpdatedContentItem() {
        return contentItem;
    }

    @Override
    public byte[] getUpdatedMetadata() {
        return metadata;
    }

    public void setUpdatedMetadata(byte[] metadata) {
        this.metadata = metadata;
    }

    @Override
    public String getUpdatedMetadataMimeType() {
        return mimeType;
    }

    public void setUpdatedMetadataMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}
