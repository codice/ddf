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
package org.codice.ddf.catalog.plugin.validationfilter;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Validation;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.delegate.ValidationQueryDelegate;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.Request;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreFederatedQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.Source;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
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

public class ValidationFilterPlugin implements PreFederatedQueryPlugin {
  private static final Logger LOGGER = LoggerFactory.getLogger(ValidationFilterPlugin.class);

  private static final String ENTERING = "ENTERING {}";

  private static final String EXITING = "EXITING {}";

  private FilterAdapter filterAdapter;

  private FilterBuilder filterBuilder;

  private static Map<String, List<String>> attributeMap = new HashMap<>();

  private boolean showErrors = false;

  private boolean showWarnings = false;

  public ValidationFilterPlugin(FilterAdapter filterAdapter, FilterBuilder filterBuilder) {
    LOGGER.trace("INSIDE: QueryRequestValidityFilterPlugin constructor");
    this.filterAdapter = filterAdapter;
    this.filterBuilder = filterBuilder;
  }

  public Map<String, List<String>> getAttributeMap() {
    return attributeMap;
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
          Arrays.asList(keyValue[1].split(","))
              .stream()
              .map(String::trim)
              .collect(Collectors.toList()));
    }
  }

  public void setAttributeMap(Map<String, List<String>> attributeMap) {
    ValidationFilterPlugin.attributeMap = attributeMap;
  }

  @Override
  public QueryRequest process(Source source, QueryRequest input)
      throws PluginExecutionException, StopProcessingException {
    String methodName = "process";
    LOGGER.trace(ENTERING, methodName);
    QueryRequest newQueryRequest = input;

    if (input != null) {
      Query query = input.getQuery();

      try {
        // already contain validation error or warning filter no need to modify filter
        if (filterAdapter.adapt(query, new ValidationQueryDelegate())) {
          return input;
        }
      } catch (UnsupportedQueryException e) {
        LOGGER.info("This attribute filter is not supported by ValidationQueryDelegate.", e);
      }

      if (query != null) {

        Subject subject = getSubject(input);
        HashMap<String, Set<String>> securityMap = new HashMap<>();

        for (Map.Entry<String, List<String>> attributeMapping : attributeMap.entrySet()) {
          securityMap.put(attributeMapping.getKey(), new HashSet<>(attributeMapping.getValue()));
        }

        Attribute attr = new AttributeImpl(Metacard.SECURITY, securityMap);
        boolean subjectCanViewInvalidData =
            checkPermissions(attr, subject, CollectionPermission.READ_ACTION);

        List<Filter> filters = new ArrayList<>();
        if (!subjectCanViewInvalidData || !showErrors) {
          filters.add(filterBuilder.attribute(Validation.VALIDATION_ERRORS).is().empty());
        }
        if (!subjectCanViewInvalidData || !showWarnings) {
          filters.add(filterBuilder.attribute(Validation.VALIDATION_WARNINGS).is().empty());
        }

        if (!filters.isEmpty()) {
          // Create a new QueryRequest using the modified filter and the attributes from
          // the original query
          QueryImpl newQuery =
              new QueryImpl(
                  filterBuilder.allOf(filterBuilder.allOf(filters), query),
                  query.getStartIndex(),
                  query.getPageSize(),
                  query.getSortBy(),
                  query.requestsTotalResultsCount(),
                  query.getTimeoutMillis());
          newQueryRequest =
              new QueryRequestImpl(
                  newQuery, input.isEnterprise(), input.getSourceIds(), input.getProperties());
        }
      }
    }

    LOGGER.trace(EXITING, methodName);

    return newQueryRequest;
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

  private boolean checkPermissions(Attribute attr, Subject subject, String action) {

    Map<String, Set<String>> map = null;
    KeyValueCollectionPermission securityPermission;

    if (attr != null) {
      map = (Map<String, Set<String>>) attr.getValue();
    }
    if (MapUtils.isNotEmpty(map)) {
      securityPermission = new KeyValueCollectionPermission(action, map);
    } else {
      securityPermission = new KeyValueCollectionPermission(action);
    }
    return subject.isPermitted(securityPermission);
  }

  public boolean isShowErrors() {
    return showErrors;
  }

  public void setShowErrors(boolean showErrors) {
    this.showErrors = showErrors;
  }

  public boolean isShowWarnings() {
    return showWarnings;
  }

  public void setShowWarnings(boolean showWarnings) {
    this.showWarnings = showWarnings;
  }
}
