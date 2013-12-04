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

/**
 * The ResponseImpl class provides a generic means of providing a
 * {@link Response} on operations.
 * 
 * @deprecated As of release 2.3.0, replaced by
 *             ddf.catalog.operation.impl.ResponseImpl
 * 
 */
@Deprecated
public class ResponseImpl<T extends Request> extends OperationImpl implements Response<T> {

    protected T request;

    /**
     * Instantiates a new ResponseImpl
     * 
     * @param request
     *            - the original request
     * @param properties
     *            - the properties associated with the operation
     */
    public ResponseImpl(T request, Map<String, Serializable> properties) {
        super(properties);
        this.request = request;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.operation.Response#getRequest()
     */
    @Override
    public T getRequest() {
        return request;
    }

}
