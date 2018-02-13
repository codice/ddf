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
package ddf.catalog.impl;

import ddf.catalog.CatalogFramework;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.impl.operations.CreateOperations;
import ddf.catalog.impl.operations.DeleteOperations;
import ddf.catalog.impl.operations.QueryOperations;
import ddf.catalog.impl.operations.ResourceOperations;
import ddf.catalog.impl.operations.SourceOperations;
import ddf.catalog.impl.operations.TransformOperations;
import ddf.catalog.impl.operations.UpdateOperations;
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
import ddf.catalog.util.impl.DescribableImpl;
import ddf.catalog.util.impl.Masker;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.codice.ddf.configuration.DictionaryMap;
import org.codice.ddf.configuration.SystemInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CatalogFrameworkImpl is the core class of DDF. It is used for query, create, update, delete, and
 * resource retrieval operations.
 */
@SuppressWarnings("deprecation")
public class CatalogFrameworkImpl extends DescribableImpl implements CatalogFramework {
  private static final Logger LOGGER = LoggerFactory.getLogger(CatalogFrameworkImpl.class);

  private static final String FANOUT_MESSAGE =
      "Fanout proxy does not support create, update, and delete operations";

  //
  // Injected properties
  //
  private boolean fanoutEnabled;

  private List<String> fanoutTagBlacklist = new ArrayList<>();

  private Masker masker;

  private CreateOperations createOperations;

  private UpdateOperations updateOperations;

  private DeleteOperations deleteOperations;

  private QueryOperations queryOperations;

  private ResourceOperations resourceOperations;

  private SourceOperations sourceOperations;

  private TransformOperations transformOperations;

  /**
   * Instantiates a new CatalogFrameworkImpl which delegates its work to surrogate operations
   * classes.
   *
   * @param createOperations delegate that handles create operations
   * @param updateOperations delegate that handles update operations
   * @param deleteOperations delegate that handles delete operations
   * @param queryOperations delegate that handles query operations
   * @param resourceOperations delegate that handles resource operations
   * @param sourceOperations delegate that handles source operations
   * @param transformOperations delegate that handles transformation operations
   */
  public CatalogFrameworkImpl(
      CreateOperations createOperations,
      UpdateOperations updateOperations,
      DeleteOperations deleteOperations,
      QueryOperations queryOperations,
      ResourceOperations resourceOperations,
      SourceOperations sourceOperations,
      TransformOperations transformOperations) {

    this.createOperations = createOperations;
    this.updateOperations = updateOperations;
    this.deleteOperations = deleteOperations;
    this.queryOperations = queryOperations;
    this.resourceOperations = resourceOperations;
    this.sourceOperations = sourceOperations;
    this.transformOperations = transformOperations;

    setId(SystemInfo.getSiteName());
    setVersion(SystemInfo.getVersion());
    setOrganization(SystemInfo.getOrganization());
    registerBasicMetacard();
  }

  private void registerBasicMetacard() {
    Bundle bundle = FrameworkUtil.getBundle(CatalogFrameworkImpl.class);
    if (bundle != null && bundle.getBundleContext() != null) {
      Dictionary<String, Object> properties = new DictionaryMap<>();
      properties.put("name", MetacardImpl.BASIC_METACARD.getName());
      bundle
          .getBundleContext()
          .registerService(MetacardType.class, MetacardImpl.BASIC_METACARD, properties);
    }
  }

  public QueryOperations getQueryOperations() {
    return queryOperations;
  }

  public ResourceOperations getResourceOperations() {
    return resourceOperations;
  }

  public SourceOperations getSourceOperations() {
    return sourceOperations;
  }

  public TransformOperations getTransformOperations() {
    return transformOperations;
  }

  public void setFanoutEnabled(boolean fanoutEnabled) {
    this.fanoutEnabled = fanoutEnabled;
  }

  public void setFanoutTagBlacklist(List<String> fanoutTagBlacklist) {
    this.fanoutTagBlacklist = fanoutTagBlacklist;
  }

  /**
   * Sets the {@link Masker}
   *
   * @param masker the {@link Masker} this framework will use
   */
  public void setMasker(Masker masker) {
    synchronized (this) {
      this.masker = masker;
      if (this.getId() != null) {
        masker.setId(getId());
      }
    }
  }

  /**
   * Sets the source id to identify this framework (DDF). This is also referred to as the site name.
   *
   * @param sourceId the sourceId to set
   */
  @Override
  public void setId(String sourceId) {
    LOGGER.debug("Setting id = {}", sourceId);
    synchronized (this) {
      super.setId(sourceId);
      if (masker != null) {
        masker.setId(sourceId);
      }

      // Set the id of the describable delegate objects
      if (queryOperations != null) {
        queryOperations.setId(sourceId);
      }
      if (resourceOperations != null) {
        resourceOperations.setId(sourceId);
      }
      if (sourceOperations != null) {
        sourceOperations.setId(sourceId);
      }
    }
  }

  @Override
  public Set<String> getSourceIds() {
    return sourceOperations.getSourceIds(fanoutEnabled);
  }

  @Override
  public SourceInfoResponse getSourceInfo(SourceInfoRequest sourceInfoRequest)
      throws SourceUnavailableException {
    return sourceOperations.getSourceInfo(sourceInfoRequest, fanoutEnabled);
  }

  @Override
  public CreateResponse create(CreateStorageRequest createRequest)
      throws IngestException, SourceUnavailableException {
    List<String> blacklist = Collections.emptyList();

    if (fanoutEnabled) {
      blacklist = new ArrayList<>(fanoutTagBlacklist);
    }

    return createOperations.create(createRequest, blacklist);
  }

