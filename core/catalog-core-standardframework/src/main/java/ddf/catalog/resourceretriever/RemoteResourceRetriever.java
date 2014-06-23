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
package ddf.catalog.resourceretriever;

import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.source.RemoteSource;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class RemoteResourceRetriever implements ResourceRetriever {

    private RemoteSource source;
    private URI resourceUri;
    private Map<String, Serializable> properties;

    public RemoteResourceRetriever(RemoteSource source) {
        this(source, null, null);
    }
    
    public RemoteResourceRetriever(RemoteSource source, URI resourceUri,
            Map<String, Serializable> properties) {
        this.source = source;
        this.resourceUri = resourceUri;
        this.properties = properties;
    }
    
    @Override
    public ResourceResponse retrieveResource() throws ResourceNotFoundException, IOException,
            ResourceNotSupportedException {
        return retrieveResource(null);
    }

    @Override
    public ResourceResponse retrieveResource(String bytesToSkip) throws ResourceNotFoundException, IOException,
            ResourceNotSupportedException {

        if (resourceUri == null) {
            throw new ResourceNotFoundException("Cannot retrieve resource because resourceUri is null.");
        }

        // Create a fresh HashMap so as not to disturb the existing properties if we need to add to them
        Map<String, Serializable> props = new HashMap<String, Serializable>(properties);

        if (bytesToSkip != null) {
            props.put(BYTES_TO_SKIP, bytesToSkip);
        }

        return source.retrieveResource(resourceUri, props);
    }

}
