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
package ddf.catalog.source.solr;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.IndexQueryResponse;
import ddf.catalog.operation.IndexQueryResult;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.operation.impl.UpdateImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.operation.impl.UpdateResponseImpl;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.IndexProvider;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.StorageProvider;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.MaskableImpl;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common base class for all Catalog providers. This class utilize two providers: one for handling
 * index, one for handling the storage of data
 */
public abstract class AbstractCatalogProvider extends MaskableImpl implements CatalogProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCatalogProvider.class);

  private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

  private static final Properties DESCRIBABLE_PROPERTIES = new Properties();

  private static final int THRESHOLD = 1000;

  static {
    try (InputStream inputStream =
        AbstractCatalogProvider.class.getResourceAsStream(DESCRIBABLE_PROPERTIES_FILE)) {
      DESCRIBABLE_PROPERTIES.load(inputStream);
    } catch (IOException e) {
      LOGGER.info("Did not load properties properly.", e);
    }
  }

  private IndexProvider indexProvider;

  private StorageProvider storageProvider;

  private FilterBuilder filterBuilder;

  public AbstractCatalogProvider(
      IndexProvider indexProvider, StorageProvider storageProvider, FilterBuilder filterBuilder) {
    this.indexProvider = indexProvider;
    this.storageProvider = storageProvider;
    this.filterBuilder = filterBuilder;
    indexProvider.maskId(getId());
    storageProvider.maskId(getId());
  }

  @Override
  public void maskId(String id) {
    super.maskId(id);
    indexProvider.maskId(id);
    storageProvider.maskId(id);
  }

  /**
   * Used to signal to the Solr client to commit on every transaction. Updates the underlying {@link
   * ConfigurationStore} so that the property is propagated throughout the Solr Catalog Provider
   * code.
   *
   * @param forceAutoCommit {@code true} to force auto-commits
   */
  public void setForceAutoCommit(boolean forceAutoCommit) {
    ConfigurationStore.getInstance().setForceAutoCommit(forceAutoCommit);
    indexProvider.setForceAutoCommit(forceAutoCommit);
  }

  /**
   * Disables text path indexing for every subsequent update or insert.
   *
   * @param disableTextPath {@code true} to turn off text path indexing
   */
  public void setDisableTextPath(boolean disableTextPath) {
    ConfigurationStore.getInstance().setDisableTextPath(disableTextPath);
  }

  @Override
  public Set<ContentType> getContentTypes() {
    return storageProvider.getContentTypes();
  }

  @Override
  public boolean isAvailable() {
    return storageProvider.isAvailable() && indexProvider.isAvailable();
  }

  @Override
  public boolean isAvailable(SourceMonitor callback) {
    return storageProvider.isAvailable(callback);
  }

  @Override
  public SourceResponse query(QueryRequest queryRequest) throws UnsupportedQueryException {
    long startTime = System.currentTimeMillis();
    IndexQueryResponse indexQueryResponse = indexProvider.query(queryRequest);
    long indexElapsedTime = System.currentTimeMillis() - startTime;
    long numHits = 0;
    SourceResponse queryResponse =
        new QueryResponseImpl(
            queryRequest, Collections.emptyList(), true, numHits, queryRequest.getProperties());
    List<String> ids = null;
    if (indexQueryResponse != null) {
      numHits = indexQueryResponse.getHits();
      if (CollectionUtils.isNotEmpty(indexQueryResponse.getScoredResults())) {
        ids =
            indexQueryResponse
                .getScoredResults()
                .stream()
                .map(IndexQueryResult::getId)
                .collect(Collectors.toList());
        queryResponse =
            storageProvider.queryByIds(queryRequest, indexQueryResponse.getProperties(), ids);
      } else {
        queryResponse =
            new QueryResponseImpl(
                queryRequest, Collections.emptyList(), true, 0, indexQueryResponse.getProperties());
      }
    }

    if (LOGGER.isTraceEnabled()) {
      long totalElapsedTime = System.currentTimeMillis() - startTime;
      long queryElapsedTime = totalElapsedTime - indexElapsedTime;

      LOGGER.trace(
          "Query Index elapsed time {} ms and query storage elapsed time {} ms",
          indexElapsedTime,
          queryElapsedTime);

      if (indexElapsedTime > THRESHOLD) {
        LOGGER.trace(
            "Index query time was slow ({} ms): {}", indexElapsedTime, queryRequest.getQuery());
      }

      if (queryElapsedTime > THRESHOLD && indexQueryResponse != null && ids != null) {
        LOGGER.trace(
            "Storage query time was slow ({} ms), num records requested: {}",
            queryElapsedTime,
            ids.size());
      }
    }

    return new QueryResponseImpl(
        queryRequest, queryResponse.getResults(), true, numHits, queryResponse.getProperties());
  }

  @Override
  public String getDescription() {
    return DESCRIBABLE_PROPERTIES.getProperty("description", "");
  }

  @Override
  public String getOrganization() {
    return DESCRIBABLE_PROPERTIES.getProperty("organization", "");
  }

  @Override
  public String getTitle() {
    return DESCRIBABLE_PROPERTIES.getProperty("name", "");
  }

  @Override
  public String getVersion() {
    return DESCRIBABLE_PROPERTIES.getProperty("version", "");
  }

  @Override
  public CreateResponse create(CreateRequest createRequest) throws IngestException {
    long startTime = System.currentTimeMillis();
    CreateResponse createResponse = storageProvider.create(createRequest);
    long storageElapsedTime = System.currentTimeMillis() - startTime;
    // create index only for those inserted metacard
    if (createResponse != null && !createResponse.getCreatedMetacards().isEmpty()) {
      CreateRequest indexRequest = new CreateRequestImpl(createResponse.getCreatedMetacards());
      indexProvider.create(indexRequest);
    }
    long totalElapsedTime = System.currentTimeMillis() - startTime;
    LOGGER.trace(
        "Create Index elapsed time {} ms and create storage elapsed time {} ms",
        totalElapsedTime - storageElapsedTime,
        storageElapsedTime);
    return createResponse;
  }

  @Override
  public DeleteResponse delete(DeleteRequest deleteRequest) throws IngestException {
    indexProvider.delete(deleteRequest);
    DeleteResponse deleteResponse = storageProvider.delete(deleteRequest);
    return deleteResponse;
  }

  @Override
  public UpdateResponse update(UpdateRequest updateRequest) throws IngestException {
    if (updateRequest.getAttributeName().equals(UpdateRequest.UPDATE_BY_ID)) {
      // We can directly perform updates in the storage provider and index provider in this case
      UpdateResponse response = storageProvider.update(updateRequest);
      indexProvider.update(updateRequest);
      return response;
    }

    // If not ID update, then we query for metacards by attribute name/values, create ID based
    // update
    // for storage and index providers.
    String attributeName = updateRequest.getAttributeName();
    List<Filter> attributeFilters = new ArrayList<>();
    for (Entry<Serializable, Metacard> entry : updateRequest.getUpdates()) {
      attributeFilters.add(
          filterBuilder
              .attribute(attributeName)
              .is()
              .equalTo()
              .text(String.valueOf(entry.getKey())));
    }

    QueryImpl query = new QueryImpl(filterBuilder.anyOf(attributeFilters));
    query.setPageSize(attributeFilters.size() + 1);

    QueryRequest queryRequest = new QueryRequestImpl(query);
    try {
      IndexQueryResponse queryResponse = indexProvider.query(queryRequest);
      List<String> metacardsToUpdate =
          queryResponse
              .getScoredResults()
              .stream()
              .map(IndexQueryResult::getId)
              .collect(Collectors.toList());
      if (metacardsToUpdate.size() > updateRequest.getUpdates().size()) {
        throw new IngestException(
            "Found more metacards than updated metacards provided. Please ensure your attribute values match unique records.");
      }

      SourceResponse storageResponse =
          storageProvider.queryByIds(queryRequest, Collections.emptyMap(), metacardsToUpdate);
      Map<Serializable, Metacard> oldMetacardByQueryAttribute =
          storageResponse
              .getResults()
              .stream()
              .map(Result::getMetacard)
              .collect(
                  Collectors.toMap(
                      mc -> mc.getAttribute(attributeName).getValue(), Function.identity()));
      List<Update> updateList = new ArrayList<>();
      List<Entry<Serializable, Metacard>> newUpdateEntryById = new ArrayList<>();
      for (Entry<Serializable, Metacard> newEntries : updateRequest.getUpdates()) {
        Metacard oldMetacard = oldMetacardByQueryAttribute.get(newEntries.getKey());
        MetacardImpl newMetacard = new MetacardImpl(newEntries.getValue());
        newMetacard.setId(oldMetacard.getId());
        newMetacard.setSourceId(getId());
        updateList.add(new UpdateImpl(newMetacard, oldMetacard));
        newUpdateEntryById.add(
            new AbstractMap.SimpleEntry<Serializable, Metacard>(newMetacard.getId(), newMetacard));
      }

      UpdateRequest updateByIdRequest =
          new UpdateRequestImpl(
              newUpdateEntryById, UpdateRequestImpl.UPDATE_BY_ID, updateRequest.getProperties());
      storageProvider.update(updateByIdRequest);
      indexProvider.update(updateByIdRequest);

      return new UpdateResponseImpl(updateRequest, updateRequest.getProperties(), updateList);
    } catch (UnsupportedQueryException e) {
      LOGGER.debug("Unable to query for metacards to update: {}", query, e);
      throw new IngestException("Unable to query for metacards to update: " + query, e);
    }
  }

  public void setStorageProvider(StorageProvider storageProvider) {
    LOGGER.warn("Setting storage provider: {}", storageProvider.getClass().getName());
    storageProvider.maskId(getId());
    this.storageProvider = storageProvider;
  }

  public void setIndexProvider(IndexProvider indexProvider) {
    LOGGER.warn("Setting index provider: {}", indexProvider.getClass().getName());
    indexProvider.maskId(getId());
    this.indexProvider = indexProvider;
  }

  /** Shuts down the connection to Solr and releases resources. */
  public void shutdown() {
    indexProvider.shutdown();
    storageProvider.shutdown();
  }
}
