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
package ddf.catalog.source.solr.rest;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import ddf.security.encryption.EncryptionService;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrRest {
  private static final Logger LOGGER = LoggerFactory.getLogger(SolrRest.class);

  private static final String SOLR_BASE_URL_PROPERTY = "solrBaseUrl";

  private static final String SOLR_CATALOG_CORE = "/catalog";

  private static final String SOLR_METACARD_CACHE_CORE = "/metacard_cache";

  private static final String SOLR_SCHEMA_URI = "/schema";

  private static final String SOLR_UPDATE_URI = "/update";

  private static final String K1_PROPERTY = "k1";

  private static final String B_PROPERTY = "b";

  private final ClientFactoryFactory clientFactoryFactory;

  private final EncryptionService encryptionService;

  private LinkedTreeMap<String, Object> similarityFormat;

  private List fieldTypes;

  private SecureCxfClientFactory<SolrRestClient> solrCatalogSchemaClientFactory;

  private SecureCxfClientFactory<SolrUpdateClient> solrCatalogUpdateClientFactory;

  private SecureCxfClientFactory<SolrRestClient> solrMetacardCacheSchemaClientFactory;

  private SecureCxfClientFactory<SolrUpdateClient> solrMetacardCacheUpdateClientFactory;

  private Float k1;

  private Gson gson;

  private Float b;

  private String solrBaseUrl;

  public SolrRest(ClientFactoryFactory clientFactoryFactory, EncryptionService encryptionService) {
    gson = new Gson();
    this.clientFactoryFactory = clientFactoryFactory;
    this.encryptionService = encryptionService;
  }

  public void setK1(Float k1) {
    LOGGER.trace("Setting K1 property: {}", k1);
    this.k1 = k1;
  }

  public void setB(Float b) {
    LOGGER.trace("Setting B property: {}", b);
    this.b = b;
  }

  public Float getK1() {
    return k1;
  }

  public Float getB() {
    return b;
  }

  public void setSolrBaseUrl(String solrBaseUrl) {
    this.solrBaseUrl = solrBaseUrl;
  }

  public String getSolrBaseUrl() {
    return solrBaseUrl;
  }

  public void getProperties() {
    try {
      String response = solrCatalogSchemaClientFactory.getClient().getFieldTypes("json");

      Map<String, Object> map =
          gson.fromJson(response, new TypeToken<Map<String, Object>>() {}.getType());

      if (map != null) {
        fieldTypes = (ArrayList<Object>) map.get("fieldTypes");
      }
    } catch (Exception e) {
      LOGGER.debug("Unable to getProperties from: {}", getSolrCatalogSchemaUrl());
    }
  }

  public void init() {
    String username = getUsername();
    String password = getPassword();
    if (StringUtils.isNotBlank(solrBaseUrl)) {
      if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
        solrCatalogSchemaClientFactory =
            clientFactoryFactory.getSecureCxfClientFactory(
                getSolrCatalogSchemaUrl(), SolrRestClient.class);
        solrCatalogUpdateClientFactory =
            clientFactoryFactory.getSecureCxfClientFactory(
                getSolrCatalogUpdateUrl(), SolrUpdateClient.class);
        solrMetacardCacheSchemaClientFactory =
            clientFactoryFactory.getSecureCxfClientFactory(
                getSolrMetacardCacheSchemaUrl(), SolrRestClient.class);
        solrMetacardCacheUpdateClientFactory =
            clientFactoryFactory.getSecureCxfClientFactory(
                getSolrMetacardCacheUpdateUrl(), SolrUpdateClient.class);
      } else {
        solrCatalogSchemaClientFactory =
            clientFactoryFactory.getSecureCxfClientFactory(
                getSolrCatalogSchemaUrl(), SolrRestClient.class, username, password);
        solrCatalogUpdateClientFactory =
            clientFactoryFactory.getSecureCxfClientFactory(
                getSolrCatalogUpdateUrl(), SolrUpdateClient.class, username, password);
        solrMetacardCacheSchemaClientFactory =
            clientFactoryFactory.getSecureCxfClientFactory(
                getSolrMetacardCacheSchemaUrl(), SolrRestClient.class, username, password);
        solrMetacardCacheUpdateClientFactory =
            clientFactoryFactory.getSecureCxfClientFactory(
                getSolrMetacardCacheUpdateUrl(), SolrUpdateClient.class, username, password);
      }

      similarityFormat = new LinkedTreeMap<>();
      similarityFormat.put("class", "solr.BM25SimilarityFactory");
      similarityFormat.put(K1_PROPERTY, k1);
      similarityFormat.put(B_PROPERTY, b);

      getProperties();
      setSimilarities();
    }
  }

  public void refresh(Map<String, Object> properties) {
    if (MapUtils.isEmpty(properties)) {
      LOGGER.debug("No properties specified during refresh.");
      return;
    }

    LOGGER.trace("Refresh configuration with properties: {}", properties);

    Object solrUrlObject = properties.get(SOLR_BASE_URL_PROPERTY);
    if (solrUrlObject instanceof String) {
      setSolrBaseUrl((String) solrUrlObject);
    }

    Object k1Object = properties.get(K1_PROPERTY);
    if (k1Object instanceof Float) {
      setK1((Float) k1Object);
    }

    Object bObject = properties.get(B_PROPERTY);
    if (bObject instanceof Float) {
      setB((Float) bObject);
    }

    init();
  }

  private void setSimilarities() {
    if (CollectionUtils.isNotEmpty(fieldTypes)) {
      LinkedTreeMap<String, Object> replaceField = new LinkedTreeMap<>();
      for (Object fieldType : fieldTypes) {
        LinkedTreeMap<String, Object> objectLinkedTreeMap =
            (LinkedTreeMap<String, Object>) fieldType;
        Object nameObj = objectLinkedTreeMap.get("name");
        if (nameObj instanceof String) {
          String name = (String) nameObj;
          if (name.contains("suggest")) {
            LOGGER.trace("Skipping suggest field");
            continue;
          }
        }

        objectLinkedTreeMap.put("similarity", similarityFormat);
        replaceField.put("replace-field-type", objectLinkedTreeMap);

        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Replacing field: {}", gson.toJson(replaceField));
        }

        String response =
            solrCatalogSchemaClientFactory.getClient().replaceField(gson.toJson(replaceField));
        LOGGER.trace("Catalog Configuration update response: {}", response);

        response =
            solrMetacardCacheSchemaClientFactory
                .getClient()
                .replaceField(gson.toJson(replaceField));
        LOGGER.trace("Metacard Cache Configuration update response: {}", response);
      }
    }
  }

  private String getSolrCatalogSchemaUrl() {
    return solrBaseUrl + SOLR_CATALOG_CORE + SOLR_SCHEMA_URI;
  }

  private String getSolrCatalogUpdateUrl() {
    return solrBaseUrl + SOLR_CATALOG_CORE + SOLR_UPDATE_URI;
  }

  private String getSolrMetacardCacheSchemaUrl() {
    return solrBaseUrl + SOLR_METACARD_CACHE_CORE + SOLR_SCHEMA_URI;
  }

  private String getSolrMetacardCacheUpdateUrl() {
    return solrBaseUrl + SOLR_METACARD_CACHE_CORE + SOLR_UPDATE_URI;
  }

  private String getUsername() {
    return AccessController.doPrivileged(
        (PrivilegedAction<String>) () -> System.getProperty("solr.username"));
  }

  private String getPassword() {
    return encryptionService.decryptValue(
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("solr.password")));
  }
}
