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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.ResourceReader;

public class LocalResourceRetriever implements ResourceRetriever {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalResourceRetriever.class);

    private List<ResourceReader> resourceReaders;
    private URI resourceUri;
    private Map<String, Serializable> properties;
    
    
    public LocalResourceRetriever(List<ResourceReader> resourceReaders, URI resourceUri, Map<String, Serializable> properties) {
        this.resourceReaders = resourceReaders;
        this.resourceUri = resourceUri;
        this.properties = properties;
    }
    
    @Override
    public ResourceResponse retrieveResource() throws ResourceNotFoundException {
        final String methodName = "retrieveResource";
        LOGGER.trace("ENTERING: {}", methodName);
        ResourceResponse resource = null;

        if (resourceUri == null) {
            throw new ResourceNotFoundException("Unable to find resource due to null URI");
        }

        for (ResourceReader reader : resourceReaders) {
            if (reader != null) {
                String scheme = resourceUri.getScheme();
                if (reader.getSupportedSchemes().contains(scheme)) {
                    try {
                        LOGGER.debug("Found an acceptable resource reader ("
                                + reader.getId() + ") for URI "
                                + resourceUri.toASCIIString());
                        resource = reader.retrieveResource(resourceUri,
                                properties);
                        if (resource != null) {
                            break;
                        } else {
                            LOGGER.info("Resource returned from ResourceReader "
                                    + reader.getId()
                                    + " was null.  Checking other readers for URI: "
                                    + resourceUri);
                        }
                    } catch (ResourceNotFoundException e) {
                        LOGGER.debug("Enterprise Search: Product not found using resource reader with name "
                                + reader.getId());
                    } catch (ResourceNotSupportedException e) {
                        LOGGER.debug("Enterprise Search: Product not found using resource reader with name "
                                + reader.getId());
                    } catch (IOException ioe) {
                        LOGGER.debug("Enterprise Search: Product not found using resource reader with name "
                                + reader.getId());
                    }
                }
            }
        }

        if (resource == null) {
            throw new ResourceNotFoundException(
                    "Resource Readers could not find resource (or returned null resource) for URI: "
                            + resourceUri.toASCIIString() + ". Scheme: " + resourceUri.getScheme());
        }
        LOGGER.debug("Received resource, sending back: " + resource.getResource().getName());
        LOGGER.trace("EXITING: {}", methodName);
        
        return resource;
    }

}
