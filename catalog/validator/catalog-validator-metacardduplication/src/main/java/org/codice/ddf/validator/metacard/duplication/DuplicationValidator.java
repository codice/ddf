/*
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.validator.metacard.duplication;

import ddf.catalog.CatalogFramework;
import ddf.catalog.Constants;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Validation;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DuplicationValidator
    implements PreIngestPlugin,
        ddf.catalog.util.Describable,
        org.codice.ddf.platform.services.common.Describable {
  private static final Logger LOGGER = LoggerFactory.getLogger(DuplicationValidator.class);

  private static final Logger INGEST_LOGGER = LoggerFactory.getLogger(Constants.INGEST_LOGGER_NAME);

  private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

  private static final String ORGANIZATION = "organization";

  private static final String VERSION = "version";

  private static final String INVALID_TAG = "INVALID";

  private static Properties describableProperties = new Properties();

  static {
    try (InputStream properties =
        DuplicationValidator.class.getResourceAsStream(DESCRIBABLE_PROPERTIES_FILE)) {
      describableProperties.load(properties);
    } catch (IOException e) {
      LOGGER.info("Failed to load properties", e);
    }
  }

  private final CatalogFramework catalogFramework;

  private final FilterBuilder filterBuilder;

  private String[] errorOnDuplicateAttributes;

  private String[] warnOnDuplicateAttributes;

  private String[] rejectOnDuplicateAttributes;

  public DuplicationValidator(CatalogFramework catalogFramework, FilterBuilder filterBuilder) {
    this.catalogFramework = catalogFramework;
    this.filterBuilder = filterBuilder;
  }

  /**
   * Setter for the list of attributes to test for duplication in the local catalog. Resulting
   * attributes will cause the {@link ddf.catalog.data.types.Validation#VALIDATION_ERRORS} attribute
   * to be set on the metacard.
   *
   * @param attributeStrings
   */
  public void setErrorOnDuplicateAttributes(String[] attributeStrings) {
    if (attributeStrings != null) {
      this.errorOnDuplicateAttributes = Arrays.copyOf(attributeStrings, attributeStrings.length);
    }
  }

  /**
   * Setter for the list of attributes to test for duplication in the local catalog. Resulting
   * attributes will cause the {@link ddf.catalog.data.types.Validation#VALIDATION_WARNINGS}
   * attribute to be set on the metacard.
   *
   * @param attributeStrings
   */
  public void setWarnOnDuplicateAttributes(String[] attributeStrings) {
    if (attributeStrings != null) {
      this.warnOnDuplicateAttributes = Arrays.copyOf(attributeStrings, attributeStrings.length);
    }
  }

  /**
   * Setter for the list of attributes to test for duplication in the local catalog. Resulting
   * attributes will cause the metacard to be rejected.
   *
   * @param attributeStrings
   */
  public void setRejectOnDuplicateAttributes(String[] attributeStrings) {
    if (attributeStrings != null) {
      this.rejectOnDuplicateAttributes = Arrays.copyOf(attributeStrings, attributeStrings.length);
    }
  }

  @Override
  public CreateRequest process(CreateRequest input) {
    List<Metacard> validatedMetacards = handleDuplicates(input.getMetacards());
    return new CreateRequestImpl(validatedMetacards, input.getProperties(), input.getStoreIds());
  }

  private List<Metacard> handleDuplicates(List<Metacard> metacards) {
    List<Metacard> acceptedMetacards =
        metacards
            .stream()
            .map(item -> validateDuplicates(item))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    return acceptedMetacards;
  }

  @Override
  public UpdateRequest process(UpdateRequest input) {
    List<Entry<Serializable, Metacard>> result = handleDuplicatesEntries(input.getUpdates());
    return new UpdateRequestImpl(result, input.getAttributeName(), input.getProperties());
  }

  private List<Entry<Serializable, Metacard>> handleDuplicatesEntries(
      List<Entry<Serializable, Metacard>> updateList) {
    List acceptedMetacards =
        updateList
            .stream()
            .map(entry -> new SimpleEntry<>(entry.getKey(), validateDuplicates(entry.getValue())))
            .filter(entry -> entry.getValue() != null)
            .collect(Collectors.toList());

    return acceptedMetacards;
  }

  private Metacard validateDuplicates(Metacard metacard) {
    Set<String> rejectedAttributes;
    Set<String> errorAttributes;
    Set<String> warningAttributes;
    String validationMessage = "Duplicates values of existing metacard for attribute(s): %s";

    if (ArrayUtils.isNotEmpty(rejectOnDuplicateAttributes)) {
      rejectedAttributes = reportDuplicates(metacard, rejectOnDuplicateAttributes);
      if (!rejectedAttributes.isEmpty()) {
        INGEST_LOGGER.debug(
            "The metacard with id={} is being removed from the operation because it duplicates the attribute(s) {} of an existing metacard",
            metacard.getId(),
            rejectedAttributes);
        return null;
      }
    }

    if (ArrayUtils.isNotEmpty(errorOnDuplicateAttributes)) {
      errorAttributes = reportDuplicates(metacard, errorOnDuplicateAttributes);
      if (!errorAttributes.isEmpty()) {
        addAttribute(
            metacard,
            Validation.VALIDATION_ERRORS,
            String.format(validationMessage, collectionToString(errorAttributes)));
        addAttribute(metacard, Metacard.TAGS, INVALID_TAG);
      }
    }

    if (ArrayUtils.isNotEmpty(warnOnDuplicateAttributes)) {
      warningAttributes = reportDuplicates(metacard, warnOnDuplicateAttributes);
      if (!warningAttributes.isEmpty()) {
        addAttribute(
            metacard,
            Validation.VALIDATION_WARNINGS,
            String.format(validationMessage, collectionToString(warningAttributes)));
        addAttribute(metacard, Metacard.TAGS, INVALID_TAG);
      }
    }

    return metacard;
  }

  private void addAttribute(Metacard metacard, String attribute, Serializable value) {
    Attribute existingAttribute = metacard.getAttribute(attribute);
    if (existingAttribute == null || existingAttribute.getValue() == null) {
      metacard.setAttribute(new AttributeImpl(attribute, value));
    } else {
      List<Serializable> existingValues = existingAttribute.getValues();
      List<Serializable> newValues = new ArrayList<>();

      // Attribute.getValues() can return null even if there is a single value present
      if (existingValues == null) {
        newValues.add(existingAttribute.getValue());
      } else {
        newValues.addAll(existingValues);
      }

      newValues.add(value);

      metacard.setAttribute(new AttributeImpl(attribute, newValues));
    }
  }

  private Set<String> reportDuplicates(final Metacard metacard, String[] attributeNames) {
    Set<String> duplicatedAttributes = new HashSet<>();

    final Set<String> nonNullAttributeNames =
        Stream.of(attributeNames)
            .filter(attribute -> metacard.getAttribute(attribute) != null)
            .collect(Collectors.toSet());
    final Set<Attribute> nonNullAttributes =
        nonNullAttributeNames.stream().map(metacard::getAttribute).collect(Collectors.toSet());
    if (!nonNullAttributes.isEmpty()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Checking for duplicates for id {} against attributes [{}]",
            metacard.getId(),
            collectionToString(nonNullAttributeNames));
      }

      SourceResponse response = query(nonNullAttributes);
      if (response != null) {
        for (String attribute : nonNullAttributeNames) {
          for (Result result : response.getResults()) {
            Metacard resultMetacard = result.getMetacard();
            if (doAttributesIntersect(resultMetacard, metacard, attribute)) {
              duplicatedAttributes.add(attribute);
              break;
            }
          }
        }
      }
    }
    return duplicatedAttributes;
  }

  private boolean doAttributesIntersect(
      Metacard metacard1, Metacard metacard2, String attributeName) {
    if (metacard1 == null || metacard2 == null) {
      return false;
    }

    List<Serializable> values1 = getAttributeValues(metacard1, attributeName);
    List<Serializable> values2 = getAttributeValues(metacard2, attributeName);

    return CollectionUtils.containsAny(values1, values2);
  }

  private List<Serializable> getAttributeValues(Metacard metacard, String attributeName) {
    Attribute attribute = metacard.getAttribute(attributeName);
    return getAttributeValues(attribute);
  }

  private List<Serializable> getAttributeValues(Attribute attribute) {
    List<Serializable> values = attribute.getValues();
    if (values == null) { // 1 or 0 values
      Serializable singleValue = attribute.getValue();
      if (singleValue != null) {
        values = Collections.singletonList(singleValue);
      } else {
        return Collections.EMPTY_LIST;
      }
    }
    return values;
  }

  private Filter[] buildFilters(Set<Attribute> attributes) {
    return attributes
        .stream()
        .flatMap(
            attribute ->
                getAttributeValues(attribute)
                    .stream()
                    .map(
                        value ->
                            filterBuilder
                                .attribute(attribute.getName())
                                .equalTo()
                                .text(value.toString().trim())))
        .toArray(Filter[]::new);
  }

  private SourceResponse query(Set<Attribute> attributes) {

    final Filter filter = filterBuilder.allOf(filterBuilder.anyOf(buildFilters(attributes)));

    LOGGER.debug("filter {}", filter);

    QueryImpl query = new QueryImpl(filter);
    query.setRequestsTotalResultsCount(false);
    QueryRequest request = new QueryRequestImpl(query);

    SourceResponse response = null;
    try {
      response = catalogFramework.query(request);
    } catch (FederationException | SourceUnavailableException | UnsupportedQueryException e) {
      LOGGER.debug("Query failed ", e);
    }
    return response;
  }

  private String collectionToString(final Collection collection) {

    return (String)
        collection.stream().map(Object::toString).sorted().collect(Collectors.joining(", "));
  }

  @Override
  public DeleteRequest process(DeleteRequest input) {
    // No operation for delete
    return input;
  }

  @Override
  public String getVersion() {
    return describableProperties.getProperty(VERSION);
  }

  @Override
  public String getId() {
    return this.getClass().getSimpleName();
  }

  @Override
  public String getTitle() {
    return this.getClass().getSimpleName();
  }

  @Override
  public String getDescription() {
    return "Checks metacard against the local catalog for duplicates based on configurable attributes.";
  }

  @Override
  public String getOrganization() {
    return describableProperties.getProperty(ORGANIZATION);
  }
}
