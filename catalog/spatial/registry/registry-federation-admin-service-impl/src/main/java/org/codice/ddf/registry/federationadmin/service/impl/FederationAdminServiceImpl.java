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
package org.codice.ddf.registry.federationadmin.service.impl;

import com.google.common.collect.ImmutableSet;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.impl.PropertyNameImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.security.service.SecurityServiceException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.common.metacard.RegistryUtility;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminService;
import org.codice.ddf.registry.schemabindings.helper.MetacardMarshaller;
import org.codice.ddf.security.common.Security;
import org.geotools.filter.SortByImpl;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PackageAccessibility")
public class FederationAdminServiceImpl implements FederationAdminService {

  private static final Logger LOGGER = LoggerFactory.getLogger(FederationAdminServiceImpl.class);

  private static final int PAGE_SIZE = 1000;

  private final Security security;

  private CatalogFramework catalogFramework;

  private InputTransformer registryTransformer;

  private MetacardMarshaller metacardMarshaller;

  private FilterBuilder filterBuilder;

  public FederationAdminServiceImpl() {
    this(Security.getInstance());
  }

  FederationAdminServiceImpl(Security security) {
    this(null, security);
  }

  FederationAdminServiceImpl(CatalogFramework catalogFramework) {
    this(catalogFramework, Security.getInstance());
  }

  FederationAdminServiceImpl(CatalogFramework catalogFramework, Security security) {
    if (catalogFramework != null) {
      this.catalogFramework = catalogFramework;
    }
    this.security = security;
  }

  @Override
  public String addRegistryEntry(String xml) throws FederationAdminException {
    Metacard metacard = getRegistryMetacardFromString(xml);
    return addRegistryEntry(metacard);
  }

  @Override
  public String addRegistryEntry(String xml, Set<String> destinations)
      throws FederationAdminException {
    Metacard metacard = getRegistryMetacardFromString(xml);
    return addRegistryEntry(metacard, destinations);
  }

  @Override
  public String addRegistryEntry(Metacard metacard) throws FederationAdminException {
    return addRegistryEntry(metacard, null);
  }

  @Override
  public List<String> addRegistryEntries(List<Metacard> metacards, Set<String> destinations)
      throws FederationAdminException {
    validateRegistryMetacards(metacards);

    List<String> registryIds;
    Map<String, Serializable> properties = new HashMap<>();
    CreateRequest createRequest = new CreateRequestImpl(metacards, properties, destinations);

    try {
      CreateResponse createResponse =
          security.runWithSubjectOrElevate(() -> catalogFramework.create(createRequest));
      // loop through to get id's
      if (!createResponse.getProcessingErrors().isEmpty()) {
        throw new FederationAdminException(
            "Processing error occurred while creating registry entry. Details:"
                + System.lineSeparator()
                + stringifyProcessingErrors(createResponse.getProcessingErrors()));
      }
      registryIds =
          createResponse
              .getCreatedMetacards()
              .stream()
              .filter(RegistryUtility::isRegistryMetacard)
              .map(RegistryUtility::getRegistryId)
              .collect(Collectors.toList());
    } catch (SecurityServiceException | InvocationTargetException e) {
      throw new FederationAdminException("Error adding local registry entry.", e);
    }

    return registryIds;
  }

  @Override
  public String addRegistryEntry(Metacard metacard, Set<String> destinations)
      throws FederationAdminException {
    List<String> registryIds =
        addRegistryEntries(Collections.singletonList(metacard), destinations);
    if (CollectionUtils.isNotEmpty(registryIds)) {
      return registryIds.get(0);
    }
    return null;
  }

  @Override
  public void updateRegistryEntry(String xml) throws FederationAdminException {
    Metacard updateMetacard = getRegistryMetacardFromString(xml);
    updateRegistryEntry(updateMetacard);
  }

  @Override
  public void updateRegistryEntry(String xml, Set<String> destinations)
      throws FederationAdminException {
    Metacard updateMetacard = getRegistryMetacardFromString(xml);
    updateRegistryEntry(updateMetacard, destinations);
  }

  @Override
  public void updateRegistryEntry(Metacard updateMetacard) throws FederationAdminException {
    updateRegistryEntry(updateMetacard, null);
  }

