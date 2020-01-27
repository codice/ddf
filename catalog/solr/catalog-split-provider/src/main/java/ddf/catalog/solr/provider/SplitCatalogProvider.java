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
package ddf.catalog.solr.provider;

import com.google.common.annotations.VisibleForTesting;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.Request;
import ddf.catalog.operation.Response;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.operation.impl.UpdateResponseImpl;
import ddf.catalog.solr.collection.SolrCollectionProvider;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.solr.ConfigurationStore;
import ddf.catalog.source.solr.DynamicSchemaResolver;
import ddf.catalog.source.solr.RealTimeGetDelegate;
import ddf.catalog.source.solr.SolrCatalogProviderImpl;
import ddf.catalog.source.solr.SolrFilterDelegateFactory;
import ddf.catalog.source.solr.SolrMetacardClientImpl;
import ddf.catalog.util.impl.MaskableImpl;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codice.solr.factory.SolrClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplitCatalogProvider extends MaskableImpl implements CatalogProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(SplitCatalogProvider.class);

  private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

  private static final Properties DESCRIBABLE_PROPERTIES = new Properties();

  static {
    try (InputStream inputStream =
        SplitCatalogProvider.class.getResourceAsStream(DESCRIBABLE_PROPERTIES_FILE)) {
      DESCRIBABLE_PROPERTIES.load(inputStream);
    } catch (IOException e) {
      LOGGER.info("Did not load properties properly.", e);
    }
  }

  protected static final String DEFAULT_COLLECTION = "default";

  protected static final String DEFAULT_QUERY_ALIAS = "catalog";

  protected static final String CATALOG_PREFIX_SEPARATOR = "_";

  @VisibleForTesting static final String GET_HANDLER_WORKAROUND_PROP = "getHandlerWorkaround";

  @VisibleForTesting static final String COLLECTION_ALIAS_PROP = "collectionAlias";

  protected final Map<String, SolrCatalogProviderImpl> catalogProviders = new ConcurrentHashMap<>();

  protected final SolrClientFactory clientFactory;

  protected final FilterAdapter filterAdapter;

  protected final SolrFilterDelegateFactory solrFilterDelegateFactory;

  protected final DynamicSchemaResolver resolver;

  protected final List<SolrCollectionProvider> solrCollectionProviders;

  private IdQueryResultSorter sorter = new IdQueryResultSorter();

  private boolean getHandlerWorkaround = true;

  private String collectionAlias = DEFAULT_QUERY_ALIAS;

  protected static final String COLLECTION_HINT = "collection-hint";

  private static final String ALIAS_PROP = "alias";

  /**
   * Constructor that creates a new instance and allows for a custom {@link DynamicSchemaResolver}
   *
   * @param clientFactory Solr client factory
   * @param adapter injected implementation of FilterAdapter
   * @param resolver Solr schema resolver
   * @param solrCollectionProviders collection provider list for each solr collection
   */
  public SplitCatalogProvider(
      SolrClientFactory clientFactory,
      FilterAdapter adapter,
      SolrFilterDelegateFactory solrFilterDelegateFactory,
      DynamicSchemaResolver resolver,
      List<SolrCollectionProvider> solrCollectionProviders) {
    Validate.notNull(clientFactory, "SolrClientFactory cannot be null.");
    Validate.notNull(adapter, "FilterAdapter cannot be null");
    Validate.notNull(solrFilterDelegateFactory, "SolrFilterDelegateFactory cannot be null");
    Validate.notNull(resolver, "DynamicSchemaResolver cannot be null");
    Validate.notEmpty(solrCollectionProviders, "CollectionProviders cannot be empty");

    this.clientFactory = clientFactory;
    this.filterAdapter = adapter;
    this.solrFilterDelegateFactory = solrFilterDelegateFactory;
    this.resolver = resolver;
    this.solrCollectionProviders = solrCollectionProviders;

    initProviders();
  }

  /**
   * Convenience constructor that creates a new ddf.catalog.source.solr.DynamicSchemaResolver
   *
   * @param clientFactory Solr client factory
   * @param adapter injected implementation of FilterAdapter
   */
  public SplitCatalogProvider(
      SolrClientFactory clientFactory,
      FilterAdapter adapter,
      SolrFilterDelegateFactory solrFilterDelegateFactory,
      List<SolrCollectionProvider> solrCollectionProviders) {
    this(
        clientFactory,
        adapter,
        solrFilterDelegateFactory,
        new DynamicSchemaResolver(),
        solrCollectionProviders);
  }

  protected SolrCatalogProviderImpl newProvider(String core) {
    Map<String, Object> properties = new HashMap<>();
    properties.put(ALIAS_PROP, getCollectionAlias());
    return new SolrCatalogProviderImpl(
        clientFactory.newClient(core, properties),
        filterAdapter,
        solrFilterDelegateFactory,
        resolver);
  }

  @Override
  public CreateResponse create(CreateRequest createRequest) throws IngestException {
    LOGGER.trace("Create request received");
    return (CreateResponse) executeRequest(createRequest);
  }

  @Override
  public UpdateResponse update(UpdateRequest updateRequest) throws IngestException {
    LOGGER.trace("Update request received");
    return (UpdateResponse) executeRequest(updateRequest);
  }

  @Override
  public DeleteResponse delete(DeleteRequest deleteRequest) throws IngestException {
    /** Delete across all collections Assuming id uniqueness is preserved across all collection */
    int numItems = deleteRequest.getAttributeValues().size();
    List<DeleteResponse> responses = new ArrayList<>();
    for (Map.Entry<String, SolrCatalogProviderImpl> entry : catalogProviders.entrySet()) {
      responses.add(entry.getValue().delete(deleteRequest));
      LOGGER.debug(
          "Sending delete request for {} items to collection: {}", numItems, entry.getKey());
    }
    return responses.isEmpty()
        ? new DeleteResponseImpl(deleteRequest, null, new ArrayList<>())
        : responses.get(0);
  }

  @Override
  public SourceResponse query(QueryRequest queryRequest) throws UnsupportedQueryException {
    Boolean doRealTimeGet = filterAdapter.adapt(queryRequest.getQuery(), new RealTimeGetDelegate());

    queryRequest.getProperties().put(SolrMetacardClientImpl.DO_REALTIME_GET, doRealTimeGet);

    SourceResponse hintedResponse = handleHintedQuery(queryRequest);
    if (hintedResponse != null) {
      return hintedResponse;
    }

    SourceResponse getHandlerResponse = handleGetHandlerQuery(queryRequest);
    if (getHandlerResponse != null) {
      return getHandlerResponse;
    }

    if (BooleanUtils.isTrue(doRealTimeGet)) {
      LOGGER.trace("NRT Query alias directly (/get)");
    } else {
      LOGGER.trace("Query the collection alias: {}", getCollectionAlias());
    }

    ensureDefaultCollectionExists();

    return catalogProviders
        .computeIfAbsent(getCollectionAlias(), this::newProvider)
        .query(queryRequest);
  }

  private SourceResponse handleHintedQuery(QueryRequest queryRequest)
      throws UnsupportedQueryException {
    // Query the only provided collection hint
    String hintCollection = (String) queryRequest.getPropertyValue(COLLECTION_HINT);
    if (StringUtils.isNotBlank(hintCollection)) {
      LOGGER.trace(
          "Querying collection provided via property ({}): {}", COLLECTION_HINT, hintCollection);

      SolrCatalogProviderImpl provider = catalogProviders.get(hintCollection);

      if (provider == null) {
        LOGGER.warn(
            "Handling Hinted Query: unable to find catalog provider for {}", hintCollection);
        return null;
      }

      return provider.query(queryRequest);
    }
    return null;
  }

  /**
   * Solr doesn’t support querying the /get handler via the alias, so instances where we need the
   * NRT information from Solr we have to get the collections in the alias and manually query the
   * /get handler. In the case of ingesting/uploading a image and query by id using the getHandler
   * via alias right away, the record is not found. To handle getHandlerWorkaround, need to handle
   * real time query up front
   */
  private SourceResponse handleGetHandlerQuery(QueryRequest queryRequest)
      throws UnsupportedQueryException {
    if ((Boolean)
            queryRequest.getProperties().getOrDefault(SolrMetacardClientImpl.DO_REALTIME_GET, false)
        && getHandlerWorkaround) {
      List<Result> results = new ArrayList<>();
      long totalHits = 0;
      LOGGER.trace("Using custom query for /get handler workaround");
      for (SolrCatalogProviderImpl provider : getAliasProviderNonAlias().values()) {
        long providerStart = System.currentTimeMillis();

        SourceResponse response = provider.query(queryRequest);
        results.addAll(response.getResults());
        totalHits += response.getHits();

        long total = System.currentTimeMillis() - providerStart;
        LOGGER.trace("Provider took {} ms to respond", total);
      }
      List<Result> topResults =
          getLimitedScoredResults(results, queryRequest.getQuery().getPageSize());

      return new SourceResponseImpl(queryRequest, topResults, totalHits);
    }
    return null;
  }

  public void shutdown() {
    LOGGER.debug("Closing down Solr client.");
    this.catalogProviders.forEach((k, p) -> p.shutdown());
  }

  @Override
  public void maskId(String id) {
    super.maskId(id);
    catalogProviders.forEach((k, p) -> p.maskId(id));
  }

  @Override
  public boolean isAvailable() {
    return catalogProviders
        .values()
        .stream()
        .map(p -> p.isAvailable())
        .reduce(true, (a, b) -> a && b);
  }

  @Override
  public boolean isAvailable(SourceMonitor callback) {
    return catalogProviders
        .values()
        .stream()
        .map(p -> p.isAvailable(callback))
        .reduce(true, (a, b) -> a && b);
  }

  public void setGetHandlerWorkaround(boolean getHandlerWorkaround) {
    this.getHandlerWorkaround = getHandlerWorkaround;
  }

  public void setCollectionAlias(String collectionAlias) {
    LOGGER.info("Setting collection alias to: {}", collectionAlias);

    if (StringUtils.isNotBlank(collectionAlias)) {
      this.collectionAlias = collectionAlias;
    }
  }

  public boolean isGetHandlerWorkaround() {
    return getHandlerWorkaround;
  }

  public String getCollectionAlias() {
    return collectionAlias;
  }

  private String getDefaultCollection() {
    return getCatalogPrefix() + DEFAULT_COLLECTION;
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
  }

  public void refresh(Map<String, Object> configuration) {
    if (configuration == null) {
      LOGGER.debug("Null configuration");
      return;
    }

    setGetHandlerWorkaround(
        BooleanUtils.toBoolean((Boolean) configuration.get(GET_HANDLER_WORKAROUND_PROP)));

    boolean reinitCollection = false;
    String collectionAlias = (String) configuration.get(COLLECTION_ALIAS_PROP);
    if (StringUtils.isNotBlank(collectionAlias)) {
      setCollectionAlias(collectionAlias);
      reinitCollection = true;
    }

    if (reinitCollection) {
      catalogProviders.clear();
      initProviders();
    }
  }

  protected Response executeRequest(Request request) throws IngestException {
    if (request instanceof CreateRequest) {
      Map<String, Serializable> props = new HashMap<>();
      List<Metacard> createdMetacards = new ArrayList<>();
      Set<ProcessingDetails> errors = new HashSet<>();
      Map<String, CreateRequest> requests = getCreateRequests((CreateRequest) request);
      for (Map.Entry<String, CreateRequest> entry : requests.entrySet()) {
        CreateResponse response =
            catalogProviders
                .computeIfAbsent(entry.getKey(), this::newProvider)
                .create(entry.getValue());
        props.putAll(response.getProperties());
        createdMetacards.addAll(response.getCreatedMetacards());
        errors.addAll(response.getProcessingErrors());
      }

      return new CreateResponseImpl((CreateRequest) request, props, createdMetacards, errors);
    } else if (request instanceof UpdateRequest) {
      Map<String, UpdateRequest> requests = getUpdateRequests((UpdateRequest) request);
      Map<String, Serializable> props = new HashMap<>();
      List<Metacard> updatedMetacards = new ArrayList<>();
      List<Metacard> oldMetacards = new ArrayList<>();
      Set<ProcessingDetails> errors = new HashSet<>();
      for (Map.Entry<String, UpdateRequest> entry : requests.entrySet()) {
        SolrCatalogProviderImpl provider =
            catalogProviders.computeIfAbsent(entry.getKey(), this::newProvider);
        UpdateResponse response = provider.update(entry.getValue());
        if (response.getProperties() != null) {
          props.putAll(response.getProperties());
        }
        for (Update update : response.getUpdatedMetacards()) {
          updatedMetacards.add(update.getNewMetacard());
          oldMetacards.add(update.getOldMetacard());
        }
        errors.addAll(response.getProcessingErrors());
      }

      return new UpdateResponseImpl(
          (UpdateRequest) request, props, updatedMetacards, oldMetacards, errors);
    }

    return null;
  }

  /**
   * From a list of tags extracted from a metacard, return the associate solr core assuming ...
   *
   * @param metacard The metacard used to determine collection
   * @return the first associate solr core for those given tags
   */
  private String getCollection(Metacard metacard) {
    LOGGER.trace("Getting collection for metacard: {}", metacard);
    List<String> matchedCollections = new ArrayList<>();
    for (SolrCollectionProvider provider : solrCollectionProviders) {
      String collection = getCollectionName(provider.getCollection(metacard));
      if (collection != null) {
        matchedCollections.add(collection);
        createCollectionIfRequired(collection, provider);
      }
    }

    if (matchedCollections.isEmpty()) {
      return getDefaultCollection();
    }

    if (matchedCollections.size() == 1) {
      return matchedCollections.get(0);
    }
    // if there is more than once match, should not consider default collection
    matchedCollections.remove(getDefaultCollection());

    return matchedCollections.get(0);
  }

  /**
   * Create solr remote collection and a local client connection reference
   *
   * @param collection
   * @param provider
   */
  private void createCollectionIfRequired(String collection, SolrCollectionProvider provider) {
    if (StringUtils.isBlank(collection)) {
      return;
    }

    if (catalogProviders.containsKey(collection)) {
      return;
    }
    catalogProviders.computeIfAbsent(collection, this::newProvider);
  }

  private Map<String, CreateRequest> getCreateRequests(CreateRequest createRequest) {
    Map<String, CreateRequest> createRequests = new HashMap<>();
    Map<String, List<Metacard>> collectionMetacardMap = new HashMap<>();
    for (Metacard metacard : createRequest.getMetacards()) {
      String collection = getCollection(metacard);
      if (collectionMetacardMap.containsKey(collection)) {
        List<Metacard> metacards = collectionMetacardMap.get(collection);
        metacards.add(metacard);
      } else {
        List<Metacard> metacards = new ArrayList<>();
        metacards.add(metacard);
        collectionMetacardMap.put(collection, metacards);
      }
    }
    for (Map.Entry<String, List<Metacard>> entry : collectionMetacardMap.entrySet()) {
      CreateRequest request =
          new CreateRequestImpl(
              entry.getValue(), createRequest.getProperties(), createRequest.getStoreIds());
      createRequests.put(entry.getKey(), request);
    }

    return createRequests;
  }

  private Map<String, UpdateRequest> getUpdateRequests(UpdateRequest updateRequest) {
    Map<String, UpdateRequest> updateRequests = new HashMap<>();
    Map<String, List<Entry<Serializable, Metacard>>> collectionMetacardMap = new HashMap<>();
    for (Entry<Serializable, Metacard> entry : updateRequest.getUpdates()) {
      String collection = getCollection(entry.getValue());
      if (collectionMetacardMap.containsKey(collection)) {
        List<Entry<Serializable, Metacard>> collectionEntries =
            collectionMetacardMap.get(collection);
        collectionEntries.add(entry);
      } else {
        List<Entry<Serializable, Metacard>> collectionEntries = new ArrayList<>();
        collectionEntries.add(entry);
        collectionMetacardMap.put(collection, collectionEntries);
      }
    }
    for (Map.Entry<String, List<Entry<Serializable, Metacard>>> entry :
        collectionMetacardMap.entrySet()) {
      UpdateRequest request =
          new UpdateRequestImpl(
              entry.getValue(),
              updateRequest.getAttributeName(),
              updateRequest.getProperties(),
              updateRequest.getStoreIds());
      updateRequests.put(entry.getKey(), request);
    }

    return updateRequests;
  }

  private void ensureDefaultCollectionExists() {
    for (SolrCollectionProvider provider : solrCollectionProviders) {
      String collection = getCollectionName(provider.getCollection(null));
      createCollectionIfRequired(collection, provider);
    }
  }

  private Map<String, SolrCatalogProviderImpl> getAliasProviderNonAlias() {
    Map<String, SolrCatalogProviderImpl> providers = new HashMap<>(catalogProviders.size());
    for (Map.Entry<String, SolrCatalogProviderImpl> entry : catalogProviders.entrySet()) {
      if (!entry.getKey().equals(getCollectionAlias())) {
        providers.put(entry.getKey(), entry.getValue());
      }
    }
    return providers;
  }

  private String getCollectionName(String collection) {
    if (StringUtils.isBlank(collection)) {
      return null;
    }
    return getCatalogPrefix() + collection;
  }

  private void initProviders() {
    // Always add default provider
    ensureDefaultCollectionExists();
    // create a client connection for alias
    catalogProviders.computeIfAbsent(getCollectionAlias(), this::newProvider);
  }

  private List<Result> getLimitedScoredResults(List<Result> results, int num) {
    results.sort(sorter);
    return results.stream().limit(num).collect(Collectors.toList());
  }

  private String getCatalogPrefix() {
    return getCollectionAlias() + CATALOG_PREFIX_SEPARATOR;
  }

  private static class IdQueryResultSorter implements Comparator<Result> {

    @Override
    public int compare(Result o1, Result o2) {
      if (o1.getRelevanceScore() == null && o2.getRelevanceScore() == null) {
        return 0;
      } else if (o1.getRelevanceScore() == null && o2.getRelevanceScore() != null) {
        return 1;
      } else if (o1.getRelevanceScore() != null && o2.getRelevanceScore() == null) {
        return -1;
      } else
        return Objects.requireNonNull(o1.getRelevanceScore()).compareTo(o2.getRelevanceScore());
    }
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
    return this.catalogProviders.values().stream().map(p -> p.getContentTypes()).findAny().get();
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
}
