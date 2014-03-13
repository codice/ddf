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

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Map;

import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.source.FederatedSource;

public class RemoteResourceRetriever implements ResourceRetriever {

    private FederatedSource source;
    private URI resourceUri;
    private Map<String, Serializable> properties;
    
    
    public RemoteResourceRetriever(FederatedSource source) {
        this(source, null, null);
    }
    
    public RemoteResourceRetriever(FederatedSource source, URI resourceUri, Map<String, Serializable> properties) {
        this.source = source;
        this.resourceUri = resourceUri;
        this.properties = properties;
    }
    
    @Override
    public ResourceResponse retrieveResource() throws ResourceNotFoundException {
        if (resourceUri == null) {
            throw new ResourceNotFoundException("Cannot retrieve resource because resourceUri is null.");
        }
        
        try {
            return source.retrieveResource(resourceUri, properties);
        } catch (IOException e) {
            throw new ResourceNotFoundException(e);
        } catch (ResourceNotSupportedException e) {
            throw new ResourceNotFoundException(e);
        }
    }

}
