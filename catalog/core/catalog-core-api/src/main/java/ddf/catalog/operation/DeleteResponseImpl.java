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
 * The DeleteResponseImpl represents a default implementation of the {@link DefaultResponse}.
 * @deprecated Use ddf.catalog.operation.impl.DeleteResponseImpl
 */
@Deprecated
public class DeleteResponseImpl extends ResponseImpl<DeleteRequest> implements DeleteResponse {

    protected List<Metacard> deletedMetacards;

    /**
     * Instantiates a new DeleteResponseImpl.
     * 
     * @param request
     *            the original request
     * @param properties
     *            the properties associated with the operation
     * @param deletedMetacards
     *            the deleted {@link Metacard}(s)
     */
    public DeleteResponseImpl(DeleteRequest request, Map<String, Serializable> properties,
            List<Metacard> deletedMetacards) {
        super(request, properties);
        this.deletedMetacards = deletedMetacards;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.operation.DeleteResponse#getDeletedMetacards()
     */
    @Override
    public List<Metacard> getDeletedMetacards() {
        return deletedMetacards;
    }

}
