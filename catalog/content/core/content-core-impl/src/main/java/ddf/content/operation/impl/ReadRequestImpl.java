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
package ddf.content.operation.impl;

import java.io.Serializable;
import java.util.Map;

import ddf.content.operation.ReadRequest;

/**
 * ReadRequestImpl represents a {@link ReadRequest} and supports passing a {@link Map} of properties
 * for read operations.
 */
public class ReadRequestImpl extends OperationImpl implements ReadRequest {
    private String id;

    /**
     * Instantiates a new ReadRequestImpl with the ID of the {@link ddf.content.data.ContentItem} to be retrieved.
     *
     * @param id the GUID of the {@link ddf.content.data.ContentItem}
     */
    public ReadRequestImpl(String id) {
        this(id, null);
    }

    /**
     * Instantiates a new ReadRequestImpl with the ID of the {@link ddf.content.data.ContentItem} to be retrieved and
     * a {@link Map} of properties.
     *
     * @param id         the GUID of the {@link ddf.content.data.ContentItem}
     * @param properties the properties of the operation
     */
    public ReadRequestImpl(String id, Map<String, Serializable> properties) {
        super(properties);

        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }
}
