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
package ddf.catalog.metacard.validation;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Validation;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.Request;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.PreFederatedLocalProviderQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.plugin.impl.PolicyResponseImpl;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.Source;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.impl.KeyValueCollectionPermissionImpl;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ddf.catalog.plugin.PreFederatedQueryPlugin} and {@link PolicyPlugin} which performs
 * filtering of {@link Metacard}s. For the {@link ddf.catalog.plugin.PreFederatedQueryPlugin}, the
 * {@link Query}'s filter is modified, which can help improve performance. For the {@link
 * PolicyPlugin}, filtering is done post-query on the resulting {@link Metacard}s from the query.
 *
 * <p>User roles are checked at both the {@link ddf.catalog.plugin.PreFederatedQueryPlugin} and
 * {@link PolicyPlugin} post-query points.
 */
public class MetacardValidityFilterPlugin extends PreFederatedLocalProviderQueryPlugin
    implements PolicyPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetacardValidityFilterPlugin.class);

  private final FilterBuilder filterBuilder;

  private Map<String, List<String>> attributeMap = new HashMap<>();

  private boolean filterErrors = true;

  private boolean filterWarnings = false;

  public MetacardValidityFilterPlugin(
      FilterBuilder filterBuilder, List<CatalogProvider> catalogProviders) {
    super(catalogProviders);
    this.filterBuilder = filterBuilder;
  }

  @Override
  public PolicyResponse processPreCreate(Metacard input, Map<String, Serializable> properties)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreUpdate(Metacard input, Map<String, Serializable> properties)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreDelete(
      List<Metacard> metacards, Map<String, Serializable> properties)
      throws StopProcessingException {
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

    if (input == null || input.getMetacard() == null) {
      return new PolicyResponseImpl();
    }
    Metacard metacard = input.getMetacard();

    Map<String, Set<String>> securityMap = new HashMap<>();
    if (isMarkedForFilter(filterErrors, metacard, Validation.VALIDATION_ERRORS)
        || isMarkedForFilter(filterWarnings, metacard, Validation.VALIDATION_WARNINGS)) {
      securityMap = getSecurityMapConfiguration();
    }

    return new PolicyResponseImpl(new HashMap<>(), securityMap);
  }

  /**
   * Modifies the incoming {@link QueryRequest}s filter depending upon the configuration for this
   * plugin. If a {@link Subject} contains the appropriate configured roles, it will always receive
   * {@link Metacard}s with errors and/or warnings, otherwise the {@link Subject} will receive
   * results based on the {@code filterErrors} and {@code filterWarnings} flags.
   *
   * @param source {@link Source} being queried
   * @param input {@link QueryRequest} being sent to the {@link Source}
   * @return new {@link QueryRequest} if the filter was modified, otherwise the original {@link
   *     QueryRequest}
   * @throws StopProcessingException if the {@link Subject} is not available
   */
  @Override
  public QueryRequest process(Source source, QueryRequest input) throws StopProcessingException {
    if (!isLocalSource(source)) {
      return input;
    }

    Query query = input.getQuery();
    if (query != null) {
      Subject subject = getSubject(input);
      boolean subjectCanViewInvalidData = checkReadPermissions(subject);

      List<Filter> filters = new ArrayList<>();
      if (!subjectCanViewInvalidData && filterErrors) {
        filters.add(filterBuilder.attribute(Validation.VALIDATION_ERRORS).is().empty());
      }
      if (!subjectCanViewInvalidData && filterWarnings) {
        filters.add(filterBuilder.attribute(Validation.VALIDATION_WARNINGS).is().empty());
      }

      if (!filters.isEmpty()) {
        // Create a new QueryRequest using the modified filter and the attributes from
        // the original query
        QueryImpl newQuery =
            new QueryImpl(
                filterBuilder.allOf(query, filterBuilder.allOf(filters)),
                query.getStartIndex(),
                query.getPageSize(),
                query.getSortBy(),
                query.requestsTotalResultsCount(),
                query.getTimeoutMillis());
        return new QueryRequestImpl(
            newQuery, input.isEnterprise(), input.getSourceIds(), input.getProperties());
      }
    }

    return input;
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

  private boolean checkReadPermissions(Subject subject) {
    Map<String, Set<String>> securityMap = getSecurityMapConfiguration();

    if (MapUtils.isNotEmpty(securityMap)) {
      KeyValueCollectionPermission securityPermission =
          new KeyValueCollectionPermissionImpl(CollectionPermission.READ_ACTION, securityMap);
      return subject.isPermitted(securityPermission);
    } else {
      return false;
    }
  }

  private Subject getSubject(Request input) throws StopProcessingException {
    Object securityAssertion = input.getProperties().get(SecurityConstants.SECURITY_SUBJECT);
    Subject subject;
    if (securityAssertion instanceof Subject) {
      subject = (Subject) securityAssertion;
      LOGGER.debug("Filter plugin found Subject for query response.");
    } else {
      throw new StopProcessingException(
          "Unable to filter contents of current message, no user Subject available.");
    }
    return subject;
  }

  private boolean isMarkedForFilter(boolean filterFlag, Metacard metacard, String attribute) {
    return filterFlag
        && metacard.getAttribute(attribute) != null
        && metacard.getAttribute(attribute).getValues() != null;
  }

  private HashMap<String, Set<String>> getSecurityMapConfiguration() {
    HashMap<String, Set<String>> securityMap = new HashMap<>();
    for (Map.Entry<String, List<String>> attributeMapping : getAttributeMap().entrySet()) {
      securityMap.put(attributeMapping.getKey(), new HashSet<>(attributeMapping.getValue()));
    }

    return securityMap;
  }

  public void setFilterErrors(boolean filterErrors) {
    this.filterErrors = filterErrors;
  }

  public boolean getFilterErrors() {
    return filterErrors;
  }

  public void setFilterWarnings(boolean filterWarnings) {
    this.filterWarnings = filterWarnings;
  }

  public boolean getFilterWarnings() {
    return filterWarnings;
  }

  public Map<String, List<String>> getAttributeMap() {
    return attributeMap;
  }

  /**
   * Even though Blueprint uses the other setter, at least one Setter method has to match the type
   * of the Getter method for property attributeMap or an exception will be thrown.
   */
  public void setAttributeMap(Map<String, List<String>> attributeMap) {
    this.attributeMap = attributeMap;
  }

  public void setAttributeMap(List<String> attributeMappings) {
    if (CollectionUtils.isEmpty(attributeMappings)
        || (attributeMappings.size() == 1 && attributeMappings.get(0).isEmpty())) {
      attributeMap = new HashMap<>();
      return;
    }
    for (String attributeMapping : attributeMappings) {
      String[] keyValue = attributeMapping.split("=");
      attributeMap.put(
          keyValue[0].trim(),
          Arrays.stream(keyValue[1].split(",")).map(String::trim).collect(Collectors.toList()));
    }
  }
}
