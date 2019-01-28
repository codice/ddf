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
package org.codice.ddf.opensearch.source;

import ddf.catalog.data.Metacard;
import ddf.catalog.impl.filter.TemporalFilter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.codice.ddf.opensearch.OpenSearchConstants;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.LikeFilterImpl;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.geotools.geometry.jts.spatialschema.geometry.GeometryImpl;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.SurfaceImpl;
import org.geotools.temporal.object.DefaultInstant;
import org.geotools.temporal.object.DefaultPeriodDuration;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.filter.And;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.BinaryTemporalOperator;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.TOverlaps;
import org.opengis.temporal.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSearchFilterVisitor extends DefaultFilterVisitor {
  private static final String ONLY_AND_MSG =
      "The OpenSearch Source only supports AND operations for non-contextual criteria.";

  private static final String NOT_OPERATOR_UNSUPPORTED_MSG =
      "The OpenSearch Source does not support NOT operation.";

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchFilterVisitor.class);

  @Override
  public Object visit(Not filter, Object data) {
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        getOpenSearchFilterVisitorObjectFromData(data);
    if (openSearchFilterVisitorObject == null) {
      return data;
    }

    Object newData;
    NestedTypes parentNest = openSearchFilterVisitorObject.getCurrentNest();
    LOGGER.trace("ENTERING: NOT filter");
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.NOT);
    newData = super.visit(filter, data);
    openSearchFilterVisitorObject.setCurrentNest(parentNest);
    LOGGER.trace("EXITING: NOT filter");

    return newData;
  }

  @Override
  public Object visit(Or filter, Object data) {
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        getOpenSearchFilterVisitorObjectFromData(data);
    if (openSearchFilterVisitorObject == null) {
      return data;
    }

    Object newData;
    NestedTypes parentNest = openSearchFilterVisitorObject.getCurrentNest();
    LOGGER.trace("ENTERING: OR filter");
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.OR);
    newData = super.visit(filter, data);
    openSearchFilterVisitorObject.setCurrentNest(parentNest);
    LOGGER.trace("EXITING: OR filter");

    return newData;
  }

  @Override
  public Object visit(And filter, Object data) {
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        getOpenSearchFilterVisitorObjectFromData(data);
    if (openSearchFilterVisitorObject == null) {
      return data;
    }

    Object newData;
    NestedTypes parentNest = openSearchFilterVisitorObject.getCurrentNest();
    LOGGER.trace("ENTERING: AND filter");
    openSearchFilterVisitorObject.setCurrentNest(NestedTypes.AND);
    newData = super.visit(filter, data);
    openSearchFilterVisitorObject.setCurrentNest(parentNest);
    LOGGER.trace("EXITING: AND filter");

    return newData;
  }

  /** DWithin filter maps to a Point/Radius distance Spatial search criteria. */
  @Override
  public Object visit(DWithin filter, Object data) {
    LOGGER.trace("ENTERING: DWithin filter");

    buildPointRadiusSearch(filter, data);

    LOGGER.trace("EXITING: DWithin filter");

    return super.visit(filter, data);
  }

  /** Contains filter maps to a Polygon or BBox Spatial search criteria. */
  @Override
  public Object visit(Contains filter, Object data) {
    LOGGER.trace("ENTERING: Contains filter");

    buildGeometrySearch(filter, data);

    LOGGER.trace("EXITING: Contains filter");

    return super.visit(filter, data);
  }

  /** Intersects filter maps to a Polygon or BBox Spatial search criteria. */
  @Override
  public Object visit(Intersects filter, Object data) {
    LOGGER.trace("ENTERING: Intersects filter");

    buildGeometrySearch(filter, data);

    LOGGER.trace("EXITING: Intersects filter");

    return super.visit(filter, data);
  }

  /** TOverlaps filter maps to a Temporal (Absolute and Offset) search criteria. */
  @Override
  public Object visit(TOverlaps filter, Object data) {
    LOGGER.trace("ENTERING: TOverlaps filter");

    buildTemporalSearch(
        filter,
        data,
        literal -> {
          final Date date = extractDate(literal);
          if (date != null) {
            return new DateRange(date, date);
          } else {
            final DateRange dateRange = extractDateRange(literal);
            if (dateRange != null) {
              return dateRange;
            } else {
              LOGGER.debug(
                  "Unable to extract date(s) from TOverlaps filter {}. Ignoring filter.", literal);
              return null;
            }
          }
        });

    LOGGER.trace("EXITING: TOverlaps filter");

    return super.visit(filter, data);
  }

  /** During filter maps to a Temporal (Absolute and Offset) search criteria. */
  @Override
  public Object visit(During filter, Object data) {
    LOGGER.trace("ENTERING: During filter");

    buildTemporalSearch(
        filter,
        data,
        literal -> {
          final DateRange dateRange = extractDateRange(literal);
          if (dateRange != null) {
            return dateRange;
          } else {
            LOGGER.debug(
                "Unable to extract date range from During filter {}. Ignoring filter.", literal);
            return null;
          }
        });

    LOGGER.trace("EXITING: During filter");

    return super.visit(filter, data);
  }

  /** PropertyIsLike filter maps to a Contextual search criteria. */
  @Override
  public Object visit(PropertyIsLike filter, Object data) {
    LOGGER.trace("ENTERING: PropertyIsLike filter");
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        getOpenSearchFilterVisitorObjectFromData(data);
    if (openSearchFilterVisitorObject == null) {
      return data;
    }

    if (openSearchFilterVisitorObject.getCurrentNest() != NestedTypes.NOT) {

      LikeFilterImpl likeFilter = (LikeFilterImpl) filter;

      AttributeExpressionImpl expression = (AttributeExpressionImpl) likeFilter.getExpression();
      String selectors = expression.getPropertyName();
      LOGGER.trace("selectors = {}", selectors);

      String searchPhrase =
          normalizePattern(
              likeFilter.getLiteral(),
              filter.getWildCard(),
              filter.getSingleChar(),
              filter.getEscape());
      LOGGER.trace("searchPhrase = [{}]", searchPhrase);

      final ContextualSearch contextualSearch = openSearchFilterVisitorObject.getContextualSearch();
      if (contextualSearch != null) {
        Map<String, String> searchPhraseMap = contextualSearch.getSearchPhraseMap();
        if (searchPhraseMap.containsKey(OpenSearchConstants.SEARCH_TERMS)) {
          searchPhraseMap.put(
              OpenSearchConstants.SEARCH_TERMS,
              searchPhraseMap.get(OpenSearchConstants.SEARCH_TERMS)
                  + OpenSearchConstants.SEARCH_TERMS_DELIMITER
                  + openSearchFilterVisitorObject.getCurrentNest()
                  + OpenSearchConstants.SEARCH_TERMS_DELIMITER
                  + searchPhrase);
        } else {
          searchPhraseMap.put(OpenSearchConstants.SEARCH_TERMS, searchPhrase);
        }
      } else {
        Map<String, String> searchPhraseMap = new HashMap<>();
        searchPhraseMap.put(OpenSearchConstants.SEARCH_TERMS, searchPhrase);
        openSearchFilterVisitorObject.setContextualSearch(
            new ContextualSearch(selectors, searchPhraseMap, likeFilter.isMatchingCase()));
      }
    }

    LOGGER.trace("EXITING: PropertyIsLike filter");

    return super.visit(filter, data);
  }

  /** PropertyIsEqualTo filter maps to a type/version criteria. */
  @Override
  public Object visit(PropertyIsEqualTo filter, Object data) {
    LOGGER.trace("ENTERING: PropertyIsEqualTo filter");
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        getOpenSearchFilterVisitorObjectFromData(data);
    if (openSearchFilterVisitorObject == null) {
      return data;
    }

    if (openSearchFilterVisitorObject.getCurrentNest() != NestedTypes.NOT) {
      final Expression expression1 = filter.getExpression1();
      if (expression1 instanceof PropertyName) {
        final String propertyName = ((PropertyName) expression1).getPropertyName();
        final String expectedPropertyIsEqualToTerm = Metacard.ID;
        if (expectedPropertyIsEqualToTerm.equals(propertyName)) {
          final Expression expression2 = filter.getExpression2();
          if (expression2 instanceof Literal) {
            openSearchFilterVisitorObject.setId((String) ((Literal) expression2).getValue());
          }
        } else {
          LOGGER.debug(
              "The OpenSearch Source only supports PropertyIsEqualTo criteria on the term \"{}\", but the property name is \"{}\". Ignoring filter.",
              expectedPropertyIsEqualToTerm,
              propertyName);
        }
      }
    }

    LOGGER.trace("EXITING: PropertyIsEqualTo filter");

    return super.visit(filter, data);
  }

  @Override
  public Object visit(PropertyName expression, Object data) {
    LOGGER.trace("Visiting PropertyName expression");
    return data;
  }

  /** {@link After} maps to a Temporal (Absolute and Offset) search criteria. */
  @Override
  public Object visit(After filter, Object data) {
    LOGGER.trace("ENTERING: After filter");

    buildTemporalSearch(
        filter,
        data,
        literal -> {
          final Date date = extractDate(literal);
          if (date != null) {
            Date end = new Date(Long.MAX_VALUE); // maximum date
            return new DateRange(date, end);
          } else {
            LOGGER.debug("Unable to extract date from After filter {}. Ignoring filter.", literal);
            return null;
          }
        });

    LOGGER.trace("EXITING: After filter");

    return super.visit(filter, data);
  }

  /** {@link Before} maps to a Temporal (Absolute and Offset) search criteria. */
  @Override
  public Object visit(Before filter, Object data) {
    LOGGER.trace("ENTERING: Before filter");

    buildTemporalSearch(
        filter,
        data,
        literal -> {
          final Date date = extractDate(literal);
          if (date != null) {
            Date start = new Date(0L); // minimum date
            return new DateRange(start, date);
          } else {
            LOGGER.debug("Unable to extract date from Before filter {}. Ignoring filter.", literal);
            return null;
          }
        });

    LOGGER.trace("EXITING: Before filter");

    return super.visit(filter, data);
  }

  @Override
  public Object visit(Literal expression, Object data) {
    LOGGER.trace("Visiting Literal expression");
    return data;
  }

  protected String normalizePattern(
      String pattern, String wildcard, String singleChar, String escapeChar) {
    StringBuilder sb = new StringBuilder(pattern.length());
    for (int i = 0; i < pattern.length(); i++) {
      char c = pattern.charAt(i);
      if (c == escapeChar.charAt(0)) {
        if (i + 1 < pattern.length()) {
          i++;
          char next = pattern.charAt(i);
          if ('*' == next || '?' == next || '\\' == next) {
            // target normalized character needs to be escaped
            sb.append("\\");
            sb.append(next);
          } else {
            // escaped character is not a normalized character
            // and does not need to be escaped anymore
            sb.append(next);
          }
        }
      } else if (c == singleChar.charAt(0)) {
        sb.append("?");
      } else if (c == wildcard.charAt(0)) {
        sb.append("*");
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  protected static OpenSearchFilterVisitorObject getOpenSearchFilterVisitorObjectFromData(
      Object data) {
    if (data instanceof OpenSearchFilterVisitorObject) {
      return (OpenSearchFilterVisitorObject) data;
    }
    return null;
  }

  protected static void buildPointRadiusSearch(DWithin filter, Object data) {
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        getOpenSearchFilterVisitorObjectFromData(data);
    if (openSearchFilterVisitorObject == null) {
      return;
    }

    if (NestedTypes.NOT.equals(openSearchFilterVisitorObject.getCurrentNest())) {
      LOGGER.debug(NOT_OPERATOR_UNSUPPORTED_MSG);
      return;
    }

    final org.opengis.filter.expression.Expression expression1 = filter.getExpression1();
    final String expectedSpatialSearchTerm = OpenSearchConstants.SUPPORTED_SPATIAL_SEARCH_TERM;
    if (!expectedSpatialSearchTerm.equals(expression1.toString())) {
      LOGGER.debug(
          "The OpenSearch Source only supports spatial criteria on the term \"{}\", but expression1 is \"{}\". Ignoring filter.",
          expectedSpatialSearchTerm,
          expression1);
      return;
    }

    // The geometry is wrapped in a <Literal> element, so have to get the geometry expression as a
    // literal and then evaluate it to get the geometry.
    // Example:
    // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl@dc33f184</ogc:Literal>
    Literal literalWrapper = (Literal) filter.getExpression2();
    Object geometryExpression = literalWrapper.getValue();

    double distance = filter.getDistance();

    final double radiusRangeLowerBound = 0;
    if (distance <= radiusRangeLowerBound) {
      LOGGER.debug(
          "Radius must be greater than {}. Ignoring DWithin filter.", radiusRangeLowerBound);
    } else if (geometryExpression instanceof PointImpl) {
      PointImpl point = (PointImpl) literalWrapper.evaluate(null);
      double[] coords = point.getCentroid().getCoordinate();
      LOGGER.trace("point: coords[0] = {},   coords[1] = {}", coords[0], coords[1]);
      LOGGER.trace("radius = {}", distance);
      openSearchFilterVisitorObject.addPointRadiusSearch(
          new PointRadius(coords[0], coords[1], distance));
    } else if (geometryExpression instanceof Point) {
      Point point = (Point) literalWrapper.evaluate(null);
      Coordinate coords = point.getCoordinate();
      LOGGER.trace("point: coords.x = {},   coords.y = {}", coords.x, coords.y);
      LOGGER.trace("radius = {}", distance);
      openSearchFilterVisitorObject.addPointRadiusSearch(
          new PointRadius(coords.x, coords.y, distance));
    } else {
      LOGGER.debug(
          "The OpenSearch Source only supports POINT geometry WKT for DWithin filter, but the geometry is {}.",
          geometryExpression);
    }
  }

  protected static void buildGeometrySearch(BinarySpatialOperator filter, Object data) {
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        getOpenSearchFilterVisitorObjectFromData(data);
    if (openSearchFilterVisitorObject == null) {
      return;
    }

    if (NestedTypes.NOT.equals(openSearchFilterVisitorObject.getCurrentNest())) {
      LOGGER.debug(NOT_OPERATOR_UNSUPPORTED_MSG);
      return;
    }

    final org.opengis.filter.expression.Expression expression1 = filter.getExpression1();
    final String expectedSpatialSearchTerm = OpenSearchConstants.SUPPORTED_SPATIAL_SEARCH_TERM;
    if (!expectedSpatialSearchTerm.equals(expression1.toString())) {
      LOGGER.debug(
          "Opensearch only supports spatial criteria on the term \"{}\", but expression1 is \"{}\". Ignoring filter.",
          expectedSpatialSearchTerm,
          expression1);
      return;
    }

    // The geometry is wrapped in a <Literal> element, so have to get the geometry expression as a
    // literal and then evaluate it to get the geometry.
    // Example:
    // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl@dc33f184</ogc:Literal>
    Literal literalWrapper = (Literal) filter.getExpression2();
    Object geometryExpression = literalWrapper.getValue();

    if (geometryExpression instanceof SurfaceImpl) {
      SurfaceImpl surface = (SurfaceImpl) literalWrapper.evaluate(null);
      Polygon polygon = (Polygon) surface.getJTSGeometry();
      openSearchFilterVisitorObject.addGeometrySearch(polygon);
    } else if (geometryExpression instanceof Polygon) {
      Polygon polygon = (Polygon) literalWrapper.evaluate(null);
      openSearchFilterVisitorObject.addGeometrySearch(polygon);
    } else if (geometryExpression instanceof GeometryImpl) {
      Geometry polygon = ((GeometryImpl) geometryExpression).getJTSGeometry();
      openSearchFilterVisitorObject.addGeometrySearch(polygon);
    } else if (geometryExpression instanceof MultiPolygon) {
      Geometry polygon = ((MultiPolygon) geometryExpression);
      openSearchFilterVisitorObject.addGeometrySearch(polygon);
    } else {
      LOGGER.debug("Unsupported filter constraint");
    }
  }

  protected static void buildTemporalSearch(
      final BinaryTemporalOperator filter,
      final Object data,
      final Function<Object, DateRange> literalToDatesFunction) {
    final OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        getOpenSearchFilterVisitorObjectFromData(data);
    if (openSearchFilterVisitorObject == null) {
      return;
    }

    if (openSearchFilterVisitorObject.getCurrentNest() != null
        && !NestedTypes.AND.equals(openSearchFilterVisitorObject.getCurrentNest())) {
      LOGGER.debug(ONLY_AND_MSG);
      return;
    }

    final org.opengis.filter.expression.Expression expression1 = filter.getExpression1();
    final String expectedTemporalSearchTerm = OpenSearchConstants.SUPPORTED_TEMPORAL_SEARCH_TERM;
    if (!expectedTemporalSearchTerm.equals(expression1.toString())) {
      LOGGER.debug(
          "The OpenSearch Source only supports temporal criteria on the term \"{}\", but expression1 is \"{}\". Ignoring filter.",
          expectedTemporalSearchTerm,
          expression1);
      return;
    }

    // The geometry is wrapped in a <Literal> element, so have to get the geometry expression as a
    // literal and then evaluate it to get the geometry.
    // Example:
    // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl@dc33f184</ogc:Literal>
    final Literal literalWrapper = (Literal) filter.getExpression2();
    LOGGER.trace("literalWrapper.getValue() = {}", literalWrapper.getValue());
    final Object literal = literalWrapper.evaluate(null);

    final DateRange dateRange = literalToDatesFunction.apply(literal);
    if (dateRange != null) {
      openSearchFilterVisitorObject.setTemporalSearch(
          new TemporalFilter(dateRange.getStart(), dateRange.getEnd()));
    }
  }

  protected static Date extractDate(Object literal) {
    if (literal instanceof DefaultInstant) {
      final DefaultInstant defaultInstant = (DefaultInstant) literal;

      // Extract date from the filter
      return defaultInstant.getPosition().getDate();
    } else if (literal instanceof Date) {
      return (Date) literal;
    } else {
      return null;
    }
  }

  protected static DateRange extractDateRange(Object literal) {
    if (literal instanceof Period) {
      final Period period = (Period) literal;

      // Extract the start and end dates from the filter
      return new DateRange(
          period.getBeginning().getPosition().getDate(),
          period.getEnding().getPosition().getDate());
    } else if (literal instanceof DefaultPeriodDuration) {
      final DefaultPeriodDuration duration = (DefaultPeriodDuration) literal;

      // Extract the end date from the filter
      final Date end = Calendar.getInstance().getTime(); // current date
      final Date start = new Date(end.getTime() - duration.getTimeInMillis());
      return new DateRange(start, end);
    } else {
      return null;
    }
  }

  protected static class DateRange {

    private final Date start;

    private final Date end;

    public DateRange(Date start, Date end) {
      this.start = start;
      this.end = end;
    }

    public Date getStart() {
      return start;
    }

    public Date getEnd() {
      return end;
    }
  }
}
