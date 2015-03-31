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
package org.codice.ddf.endpoints;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceInfoRequest;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;

/**
 * {@link CatalogFramework} proxy class used to transform the URIs found in a {@link SourceResponse}
 * {@link Metacard}s using the injected {@link ActionProvider} before transformation by the
 * {@link CatalogFramework}.
 */
public class ResourceUriTransformerCatalogFrameworkProxy implements ddf.catalog.CatalogFramework {
    // PreCatalogTransformPlugin {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ResourceUriTransformerCatalogFrameworkProxy.class);

    private CatalogFramework catalog;
    private ActionProvider resourceActionProvider;

    public ResourceUriTransformerCatalogFrameworkProxy(CatalogFramework catalog,
            ActionProvider resourceActionProvider) {
        this.catalog = catalog;
        this.resourceActionProvider = resourceActionProvider;
    }

    public String getVersion() {
        return catalog.getVersion();
    }

    public String getId() {
        return catalog.getId();
    }

    public String getTitle() {
        return catalog.getTitle();
    }

    public String getDescription() {
        return catalog.getDescription();
    }

    public String getOrganization() {
        return catalog.getOrganization();
    }

    public CreateResponse create(CreateRequest createRequest) throws IngestException,
        SourceUnavailableException {
        return catalog.create(createRequest);
    }

    public DeleteResponse delete(DeleteRequest deleteRequest) throws IngestException,
        SourceUnavailableException {
        return catalog.delete(deleteRequest);
    }

    public ResourceResponse getEnterpriseResource(ResourceRequest request) throws IOException,
        ResourceNotFoundException, ResourceNotSupportedException {
        return catalog.getEnterpriseResource(request);
    }

    public Map<String, Set<String>> getEnterpriseResourceOptions(String metacardId)
        throws ResourceNotFoundException {
        return catalog.getEnterpriseResourceOptions(metacardId);
    }

    public ResourceResponse getLocalResource(ResourceRequest request) throws IOException,
        ResourceNotFoundException, ResourceNotSupportedException {
        return catalog.getLocalResource(request);
    }

    public Map<String, Set<String>> getLocalResourceOptions(String metacardId)
        throws ResourceNotFoundException {
        return catalog.getLocalResourceOptions(metacardId);
    }

    public ResourceResponse getResource(ResourceRequest request, String resourceSiteName)
        throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
        return catalog.getResource(request, resourceSiteName);
    }

    public Map<String, Set<String>> getResourceOptions(String metacardId, String sourceId)
        throws ResourceNotFoundException {
        return catalog.getResourceOptions(metacardId, sourceId);
    }

    public Set<String> getSourceIds() {
        return catalog.getSourceIds();
    }

    public SourceInfoResponse getSourceInfo(SourceInfoRequest sourceInfoRequest)
        throws SourceUnavailableException {
        return catalog.getSourceInfo(sourceInfoRequest);
    }

    public QueryResponse query(QueryRequest query) throws UnsupportedQueryException,
        SourceUnavailableException, FederationException {
        return catalog.query(query);
    }

    public QueryResponse query(QueryRequest queryRequest, FederationStrategy strategy)
        throws SourceUnavailableException, UnsupportedQueryException, FederationException {
        return catalog.query(queryRequest, strategy);
    }

    public BinaryContent transform(Metacard metacard, String transformerId,
            Map<String, Serializable> requestProperties) throws CatalogTransformerException {
        return catalog.transform(metacard, transformerId, requestProperties);
    }

    /**
     * Converts the resource URIs found in the {@link SourceResponse} {@link Metacard}s using the
     * {@link ActionProvider} injected in the constructor before delegating the call to the proxied
     * {@link CatalogFramework} object.
     */
    public BinaryContent transform(SourceResponse response, String transformerId,
            Map<String, Serializable> requestProperties) throws CatalogTransformerException {
        transform(response);
        return catalog.transform(response, transformerId, requestProperties);
    }

    public UpdateResponse update(UpdateRequest updateRequest) throws IngestException,
        SourceUnavailableException {
        return catalog.update(updateRequest);
    }

    private void transform(SourceResponse queryResponse) {
        for (Result result : queryResponse.getResults()) {
            final Metacard metacard = result.getMetacard();

            if (metacard.getResourceURI() != null && resourceActionProvider != null) {
                Action action = resourceActionProvider.getAction(metacard);

                if (action != null) {
                    final URL resourceUrl = action.getUrl();

                    if (resourceUrl != null) {
                        try {
                            metacard.setAttribute(new AttributeImpl("resource-uri", resourceUrl
                                    .toURI().toString()));
                        } catch (URISyntaxException e) {
                            LOGGER.warn("Unable to retrieve '{}' from '{}' for metacard ID [{}]",
                                    Metacard.RESOURCE_URI, resourceActionProvider.getClass()
                                            .getName(), metacard.getId());
                        }
                    }
                }
            }
        }
    }
}
