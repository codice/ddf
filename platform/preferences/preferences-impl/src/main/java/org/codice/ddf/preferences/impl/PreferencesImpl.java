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
package org.codice.ddf.preferences.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.preferences.Preferences;
import org.codice.ddf.preferences.PreferencesException;
import org.codice.gsonsupport.GsonTypeAdapters;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreferencesImpl implements Preferences {

  private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesImpl.class);

  private static final String TYPE = PersistentStore.PersistenceType.PREFERENCES_TYPE.toString();

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .registerTypeAdapterFactory(GsonTypeAdapters.LongDoubleTypeAdapter.FACTORY)
          .create();

  private final PersistentStore persistentStore;

  private final List<MetacardType> metacardTypes;

  private final CatalogFramework catalogFramework;

  private final FilterBuilder filterBuilder;

  public PreferencesImpl(
      PersistentStore persistentStore,
      List<MetacardType> metacardTypes,
      CatalogFramework catalogFramework,
      FilterBuilder filterBuilder) {
    this.persistentStore = persistentStore;
    this.metacardTypes = metacardTypes;
    this.catalogFramework = catalogFramework;
    this.filterBuilder = filterBuilder;
  }

  @Override
  public void add(Map<String, Object> properties) throws PreferencesException {
    //    try {
    //      persistentStore.add(TYPE, properties);
    //    } catch (PersistenceException e) {
    //      throw new PreferencesException(e);
    //    }

    Optional<MetacardType> optionalDdfPreferencesMetacardType =
        metacardTypes.stream()
            .filter(metacardType -> metacardType.getName().equals("ddf.preferences"))
            .findFirst();

    if (optionalDdfPreferencesMetacardType.isPresent()) {

      String json =
          new String(Base64.getDecoder().decode(properties.get("preferences_json_bin").toString()));

      Map<Object, Object> originalPreferences = GSON.fromJson(json, Map.class);

      Set<String> availableAttributes =
          optionalDdfPreferencesMetacardType.get().getAttributeDescriptors().stream()
              .map(AttributeDescriptor::getName)
              .collect(Collectors.toSet());

      Metacard metacard = new MetacardImpl(optionalDdfPreferencesMetacardType.get());
      metacard.setAttribute(new AttributeImpl("user", properties.get("user_txt").toString()));
      originalPreferences.forEach(
          (key, value) -> {
            if (availableAttributes.contains(key.toString())) {
              String json2 = GSON.toJson(value);
              // value.getClass().getName() + ":" +
              metacard.setAttribute(new AttributeImpl(key.toString(), json2));
            } else {
              LOGGER.debug(
                  "Unable to save the preference attribute {} because it is not defined on the MetacardType",
                  key);
            }
          });

      Filter filter =
          filterBuilder.allOf(
              filterBuilder
                  .attribute("user")
                  .is()
                  .equalTo()
                  .text(properties.get("user_txt").toString()),
              filterBuilder.attribute("metacard-tags").is().equalTo().text("ddf-preferences"));

      QueryResponse queryResponse = null;
      try {
        queryResponse = catalogFramework.query(new QueryRequestImpl(new QueryImpl(filter)));
      } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
        throw new PreferencesException(e);
      }

      for (String id :
          queryResponse.getResults().stream()
              .map(Result::getMetacard)
              .map(Metacard::getId)
              .collect(Collectors.toList())) {
        try {
          catalogFramework.delete(new DeleteRequestImpl(id));
        } catch (IngestException | SourceUnavailableException e) {
          throw new PreferencesException(e);
        }
      }

      try {
        catalogFramework.create(new CreateRequestImpl(metacard));
      } catch (IngestException | SourceUnavailableException e) {
        throw new PreferencesException(e);
      }

      LOGGER.info("metacard: {}", metacard);
    }
  }

  @Override
  public void add(Collection<Map<String, Object>> items) throws PreferencesException {
    try {
      persistentStore.add(TYPE, items);
    } catch (PersistenceException e) {
      throw new PreferencesException(e);
    }
  }

  @Override
  public List<Map<String, Object>> get() throws PreferencesException {
    try {
      return persistentStore.get(TYPE);
    } catch (PersistenceException e) {
      throw new PreferencesException(e);
    }
  }

  @Override
  public List<Map<String, Object>> get(String ecql) throws PreferencesException {

    Map<String, Object> results = new HashMap<>();

    String user = null;

    Pattern pattern = Pattern.compile("^user = '(.*)'$");

    Matcher matcher = pattern.matcher(ecql);

    if (matcher.matches()) {
      user = matcher.group(1);
    }

    Filter filter =
        filterBuilder.allOf(
            filterBuilder.attribute("user").is().equalTo().text(user),
            filterBuilder.attribute("metacard-tags").is().equalTo().text("ddf-preferences"));

    QueryResponse queryResponse = null;
    try {
      queryResponse = catalogFramework.query(new QueryRequestImpl(new QueryImpl(filter)));
    } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
      throw new PreferencesException(e);
    }

    if (queryResponse.getResults().isEmpty()) {
      return Collections.emptyList();
    }

    Optional<MetacardType> optionalDdfPreferencesMetacardType =
        metacardTypes.stream()
            .filter(metacardType -> metacardType.getName().equals("ddf.preferences"))
            .findFirst();

    if (optionalDdfPreferencesMetacardType.isPresent()) {

      Map<String, Object> preferences = new HashMap<>();

      Metacard metacard = queryResponse.getResults().get(0).getMetacard();

      Set<String> coreAttributes =
          new CoreAttributes()
              .getAttributeDescriptors().stream()
                  .map(AttributeDescriptor::getName)
                  .collect(Collectors.toSet());
      coreAttributes.add("effective");

      optionalDdfPreferencesMetacardType.get().getAttributeDescriptors().stream()
          .map(attributeDescriptor -> metacard.getAttribute(attributeDescriptor.getName()))
          .filter(Objects::nonNull)
          .filter(attribute -> !coreAttributes.contains(attribute.getName()))
          .forEach(
              attribute -> {
                String value = attribute.getValue().toString();
                // Pattern p = Pattern.compile("([^:]+):(.*)");
                // Matcher m = p.matcher(value);
                // if (m.matches()) {
                // String clazz = m.group(1);
                // String json = m.group(2);
                try {
                  preferences.put(attribute.getName(), GSON.fromJson(value, Object.class));
                } catch (JsonSyntaxException e) {
                  // throw new RuntimeException(e);
                  LOGGER.info(
                      "failed to parse json: name={} value={}", attribute.getName(), value, e);
                }
                // }
              });

      String json = GSON.toJson(preferences);

      results.put("preferences_json_bin", json.getBytes(Charset.defaultCharset()));
      results.put("id_txt", user);
      results.put(
          "ext.security-filter-match-all-count_int",
          metacard.getAttribute("ext.security-filter-match-all-count").getValue());
      results.put(
          "ext.replication-node-id_txt",
          metacard.getAttribute("ext.replication-node-id").getValue());
      results.put("createdate_tdt", metacard.getCreatedDate());
      results.put("user_txt", user);
      results.put(
          "ext.security-redaction-match-all-count_int",
          metacard.getAttribute("ext.security-redaction-match-all-count").getValue());
      results.put(
          "ext.replication-timestamp_lng",
          metacard.getAttribute("ext.replication-timestamp").getValue());

      return Collections.singletonList(results);
    }

    //    try {
    //      List<Map<String, Object>> tmp = persistentStore.get(TYPE, ecql);
    //      return tmp;
    //    } catch (PersistenceException e) {
    //      throw new PreferencesException(e);
    //    }

    throw new PreferencesException();
  }

  @Override
  public List<Map<String, Object>> get(String ecql, int startIndex, int pageSize)
      throws PreferencesException {
    try {
      return persistentStore.get(TYPE, ecql, startIndex, pageSize);
    } catch (PersistenceException e) {
      throw new PreferencesException(e);
    }
  }

  @Override
  public int delete(String ecql) throws PreferencesException {
    try {
      return persistentStore.delete(TYPE, ecql);
    } catch (PersistenceException e) {
      throw new PreferencesException(e);
    }
  }

  @Override
  public int delete(String ecql, int startIndex, int pageSize) throws PreferencesException {
    try {
      return persistentStore.delete(TYPE, ecql, startIndex, pageSize);
    } catch (PersistenceException e) {
      throw new PreferencesException(e);
    }
  }
}
