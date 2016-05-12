/**
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
package org.codice.ddf.catalog.ui.query.cql;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.catalog.ui.query.delegate.WktQueryDelegate;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.io.ShapeReader;
import org.locationtech.spatial4j.shape.Shape;
import org.opengis.filter.sort.SortBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.action.Action;
import ddf.action.ActionRegistry;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.metacard.geojson.GeoJsonMetacardTransformer;

public class CqlResult {

    private static final Logger LOGGER = LoggerFactory.getLogger(CqlQueryResponse.class);

    private static final SpatialContext SPATIAL_CONTEXT = new JtsSpatialContextFactory().newSpatialContext();

    private static final ShapeReader WKT_READER = SPATIAL_CONTEXT.getFormats().getWktReader();

    private static final WktQueryDelegate WKT_QUERY_DELEGATE = new WktQueryDelegate();

    private static final String CACHED = "cached";

    private static final DateTimeFormatter ISO_8601_DATE_FORMAT = DateTimeFormat.forPattern(
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .withZoneUTC();

    private Map<String, Integer> matches;

    private Map<String, Object> metacard;

    private Double distance;

    private Double relevance;

    // TODO remove
    private Double testRelevance;

    private List<Action> actions;

    private boolean hasThumbnail = false;

    public CqlResult(Result result, Set<String> searchTerms, QueryRequest queryRequest,
            FilterAdapter filterAdapter, ActionRegistry actionRegistry) {
        Metacard mc = result.getMetacard();
        if (mc.getThumbnail() != null && mc.getThumbnail().length > 0) {
            hasThumbnail = true;
        }

        if (calculateDistance(queryRequest.getQuery())) {
            distance = normalizeDistance(result, queryRequest.getQuery(), filterAdapter);
        }

        if (calculateRelevance(queryRequest.getQuery())) {
            relevance = result.getRelevanceScore();
            matches = mc.getMetacardType()
                    .getAttributeDescriptors()
                    .stream()
                    .filter((descriptor1) -> isTextAttribute(descriptor1))
                    .filter(Objects::nonNull)
                    .map(descriptor -> mc.getAttribute(descriptor.getName()))
                    .filter(Objects::nonNull)
                    .map(attribute -> Optional.ofNullable(attribute.getValue()))
                    .map(Object::toString)
                    .map(value -> tokenize(value))
                    .flatMap(Collection::stream)
                    .map(token -> searchTerms.stream()
                            .filter(token::matches)
                            .collect(Collectors.toList()))
                    .flatMap(Collection::stream)
                    .map(match -> match.replace(".*", "%"))
                    .collect(Collectors.toMap(match -> match, value -> 1, Integer::sum));
        }

        actions = actionRegistry.list(result.getMetacard());
        metacard = metacardToMap(result);
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

    private void addCachedDate(Metacard metacard, Map<String, Object> json) {
        Attribute cachedDate = metacard.getAttribute(CACHED);
        if (cachedDate != null && cachedDate.getValue() != null) {
            json.put(CACHED, ISO_8601_DATE_FORMAT.print(new DateTime(cachedDate.getValue())));
        } else {
            json.put(CACHED, ISO_8601_DATE_FORMAT.print(new DateTime()));
        }
    }

    private Double normalizeDistance(Result result, Query query,
                                     FilterAdapter filterAdapter) {
        Double distance = result.getDistanceInMeters();

        try {
            String queryWkt = filterAdapter.adapt(query, WKT_QUERY_DELEGATE);
            if (StringUtils.isNotBlank(queryWkt)) {
                Shape queryShape = WKT_READER.read(queryWkt);
                if (result.getMetacard() != null && StringUtils.isNotBlank(result.getMetacard()
                        .getLocation())) {
                    Shape locationShape = WKT_READER.read(result.getMetacard()
                            .getLocation());

                    distance = DistanceUtils.degrees2Dist(SPATIAL_CONTEXT.calcDistance(locationShape.getCenter(),
                            queryShape.getCenter()), DistanceUtils.EARTH_MEAN_RADIUS_KM)
                            * 1000;
                }
            }
        } catch (IOException|ParseException|UnsupportedQueryException e) {
            LOGGER.debug("Unable to parse query wkt", e);
        }

        return distance;
    }

    public boolean calculateDistance(Query query) {
        return Result.DISTANCE.equals(getSortBy(query));
    }

    public boolean calculateRelevance(Query query) {
        return Result.RELEVANCE.equals(getSortBy(query));
    }

    public String getSortBy(Query query) {
        String result = null;
        SortBy sortBy = query.getSortBy();

        if (sortBy != null && sortBy.getPropertyName() != null) {
            result = sortBy.getPropertyName()
                    .getPropertyName();
        }

        return result;
    }

    private Map<String, Object> metacardToMap(Result result) {
        Map<String, Object> geoJson = null;
        MetacardImpl resultMetacard = new MetacardImpl(result.getMetacard());
        try {

            for (AttributeDescriptor descriptor : resultMetacard.getMetacardType()
                    .getAttributeDescriptors()) {
                switch (descriptor.getType()
                        .getAttributeFormat()) {
                    case BINARY:
                    case XML:
                    case OBJECT:
                        if (Metacard.THUMBNAIL.equals(descriptor.getName())) {
                            break;
                        }
                        resultMetacard.setAttribute(descriptor.getName(), null);
                    default:
                        break;
                }
            }

            geoJson = GeoJsonMetacardTransformer.convertToJSON(resultMetacard);
            addCachedDate(resultMetacard, geoJson);
        } catch (CatalogTransformerException e) {
            // TODO fix me
        }
        return geoJson;
    }

    private static List<String> tokenize(String value) {
        return StreamSupport.stream(Spliterators.spliterator(new Scanner(value.toLowerCase()).useDelimiter(
                "[\\s\\p{Punct}]+"), Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.NONNULL),
                false).collect(Collectors.toList());
    }

    private static boolean isTextAttribute(AttributeDescriptor descriptor) {
        switch (descriptor.getType()
                .getAttributeFormat()) {
        case STRING:
        case XML:
            return true;
        default:
            return false;
        }
    }

    public Map<String, Integer> getMatches() {
        return matches;
    }

    public Double getTestRelevance() {
        return testRelevance;
    }

    // TODO remove
    public void setTestRelevance(Double testRelevance) {
        this.testRelevance = testRelevance;
    }
}
