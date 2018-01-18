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
package org.codice.ddf.spatial.ogc.wfs.catalog.mapper.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.boon.json.JsonException;
import org.boon.json.JsonFactory;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Maps Metacard Attributes to WFS Feature Properties. */
public final class MetacardMapperImpl implements MetacardMapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetacardMapperImpl.class);

  private static final String ATTRIBUTE_NAME = "attributeName";

  private static final String FEATURE_NAME = "featureName";

  private static final String TEMPLATE = "template";

  private String featureType;

  private String dataUnit;

  private String sortByTemporalFeatureProperty;

  private String sortByRelevanceFeatureProperty;

  private String sortByDistanceFeatureProperty;

  private List<Entry> mappingEntryList;

  public MetacardMapperImpl() {
    LOGGER.debug("Creating {}", MetacardMapperImpl.class.getName());
    mappingEntryList = new ArrayList<>();
  }

  public Optional<Entry> getEntry(Predicate<Entry> p) {
    if (p == null) {
      return Optional.empty();
    }

    return mappingEntryList.stream().filter(p).findFirst();
  }

  public String getFeatureType() {
    return this.featureType;
  }

  public void setFeatureType(String featureType) {
    LOGGER.debug("Setting feature type to: {}", featureType);
    this.featureType = featureType;
  }

  @Override
  public String getSortByTemporalFeatureProperty() {
    return this.sortByTemporalFeatureProperty;
  }

  public void setSortByTemporalFeatureProperty(String temporalFeatureProperty) {
    LOGGER.debug("Setting sortByTemporalFeatureProperty to: {}", temporalFeatureProperty);
    this.sortByTemporalFeatureProperty = temporalFeatureProperty;
  }

  @Override
  public String getSortByRelevanceFeatureProperty() {
    return this.sortByRelevanceFeatureProperty;
  }

  public void setSortByRelevanceFeatureProperty(String relevanceFeatureProperty) {
    LOGGER.debug("Setting sortByRelevanceFeatureProperty to: {}", relevanceFeatureProperty);
    this.sortByRelevanceFeatureProperty = relevanceFeatureProperty;
  }

  @Override
  public String getSortByDistanceFeatureProperty() {
    return this.sortByDistanceFeatureProperty;
  }

  public void setSortByDistanceFeatureProperty(String distanceFeatureProperty) {
    LOGGER.debug("Setting sortByDistanceFeatureProperty to: {}", distanceFeatureProperty);
    this.sortByDistanceFeatureProperty = distanceFeatureProperty;
  }

  @Override
  public String getDataUnit() {
    return dataUnit;
  }

  public void setDataUnit(String unit) {
    LOGGER.debug("Setting data unit to: {}", unit);
    dataUnit = unit;
  }

  public void addAttributeMapping(String attributeName, String featureName, String templateText) {
    LOGGER.debug(
        "Adding attribute mapping from: {} to: {} using: {}",
        attributeName,
        featureName,
        templateText);

    this.mappingEntryList.add(new FeatureAttributeEntry(attributeName, featureName, templateText));
  }

  /** {@inheritDoc} */
  public Stream<Entry> stream() {
    return mappingEntryList.stream();
  }

  protected List<Entry> getMappingEntryList() {
    return mappingEntryList;
  }

  /**
   * Sets a list of attribute mappings from a list of JSON strings.
   *
   * @param attributeMappingsList - a list of JSON-formatted `MetacardMapper.Entry` objects.
   */
  public void setAttributeMappings(@Nullable List<String> attributeMappingsList) {

    if (attributeMappingsList != null) {
      mappingEntryList.clear();

      attributeMappingsList
          .stream()
          .filter(StringUtils::isNotEmpty)
          .map(
              string -> {
                try {
                  return JsonFactory.create().readValue(string, Map.class);
                } catch (JsonException e) {
                  LOGGER.debug("Failed to parse attribute mapping json '{}'", string, e);
                }
                return null;
              })
          .filter(Objects::nonNull)
          .filter(map -> map.get(ATTRIBUTE_NAME) instanceof String)
          .filter(map -> map.get(FEATURE_NAME) instanceof String)
          .filter(map -> map.get(TEMPLATE) instanceof String)
          .forEach(
              map ->
                  addAttributeMapping(
                      (String) map.get(ATTRIBUTE_NAME),
                      (String) map.get(FEATURE_NAME),
                      (String) map.get(TEMPLATE)));
    }
  }
}
