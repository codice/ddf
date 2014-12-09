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
import ddf.content.operation.CreateRequest;
import ddf.content.operation.CreateResponse;

import java.io.Serializable;
import java.util.Map;

/**
 * CreateResponseImpl contains the {@link CreateResponse} information (created {@link ContentItem})
 * from a {@link CreateRequest} operation.
 * 
 */
public class CreateResponseImpl extends ResponseImpl<CreateRequest> implements CreateResponse {
    private ContentItem contentItem;
    private byte[] metadata;
    private String mimeType;

    /**
     * Instantiates a CreateResponseImpl object with {@link CreateResponse} object. This is useful
     * for daisy-chaining plugins together because it preserves the response properties and
     * operation properties throughout the sequence of daisy-chained components.
     * 
     * @param response
     *            the {@link CreateResponse} to instantiate a new {@link CreateResponse} from
     */
    public CreateResponseImpl(CreateResponse response) {
        this(response.getRequest(), response.getCreatedContentItem(), response
                .getResponseProperties(), response.getProperties());
    }

    /**
     * Instantiates a CreateResponseImpl object with {@link ContentItem} created.
     * 
     * @param request
     *            the original {@link CreateRequest} that initiated this response
     * @param contentItem
     *            the {@link ContentItem} created
     */
    public CreateResponseImpl(CreateRequest request, ContentItem contentItem) {
        this(request, contentItem, null, null);
    }

    /**
     * Instantiates a CreateResponseImpl object with {@link ContentItem} created and a {@link Map}
     * of properties.
     * 
     * @param request
     *            the original {@link CreateRequest} that initiated this response
     * @param contentItem
     *            the {@link ContentItem} created
     * @param responseProperties
     *            the properties associated with this response
     */
    public CreateResponseImpl(CreateRequest request, ContentItem contentItem,
            Map<String, String> responseProperties) {
        this(request, contentItem, responseProperties, null);
    }

    /**
     * Instantiates a CreateResponseImpl object with {@link ContentItem} created and a {@link Map}
     * of properties.
     * 
     * @param request
     *            the original {@link CreateRequest} that initiated this response
     * @param contentItem
     *            the {@link ContentItem} created
     * @param responseProperties
     *            the properties associated with this response
     * @param properties
     *            the properties associated with the operation
     */
    public CreateResponseImpl(CreateRequest request, ContentItem contentItem,
            Map<String, String> responseProperties, Map<String, Serializable> properties) {
        super(request, responseProperties, properties);
        this.contentItem = contentItem;
    }

    @Override
    public ContentItem getCreatedContentItem() {
        return contentItem;
    }

    @Override
    public byte[] getCreatedMetadata() {
        return metadata;
    }

    public void setCreatedMetadata(byte[] metadata) {
        this.metadata = metadata;
    }

    @Override
    public String getCreatedMetadataMimeType() {
        return mimeType;
    }

    public void setCreatedMetadataMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}
