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
package org.codice.ddf.confluence.source;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import org.codice.ddf.configuration.PropertyResolver;
import org.codice.ddf.confluence.api.SearchResource;
import org.codice.ddf.cxf.SecureCxfClientFactory;
import org.opengis.filter.sort.SortBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.util.impl.MaskableImpl;
import ddf.security.encryption.EncryptionService;

public class ConfluenceSource extends MaskableImpl implements FederatedSource, ConfiguredService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfluenceSource.class);

    private static final ObjectMapper MAPPER = JsonFactory.create();

    private static final String USERNAME = "username";

    private static final String PASSWORD = "password";

    private static final String BASE_URL = "baseUrl";

    private String endpointUrl;

    private String configurationPid;

    private String username;

    private String password;

    private String expandedSections = "";

    private Boolean includeArchivedSpaces = false;

    private Boolean includePageContent = false;

    private String confluenceSpace = "";

    private final EncryptionService encryptionService;

    private final FilterAdapter filterAdapter;

    private final ResourceReader resourceReader;

    private final ConfluenceInputTransformer transformer;

    protected SecureCxfClientFactory<SearchResource> factory;

    private boolean lastAvailable;

    private Date lastAvailableDate = null;

    private long availabilityPollInterval = TimeUnit.SECONDS.toMillis(60);

    private Set<SourceMonitor> sourceMonitors = new HashSet<>();

    public ConfluenceSource(FilterAdapter adapter, EncryptionService encryptionService,
            ConfluenceInputTransformer transformer, ResourceReader reader) {
        this.filterAdapter = adapter;
        this.encryptionService = encryptionService;
        this.transformer = transformer;
        this.resourceReader = reader;
    }

    public void init() {
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            factory = new SecureCxfClientFactory<SearchResource>(endpointUrl,
                    SearchResource.class,
                    username,
                    password);
        } else {
            factory = new SecureCxfClientFactory<SearchResource>(endpointUrl, SearchResource.class);
        }
    }

    @Override
    public String getConfigurationPid() {
        return configurationPid;
    }

    @Override
    public void setConfigurationPid(String configurationPid) {
        this.configurationPid = configurationPid;
    }

    @Override
    public boolean isAvailable() {
        boolean isAvailable = false;
        if (!lastAvailable || (lastAvailableDate.before(new Date(
                System.currentTimeMillis() - availabilityPollInterval)))) {

            Response response = null;

            try {
                response = factory.getWebClient()
                        .head();
            } catch (Exception e) {
                LOGGER.debug("Web Client was unable to connect to endpoint.", e);
                isAvailable = false;
            }

            if (response != null && !(response.getStatus() >= HttpStatus.SC_NOT_FOUND
                    || response.getStatus() == HttpStatus.SC_BAD_REQUEST
                    || response.getStatus() == HttpStatus.SC_PAYMENT_REQUIRED)) {
                isAvailable = true;
                lastAvailableDate = new Date();
            }
        } else {
            isAvailable = lastAvailable;
        }

        if (lastAvailable != isAvailable) {
            for (SourceMonitor monitor : this.sourceMonitors) {
                if (isAvailable) {
                    monitor.setAvailable();
                } else {
                    monitor.setUnavailable();
                }
            }
        }

        lastAvailable = isAvailable;
        return isAvailable;
    }

    @Override
    public boolean isAvailable(SourceMonitor callback) {
        sourceMonitors.add(callback);
        return isAvailable();
    }

    @Override
    public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {
        SearchResource confluence = factory.getClient();
        Query query = request.getQuery();
        ConfluenceFilterDelegate confluenceDelegate = new ConfluenceFilterDelegate();

        String cql = filterAdapter.adapt(query, confluenceDelegate);
        if (!confluenceDelegate.isConfluenceQuery() || (StringUtils.isEmpty(cql) && (
                StringUtils.isEmpty(confluenceSpace) || !confluenceDelegate.isWildCardQuery()))) {
            return new SourceResponseImpl(request, Collections.emptyList());
        }
        cql = getSortedQuery(query.getSortBy(), getSpaceQuery(cql));
        LOGGER.debug(cql);

        String finalExpandedSections = expandedSections;
        if (includePageContent) {
            finalExpandedSections += ",body.view";
        }

        String cqlContext = null;
        String excerpt = null;
        Response confluenceResponse = confluence.search(cql,
                cqlContext,
                excerpt,
                finalExpandedSections,
                query.getStartIndex() - 1,
                query.getPageSize(),
                includeArchivedSpaces);

        InputStream stream = null;
        Object entityObj = confluenceResponse.getEntity();
        if (entityObj != null) {
            stream = (InputStream) entityObj;
        }
        if (Response.Status.OK.getStatusCode() != confluenceResponse.getStatus()) {
            String error = "";
            try {
                if (stream != null) {
                    error = IOUtils.toString(stream);
                }
            } catch (IOException ioe) {
                LOGGER.debug("Could not convert error message to a string for output.", ioe);
            }
            throw new UnsupportedQueryException(String.format(
                    "Received error code from remote source (status %s ): %s",
                    confluenceResponse.getStatus(),
                    error));
        }

        try {

            List<Result> results = transformer.transformConfluenceResponse(stream)
                    .stream()
                    .map(this::getResultWithSourceId)
                    .collect(Collectors.toList());

            return new SourceResponseImpl(request, results);
        } catch (IOException | CatalogTransformerException e) {
            throw new UnsupportedQueryException("Exception processing results from confluence");
        }
    }

    @Override
    public Set<ContentType> getContentTypes() {
        return Collections.emptySet();
    }

    @Override
    public ResourceResponse retrieveResource(URI uri, Map<String, Serializable> arguments)
            throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            arguments.put(USERNAME, username);
            arguments.put(PASSWORD, password);
        }
        return resourceReader.retrieveResource(uri, arguments);
    }

    @Override
    public Set<String> getSupportedSchemes() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getOptions(Metacard metacard) {
        return Collections.emptySet();
    }

    public void setAvailabilityPollInterval(long availabilityPollInterval) {
        this.availabilityPollInterval = availabilityPollInterval;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = PropertyResolver.resolveProperties(endpointUrl);
        init();
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        String updatedPassword = password;
        if (encryptionService != null) {
            updatedPassword = encryptionService.decryptValue(password);
        }
        this.password = updatedPassword;
    }

    public void setExpandedSections(List<String> expandedSections) {
        if (expandedSections == null) {
            this.expandedSections = "";
            return;
        }

        this.expandedSections = expandedSections.stream()
                .collect(Collectors.joining(","));
    }

    public void setIncludeArchivedSpaces(Boolean includeArchivedSpaces) {
        this.includeArchivedSpaces = includeArchivedSpaces;
    }

    public void setIncludePageContent(Boolean includePageContent) {
        this.includePageContent = includePageContent;
    }

    public void setConfluenceSpace(String confluenceSpace) {
        this.confluenceSpace = confluenceSpace;
    }

    private Result getResultWithSourceId(Metacard metacard) {
        metacard.setSourceId(this.getId());
        return new ResultImpl(metacard);
    }

    private String getSortedQuery(SortBy sort, String query) {
        if (sort != null && sort.getPropertyName() != null && sort.getPropertyName()
                .getPropertyName() != null) {
            String sortProperty = sort.getPropertyName()
                    .getPropertyName();
            if (ConfluenceFilterDelegate.QUERY_PARAMETERS.containsKey(sortProperty)) {
                query = String.format("%s order by %s %s",
                        query,
                        ConfluenceFilterDelegate.QUERY_PARAMETERS.get(sortProperty)
                                .getParamterName(),
                        sort.getSortOrder()
                                .toSQL());
            }
        }
        return query;
    }

    private String getSpaceQuery(String query) {
        if (StringUtils.isNotEmpty(confluenceSpace)) {
            if (StringUtils.isEmpty(query.trim())) {
                return String.format("space = %s", confluenceSpace);
            }
            return String.format("%s AND space = %s", query, confluenceSpace);
        }
        return query;
    }
}
