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
import ddf.catalog.data.InjectableAttribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.codice.ddf.preferences.DefaultPreferencesSupplier;
import org.codice.ddf.preferences.PostRetrievalPlugin;
import org.codice.ddf.preferences.Preferences;
import org.codice.ddf.preferences.PreferencesException;
import org.codice.gsonsupport.GsonTypeAdapters;
import org.geotools.api.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This stores each preference as a JSON string regardless of the underlying type. This allows every
 * preference to be treated the same way.
 *
 * <p>This implementation pulls in instances of DefaultPreferencesSupplier that are used to populate
 * the preferences if it does not already exist in the catalog. This allows new preferences and its
 * default value to defined by new modules.
 *
 * <p>This implementation pulls in instances of PostRetrievalPlugin that are applied to the
 * preferences everytime they are queried.
 */
public class PreferencesImpl implements Preferences {

  private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesImpl.class);

  private static final MetacardType METACARD_TYPE = new PreferencesMetacardType();

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .registerTypeAdapterFactory(GsonTypeAdapters.LongDoubleTypeAdapter.FACTORY)
          .create();

  private final CatalogFramework catalogFramework;

  private final FilterBuilder filterBuilder;

  private final UuidGenerator uuidGenerator;

  /** These are the attributes that will be ignored from being added to the preferences metacard. */
  private static final Set<String> IGNORED_ATTRIBUTES =
      new HashSet<>(Arrays.asList(PreferencesMetacardType.USER_ATTRIBUTE, Metacard.ID));

  private final List<PostRetrievalPlugin> postRetrievalPlugins;

  private final List<DefaultPreferencesSupplier> defaultPreferencesSuppliers;

  private final List<InjectableAttribute> injectableAttributes;

  public PreferencesImpl(
      CatalogFramework catalogFramework,
      FilterBuilder filterBuilder,
      UuidGenerator uuidGenerator,
      List<PostRetrievalPlugin> postRetrievalPlugins,
      List<DefaultPreferencesSupplier> defaultPreferencesSuppliers,
      List<InjectableAttribute> injectableAttributes) {
    this.catalogFramework = catalogFramework;
    this.filterBuilder = filterBuilder;
    this.uuidGenerator = uuidGenerator;
    this.postRetrievalPlugins = postRetrievalPlugins;
    this.defaultPreferencesSuppliers = defaultPreferencesSuppliers;
    this.injectableAttributes = injectableAttributes;
  }

  @Override
  public void add(Map<String, Object> preferences, String userId) throws PreferencesException {

    Filter filter = createFilter(userId);

    QueryResponse queryResponse = query(filter);
    if (queryResponse.getResults().size() > 1) {
      throw new PreferencesException("internal error: more than one preference metacard found");
    } else if (queryResponse.getResults().size() == 1) {
      updateExistingMetacard(preferences, queryResponse);
    } else {
      createNewMetacard(preferences, userId);
    }
  }

  private QueryResponse query(Filter filter) throws PreferencesException {
    QueryResponse queryResponse;
    try {
      queryResponse = catalogFramework.query(new QueryRequestImpl(new QueryImpl(filter)));
    } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
      throw new PreferencesException(e);
    }
    return queryResponse;
  }

  private Filter createFilter(String userId) {
    String metacardId = uuidGenerator.generateKnownId(PreferencesMetacardType.TAG, userId);
    return filterBuilder.allOf(
        filterBuilder.attribute(Metacard.ID).is().equalTo().text(metacardId),
        filterBuilder.attribute(Metacard.TAGS).is().equalTo().text(PreferencesMetacardType.TAG));
  }

  private void createNewMetacard(Map<String, Object> preferences, String userId)
      throws PreferencesException {
    Metacard metacard = new MetacardImpl(METACARD_TYPE);
    metacard.setAttribute(new AttributeImpl(PreferencesMetacardType.USER_ATTRIBUTE, userId));
    metacard.setAttribute(new AttributeImpl(Metacard.TAGS, PreferencesMetacardType.TAG));
    // TODO I could loop through the incoming preferences or loop through the known attributes, not
    // sure if one way is better than the other
    preferences.forEach(
        (key, value) -> {
          if (!IGNORED_ATTRIBUTES.contains(key)) {
            String json = GSON.toJson(value);
            metacard.setAttribute(new AttributeImpl(key, json));
          }
        });
    try {
      catalogFramework.create(new CreateRequestImpl(metacard));
      LOGGER.info(
          "Created new {} metacard for user {}. Metacard Id: {}",
          PreferencesMetacardType.TAG,
          userId,
          metacard.getId());
    } catch (IngestException | SourceUnavailableException e) {
      throw new PreferencesException(e);
    }
  }

  private void updateExistingMetacard(Map<String, Object> preferences, QueryResponse queryResponse)
      throws PreferencesException {
    Metacard metacard = queryResponse.getResults().get(0).getMetacard();
    preferences.forEach(
        (key, value) -> {
          if (!IGNORED_ATTRIBUTES.contains(key)) {
            String json = GSON.toJson(value);
            metacard.setAttribute(new AttributeImpl(key, json));
          }
        });
    try {
      catalogFramework.update(new UpdateRequestImpl(metacard.getId(), metacard));
      if (metacard.getAttribute(PreferencesMetacardType.USER_ATTRIBUTE) != null) {
        LOGGER.info(
            "Updated {} metacard for user {}. Metacard Id: {}",
            PreferencesMetacardType.TAG,
            metacard.getAttribute(PreferencesMetacardType.USER_ATTRIBUTE),
            metacard.getId());
      } else {
        LOGGER.info(
            "Updated {} metacard. Metacard Id: {}", PreferencesMetacardType.TAG, metacard.getId());
      }

    } catch (IngestException | SourceUnavailableException e) {
      throw new PreferencesException(e);
    }
  }

  @Override
  public Map<String, Object> get(String userId) throws PreferencesException {

    Filter filter = createFilter(userId);

    QueryResponse queryResponse = query(filter);

    Map<String, Object> preferences = new HashMap<>();

    if (queryResponse.getResults().isEmpty()) {
      defaultPreferencesSuppliers.forEach(
          defaultPreferencesSupplier -> preferences.putAll(defaultPreferencesSupplier.create()));
    } else {

      Metacard metacard = queryResponse.getResults().get(0).getMetacard();

      PreferencesMetacardType.DESCRIPTORS.stream()
          .map(attributeDescriptor -> metacard.getAttribute(attributeDescriptor.getName()))
          .filter(Objects::nonNull)
          .forEach(
              attribute -> {
                String value = attribute.getValue().toString();
                try {
                  preferences.put(attribute.getName(), GSON.fromJson(value, Object.class));
                } catch (JsonSyntaxException e) {
                  LOGGER.info(
                      "failed to parse json: name={} value={}", attribute.getName(), value, e);
                }
              });

      injectableAttributes.stream()
          .filter(
              injectableAttribute ->
                  injectableAttribute.metacardTypes().contains(PreferencesMetacardType.NAME))
          .map(injectableAttribute -> metacard.getAttribute(injectableAttribute.attribute()))
          .filter(Objects::nonNull)
          .forEach(
              attribute -> {
                String value = attribute.getValue().toString();
                try {
                  preferences.put(attribute.getName(), GSON.fromJson(value, Object.class));
                } catch (JsonSyntaxException e) {
                  LOGGER.info(
                      "failed to parse json: name={} value={}", attribute.getName(), value, e);
                }
              });
    }

    preferences.put(PreferencesMetacardType.USER_ATTRIBUTE, userId);

    for (PostRetrievalPlugin postRetrievaPlugin : postRetrievalPlugins) {
      postRetrievaPlugin.process(preferences);
    }

    return preferences;
  }
}
