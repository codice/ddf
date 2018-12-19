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
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.Request;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.UpdateImpl;
import ddf.catalog.operation.impl.UpdateResponseImpl;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.MaskableImpl;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.codice.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link CatalogProvider} implementation using Apache Solr */
public class SolrCatalogProvider extends MaskableImpl implements CatalogProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrCatalogProvider.class);

  private static final String COULD_NOT_COMPLETE_DELETE_REQUEST_MESSAGE =
      "Could not complete delete request.";

  private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

  private static final String REQUEST_MUST_NOT_BE_NULL_MESSAGE = "Request must not be null";

  static final int MAX_BOOLEAN_CLAUSES = 2048;

  private static final Properties DESCRIBABLE_PROPERTIES = new Properties();

  static {
    try (InputStream propertiesStream =
        ddf.catalog.source.solr.SolrCatalogProvider.class.getResourceAsStream(
            DESCRIBABLE_PROPERTIES_FILE)) {
      DESCRIBABLE_PROPERTIES.load(propertiesStream);
    } catch (IOException e) {
      LOGGER.info("Failed to load describable properties", e);
    }
  }

  private final DynamicSchemaResolver resolver;

  private final SolrClient solr;

  private final SolrMetacardClientImpl client;

  private final FilterAdapter filterAdapter;

  /**
   * Constructor that creates a new instance and allows for a custom {@link DynamicSchemaResolver}
   *
   * @param solrClient Solr client
   * @param adapter injected implementation of FilterAdapter
   * @param resolver Solr schema resolver
   */
  public SolrCatalogProvider(
      SolrClient solrClient,
      FilterAdapter adapter,
      SolrFilterDelegateFactory solrFilterDelegateFactory,
      DynamicSchemaResolver resolver) {
    Validate.notNull(solrClient, "SolrClient cannot be null.");
    Validate.notNull(adapter, "FilterAdapter cannot be null");
    Validate.notNull(solrFilterDelegateFactory, "SolrFilterDelegateFactory cannot be null");
    Validate.notNull(resolver, "DynamicSchemaResolver cannot be null");
    this.solr = solrClient;
    this.filterAdapter = adapter;
    this.resolver = resolver;

    LOGGER.debug(
        "Constructing {} with Solr client [{}]", SolrCatalogProvider.class.getName(), solr);

    solr.whenAvailable(this::addFieldsFromClientToResolver);
    this.client =
        new ProviderSolrMetacardClient(solrClient, adapter, solrFilterDelegateFactory, resolver);
  }

  /**
   * Convenience constructor that creates a new ddf.catalog.source.solr.DynamicSchemaResolver
   *
   * @param solrClient Solr client
   * @param adapter injected implementation of FilterAdapter
   */
  public SolrCatalogProvider(
      SolrClient solrClient,
      FilterAdapter adapter,
      SolrFilterDelegateFactory solrFilterDelegateFactory) {
    this(solrClient, adapter, solrFilterDelegateFactory, new DynamicSchemaResolver());
  }

  @Override
  public Set<ContentType> getContentTypes() {
    return client.getContentTypes();
  }

  @Override
  @SuppressWarnings("squid:S1181" /* bubbling out VirtualMachineError */)
  public boolean isAvailable() {
    try {
      SolrPingResponse ping = solr.ping();

      return "OK".equals(ping.getResponse().get("status"));
    } catch (VirtualMachineError e) {
      throw e;
    } catch (Throwable t) {
      /*
       * if we get any type of exception, whether declared by Solr or not, we do not want to
       * fail, we just want to return false
       */
      LOGGER.debug("Solr ping failed.", t);
      LOGGER.warn(
          "Solr ping request/response failed while checking availability. Verify Solr is available and correctly configured.");
    }

    return false;
  }

  @Override
  public boolean isAvailable(SourceMonitor callback) {
    // first register the callback
    solr.isAvailable(new SolrClientListenerAdapter(callback));
    return isAvailable(); // then trigger an active ping
  }

  @Override
  public String getDescription() {
    return DESCRIBABLE_PROPERTIES.getProperty("description");
  }

  @Override
  public String getOrganization() {
    return DESCRIBABLE_PROPERTIES.getProperty("organization");
  }

  @Override
  public String getTitle() {
    return DESCRIBABLE_PROPERTIES.getProperty("name");
  }

  @Override
  public String getVersion() {
    return DESCRIBABLE_PROPERTIES.getProperty("version");
  }

  @Override
  public void maskId(String id) {
    LOGGER.trace("Sitename changed from [{}] to [{}]", getId(), id);
    super.maskId(id);
  }

  @Override
  public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {
    SourceResponse response = client.query(request);
    return response;
  }

  @Override
  public CreateResponse create(CreateRequest request) throws IngestException {
    nonNull(request);

    List<Metacard> metacards = request.getMetacards();
    List<Metacard> output = new ArrayList<>();

    if (metacards == null) {
      return new CreateResponseImpl(request, null, output);
    }

    for (Metacard metacard : metacards) {
      boolean isSourceIdSet =
          (metacard.getSourceId() != null && !"".equals(metacard.getSourceId()));
      /*
       * If an ID is not provided, then one is generated so that documents are unique. Solr
       * will not accept documents unless the id is unique.
       */
      if (metacard.getId() == null || metacard.getId().equals("")) {
        if (isSourceIdSet) {
          throw new IngestException("Metacard from a separate distribution must have ID");
        }
        metacard.setAttribute(new AttributeImpl(Metacard.ID, generatePrimaryKey()));
      }

      if (!isSourceIdSet) {
        metacard.setSourceId(getId());
      }
      output.add(metacard);
    }

    try {
      client.add(output, isForcedAutoCommit());
    } catch (SolrServerException | SolrException | IOException | MetacardCreationException e) {
      LOGGER.info("Solr could not ingest metacard(s) during create.", e);
      throw new IngestException("Could not ingest metacard(s).");
    }

    return new CreateResponseImpl(request, request.getProperties(), output);
  }

  @Override
  public UpdateResponse update(UpdateRequest updateRequest) throws IngestException {
    nonNull(updateRequest);

    List<Entry<Serializable, Metacard>> updates = updateRequest.getUpdates();

    // the list of updates, both new and old metacards
    List<Update> updateList = new ArrayList<>();

    String attributeName = updateRequest.getAttributeName();

    // need an attribute name in order to do query
    if (attributeName == null) {
      throw new IngestException(
          "Attribute name cannot be null. " + "Please provide the name of the attribute.");
    }

    // if we have nothing to update, send the empty list
    if (CollectionUtils.isEmpty(updates)) {
      return new UpdateResponseImpl(updateRequest, null, new ArrayList<>());
    }

    /* 1. QUERY */
    // Loop to get all identifiers
    Set<String> identifiers =
        updates.stream().map(Entry::getKey).map(Serializable::toString).collect(Collectors.toSet());

    Map<Serializable, Metacard> idToMetacardMap = new HashMap<>();
    if (Metacard.ID.equals(attributeName)) {
      try {
        idToMetacardMap =
            client
                .getIds(identifiers)
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Metacard::getId, Function.identity()));
      } catch (UnsupportedQueryException e) {
        LOGGER.debug("Solr query by list of IDs failed.", e);
        LOGGER.info("Failed to query for metacard(s) by ID before update.");
      }
    } else {
      /* 1a. Create the old Metacard Query */
      String attributeQuery = getQuery(attributeName, identifiers);

      SolrQuery query = new SolrQuery(attributeQuery);
      // Set number of rows to the result size + 1.  The default row size in Solr is 10, so this
      // needs to be set in situations where the number of metacards to update is > 10.  Since there
      // could be more results in the query response than the number of metacards in the update
      // request,
      // 1 is added to the row size, so we can still determine whether we found more metacards than
      // updated metacards provided
      query.setRows(updates.size() + 1);
      QueryResponse idResults = null;

      try {
        idResults = solr.query(query, METHOD.POST);
      } catch (SolrServerException | SolrException | IOException e) {
        LOGGER.info("Failed to query for metacard(s) before update.", e);
      }
      // map of old metacards to be populated
      idToMetacardMap.putAll(computeOldMetacardIds(attributeName, updates, idResults));
    }

    if (idToMetacardMap.isEmpty()) {
      LOGGER.debug("No results found for given attribute values.");
      // return an empty list
      return new UpdateResponseImpl(updateRequest, null, new ArrayList<>());
    }
    List<Metacard> newMetacards = computeMetacardsToUpdate(updates, idToMetacardMap, updateList);

    try {
      client.add(newMetacards, isForcedAutoCommit());
    } catch (SolrServerException | SolrException | IOException | MetacardCreationException e) {
      LOGGER.info("Failed to update metacard(s) with Solr.", e);
      throw new IngestException("Failed to update metacard(s).");
    }

    return new UpdateResponseImpl(updateRequest, updateRequest.getProperties(), updateList);
  }

  @Override
  public DeleteResponse delete(DeleteRequest deleteRequest) throws IngestException {
    nonNull(deleteRequest);

    String attributeName = deleteRequest.getAttributeName();
    if (StringUtils.isBlank(attributeName)) {
      throw new IngestException(
          "Attribute name cannot be empty. Please provide the name of the attribute.");
    }

    @SuppressWarnings("unchecked")
    List<? extends Serializable> identifiers = deleteRequest.getAttributeValues();
    List<Metacard> deletedMetacards = new ArrayList<>();

    if (CollectionUtils.isEmpty(identifiers)) {
      return new DeleteResponseImpl(deleteRequest, null, deletedMetacards);
    }

    if (identifiers.size() <= MAX_BOOLEAN_CLAUSES) {
      deleteListOfMetacards(deletedMetacards, identifiers, attributeName);
    } else {
      List<? extends Serializable> identifierPaged;
      int currPagingSize;

      for (currPagingSize = MAX_BOOLEAN_CLAUSES;
          currPagingSize < identifiers.size();
          currPagingSize += MAX_BOOLEAN_CLAUSES) {
        identifierPaged = identifiers.subList(currPagingSize - MAX_BOOLEAN_CLAUSES, currPagingSize);
        deleteListOfMetacards(deletedMetacards, identifierPaged, attributeName);
      }
      identifierPaged =
          identifiers.subList(currPagingSize - MAX_BOOLEAN_CLAUSES, identifiers.size());
      deleteListOfMetacards(deletedMetacards, identifierPaged, attributeName);
    }

    return new DeleteResponseImpl(deleteRequest, null, deletedMetacards);
  }

  private void addFieldsFromClientToResolver(SolrClient client) {
    try {
      resolver.addFieldsFromClient(client);
    } catch (SolrServerException | SolrException | IOException e) {
      // retry again when it comes back available
      client.whenAvailable(this::addFieldsFromClientToResolver);
    }
  }

  private List<Metacard> computeMetacardsToUpdate(
      List<Entry<Serializable, Metacard>> updates,
      Map<Serializable, Metacard> idToMetacardMap,
      List<Update> updateList) {
    List<Metacard> newMetacards = new ArrayList<>();

    for (Entry<Serializable, Metacard> updateEntry : updates) {
      String localKey = updateEntry.getKey().toString();
      MetacardImpl newMetacard = new MetacardImpl(updateEntry.getValue());
      // Find the exact oldMetacard that corresponds with this newMetacard
      Metacard oldMetacard = idToMetacardMap.get(localKey);

      // We need to skip because of partial updates such as one entry
      // matched but another did not
      if (oldMetacard != null) {
        // overwrite the id, in case it has not been done properly/already
        newMetacard.setId(oldMetacard.getId());
        newMetacard.setSourceId(getId());
        newMetacards.add(newMetacard);
        updateList.add(new UpdateImpl(newMetacard, oldMetacard));
      }
    }
    return newMetacards;
  }

  private Map<Serializable, Metacard> computeOldMetacardIds(
      String attributeName, List<Entry<Serializable, Metacard>> updates, QueryResponse idResults)
      throws IngestException {
    Map<Serializable, Metacard> idToMetacardMap = new HashMap<>();

    if (idResults != null) {
      final SolrDocumentList results = idResults.getResults();

      if (CollectionUtils.isNotEmpty(results)) {
        LOGGER.debug("Found {} current metacard(s).", results.size());

        // CHECK updates size assertion
        if (results.size() > updates.size()) {
          throw new IngestException(
              "Found more metacards than updated metacards provided. Please ensure your attribute values match unique records.");
        }

        for (SolrDocument doc : results) {
          final Metacard old = recreateMetacard(doc);
          final Serializable oldValue = old.getAttribute(attributeName).getValue();

          if (idToMetacardMap.putIfAbsent(oldValue, old) != null) {
            throw new IngestException(
                "The attribute value given ["
                    + oldValue
                    + "] matched multiple records. Attribute values must at most match only one unique Metacard.");
          }
        }
      }
    }
    return idToMetacardMap;
  }

  private Metacard recreateMetacard(SolrDocument doc) throws IngestException {
    try {
      return client.createMetacard(doc);
    } catch (MetacardCreationException e) {
      LOGGER.info("Unable to create metacard(s) from Solr responses during update.", e);
      throw new IngestException("Could not create metacard(s).");
    }
  }

  private void deleteListOfMetacards(
      List<Metacard> deletedMetacards,
      List<? extends Serializable> identifiers,
      String attributeName)
      throws IngestException {
    String fieldName = attributeName + SchemaFields.TEXT_SUFFIX;
    List<Metacard> metacards = getMetacards(identifiers, fieldName);
    deletedMetacards.addAll(metacards);

    try {
      // the assumption is if something was deleted, it should be gone
      // right away, such as expired data, etc.
      // so we force the commit
      client.deleteByIds(fieldName, identifiers, true);
    } catch (SolrServerException | SolrException | IOException e) {
      LOGGER.info("Failed to delete metacards by ID(s).", e);
      throw new IngestException(COULD_NOT_COMPLETE_DELETE_REQUEST_MESSAGE);
    }
  }

  private List<Metacard> getMetacards(
      List<? extends Serializable> identifierPaged, String fieldName) throws IngestException {
    if (fieldName.equals(Metacard.ID + SchemaFields.TEXT_SUFFIX)) {
      Set<String> ids =
          identifierPaged
              .stream()
              .filter(Objects::nonNull)
              .map(Object::toString)
              .collect(Collectors.toSet());
      try {
        return client.getIds(ids);
      } catch (UnsupportedQueryException e) {
        LOGGER.info("Failed to get list of Solr documents by ID for delete.", e);
        throw new IngestException(COULD_NOT_COMPLETE_DELETE_REQUEST_MESSAGE);
      }
    } else {
      try {
        return client.query(client.getIdentifierQuery(fieldName, identifierPaged));
      } catch (UnsupportedQueryException e) {
        LOGGER.info("Failed to get list of Solr documents for delete.", e);
        throw new IngestException(COULD_NOT_COMPLETE_DELETE_REQUEST_MESSAGE);
      }
    }
  }

  private String getQuery(String attributeName, Set<String> ids) throws IngestException {

    List<String> mappedNames = resolver.getAnonymousField(attributeName);

    if (mappedNames.isEmpty()) {
      throw new IngestException("Could not resolve attribute name [" + attributeName + "]");
    }

    String query =
        "{!terms cache=false separator=\" OR \" f="
            + mappedNames.get(0)
            + "}"
            + String.join(" OR ", ids);

    LOGGER.debug("query = [{}]", query);

    return query;
  }

  private String generatePrimaryKey() {
    return UUID.randomUUID().toString().replaceAll("-", "");
  }

  public boolean isForcedAutoCommit() {
    return ConfigurationStore.getInstance().isForceAutoCommit();
  }

  public void shutdown() {
    LOGGER.debug("Closing down Solr client.");
    try {
      solr.close();
    } catch (IOException e) {
      LOGGER.info("Failed to close Solr client during shutdown.", e);
    }
  }

  private static void nonNull(Request request) throws IngestException {
    if (request == null) {
      throw new IngestException(REQUEST_MUST_NOT_BE_NULL_MESSAGE);
    }
  }

  private class ProviderSolrMetacardClient extends SolrMetacardClientImpl {

    public ProviderSolrMetacardClient(
        SolrClient client,
        FilterAdapter catalogFilterAdapter,
        SolrFilterDelegateFactory solrFilterDelegateFactory,
        DynamicSchemaResolver dynamicSchemaResolver) {
      super(client, catalogFilterAdapter, solrFilterDelegateFactory, dynamicSchemaResolver);
    }

    @Override
    public MetacardImpl createMetacard(SolrDocument doc) throws MetacardCreationException {
      MetacardImpl metacard = super.createMetacard(doc);
      metacard.setSourceId(getId());
      return metacard;
    }
  }

  /** Solr client listener class to adapt to a source monitor class */
  private static class SolrClientListenerAdapter implements SolrClient.Listener {
    private final SourceMonitor callback;

    private SolrClientListenerAdapter(SourceMonitor callback) {
      this.callback = callback;
    }

    @Override
    public void changed(SolrClient client, boolean available) {
      if (available) {
        callback.setAvailable();
      } else {
        callback.setUnavailable();
      }
    }

    @Override
    public int hashCode() {
      return callback.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof SolrClientListenerAdapter) {
        return callback.equals(((SolrClientListenerAdapter) obj).callback);
      }
      return false;
    }

    @Override
    public String toString() {
      return callback.toString();
    }
  }
}