  @Override
  public CreateResponse create(
      CreateStorageRequest createRequest, Map<String, ? extends Serializable> arguments)
      throws IngestException, SourceUnavailableException {
    List<String> blacklist = Collections.emptyList();

    if (fanoutEnabled) {
      blacklist = new ArrayList<>(fanoutTagBlacklist);
    }

    return createOperations.create(createRequest, blacklist, arguments);
  }

  @Override
  public CreateResponse create(CreateRequest createRequest)
      throws IngestException, SourceUnavailableException {
    if (fanoutEnabled && blockFanoutCreate(createRequest)) {
      throw new IngestException(FANOUT_MESSAGE);
    }

    return createOperations.create(createRequest);
  }

  @Override
  public UpdateResponse update(
      UpdateStorageRequest updateRequest, Map<String, ? extends Serializable> arguments)
      throws IngestException, SourceUnavailableException {
    if (fanoutEnabled && blockFanoutStorageRequest(updateRequest)) {
      throw new IngestException(FANOUT_MESSAGE);
    }

    return updateOperations.update(updateRequest, arguments);
  }

  @Override
  public UpdateResponse update(UpdateStorageRequest updateRequest)
      throws IngestException, SourceUnavailableException {
    return update(updateRequest, Collections.emptyMap());
  }

  @Override
  public UpdateResponse update(UpdateRequest updateRequest)
      throws IngestException, SourceUnavailableException {
    if (fanoutEnabled && blockFanoutUpdate(updateRequest)) {
      throw new IngestException(FANOUT_MESSAGE);
    }

    return updateOperations.update(updateRequest);
  }

  @Override
  public DeleteResponse delete(DeleteRequest deleteRequest)
      throws IngestException, SourceUnavailableException {

    List<String> blacklist = Collections.emptyList();

    if (fanoutEnabled) {
      blacklist = new ArrayList<>(fanoutTagBlacklist);
    }
    return deleteOperations.delete(deleteRequest, blacklist);
  }

  @Override
  public QueryResponse query(QueryRequest fedQueryRequest)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {
    return queryOperations.query(fedQueryRequest, fanoutEnabled);
  }

  @Override
  public QueryResponse query(QueryRequest queryRequest, FederationStrategy strategy)
      throws SourceUnavailableException, UnsupportedQueryException, FederationException {
    return queryOperations.query(queryRequest, strategy, fanoutEnabled);
  }

  @Override
  public BinaryContent transform(
      Metacard metacard, String transformerShortname, Map<String, Serializable> arguments)
      throws CatalogTransformerException {
    List<BinaryContent> binaryContents =
        transformOperations.transform(metacard, transformerShortname, arguments);
    if (CollectionUtils.isNotEmpty(binaryContents)) {
      return binaryContents.get(0);
    }
    throw new CatalogTransformerException("Unable to transform metacard.");
  }

  @Override
  public BinaryContent transform(
      SourceResponse response, String transformerShortname, Map<String, Serializable> arguments)
      throws CatalogTransformerException {
    return transformOperations.transform(response, transformerShortname, arguments);
  }

  @Override
  public ResourceResponse getLocalResource(ResourceRequest resourceRequest)
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
    return resourceOperations.getLocalResource(resourceRequest, fanoutEnabled);
  }

  @Override
  public ResourceResponse getResource(ResourceRequest resourceRequest, String resourceSiteName)
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
    return resourceOperations.getResource(resourceRequest, resourceSiteName, fanoutEnabled);
  }

  @Override
  public ResourceResponse getEnterpriseResource(ResourceRequest resourceRequest)
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
    return resourceOperations.getEnterpriseResource(resourceRequest, fanoutEnabled);
  }

  @Deprecated
  @Override
  public Map<String, Set<String>> getLocalResourceOptions(String metacardId)
      throws ResourceNotFoundException {
    return resourceOperations.getLocalResourceOptions(metacardId, fanoutEnabled);
  }

  @Deprecated
  @Override
  public Map<String, Set<String>> getEnterpriseResourceOptions(String metacardId)
      throws ResourceNotFoundException {
    return resourceOperations.getEnterpriseResourceOptions(metacardId, fanoutEnabled);
  }

  @Deprecated
  @Override
  public Map<String, Set<String>> getResourceOptions(String metacardId, String sourceId)
      throws ResourceNotFoundException {
    return resourceOperations.getResourceOptions(metacardId, sourceId, fanoutEnabled);
  }

  /** String representation of this {@code CatalogFrameworkImpl}. */
  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  private boolean blockFanoutStorageRequest(UpdateStorageRequest updateStorageRequest) {
    return blockFanoutContentItems(updateStorageRequest.getContentItems());
  }

  private boolean blockFanoutContentItems(List<ContentItem> contentItems) {
    return contentItems
        .stream()
        .map(ContentItem::getMetacard)
        .anyMatch(this::isMetacardBlacklisted);
  }

  private boolean blockFanoutCreate(CreateRequest createRequest) {
    return createRequest.getMetacards().stream().anyMatch(this::isMetacardBlacklisted);
  }

  private boolean blockFanoutUpdate(UpdateRequest updateRequest) {
    return updateRequest
        .getUpdates()
        .stream()
        .anyMatch((updateEntry) -> isMetacardBlacklisted(updateEntry.getValue()));
  }

  private boolean isMetacardBlacklisted(Metacard metacard) {
    Set<String> tags = new HashSet<>(metacard.getTags());

    // defaulting to resource tag if the metacard doesn't contain any tags
    if (tags.isEmpty()) {
      tags.add(Metacard.DEFAULT_TAG);
    }

    return CollectionUtils.containsAny(tags, fanoutTagBlacklist);
  }
}