  @Override
  public void updateRegistryEntry(Metacard updateMetacard, Set<String> destinations)
      throws FederationAdminException {
    validateRegistryMetacards(Collections.singletonList(updateMetacard));

    Map<String, Serializable> properties = new HashMap<>();
    String mcardId = updateMetacard.getId();
    if (isRemoteMetacard(updateMetacard) || CollectionUtils.isNotEmpty(destinations)) {

      Filter idFilter =
          filterBuilder
              .attribute(RegistryObjectMetacardType.REMOTE_METACARD_ID)
              .is()
              .equalTo()
              .text(updateMetacard.getId());
      Filter tagFilter =
          filterBuilder
              .attribute(Metacard.TAGS)
              .is()
              .like()
              .text(RegistryConstants.REGISTRY_TAG_INTERNAL);
      List<Metacard> results =
          this.getRegistryMetacardsByFilter(filterBuilder.allOf(tagFilter, idFilter), destinations);
      if (results.size() != 1) {
        throw new FederationAdminException("Could not find metacard to update.");
      }
      mcardId = results.get(0).getId();

      LOGGER.debug("Looked up remote-mcard-id {} and got id {}", updateMetacard.getId(), mcardId);
    }

    List<Map.Entry<Serializable, Metacard>> updateList = new ArrayList<>();
    updateList.add(new AbstractMap.SimpleEntry<>(mcardId, updateMetacard));

    properties.put(
        "operation.query-tags",
        ImmutableSet.of(RegistryConstants.REGISTRY_TAG, RegistryConstants.REGISTRY_TAG_INTERNAL));

    UpdateRequest updateRequest =
        new UpdateRequestImpl(updateList, Metacard.ID, properties, destinations);

    try {
      UpdateResponse updateResponse =
          security.runWithSubjectOrElevate(() -> catalogFramework.update(updateRequest));
      if (!updateResponse.getProcessingErrors().isEmpty()) {
        throw new FederationAdminException(
            "Processing error occurred while updating registry entry. Details:"
                + System.lineSeparator()
                + stringifyProcessingErrors(updateResponse.getProcessingErrors()));
      }
    } catch (SecurityServiceException | InvocationTargetException e) {
      String message = "Error updating registry entry.";
      LOGGER.debug("{} Metacard ID: {}", message, updateMetacard.getId());
      throw new FederationAdminException(message, e);
    }
  }

  @Override
  public void deleteRegistryEntriesByRegistryIds(List<String> registryIds)
      throws FederationAdminException {
    deleteRegistryEntriesByRegistryIds(registryIds, null);
  }

  @Override
  public void deleteRegistryEntriesByRegistryIds(List<String> registryIds, Set<String> destinations)
      throws FederationAdminException {
    if (CollectionUtils.isEmpty(registryIds)) {
      throw new FederationAdminException(
          "An empty list of registry ids to be deleted was received. Nothing to delete.");
    }

    List<Serializable> serializableIds = new ArrayList<>(registryIds);
    Map<String, Serializable> properties = new HashMap<>();

    String deleteField = RegistryObjectMetacardType.REGISTRY_ID;
    if (CollectionUtils.isNotEmpty(destinations)) {
      deleteField = Metacard.ID;
      try {
        List<Metacard> localMetacards =
            security.runWithSubjectOrElevate(
                () -> this.getRegistryMetacardsByRegistryIds(registryIds));
        List<Filter> idFilters =
            localMetacards
                .stream()
                .map(
                    e ->
                        filterBuilder
                            .attribute(RegistryObjectMetacardType.REMOTE_METACARD_ID)
                            .is()
                            .equalTo()
                            .text(e.getId()))
                .collect(Collectors.toList());
        Filter baseFilter =
            filterBuilder.allOf(getBasicFilter(RegistryConstants.REGISTRY_TAG_INTERNAL));
        List<Metacard> toDelete =
            security.runWithSubjectOrElevate(
                () ->
                    this.getRegistryMetacardsByFilter(
                        filterBuilder.allOf(baseFilter, filterBuilder.anyOf(idFilters)),
                        destinations));
        serializableIds = toDelete.stream().map(e -> e.getId()).collect(Collectors.toList());
      } catch (SecurityServiceException | InvocationTargetException e) {
        throw new FederationAdminException("Error looking up metacards to delete.", e);
      }
    }
    properties.put(
        "operation.query-tags",
        ImmutableSet.of(RegistryConstants.REGISTRY_TAG, RegistryConstants.REGISTRY_TAG_INTERNAL));
    DeleteRequest deleteRequest =
        new DeleteRequestImpl(serializableIds, deleteField, properties, destinations);
    try {
      DeleteResponse deleteResponse =
          security.runWithSubjectOrElevate(() -> catalogFramework.delete(deleteRequest));
      if (!deleteResponse.getProcessingErrors().isEmpty()) {
        throw new FederationAdminException(
            "Processing error occurred while deleting registry entry. Details:"
                + System.lineSeparator()
                + stringifyProcessingErrors(deleteResponse.getProcessingErrors()));
      }
    } catch (SecurityServiceException | InvocationTargetException e) {
      String message = "Error deleting registry entries by registry id.";
      LOGGER.debug("{} Registry Ids provided: {}", message, registryIds);
      throw new FederationAdminException(message, e);
    }
  }

