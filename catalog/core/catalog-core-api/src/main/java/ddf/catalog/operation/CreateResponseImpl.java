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
import java.util.List;
import java.util.Map;

import ddf.catalog.data.Metacard;

/**
 * CreateResponseImpl contains the {@link Response} information (created
 * metacards) on a {@link CreateRequest}.
 * 
 * @deprecated As of release 2.3.0, replaced by
 *             ddf.catalog.operation.impl.CreateResponseImpl
 */
@Deprecated
public class CreateResponseImpl extends ResponseImpl<CreateRequest> implements CreateResponse {

    /** The created metacards. */
    protected List<Metacard> createdMetacards;

    /**
     * Instantiates a new CreateResponsImpl
     * 
     * @param request
     *            - {@link CreateRequest} used in the create operation
     * @param properties
     *            - the properties associated with the operation
     * @param createdMetacards
     *            - the created metacards
     */
    public CreateResponseImpl(CreateRequest request, Map<String, Serializable> properties,
            List<Metacard> createdMetacards) {
        super(request, properties);
        this.createdMetacards = createdMetacards;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.operation.CreateResponse#getCreatedMetacards()
     */
    @Override
    public List<Metacard> getCreatedMetacards() {
        return createdMetacards;
    }

}
