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
package org.codice.ddf.catalog.twitter.source;

import ddf.catalog.impl.filter.TemporalFilter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.LikeFilterImpl;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.SurfaceImpl;
import org.geotools.temporal.object.DefaultPeriodDuration;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.temporal.BinaryTemporalOperator;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.TOverlaps;
import org.opengis.temporal.Period;
import org.opengis.temporal.PeriodDuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TwitterFilterVisitor extends DefaultFilterVisitor {
  private static final String ONLY_AND_MSG =
      "Twitter only supports AND operations for non-contextual criteria.";

  private static final Logger LOGGER = LoggerFactory.getLogger(TwitterFilterVisitor.class);

  private List<Filter> filters;

  // Can only have one each of each type of filter in an Twitter query
  private ContextualSearch contextualSearch;

  private TemporalFilter temporalSearch;

  private double latitude;

  private double longitude;

  private double radius;

  private boolean hasSpatial;

  private NestedTypes currentNest = null;

  public TwitterFilterVisitor() {
    filters = new ArrayList<>();

    contextualSearch = null;
    temporalSearch = null;
  }

  @Override
  public Object visit(Not filter, Object data) {
    Object newData;
    NestedTypes parentNest = currentNest;
    LOGGER.trace("ENTERING: NOT filter");
    currentNest = NestedTypes.NOT;
    filters.add(filter);
    newData = super.visit(filter, data);
    currentNest = parentNest;
    LOGGER.trace("EXITING: NOT filter");

    return newData;
  }

  @Override
  public Object visit(Or filter, Object data) {
    Object newData;
    NestedTypes parentNest = currentNest;
    LOGGER.trace("ENTERING: OR filter");
    currentNest = NestedTypes.OR;
    filters.add(filter);
    newData = super.visit(filter, data);
    currentNest = parentNest;
    LOGGER.trace("EXITING: OR filter");

    return newData;
  }

  @Override
  public Object visit(And filter, Object data) {
    Object newData;
    NestedTypes parentNest = currentNest;
    LOGGER.trace("ENTERING: AND filter");
    currentNest = NestedTypes.AND;
    filters.add(filter);
    newData = super.visit(filter, data);
    currentNest = parentNest;
    LOGGER.trace("EXITING: AND filter");

    return newData;
  }

  /** DWithin filter maps to a Point/Radius distance Spatial search criteria. */
  @Override
  public Object visit(DWithin filter, Object data) {
    LOGGER.trace("ENTERING: DWithin filter");
    if (currentNest == null || NestedTypes.AND.equals(currentNest)) {
      // The geometric point is wrapped in a <Literal> element, so have to
      // get geometry expression as literal and then evaluate it to get
      // the geometry.
      // Example:
      // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl@dc33f184</ogc:Literal>
      Literal literalWrapper = (Literal) filter.getExpression2();

      // Luckily we know what type the geometry expression should be, so
      // we
      // can cast it
      Point point = (Point) literalWrapper.evaluate(null);
      Coordinate coords = point.getCentroid().getCoordinate();
      double distance = filter.getDistance();

      LOGGER.debug("point: coords[0] = {},   coords[1] = {}", coords.x, coords.y);
      LOGGER.debug("radius = {}", distance);

      longitude = coords.x;
      latitude = coords.y;
      radius = distance / 1000;

      hasSpatial = true;

      filters.add(filter);
    } else {
      LOGGER.warn(ONLY_AND_MSG);
    }

    LOGGER.trace("EXITING: DWithin filter");

    return super.visit(filter, data);
  }

  /** Contains filter maps to a Polygon or BBox Spatial search criteria. */
  @Override
  public Object visit(Contains filter, Object data) {
    LOGGER.trace("ENTERING: Contains filter");
    if (currentNest == null || NestedTypes.AND.equals(currentNest)) {
      // The geometric point is wrapped in a <Literal> element, so have to
      // get geometry expression as literal and then evaluate it to get
      // the geometry.
      // Example:
      // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.SurfaceImpl@64a7c45e</ogc:Literal>
      Literal literalWrapper = (Literal) filter.getExpression2();
      Object geometryExpression = literalWrapper.getValue();

      if (geometryExpression instanceof SurfaceImpl) {
        SurfaceImpl polygon = (SurfaceImpl) literalWrapper.evaluate(null);

        Point point = polygon.getJTSGeometry().getCentroid();

        longitude = point.getX();

        latitude = point.getY();

        radius = point.getBoundary().getLength() * 10;

        hasSpatial = true;

        filters.add(filter);

      } else if (geometryExpression instanceof Polygon) {
        Polygon polygon = (Polygon) geometryExpression;
        Point centroid = polygon.getCentroid();
        longitude = centroid.getX();
        latitude = centroid.getY();
        radius = polygon.getBoundary().getLength() * 10;
        hasSpatial = true;

        filters.add(filter);
      } else {
        LOGGER.warn("Only POLYGON geometry WKT for Contains filter is supported");
      }
    } else {
      LOGGER.warn(ONLY_AND_MSG);
    }

    LOGGER.trace("EXITING: Contains filter");

    return super.visit(filter, data);
  }

  /** Intersects filter maps to a Polygon or BBox Spatial search criteria. */
  @Override
  public Object visit(Intersects filter, Object data) {
    LOGGER.trace("ENTERING: Intersects filter");
    if (currentNest == null || NestedTypes.AND.equals(currentNest)) {
      // The geometric point is wrapped in a <Literal> element, so have to
      // get geometry expression as literal and then evaluate it to get
      // the geometry.
      // Example:
      // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.SurfaceImpl@64a7c45e</ogc:Literal>
      Literal literalWrapper = (Literal) filter.getExpression2();
      Object geometryExpression = literalWrapper.getValue();

      if (geometryExpression instanceof SurfaceImpl) {
        SurfaceImpl polygon = (SurfaceImpl) literalWrapper.evaluate(null);

        Point point = polygon.getJTSGeometry().getCentroid();

        longitude = point.getX();

        latitude = point.getY();

        radius = point.getBoundary().getLength() * 10;

        hasSpatial = true;

        filters.add(filter);
      } else if (geometryExpression instanceof Polygon) {
        Polygon polygon = (Polygon) geometryExpression;
        Point centroid = polygon.getCentroid();
        longitude = centroid.getX();
        latitude = centroid.getY();
        radius = polygon.getBoundary().getLength() * 10;
        hasSpatial = true;

        filters.add(filter);
      } else {
        LOGGER.warn("Only POLYGON geometry WKT for Intersects filter is supported");
      }
    } else {
      LOGGER.warn(ONLY_AND_MSG);
    }

    LOGGER.trace("EXITING: Intersects filter");

    return super.visit(filter, data);
  }

  /** TOverlaps filter maps to a Temporal (Absolute and Offset) search criteria. */
  @Override
  public Object visit(TOverlaps filter, Object data) {
    LOGGER.trace("ENTERING: TOverlaps filter");
    if (currentNest == null || NestedTypes.AND.equals(currentNest)) {
      handleTemporal(filter);
    } else {
      LOGGER.warn(ONLY_AND_MSG);
    }
    LOGGER.trace("EXITING: TOverlaps filter");

    return super.visit(filter, data);
  }

  /** During filter maps to a Temporal (Absolute and Offset) search criteria. */
  @Override
  public Object visit(During filter, Object data) {
    LOGGER.trace("ENTERING: TOverlaps filter");
    if (currentNest == null || NestedTypes.AND.equals(currentNest)) {
      handleTemporal(filter);
    } else {
      LOGGER.warn(ONLY_AND_MSG);
    }
    LOGGER.trace("EXITING: TOverlaps filter");

    return super.visit(filter, data);
  }

  private void handleTemporal(BinaryTemporalOperator filter) {

    Literal literalWrapper = (Literal) filter.getExpression2();
    LOGGER.debug("literalWrapper.getValue() = {}", literalWrapper.getValue());

    Object literal = literalWrapper.evaluate(null);
    if (literal instanceof Period) {
      Period period = (Period) literal;

      // Extract the start and end dates from the filter
      Date start = period.getBeginning().getPosition().getDate();
      Date end = period.getEnding().getPosition().getDate();

      temporalSearch = new TemporalFilter(start, end);

      filters.add(filter);
    } else if (literal instanceof PeriodDuration) {

      DefaultPeriodDuration duration = (DefaultPeriodDuration) literal;

      // Extract the start and end dates from the filter
      Date end = Calendar.getInstance().getTime();
      Date start = new Date(end.getTime() - duration.getTimeInMillis());

      temporalSearch = new TemporalFilter(start, end);

      filters.add(filter);
    }
  }

  /** PropertyIsEqualTo filter maps to a Type/Version(s) search criteria. */
  @Override
  public Object visit(PropertyIsEqualTo filter, Object data) {
    LOGGER.trace("ENTERING: PropertyIsEqualTo filter");

    filters.add(filter);

    LOGGER.trace("EXITING: PropertyIsEqualTo filter");

    return super.visit(filter, data);
  }

  /** PropertyIsLike filter maps to a Contextual search criteria. */
  @Override
  public Object visit(PropertyIsLike filter, Object data) {
    LOGGER.trace("ENTERING: PropertyIsLike filter");

    if (currentNest != NestedTypes.NOT) {

      LikeFilterImpl likeFilter = (LikeFilterImpl) filter;

      AttributeExpressionImpl expression = (AttributeExpressionImpl) likeFilter.getExpression();
      String selectors = expression.getPropertyName();
      LOGGER.debug("selectors = {}", selectors);

      String searchPhrase = likeFilter.getLiteral();
      LOGGER.debug("searchPhrase = [{}]", searchPhrase);
      if (contextualSearch != null) {
        contextualSearch.setSearchPhrase(
            contextualSearch.getSearchPhrase() + " " + currentNest.toString() + " " + searchPhrase);
      } else {
        contextualSearch =
            new ContextualSearch(selectors, searchPhrase, likeFilter.isMatchingCase());
      }
    }

    LOGGER.trace("EXITING: PropertyIsLike filter");

    return super.visit(filter, data);
  }

  @Override
  public Object visit(PropertyName expression, Object data) {
    LOGGER.trace("ENTERING: PropertyName expression");

    // countOccurrence( expression );

    LOGGER.trace("EXITING: PropertyName expression");

    return data;
  }

  @Override
  public Object visit(Literal expression, Object data) {
    LOGGER.trace("ENTERING: Literal expression");

    // countOccurrence( expression );

    LOGGER.trace("EXITING: Literal expression");

    return data;
  }

  public List<Filter> getFilters() {
    return filters;
  }

  public ContextualSearch getContextualSearch() {
    return contextualSearch;
  }

  public TemporalFilter getTemporalSearch() {
    return temporalSearch;
  }

  public double getLatitude() {
    return latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public double getRadius() {
    return radius;
  }

  public boolean hasSpatial() {
    return hasSpatial;
  }

  private enum NestedTypes {
    AND,
    OR,
    NOT
  }
}
