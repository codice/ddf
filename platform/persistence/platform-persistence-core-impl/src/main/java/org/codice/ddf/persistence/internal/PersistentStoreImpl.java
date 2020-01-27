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
package org.codice.ddf.persistence.internal;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.solr.client.solrj.SolrClient;
import org.codice.solr.factory.SolrClientFactory;
import org.codice.solr.query.SolrQueryFilterVisitor;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistentStoreImpl implements PersistentStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(PersistentStoreImpl.class);

  private final SolrClientFactory clientFactory;

  private final Map<String, SolrClient> solrClients = new ConcurrentHashMap<>();

  public static final int DEFAULT_START_INDEX = 0;

  public static final int DEFAULT_PAGE_SIZE = 10;

  public static final int MAX_PAGE_SIZE = 1000;

  private static final String SOLR_COMMIT_NRT_COMMITWITHINMS = "solr.commit.nrt.commitWithinMs";

  private final int commitNrtCommitWithinMs =
      Math.max(NumberUtils.toInt(accessProperty(SOLR_COMMIT_NRT_COMMITWITHINMS, "1000")), 0);

  public PersistentStoreImpl(SolrClientFactory clientFactory) {
    this.clientFactory = clientFactory;
  }

  @Override
  public void add(String type, Collection<Map<String, Object>> items) throws PersistenceException {
    LOGGER.debug("type = {}", type);
    if (StringUtils.isEmpty(type)) {
      throw new PersistenceException(
          "The type of object(s) to be added must be non-null and not blank, e.g., notification, metacard, etc.");
    }
    if (CollectionUtils.isEmpty(items)) {
      return;
    }

    // Set Solr Core name to type and create solr client
    SolrClient solrClient = getSolrClient(type);
    List<SolrInputDocument> inputDocuments = new ArrayList<>();
    for (Map<String, Object> properties : items) {

      if (MapUtils.isEmpty(properties)) {
        continue;
      }

      LOGGER.debug("Adding entry of type {}", type);

      SolrInputDocument solrInputDocument = new SolrInputDocument();
      solrInputDocument.addField("createddate_tdt", new Date());

      for (Map.Entry<String, Object> entry : properties.entrySet()) {
        solrInputDocument.addField(entry.getKey(), entry.getValue());
      }
      inputDocuments.add(solrInputDocument);
    }

    if (inputDocuments.isEmpty()) {
      return;
    }

    try {
      UpdateResponse response = solrClient.add(inputDocuments, commitNrtCommitWithinMs);
      LOGGER.debug("UpdateResponse from add of SolrInputDocument:  {}", response);
    } catch (SolrServerException | SolrException | IOException e) {
      LOGGER.info("Exception while adding Solr index for persistent type {}", type, e);
      doRollback(solrClient, type);
      throw new PersistenceException(
          "Exception while adding Solr index for persistent type " + type, e);
    } catch (RuntimeException e) {
      LOGGER.info("RuntimeException while adding Solr index for persistent type {}", type, e);
      doRollback(solrClient, type);
      throw new PersistenceException(
          "RuntimeException while adding Solr index for persistent type " + type, e);
    }
  }

  @Override
  public void add(String type, Map<String, Object> properties) throws PersistenceException {
    add(type, Collections.singletonList(properties));
  }

  private void doRollback(SolrClient solrClient, String type) {
    LOGGER.debug("ENTERING: doRollback()");
    try {
      solrClient.rollback();
    } catch (SolrServerException | SolrException | IOException e) {
      LOGGER.info("Exception while doing rollback for persistent type {}", type, e);
    }
    LOGGER.debug("EXITING: doRollback()");
  }

  @Override
  public List<Map<String, Object>> get(String type) throws PersistenceException {
    return get(type, "");
  }

  @Override
  /**
   * {@inheritDoc} Returned Map will have suffixes in the key names - client is responsible for
   * handling them
   */
  public List<Map<String, Object>> get(String type, String cql) throws PersistenceException {
    return get(type, cql, DEFAULT_START_INDEX, DEFAULT_PAGE_SIZE);
  }

  @Override
  public List<Map<String, Object>> get(String type, String cql, int startIndex, int pageSize)
      throws PersistenceException {
    if (StringUtils.isBlank(type)) {
      throw new PersistenceException(
          "The type of object(s) to retrieve must be non-null and not blank, e.g., notification, metacard, etc.");
    }

    if (startIndex < 0) {
      throw new IllegalArgumentException("The start index must be nonnegative.");
    }

    if (pageSize <= 0 || pageSize > MAX_PAGE_SIZE) {
      throw new IllegalArgumentException(
          String.format(
              "The page size must be greater than 0 and less than or equal to %d.", MAX_PAGE_SIZE));
    }

    // Set Solr Core name to type and create/connect to Solr Core
    SolrClient solrClient = getSolrClient(type);
    SolrQueryFilterVisitor visitor = new SolrQueryFilterVisitor(solrClient, type);

    try {
      SolrQuery solrQuery;
      // If not cql specified, then return all items
      if (StringUtils.isBlank(cql)) {
        solrQuery = new SolrQuery("*:*");
      } else {
        Filter filter = ECQL.toFilter(cql);
        solrQuery = (SolrQuery) filter.accept(visitor, null);
      }
      if (solrQuery == null) {
        throw new PersistenceException("Unsupported query " + cql);
      }

      solrQuery.setRows(pageSize);
      solrQuery.setStart(startIndex);

      QueryResponse solrResponse = solrClient.query(solrQuery, METHOD.POST);

      long numResults = solrResponse.getResults().getNumFound();
      LOGGER.debug("numResults = {}", numResults);

      final SolrDocumentList docs = solrResponse.getResults();
      return documentListToResultList(docs);
    } catch (CQLException e) {
      throw new PersistenceException(
          "CQLException while getting Solr data with cql statement " + cql, e);
    } catch (SolrServerException | SolrException | IOException e) {
      throw new PersistenceException(
          "Exception while getting Solr data with cql statement " + cql, e);
    }
  }

  private List<Map<String, Object>> documentListToResultList(SolrDocumentList docs) {
    final List<Map<String, Object>> results = new ArrayList<>();
    for (SolrDocument doc : docs) {
      final PersistentItem result = new PersistentItem();
      final Collection<String> fieldNames = doc.getFieldNames();
      for (String name : fieldNames) {
        LOGGER.debug("field name = {} has value = {}", name, doc.getFieldValue(name));
        Collection<Object> fieldValues = doc.getFieldValues(name);
        if (name.endsWith(PersistentItem.TEXT_SUFFIX) && fieldValues.size() > 1) {
          result.addProperty(
              name,
              fieldValues
                  .stream()
                  .filter(String.class::isInstance)
                  .map(String.class::cast)
                  .collect(Collectors.toSet()));
        } else {
          addPropertyBasedOnSuffix(result, name, doc.getFirstValue(name));
        }
      }
      results.add(result);
    }

    return results;
  }

  @Override
  public int delete(String type, String cql) throws PersistenceException {
    return delete(type, cql, DEFAULT_START_INDEX, DEFAULT_PAGE_SIZE);
  }

  @Override
  public int delete(String type, String cql, int startIndex, int pageSize)
      throws PersistenceException {
    List<Map<String, Object>> itemsToDelete = this.get(type, cql, startIndex, pageSize);
    SolrClient solrClient = getSolrClient(type);
    List<String> idsToDelete = new ArrayList<>();
    for (Map<String, Object> item : itemsToDelete) {
      String uuid = (String) item.get(PersistentItem.ID);
      if (StringUtils.isNotBlank(uuid)) {
        idsToDelete.add(uuid);
      }
    }

    if (!idsToDelete.isEmpty()) {
      try {
        LOGGER.debug("Deleting {} items by ID", idsToDelete.size());
        solrClient.deleteById(idsToDelete);
      } catch (SolrServerException | SolrException | IOException e) {
        LOGGER.info("Exception while trying to delete items by ID for persistent type {}", type, e);
        doRollback(solrClient, type);
        throw new PersistenceException(
            "Exception while trying to delete items by ID for persistent type " + type, e);
      } catch (RuntimeException e) {
        LOGGER.info(
            "RuntimeException while trying to delete items by ID for persistent type {}", type, e);
        doRollback(solrClient, type);
        throw new PersistenceException(
            "RuntimeException while trying to delete items by ID for persistent type " + type, e);
      }
    }

    return idsToDelete.size();
  }

  private void addPropertyBasedOnSuffix(PersistentItem result, String name, Object firstValue) {
    if (name.endsWith(PersistentItem.XML_SUFFIX)) {
      result.addXmlProperty(name, (String) firstValue);
    } else if (name.endsWith(PersistentItem.TEXT_SUFFIX)) {
      result.addProperty(name, (String) firstValue);
    } else if (name.endsWith(PersistentItem.LONG_SUFFIX)) {
      result.addProperty(name, (long) firstValue);
    } else if (name.endsWith(PersistentItem.INT_SUFFIX)) {
      result.addProperty(name, (int) firstValue);
    } else if (name.endsWith(PersistentItem.DATE_SUFFIX)) {
      result.addProperty(name, (Date) firstValue);
    } else if (name.endsWith(PersistentItem.BINARY_SUFFIX)) {
      result.addProperty(name, (byte[]) firstValue);
    } else {
      LOGGER.debug("Not adding field {} because it has invalid suffix", name);
    }
  }

  private SolrClient getSolrClient(String storeName) throws PersistenceException {
    try {
      final SolrClient solrClient =
          solrClients.computeIfAbsent(storeName, clientFactory::newClient);

      if (solrClient.isAvailable(30, 5, TimeUnit.SECONDS)) {
        return solrClient;
      }
    } catch (InterruptedException e) {
      LOGGER.warn("Error getting available solr server for core: {}", storeName, e);
      Thread.currentThread().interrupt();
    }
    throw new PersistenceException("Solr client is not available");
  }

  private static String accessProperty(String key, String defaultValue) {
    String value =
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty(key, defaultValue));
    LOGGER.debug("Read system property [{}] with value [{}]", key, value);
    return value;
  }
}
