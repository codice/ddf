/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.security;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.opengis.filter.Filter;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.plugin.impl.PolicyResponseImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * Restricts how resource URIs are updated and created.
 * There are security risk to allowing users to update URIs as well as allowing users to
 * pass in a URI when the metacard is created.
 */
public class ResourceUriSecurityPolicy implements PolicyPlugin {

    private String[] updateResourceUriPermissions;

    private CatalogFramework catalogFramework;

    private FilterBuilder filterBuilder;

    @Override
    public PolicyResponse processPreCreate(Metacard input, Map<String, Serializable> properties)
            throws StopProcessingException {

        return new PolicyResponseImpl();
    }

    protected URI getResourceUriValueFor(String id)  {
        Filter filter = filterBuilder.attribute(Metacard.ID)
                .is()
                .equalTo()
                .text(id);
        Query query = new QueryImpl(filter);
        QueryRequest queryRequest = new QueryRequestImpl(query);
        QueryResponse queryResponse;
        try {
            queryResponse = catalogFramework.query(queryRequest);
        } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
            throw new RuntimeException(e);
        }
        Result queryResult = queryResponse.getResults()
                .get(0);
        Metacard metacard = queryResult.getMetacard();
        return metacard.getResourceURI();
    }

    @Override
    public PolicyResponse processPreUpdate(Metacard input, Map<String, Serializable> properties)
            throws StopProcessingException {

        URI inputUri = input.getResourceURI();
        if (inputUri != null && StringUtils.isNotEmpty(inputUri.toString())) {
            URI catalogUri = getResourceUriValueFor(input.getId());

            if (catalogUri == null) {
                return new PolicyResponseImpl();

            } else {
                if (input.getResourceURI()
                        .equals(catalogUri)) {
                    return new PolicyResponseImpl();
                } else {
                    // come back here and add correct logic
                    Map<String, Set<String>> map = new HashMap();
                    Set<String> permissions = new HashSet<>();
                    permissions.add("admin");
                    map.put("role", permissions);
                    return new PolicyResponseImpl(null, map);
                }
            }

        } else {
            return new PolicyResponseImpl();
        }
    }

    @Override
    public PolicyResponse processPreDelete(List<Metacard> metacards,
            Map<String, Serializable> properties) throws StopProcessingException {

        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPostDelete(Metacard input, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPreQuery(Query query, Map<String, Serializable> properties)
            throws StopProcessingException {

        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPostQuery(Result input, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPreResource(ResourceRequest resourceRequest)
            throws StopProcessingException {

        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPostResource(ResourceResponse resourceResponse, Metacard metacard)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    public CatalogFramework getCatalogFramework() {
        return catalogFramework;
    }

    public void setCatalogFramework(CatalogFramework catalogFramework) {
        this.catalogFramework = catalogFramework;
    }

    public String[] getUpdateResourceUriPermissions() {
        return updateResourceUriPermissions == null ? null : updateResourceUriPermissions.clone();
    }

    public void setUpdateResourceUriPermissions(String[] updateResourceUriPermissions) {
        this.updateResourceUriPermissions =
                updateResourceUriPermissions == null ? null : updateResourceUriPermissions.clone();
    }

    public FilterBuilder getFilterBuilder() {
        return filterBuilder;
    }

    public void setFilterBuilder(FilterBuilder filterBuilder) {
        this.filterBuilder = filterBuilder;
    }
}

