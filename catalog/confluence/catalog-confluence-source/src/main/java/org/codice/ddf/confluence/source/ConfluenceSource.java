/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.confluence.source;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
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
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.util.impl.MaskableImpl;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.encryption.EncryptionService;
import ddf.security.permission.Permissions;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.codice.ddf.configuration.PropertyResolver;
import org.codice.ddf.confluence.api.SearchResource;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.opengis.filter.sort.SortBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfluenceSource extends MaskableImpl
    implements FederatedSource, ConnectedSource, ConfiguredService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfluenceSource.class);

  private static final String BASIC = "basic";

  private static final String USERNAME_KEY = "username";

  private static final String PASSWORD_KEY = "password";

  private final ClientFactoryFactory clientFactoryFactory;

  private String endpointUrl;

  private String configurationPid;

  private String authenticationType;

  private String username;

  private String password;

  private String bodyExpansion;

  private String expandedSections = "";

  private Boolean includeArchivedSpaces = false;

  private Boolean includePageContent = false;

  private Boolean excludeSpaces = false;

  private List<String> confluenceSpaces = new ArrayList<>();

  private final EncryptionService encryptionService;

  private final FilterAdapter filterAdapter;

  private final ResourceReader resourceReader;

  private final ConfluenceInputTransformer transformer;

  private SecureCxfClientFactory<SearchResource> factory;

  private boolean lastAvailable;

  private Date lastAvailableDate = null;

  private long availabilityPollInterval = TimeUnit.SECONDS.toMillis(60);

  private Set<SourceMonitor> sourceMonitors = new HashSet<>();

  private Map<String, Set<String>> attributeOverrides = new HashMap<>();

  private AttributeRegistry attributeRegistry;

  public ConfluenceSource(
      FilterAdapter adapter,
      EncryptionService encryptionService,
      ConfluenceInputTransformer transformer,
      ResourceReader reader,
      AttributeRegistry attributeRegistry,
      ClientFactoryFactory clientFactoryFactory) {
    this.filterAdapter = adapter;
    this.encryptionService = encryptionService;
    this.transformer = transformer;
    this.resourceReader = reader;
    this.attributeRegistry = attributeRegistry;
    this.clientFactoryFactory = clientFactoryFactory;
  }

  public void init() {
    if (StringUtils.isBlank(endpointUrl)) {
      return;
    }

    if (BASIC.equals(authenticationType)
        && StringUtils.isNotBlank(username)
        && StringUtils.isNotBlank(password)) {
      SecurityLogger.audit("Setting up confluence client for user {}", username);
      factory =
          clientFactoryFactory.getSecureCxfClientFactory(
              endpointUrl, SearchResource.class, username, password);
    } else {
      SecurityLogger.audit("Setting up confluence client for anonymous access");
      factory = clientFactoryFactory.getSecureCxfClientFactory(endpointUrl, SearchResource.class);
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
  public synchronized boolean isAvailable() {
    boolean isAvailable = false;
    if (!lastAvailable
        || (lastAvailableDate.before(
            new Date(System.currentTimeMillis() - availabilityPollInterval)))) {

      Response response = null;

      try {
        response = getClientFactory().getWebClient().head();
      } catch (Exception e) {
        LOGGER.debug("Web Client was unable to connect to endpoint.", e);
      }

      if (response != null
          && !(response.getStatus() >= HttpStatus.SC_NOT_FOUND
              || response.getStatus() == HttpStatus.SC_BAD_REQUEST
              || response.getStatus() == HttpStatus.SC_PAYMENT_REQUIRED)) {
        isAvailable = true;
        lastAvailableDate = new Date();
      }
    } else {
      isAvailable = lastAvailable;
    }

    updateMonitors(isAvailable);

    lastAvailable = isAvailable;
    return isAvailable;
  }

  private void updateMonitors(boolean newAvailability) {
    if (lastAvailable != newAvailability) {
      for (SourceMonitor monitor : this.sourceMonitors) {
        if (newAvailability) {
          monitor.setAvailable();
        } else {
          monitor.setUnavailable();
        }
      }
    }
  }

  @Override
  public boolean isAvailable(SourceMonitor callback) {
    sourceMonitors.add(callback);
    return isAvailable();
  }

  @Override
  public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {
    Query query = request.getQuery();
    ConfluenceFilterDelegate confluenceDelegate = new ConfluenceFilterDelegate();

    String cql = filterAdapter.adapt(query, confluenceDelegate);
    if (!confluenceDelegate.isConfluenceQuery()
        || (StringUtils.isEmpty(cql)
            && (confluenceSpaces.isEmpty() || !confluenceDelegate.isWildCardQuery()))) {
      return new SourceResponseImpl(request, Collections.emptyList());
    }
    cql = getSortedQuery(query.getSortBy(), getSpaceQuery(cql));
    LOGGER.debug(cql);

    String finalExpandedSections = expandedSections;
    if (includePageContent && StringUtils.isNotBlank(bodyExpansion)) {
      finalExpandedSections += "," + bodyExpansion;
    }

    SearchResource confluence = getClientFactory().getClient();

    String cqlContext = null;
    String excerpt = null;
    Response confluenceResponse =
        confluence.search(
            cql,
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
      throw new UnsupportedQueryException(
          String.format(
              "Received error code from remote source (status %s ): %s",
              confluenceResponse.getStatus(), error));
    }

    try {

      List<Result> results =
          transformer
              .transformConfluenceResponse(stream, bodyExpansion)
              .stream()
              .map(this::getUpdatedResult)
              .collect(Collectors.toList());

      return new SourceResponseImpl(request, results);
    } catch (CatalogTransformerException e) {
      throw new UnsupportedQueryException("Exception processing results from Confluence");
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
      arguments.put(USERNAME_KEY, username);
      arguments.put(PASSWORD_KEY, password);
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
    if (endpointUrl != null) {
      endpointUrl = endpointUrl.trim();
    }
    this.endpointUrl = PropertyResolver.resolveProperties(endpointUrl);
    init();
  }

  public void setAuthenticationType(String authenticationType) {
    this.authenticationType = authenticationType;
    init();
  }

  public void setUsername(String username) {
    this.username = username;
    init();
  }

  public void setPassword(String password) {
    this.password = password;
    if (encryptionService != null) {
      this.password = encryptionService.decryptValue(password);
    }
    init();
  }

  public void setExpandedSections(List<String> expandedSections) {
    if (expandedSections == null) {
      this.expandedSections = "";
      return;
    }

    this.expandedSections = expandedSections.stream().collect(Collectors.joining(","));
  }

  public void setIncludeArchivedSpaces(Boolean includeArchivedSpaces) {
    this.includeArchivedSpaces = includeArchivedSpaces;
  }

  public void setIncludePageContent(Boolean includePageContent) {
    this.includePageContent = includePageContent;
  }

  public void setConfluenceSpaces(List<String> confluenceSpace) {
    this.confluenceSpaces = confluenceSpace;
  }

  public void setExcludeSpaces(Boolean excludeSpaces) {
    this.excludeSpaces = excludeSpaces;
  }

  public void setAttributeOverrides(List<String> attributes) {
    attributeOverrides = Permissions.parsePermissionsFromString(attributes);
  }

  public void setBodyExpansion(String bodyExpansion) {
    this.bodyExpansion = bodyExpansion;
  }

  public SecureCxfClientFactory<SearchResource> getClientFactory() {
    return factory;
  }

  private Result getUpdatedResult(Metacard metacard) {
    metacard.setSourceId(this.getId());
    overrideAttributes(metacard, attributeOverrides);

    return new ResultImpl(metacard);
  }

  private void overrideAttributes(Metacard metacard, Map<String, Set<String>> attributeOverrides) {
    if (MapUtils.isEmpty(attributeOverrides)) {
      return;
    }

    attributeOverrides
        .keySet()
        .stream()
        .map(attributeRegistry::lookup)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(ad -> overrideAttributeValue(ad, attributeOverrides.get(ad.getName())))
        .filter(Objects::nonNull)
        .forEach(metacard::setAttribute);
  }

  private AttributeImpl overrideAttributeValue(
      AttributeDescriptor attributeDescriptor, Set<String> overrideValue) {
    List<Serializable> newValue = new ArrayList<>();
    for (String override : overrideValue) {
      try {
        switch (attributeDescriptor.getType().getAttributeFormat()) {
          case INTEGER:
            newValue.add(Integer.parseInt(override));
            break;
          case FLOAT:
            newValue.add(Float.parseFloat(override));
            break;
          case DOUBLE:
            newValue.add(Double.parseDouble(override));
            break;
          case SHORT:
            newValue.add(Short.parseShort(override));
            break;
          case LONG:
            newValue.add(Long.parseLong(override));
            break;
          case DATE:
            Calendar calendar = DatatypeConverter.parseDateTime(override);
            newValue.add(calendar.getTime());
            break;
          case BOOLEAN:
            newValue.add(Boolean.parseBoolean(override));
            break;
          case BINARY:
            newValue.add(override.getBytes(Charset.forName("UTF-8")));
            break;
          case OBJECT:
          case STRING:
          case GEOMETRY:
          case XML:
            newValue.add(override);
            break;
        }
      } catch (IllegalArgumentException e) {
        LOGGER.debug(
            "IllegalArgument value [{}] for attribute type [{}] found when performing overrides for [{}]",
            override,
            attributeDescriptor.getType().getAttributeFormat(),
            attributeDescriptor.getName());
      }
    }
    return new AttributeImpl(attributeDescriptor.getName(), newValue);
  }

  private String getSortedQuery(SortBy sort, String query) {
    if (sort != null
        && sort.getPropertyName() != null
        && sort.getPropertyName().getPropertyName() != null) {
      String sortProperty = sort.getPropertyName().getPropertyName();
      if (ConfluenceFilterDelegate.QUERY_PARAMETERS.containsKey(sortProperty)) {
        query =
            String.format(
                "%s order by %s %s",
                query,
                ConfluenceFilterDelegate.QUERY_PARAMETERS.get(sortProperty).getParamterName(),
                sort.getSortOrder().toSQL());
      }
    }
    return query;
  }

  private String getSpaceQuery(String query) {
    if (!confluenceSpaces.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      sb.append(query);
      if (StringUtils.isNotEmpty(query.trim())) {
        sb.append(" AND ");
      }
      sb.append("space");
      if (excludeSpaces) {
        sb.append(" NOT IN (");
      } else {
        sb.append(" IN (");
      }
      sb.append(String.join(", ", confluenceSpaces));
      sb.append(")");
      return sb.toString();
    }
    return query;
  }
}