  @Override
  public void deleteRegistryEntriesByMetacardIds(List<String> metacardIds)
      throws FederationAdminException {
    deleteRegistryEntriesByMetacardIds(metacardIds, null);
  }

  @Override
  public void deleteRegistryEntriesByMetacardIds(List<String> metacardIds, Set<String> destinations)
      throws FederationAdminException {
    if (CollectionUtils.isEmpty(metacardIds)) {
      throw new FederationAdminException(
          "An empty list of metacard ids to be deleted was received. Nothing to delete.");
    }

    List<Serializable> serializableIds = new ArrayList<>(metacardIds);
    Map<String, Serializable> properties = new HashMap<>();
    DeleteRequest deleteRequest =
        new DeleteRequestImpl(serializableIds, Metacard.ID, properties, destinations);
    try {
      DeleteResponse deleteResponse =
          security.runWithSubjectOrElevate(() -> catalogFramework.delete(deleteRequest));
      if (!deleteResponse.getProcessingErrors().isEmpty()) {
        throw new FederationAdminException(
            "Processing error occurred while deleting registry entry. Details"
                + System.lineSeparator()
                + stringifyProcessingErrors(deleteResponse.getProcessingErrors()));
      }
    } catch (SecurityServiceException | InvocationTargetException e) {
      String message = "Error deleting registry entries by metacard ids.";
      LOGGER.debug("{} Metacard Ids provided: {}", message, metacardIds);
      throw new FederationAdminException(message, e);
    }
  }

  @Override
  public List<Metacard> getRegistryMetacards() throws FederationAdminException {

    Filter filter = filterBuilder.allOf(getBasicFilter());

    return getRegistryMetacardsByFilter(filter);
  }

  @Override
  public List<Metacard> getInternalRegistryMetacards() throws FederationAdminException {
    return getRegistryMetacardsByFilter(
        filterBuilder.allOf(getBasicFilter(RegistryConstants.REGISTRY_TAG_INTERNAL)));
  }

  @Override
  public List<Metacard> getInternalRegistryMetacardsByRegistryId(String registryId)
      throws FederationAdminException {
    List<Filter> filters = getBasicFilter(RegistryConstants.REGISTRY_TAG_INTERNAL);
    filters.add(
        filterBuilder
            .attribute(RegistryObjectMetacardType.REGISTRY_ID)
            .is()
            .equalTo()
            .text(registryId));
    return getRegistryMetacardsByFilter(filterBuilder.allOf(filters));
  }

  @Override
  public List<Metacard> getRegistryMetacards(Set<String> destinations)
      throws FederationAdminException {
    Filter filter = filterBuilder.allOf(getBasicFilter());

    return getRegistryMetacardsByFilter(filter, destinations);
  }

  @Override
  public List<Metacard> getLocalRegistryMetacards() throws FederationAdminException {

    List<Filter> filters = getBasicFilter();
    filters.add(
        filterBuilder.attribute(RegistryObjectMetacardType.REGISTRY_LOCAL_NODE).is().bool(true));
    Filter filter = filterBuilder.allOf(filters);
    return getRegistryMetacardsByFilter(filter);
  }

  @Override
  public List<Metacard> getLocalRegistryMetacardsByRegistryIds(List<String> ids)
      throws FederationAdminException {
    if (CollectionUtils.isEmpty(ids)) {
      throw new FederationAdminException(
          "Error getting local registry metacards by registry ids. Null list of Ids provided.");
    }
    List<Filter> idFilters =
        ids.stream()
            .map(
                id ->
                    filterBuilder
                        .attribute(RegistryObjectMetacardType.REGISTRY_ID)
                        .is()
                        .equalTo()
                        .text(id))
            .collect(Collectors.toList());
    List<Filter> filters = getBasicFilter();
    filters.add(
        filterBuilder.attribute(RegistryObjectMetacardType.REGISTRY_LOCAL_NODE).is().bool(true));
    Filter filter = filterBuilder.allOf(filters);
    filter = filterBuilder.allOf(filter, filterBuilder.anyOf(idFilters));
    return getRegistryMetacardsByFilter(filter);
  }

