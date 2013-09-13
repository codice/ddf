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

import ddf.catalog.resource.Resource;

/**
 * The ResourceResponseImpl provides a means of providing a {@link ResourceResponse}.
 */
public class ResourceResponseImpl extends ResponseImpl<ResourceRequest> implements ResourceResponse {

    protected Resource resource;

    /**
     * Instantiates a new ResourceResponseImpl from the given {@link Resource}.
     * 
     * @param resource
     *            the resource to create this response from
     */
    public ResourceResponseImpl(Resource resource) {
        this(null, null, resource);
    }

    /**
     * Instantiates a new ResourceResponseImpl from the given {@link ResourceRequest} and
     * {@link Resource}.
     * 
     * @param request
     *            - the original request ResourceRequest
     * @param resource
     *            - the resource to create this response from
     */
    public ResourceResponseImpl(ResourceRequest request, Resource resource) {
        this(request, null, resource);
    }

    /**
     * Instantiates a new ResourceResponseImpl from the {@link Request}, {@link Map}, and
     * {@link Resource}
     * 
     * @param request
     *            - the original request
     * @param properties
     *            - the properties associated with this operation
     * @param resource
     *            - the resource to create this response from
     */
    public ResourceResponseImpl(ResourceRequest request, Map<String, Serializable> properties,
            Resource resource) {
        super(request, properties);
        this.resource = resource;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.operation.ResourceResponse#getResource()
     */
    @Override
    public Resource getResource() {
        return resource;
    }

}
