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
package ddf.catalog.security.filter.plugin;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.Request;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.Response;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.security.FilterResult;
import ddf.catalog.security.FilterStrategy;
import ddf.catalog.util.impl.ServiceComparator;
import ddf.security.SecurityConstants;
import ddf.security.SubjectUtils;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.security.Security;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link AccessPlugin} performs redaction and filtering on {@link QueryResponse} objects as
 * they pass through the framework.
 */
public class FilterPlugin implements AccessPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(FilterPlugin.class);

  private Map<ServiceReference, FilterStrategy> filterStrategies =
      Collections.synchronizedMap(new TreeMap<>(new ServiceComparator()));

  private Security security;

  public FilterPlugin(Security security) {
    this.security = security;
  }

  public void addStrategy(ServiceReference<FilterStrategy> filterStrategyRef) {
    Bundle bundle = FrameworkUtil.getBundle(FilterPlugin.class);
    if (bundle != null) {
      FilterStrategy filterStrategy = bundle.getBundleContext().getService(filterStrategyRef);
      filterStrategies.put(filterStrategyRef, filterStrategy);
    }
  }

  public void removeStrategy(ServiceReference<FilterStrategy> filterStrategyRef) {
    filterStrategies.remove(filterStrategyRef);
  }

  protected Subject getSystemSubject() {
    return AccessController.doPrivileged(
        (PrivilegedAction<Subject>) () -> security.runAsAdmin(security::getSystemSubject));
  }

  @Override
  public CreateRequest processPreCreate(CreateRequest input) throws StopProcessingException {
    KeyValueCollectionPermission securityPermission =
        new KeyValueCollectionPermission(CollectionPermission.CREATE_ACTION);
    List<Metacard> metacards = input.getMetacards();
    Subject subject = getSubject(input);
    Subject systemSubject = getSystemSubject();
    List<String> userNotPermittedTitles = new ArrayList<>();
    List<String> systemNotPermittedTitles = new ArrayList<>();
    for (Metacard metacard : metacards) {
      Attribute attr = metacard.getAttribute(Metacard.SECURITY);
      if (!checkPermissions(
          attr, securityPermission, subject, CollectionPermission.CREATE_ACTION)) {
        userNotPermittedTitles.add(metacard.getTitle());
      }
      if (!checkPermissions(
          attr, securityPermission, systemSubject, CollectionPermission.CREATE_ACTION)) {
        systemNotPermittedTitles.add(metacard.getTitle());
      }
    }
    if (!userNotPermittedTitles.isEmpty()) {
      throw new StopProcessingException(
          "Metacard creation not permitted for "
              + SubjectUtils.getName(subject)
              + ": [ "
              + listToString(userNotPermittedTitles)
              + " ]");
    }
    if (!systemNotPermittedTitles.isEmpty()) {
      throw new StopProcessingException(
          "Metacard creation not permitted for this system: [ "
              + listToString(systemNotPermittedTitles)
              + " ]");
    }

    return input;
  }

  @Override
  public UpdateRequest processPreUpdate(UpdateRequest input, Map<String, Metacard> metacards)
      throws StopProcessingException {
    KeyValueCollectionPermission securityPermission =
        new KeyValueCollectionPermission(CollectionPermission.UPDATE_ACTION);
    List<Map.Entry<Serializable, Metacard>> updates = input.getUpdates();
    Subject subject = getSubject(input);
    Subject systemSubject = getSystemSubject();
    List<String> unknownIds = new ArrayList<>();
    List<String> userNotPermittedIds = new ArrayList<>();
    List<String> systemNotPermittedIds = new ArrayList<>();
    for (Map.Entry<Serializable, Metacard> entry : updates) {
      Metacard newMetacard = entry.getValue();
      Attribute attr = newMetacard.getAttribute(Metacard.SECURITY);
      String id = null;
      if (entry.getKey() != null && !entry.getKey().equals("null")) {
        id = (String) entry.getKey();
      } else if (newMetacard.getId() != null && !newMetacard.getId().equals("null")) {
        id = newMetacard.getId();
      }
      Metacard oldMetacard = metacards.get(id);
      if (oldMetacard == null) {
        unknownIds.add(id);
      } else {
        Attribute oldAttr = oldMetacard.getAttribute(Metacard.SECURITY);
        if (!checkPermissions(attr, securityPermission, subject, CollectionPermission.UPDATE_ACTION)
            || !checkPermissions(
                oldAttr, securityPermission, subject, CollectionPermission.UPDATE_ACTION)) {
          userNotPermittedIds.add(newMetacard.getId());
        }
        if (!checkPermissions(
            attr, securityPermission, systemSubject, CollectionPermission.UPDATE_ACTION)) {
          systemNotPermittedIds.add(newMetacard.getId());
        }
      }
    }
    if (!unknownIds.isEmpty() || !userNotPermittedIds.isEmpty()) {
      throw new StopProcessingException(
          "Update operation not permitted with bad data. Unknown metacards: [ "
              + listToString(unknownIds)
              + " ]. Not Permitted metacards: [ "
              + listToString(userNotPermittedIds)
              + " ]");
    }
    if (!systemNotPermittedIds.isEmpty()) {
      throw new StopProcessingException(
          "Update operation not permitted for this system metacards: [ "
              + listToString(systemNotPermittedIds)
              + " ]");
    }
    return input;
  }

  @Override
  public DeleteRequest processPreDelete(DeleteRequest input) throws StopProcessingException {
    return input;
  }

  @Override
  public DeleteResponse processPostDelete(DeleteResponse input) throws StopProcessingException {
    if (input.getRequest() == null || input.getRequest().getProperties() == null) {
      throw new StopProcessingException(
          "Unable to filter contents of current message, no user Subject available.");
    }
    Subject subject = getSubject(input);

    List<Metacard> results = input.getDeletedMetacards();
    List<Metacard> newResults = new ArrayList<>(results.size());
    KeyValueCollectionPermission securityPermission =
        new KeyValueCollectionPermission(CollectionPermission.READ_ACTION);
    int filteredMetacards = 0;
    for (Metacard metacard : results) {
      Attribute attr = metacard.getAttribute(Metacard.SECURITY);
      if (!checkPermissions(attr, securityPermission, subject, CollectionPermission.READ_ACTION)) {
        for (FilterStrategy filterStrategy : filterStrategies.values()) {
          FilterResult filterResult = filterStrategy.process(input, metacard);
          if (filterResult.processed()) {
            if (filterResult.metacard() != null) {
              newResults.add(filterResult.metacard());
            }
            break;
            // returned responses are ignored for deletes
          }
        }
        filteredMetacards++;
      } else {
        newResults.add(metacard);
      }
    }

    if (filteredMetacards > 0) {
      SecurityLogger.audit(
          "Filtered " + filteredMetacards + " metacards, returned " + newResults.size(), subject);
    }

    input.getDeletedMetacards().clear();
    input.getDeletedMetacards().addAll(newResults);
    newResults.clear();
    return input;
  }

  @Override
  public QueryRequest processPreQuery(QueryRequest input) throws StopProcessingException {
    return input;
  }

  @Override
  public QueryResponse processPostQuery(QueryResponse input) throws StopProcessingException {
    if (input.getRequest() == null || input.getRequest().getProperties() == null) {
      throw new StopProcessingException(
          "Unable to filter contents of current message, no user Subject available.");
    }
    Subject subject = getSubject(input);

    List<Result> results = input.getResults();
    List<Result> newResults = new ArrayList<>(results.size());
    Metacard metacard;
    KeyValueCollectionPermission securityPermission =
        new KeyValueCollectionPermission(CollectionPermission.READ_ACTION);
    int filteredMetacards = 0;
    for (Result result : results) {
      metacard = result.getMetacard();
      Attribute attr = metacard.getAttribute(Metacard.SECURITY);
      if (!checkPermissions(attr, securityPermission, subject, CollectionPermission.READ_ACTION)) {
        for (FilterStrategy filterStrategy : filterStrategies.values()) {
          FilterResult filterResult = filterStrategy.process(input, metacard);
          if (filterResult.processed()) {
            if (filterResult.metacard() != null) {
              newResults.add(new ResultImpl(filterResult.metacard()));
            }
            break;
            // returned responses are ignored for queries
          }
        }
        filteredMetacards++;
      } else {
        newResults.add(result);
      }
    }

    if (filteredMetacards > 0) {
      SecurityLogger.audit(
          "Filtered " + filteredMetacards + " metacards, returned " + newResults.size(), subject);
    }

    input.getResults().clear();
    input.getResults().addAll(newResults);
    newResults.clear();
    return input;
  }

  @Override
  public ResourceRequest processPreResource(ResourceRequest input) throws StopProcessingException {
    return input;
  }

  @Override
  public ResourceResponse processPostResource(ResourceResponse input, Metacard metacard)
      throws StopProcessingException {
    if (input.getRequest() == null || input.getRequest().getProperties() == null) {
      throw new StopProcessingException(
          "Unable to filter contents of current message, no user Subject available.");
    }
    KeyValueCollectionPermission securityPermission =
        new KeyValueCollectionPermission(CollectionPermission.READ_ACTION);
    Subject subject = getSubject(input);
    Attribute attr = metacard.getAttribute(Metacard.SECURITY);
    if (!checkPermissions(attr, securityPermission, subject, CollectionPermission.READ_ACTION)) {
      for (FilterStrategy filterStrategy : filterStrategies.values()) {
        FilterResult filterResult = filterStrategy.process(input, metacard);
        if (filterResult.processed()) {
          if (filterResult.response() == null) {
            throw new StopProcessingException("Subject not permitted to receive resource");
          } else {
            input = (ResourceResponse) filterResult.response();
          }
          break;
          // returned metacards are ignored for resource requests
        }
      }
      if (filterStrategies.isEmpty()) {
        throw new StopProcessingException("Subject not permitted to receive resource");
      }
    }
    return input;
  }

  private String listToString(List<String> list) {
    StringJoiner stringJoiner = new StringJoiner(",");
    list.forEach(stringJoiner::add);
    return stringJoiner.toString();
  }

  private Subject getSubject(Response input) throws StopProcessingException {
    return getSubject(input.getRequest());
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

  private boolean checkPermissions(
      Attribute attr,
      KeyValueCollectionPermission securityPermission,
      Subject subject,
      String action) {
    Map<String, Set<String>> map = null;

    if (attr != null) {
      map = (Map<String, Set<String>>) attr.getValue();
    }
    if (map != null) {
      securityPermission = new KeyValueCollectionPermission(action, map);
    }
    return subject.isPermitted(securityPermission);
  }
}
