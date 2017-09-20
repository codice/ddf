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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.codice.ddf.cxf.SecureCxfClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrRest {
  private static final Logger LOGGER = LoggerFactory.getLogger(SolrRest.class);

  private static final String SOLR_URL_PROPERTY = "solrSchemaUrl";

  private static final String K1_PROPERTY = "k1";

  private static final String B_PROPERTY = "b";

  private LinkedTreeMap<String, Object> similarityFormat;

  private List fieldTypes;

  private SecureCxfClientFactory<SolrRestClient> factory;

  private Float k1;

  private Gson gson;

  private Float b;

  private String solrSchemaUrl;

  public SolrRest() {
    gson = new Gson();
  }

  public void setK1(Float k1) {
    LOGGER.trace("Setting K1 property: {}", k1);
    this.k1 = k1;
  }

  public void setB(Float b) {
    LOGGER.trace("Setting B property: {}", b);
    this.b = b;
  }

  public void setSolrSchemaUrl(String solrSchemaUrl) {
    this.solrSchemaUrl = solrSchemaUrl;
  }

  public Float getK1() {
    return k1;
  }

  public Float getB() {
    return b;
  }

  public String getSolrSchemaUrl() {
    return solrSchemaUrl;
  }

  public void getProperties() {
    try {
      String response = factory.getClient().getFieldTypes("json");

      Map<String, Object> map =
          gson.fromJson(response, new TypeToken<Map<String, Object>>() {}.getType());

      fieldTypes = (ArrayList<Object>) map.get("fieldTypes");
    } catch (Exception e) {
      LOGGER.debug("Unable to getProperties from: {}", solrSchemaUrl, e);
    }
  }

  public void init() {
    if (solrSchemaUrl != null) {
      factory = new SecureCxfClientFactory<>(solrSchemaUrl, SolrRestClient.class);
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

    Object solrUrlObject = properties.get(SOLR_URL_PROPERTY);
    if (solrUrlObject instanceof String) {
      setSolrSchemaUrl((String) solrUrlObject);
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
      for (Object fieldType : fieldTypes) {
        LinkedTreeMap<String, Object> objectLinkedTreeMap =
            (LinkedTreeMap<String, Object>) fieldType;
        objectLinkedTreeMap.put("similarity", similarityFormat);
        LinkedTreeMap<String, Object> replaceField = new LinkedTreeMap<>();
        replaceField.put("replace-field-type", objectLinkedTreeMap);

        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Replacing field: {}", gson.toJson(replaceField));
        }

        String response = factory.getClient().replaceField(gson.toJson(replaceField));

        LOGGER.trace("Configuration update response: {}", response);
      }
    }
  }
}
