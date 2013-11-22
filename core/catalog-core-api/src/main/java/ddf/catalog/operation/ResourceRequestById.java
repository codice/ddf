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
package ddf.catalog.operation;

import java.io.Serializable;
import java.util.Map;

import ddf.catalog.data.Result;

/**
 * 
 * @deprecated Use ddf.catalog.operation.impl.ResourceRequestById
 *
 */
@Deprecated
public class ResourceRequestById extends OperationImpl implements ResourceRequest {

    protected String name;

    protected String id;

    /**
     * Implements a ResourceRequestById and specifies the id
     * 
     * @param id
     *            the id
     */
    public ResourceRequestById(String id) {
        this(id, null);
    }

    /**
     * Implements a ResourceRequestById and specifies the id and a ${@link Map} of properties
     * 
     * @param properties
     *            the properties
     */
    public ResourceRequestById(String id, Map<String, Serializable> properties) {
        super(properties);
        this.name = GET_RESOURCE_BY_ID;
        this.id = id;
    }

    @Override
    public String getAttributeName() {
        return name;
    }

    @Override
    public String getAttributeValue() {
        return id;
    }

}
