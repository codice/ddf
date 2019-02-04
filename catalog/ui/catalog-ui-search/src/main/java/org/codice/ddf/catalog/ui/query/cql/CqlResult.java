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
package org.codice.ddf.catalog.ui.query.cql;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ddf.action.Action;
import ddf.action.ActionRegistry;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.metacard.propertyjson.PropertyJsonMetacardTransformer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.catalog.ui.query.delegate.SearchTerm;
import org.codice.ddf.catalog.ui.query.delegate.WktQueryDelegate;
import org.codice.ddf.catalog.ui.transformer.TransformerDescriptors;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.SpatialContextFactory;
import org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory;
import org.locationtech.spatial4j.context.jts.ValidationRule;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.io.ShapeReader;
import org.locationtech.spatial4j.shape.Shape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CqlResult {

  private static final Logger LOGGER = LoggerFactory.getLogger(CqlQueryResponse.class);

  // For queries we use repairConvexHull which my cause false positives to be returned but this
  // is better than potentially missing some results due to false negatives.
  private static final Map<String, String> SPATIAL_CONTEXT_ARGUMENTS =
      ImmutableMap.of(
          "spatialContextFactory",
          JtsSpatialContextFactory.class.getName(),
          "validationRule",
          ValidationRule.repairConvexHull.name(),
          "allowMultiOverlap",
          "true");

  private static final SpatialContext SPATIAL_CONTEXT =
      SpatialContextFactory.makeSpatialContext(
          SPATIAL_CONTEXT_ARGUMENTS, CqlResult.class.getClassLoader());

  private static final ShapeReader WKT_READER = SPATIAL_CONTEXT.getFormats().getWktReader();

  private static final WktQueryDelegate WKT_QUERY_DELEGATE = new WktQueryDelegate();

  private static final String CACHED = "cached";

  private static final DateTimeFormatter ISO_8601_DATE_FORMAT =
      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").withZoneUTC();

  private Map<String, Integer> matches = new HashMap<>();

  private Map<String, Object> metacard;

  private Double distance;

  private Double relevance;

  private List<Action> actions;

  private boolean hasThumbnail;

  private boolean isResourceLocal;

  public CqlResult(CqlResult result, TransformerDescriptors descriptors) {
    this.hasThumbnail = result.getHasThumbnail();
    this.isResourceLocal = result.getIsResourceLocal();
    this.distance = result.getDistance();
    this.relevance = result.getRelevance();
    this.metacard = result.getMetacard();
    this.actions =
        result
            .getActions()
            .stream()
            .map(
                action ->
                    new DisplayableAction(
                        action, getDisplayName(descriptors, action.getId(), action.getTitle())))
            .collect(Collectors.toList());
  }

  public CqlResult(
      Result result,
      Set<SearchTerm> searchTerms,
      QueryRequest queryRequest,
      boolean normalize,
      FilterAdapter filterAdapter,
      ActionRegistry actionRegistry) {

    Metacard mc = result.getMetacard();

    hasThumbnail =
        Optional.of(mc).map(Metacard::getThumbnail).map(thumb -> thumb.length > 0).orElse(false);

    isResourceLocal =
        Optional.of(mc)
            .map(m -> m.getAttribute("internal.local-resource"))
            .map(Attribute::getValue)
            .filter(Boolean.class::isInstance)
            .map(Boolean.class::cast)
            .orElse(false);

    distance = normalizeDistance(result, queryRequest.getQuery(), filterAdapter);

    relevance = result.getRelevanceScore();
    if (normalize) {
      countMatches(searchTerms, mc);
    }

    actions =
        actionRegistry
            .list(result.getMetacard())
            .stream()
            .map(action -> new DisplayableAction(action, action.getId()))
            .collect(Collectors.toList());
    metacard = metacardToMap(result);
  }

  private String getDisplayName(TransformerDescriptors descriptors, String id, String title) {
    Map<String, String> transformerDescriptor = descriptors.getMetacardTransformer(id);

    if (transformerDescriptor != null) {
      return transformerDescriptor.get("displayName");
    }

    return title.replaceFirst("^Export( as)?\\s+\\b", "");
  }

  private void countMatches(Set<SearchTerm> searchTerms, Metacard mc) {
    List<String> textAttributes =
        mc.getMetacardType()
            .getAttributeDescriptors()
            .stream()
            .filter(Objects::nonNull)
            .filter(CqlResult::isTextAttribute)
            .map(descriptor -> mc.getAttribute(descriptor.getName()))
            .filter(Objects::nonNull)
            .map(attribute -> Optional.ofNullable(attribute.getValue()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(Object::toString)
            .collect(Collectors.toList());

    List<SearchTerm> terms =
        searchTerms
            .stream()
            .filter(term -> !"*".equals(term.getTerm()))
            .collect(Collectors.toList());

    int totalTokens = 0;
    for (String value : textAttributes) {
      BufferedReader reader = new BufferedReader(new StringReader(value.toLowerCase()));
      String line;
      try {
        while ((line = reader.readLine()) != null) {
          String[] tokens = line.split("[\\s\\p{Punct}]+");
          for (String token : tokens) {
            totalTokens++;
            for (SearchTerm term : terms) {
              if (term.match(token)) {
                matches.put(term.getTerm(), matches.getOrDefault(term.getTerm(), 0) + 1);
              }
            }
          }
        }
      } catch (IOException e) {
        LOGGER.debug("Unable to read line", e);
      }
      matches.put("*", totalTokens);
    }
  }

  private void addCachedDate(Metacard metacard, Map<String, Object> json) {
    Attribute cachedDate = metacard.getAttribute(CACHED);
    if (cachedDate != null && cachedDate.getValue() != null) {
      json.put(CACHED, ISO_8601_DATE_FORMAT.print(new DateTime(cachedDate.getValue())));
    } else {
      json.put(CACHED, ISO_8601_DATE_FORMAT.print(new DateTime()));
    }
  }

  private Double normalizeDistance(Result result, Query query, FilterAdapter filterAdapter) {
    Double resultDistance = result.getDistanceInMeters();

    try {
      String queryWkt = filterAdapter.adapt(query, WKT_QUERY_DELEGATE);
      if (StringUtils.isNotBlank(queryWkt)) {
        Shape queryShape = WKT_READER.read(queryWkt);
        if (result.getMetacard() != null
            && StringUtils.isNotBlank(result.getMetacard().getLocation())) {
          Shape locationShape = WKT_READER.read(result.getMetacard().getLocation());

          resultDistance =
              DistanceUtils.degrees2Dist(
                      SPATIAL_CONTEXT.calcDistance(
                          locationShape.getCenter(), queryShape.getCenter()),
                      DistanceUtils.EARTH_MEAN_RADIUS_KM)
                  * 1000;
        }
      }
    } catch (IOException | ParseException | UnsupportedQueryException e) {
      LOGGER.debug("Unable to parse query wkt", e);
    }

    if (resultDistance != null && (resultDistance < 0 || resultDistance > Double.MAX_VALUE)) {
      resultDistance = null;
    }
    return resultDistance;
  }

  private Map<String, Object> metacardToMap(Result result) {
    Map<String, Object> geoJson = null;
    MetacardImpl resultMetacard =
        new MetacardImpl(result.getMetacard(), result.getMetacard().getMetacardType());
    try {

      for (AttributeDescriptor descriptor :
          resultMetacard.getMetacardType().getAttributeDescriptors()) {
        switch (descriptor.getType().getAttributeFormat()) {
          case BINARY:
          case XML:
          case OBJECT:
            resultMetacard.setAttribute(descriptor.getName(), null);
            break;
          default:
            break;
        }
      }

      geoJson =
          PropertyJsonMetacardTransformer.convertToJSON(
              resultMetacard,
              ImmutableList.of(
                  AttributeType.AttributeFormat.BINARY,
                  AttributeType.AttributeFormat.XML,
                  AttributeType.AttributeFormat.OBJECT));
      addCachedDate(resultMetacard, geoJson);
    } catch (CatalogTransformerException e) {
      LOGGER.debug("Unable to convert metacard to GeoJSON", e);
    }
    return geoJson;
  }

  private static boolean isTextAttribute(AttributeDescriptor descriptor) {
    switch (descriptor.getType().getAttributeFormat()) {
      case STRING:
      case XML:
        return true;
      default:
        return false;
    }
  }

  public Map<String, Object> getMetacard() {
    return metacard;
  }

  public Double getDistance() {
    return distance;
  }

  public Double getRelevance() {
    return relevance;
  }

  public List<Action> getActions() {
    return actions;
  }

  public boolean getHasThumbnail() {
    return hasThumbnail;
  }

  public boolean getIsResourceLocal() {
    return isResourceLocal;
  }

  public Map<String, Integer> getMatches() {
    return matches;
  }
}
