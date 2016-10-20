/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package ddf.catalog.content.operation.impl;

import java.io.Serializable;
import java.util.Map;

import javax.annotation.Nullable;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.ReadStorageRequest;
import ddf.catalog.content.operation.ReadStorageResponse;
import ddf.catalog.content.operation.StorageRequest;
import ddf.catalog.operation.impl.ResponseImpl;

public class ReadStorageResponseImpl extends ResponseImpl<ReadStorageRequest>
        implements ReadStorageResponse {
    private ContentItem contentItem;

    /**
     * Instantiates an empty, new ResponseImpl
     *
     * @param request the original request
     */
    public ReadStorageResponseImpl(ReadStorageRequest request) {
        super(request, null);
    }

    /**
     * Instantiates a new ResponseImpl
     *
     * @param request the original request
     * @param properties
     */
    public ReadStorageResponseImpl(ReadStorageRequest request,
            Map<String, Serializable> properties) {
        super(request, properties);
    }

    /**
     * Instantiates a new ResponseImpl
     *
     * @param request the original request
     * @param contentItem
     */
    public ReadStorageResponseImpl(ReadStorageRequest request, ContentItem contentItem) {
        super(request, null);
        this.contentItem = contentItem;
    }

    @Override
    @Nullable
    public ContentItem getContentItem() {
        return contentItem;
    }

    @Override
    public StorageRequest getStorageRequest() {
        return request;
    }
}