  @Override
  public List<Metacard> getRegistryMetacardsByRegistryIds(List<String> ids)
      throws FederationAdminException {
    return getRegistryMetacardsByRegistryIds(ids, false);
  }

  @Override
  public List<Metacard> getRegistryMetacardsByRegistryIds(List<String> ids, boolean includeInternal)
      throws FederationAdminException {
    if (CollectionUtils.isEmpty(ids)) {
      throw new FederationAdminException(
          "Error getting registry metacards by registry ids. Null list of Ids provided.");
    }

    List<Filter> idFilters =
        ids.stream()
            .map(
                id ->
                    filterBuilder
                        .attribute(RegistryObjectMetacardType.REGISTRY_ID)
                        .is()
                        .equalTo()
                        .text(id))
            .collect(Collectors.toList());
    List<Filter> filters = getBasicFilter();
    Filter filter = filterBuilder.allOf(filters);
    if (includeInternal) {
      filter =
          filterBuilder.anyOf(
              filter, filterBuilder.allOf(getBasicFilter(RegistryConstants.REGISTRY_TAG_INTERNAL)));
    }
    filter = filterBuilder.allOf(filter, filterBuilder.anyOf(idFilters));
    return getRegistryMetacardsByFilter(filter);
  }

  @Override
  public List<RegistryPackageType> getLocalRegistryObjects() throws FederationAdminException {
    List<RegistryPackageType> registryEntries = new ArrayList<>();

    for (Metacard metacard : getLocalRegistryMetacards()) {
      registryEntries.add(getRegistryPackageFromMetacard(metacard));
    }
    return registryEntries;
  }

  @Override
  public List<RegistryPackageType> getRegistryObjects() throws FederationAdminException {
    List<RegistryPackageType> registryEntries = new ArrayList<>();
    for (Metacard metacard : getRegistryMetacards()) {
      registryEntries.add(getRegistryPackageFromMetacard(metacard));
    }
    return registryEntries;
  }

  @Override
  public RegistryPackageType getRegistryObjectByRegistryId(String registryId)
      throws FederationAdminException {
    return getRegistryObjectByRegistryId(registryId, null);
  }

  @Override
  public RegistryPackageType getRegistryObjectByRegistryId(String registryId, Set<String> sourceIds)
      throws FederationAdminException {
    if (StringUtils.isBlank(registryId)) {
      throw new FederationAdminException(
          "Error getting registry object by metacard id. Empty id provided.");
    }
    List<Filter> filters = getBasicFilter();
    filters.add(
        filterBuilder
            .attribute(RegistryObjectMetacardType.REGISTRY_ID)
            .is()
            .equalTo()
            .text(registryId));

    Filter filter = filterBuilder.allOf(filters);

    List<Metacard> metacards = getRegistryMetacardsByFilter(filter, sourceIds);

    if (CollectionUtils.isEmpty(metacards)) {
      String message = "Error getting registry object by metacard id. No result returned.";
      LOGGER.debug("{} For metacard ID: {}, optional sources: {}", message, registryId, sourceIds);
      throw new FederationAdminException(message);
    }

    if (metacards.size() > 1) {
      String message =
          "Error getting registry object by metacard id. More than one metacards were returned.";
      LOGGER.debug("{} For metacard ID: {}, optional sources: {}", message, registryId, sourceIds);
      throw new FederationAdminException(message);
    }
    return getRegistryPackageFromMetacard(metacards.get(0));
  }

  @Override
  public Optional<Metacard> getLocalRegistryIdentityMetacard() throws FederationAdminException {
    Optional<Metacard> metacardOptional = Optional.empty();

    List<Filter> filters = getBasicFilter();
    filters.add(
        filterBuilder.attribute(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE).is().bool(true));
    Filter filter = filterBuilder.allOf(filters);

    List<Metacard> identityMetacards = getRegistryMetacardsByFilter(filter);
    if (CollectionUtils.isNotEmpty(identityMetacards)) {

      if (identityMetacards.size() > 1) {
        String message = "Error getting registry identity metacard. More than one result found.";
        LOGGER.debug("{} Found these: {}", message, identityMetacards);
        throw new FederationAdminException(message);
      }

      metacardOptional = Optional.of(identityMetacards.get(0));
    }

    return metacardOptional;
  }

