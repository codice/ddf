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
 */
package ddf.catalog.resourceretriever;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.source.RemoteSource;

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
    public ResourceResponse retrieveResource()
            throws ResourceNotFoundException, IOException, ResourceNotSupportedException {
        return retrieveResource(0);
    }

    @Override
    public ResourceResponse retrieveResource(long bytesToSkip)
            throws ResourceNotFoundException, IOException, ResourceNotSupportedException {

        if (resourceUri == null) {
            throw new ResourceNotFoundException(
                    "Cannot retrieve resource because resourceUri is null.");
        }

        // Create a fresh HashMap so as not to disturb the existing properties if we need to add to them
        Map<String, Serializable> props = new HashMap<String, Serializable>(properties);

        if (bytesToSkip > 0) {
            props.put(BYTES_TO_SKIP, new Long(bytesToSkip));
        }

        return source.retrieveResource(resourceUri, props);
    }

}