  private RegistryPackageType getRegistryPackageFromMetacard(Metacard metacard)
      throws FederationAdminException {
    try {
      return metacardMarshaller.getRegistryPackageFromMetacard(metacard);
    } catch (ParserException e) {
      throw new FederationAdminException("Error parsing ebrim xml from metacard", e);
    }
  }

  private void validateRegistryMetacards(List<Metacard> metacards) throws FederationAdminException {

    if (metacards == null) {
      throw new FederationAdminException(
          "Error creating local registry entry. Null metacard provided.");
    }
    for (Metacard metacard : metacards) {
      if (metacard == null) {
        throw new FederationAdminException("ValidationError: Metacard was null.");
      }
      if (!RegistryUtility.isRegistryMetacard(metacard)
          && !RegistryUtility.isInternalRegistryMetacard(metacard)) {
        throw new FederationAdminException(
            "ValidationError: Metacard does not have a registry tag and/or registry id.");
      }
    }
  }

  private List<Metacard> getRegistryMetacardsByFilter(Filter filter)
      throws FederationAdminException {
    return getRegistryMetacardsByFilter(filter, null);
  }

  private List<Metacard> getRegistryMetacardsByFilter(Filter filter, Set<String> sourceIds)
      throws FederationAdminException {
    if (filter == null) {
      throw new FederationAdminException("Error getting registry metacards. Null filter provided.");
    }
    PropertyName propertyName = new PropertyNameImpl(Metacard.MODIFIED);
    SortBy sortBy = new SortByImpl(propertyName, SortOrder.ASCENDING);
    QueryImpl query = new QueryImpl(filter);
    query.setSortBy(sortBy);
    query.setPageSize(PAGE_SIZE);
    QueryRequest queryRequest = new QueryRequestImpl(query, sourceIds);

    try {
      QueryResponse queryResponse =
          security.runWithSubjectOrElevate(() -> catalogFramework.query(queryRequest));
      return queryResponse
          .getResults()
          .stream()
          .map(Result::getMetacard)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    } catch (SecurityServiceException | InvocationTargetException e) {
      String message = "Error querying for registry metacards.";
      LOGGER.debug("{} For Filter: {}", message, filter);
      throw new FederationAdminException(message, e);
    }
  }

  private Metacard getRegistryMetacardFromString(String xml) throws FederationAdminException {
    if (StringUtils.isBlank(xml)) {
      throw new FederationAdminException(
          "Error unmarshalling string to Metacard. Empty string was provided.");
    }
    Metacard metacard;

    try {
      metacard = registryTransformer.transform(IOUtils.toInputStream(xml));
    } catch (IOException | CatalogTransformerException e) {
      String message = "Error transforming xml string to metacard.";
      LOGGER.debug("{}. XML: {}", message, xml);
      throw new FederationAdminException(message);
    }

    return metacard;
  }

  BundleContext getBundleContext() {
    Bundle bundle = FrameworkUtil.getBundle(this.getClass());

    if (bundle != null) {
      return bundle.getBundleContext();
    }

    return null;
  }

  private String stringifyProcessingErrors(Set<ProcessingDetails> details) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    for (ProcessingDetails detail : details) {
      pw.append(detail.getSourceId());
      pw.append(":");
      if (detail.hasException()) {
        detail.getException().printStackTrace(pw);
      }
      pw.append(System.lineSeparator());
    }
    return pw.toString();
  }

  private List<Filter> getBasicFilter() {
    return getBasicFilter(RegistryConstants.REGISTRY_TAG);
  }

  private List<Filter> getBasicFilter(String tag) {
    List<Filter> filters = new ArrayList<>();
    filters.add(filterBuilder.attribute(Metacard.TAGS).is().equalTo().text(tag));
    return filters;
  }

  private boolean isRemoteMetacard(Metacard metacard) {
    return RegistryUtility.hasAttribute(metacard, RegistryObjectMetacardType.REMOTE_REGISTRY_ID);
  }

  public void setCatalogFramework(CatalogFramework catalogFramework) {
    this.catalogFramework = catalogFramework;
  }

  public void setFilterBuilder(FilterBuilder filterBuilder) {
    this.filterBuilder = filterBuilder;
  }

  public void setMetacardMarshaller(MetacardMarshaller helper) {
    this.metacardMarshaller = helper;
  }

  public void setRegistryTransformer(InputTransformer inputTransformer) {
    this.registryTransformer = inputTransformer;
  }
}
