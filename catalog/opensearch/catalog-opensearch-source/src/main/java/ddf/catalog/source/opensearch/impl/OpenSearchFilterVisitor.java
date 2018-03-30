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
package ddf.catalog.source.opensearch.impl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTWriter;
import ddf.catalog.data.Metacard;
import ddf.catalog.filter.impl.PropertyIsEqualToLiteral;
import ddf.catalog.impl.filter.SpatialDistanceFilter;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.Expression;
import org.geotools.filter.IsEqualsToImpl;
import org.geotools.filter.LikeFilterImpl;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.SurfaceImpl;
import org.geotools.temporal.object.DefaultPeriodDuration;
import org.opengis.filter.And;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BinarySpatialOperator;
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

public class OpenSearchFilterVisitor extends DefaultFilterVisitor {
  private static final String ONLY_AND_MSG =
      "Opensearch only supports AND operations for non-contextual criteria.";

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
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        getOpenSearchFilterVisitorObjectFromData(data);
    if (openSearchFilterVisitorObject == null) {
      return data;
    }

    if (openSearchFilterVisitorObject.getCurrentNest() == null
        || NestedTypes.AND.equals(openSearchFilterVisitorObject.getCurrentNest())) {
      // The geometric point is wrapped in a <Literal> element, so have to
      // get geometry expression as literal and then evaluate it to get
      // the geometry.
      // Example:
      // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl@dc33f184</ogc:Literal>
      Literal literalWrapper = (Literal) filter.getExpression2();
      Object geometryExpression = literalWrapper.getValue();

      double distance = filter.getDistance();

      if (geometryExpression instanceof PointImpl) {
        PointImpl point = (PointImpl) literalWrapper.evaluate(null);
        double[] coords = point.getCentroid().getCoordinate();
        LOGGER.trace("point: coords[0] = {},   coords[1] = {}", coords[0], coords[1]);
        LOGGER.trace("radius = {}", distance);
        openSearchFilterVisitorObject.setSpatialSearch(
            new SpatialDistanceFilter(coords[0], coords[1], distance));
      } else if (geometryExpression instanceof Point) {
        Point point = (Point) literalWrapper.evaluate(null);
        Coordinate coords = point.getCoordinate();
        LOGGER.trace("point: coords.x = {},   coords.y = {}", coords.x, coords.y);
        LOGGER.trace("radius = {}", distance);
        openSearchFilterVisitorObject.setSpatialSearch(
            new SpatialDistanceFilter(coords.x, coords.y, distance));
      } else {
        LOGGER.debug("Only POINT geometry WKT for DWithin filter is supported");
      }
    } else {
      LOGGER.debug(ONLY_AND_MSG);
    }

    LOGGER.trace("EXITING: DWithin filter");

    return super.visit(filter, data);
  }

  /** Contains filter maps to a Polygon or BBox Spatial search criteria. */
  @Override
  public Object visit(Contains filter, Object data) {
    LOGGER.trace("ENTERING: Contains filter");

    buildSpatialSearch(filter, data);

    LOGGER.trace("EXITING: Contains filter");

    return super.visit(filter, data);
  }

  /** Intersects filter maps to a Polygon or BBox Spatial search criteria. */
  @Override
  public Object visit(Intersects filter, Object data) {
    LOGGER.trace("ENTERING: Intersects filter");

    buildSpatialSearch(filter, data);

    LOGGER.trace("EXITING: Intersects filter");

    return super.visit(filter, data);
  }

  private void buildSpatialSearch(BinarySpatialOperator filter, Object data) {
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        getOpenSearchFilterVisitorObjectFromData(data);
    if (openSearchFilterVisitorObject == null) {
      return;
    }

    if (openSearchFilterVisitorObject.getCurrentNest() == null
        || NestedTypes.AND.equals(openSearchFilterVisitorObject.getCurrentNest())) {
      // The geometric point is wrapped in a <Literal> element, so have to
      // get geometry expression as literal and then evaluate it to get
      // the geometry.
      // Example:
      // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.SurfaceImpl@64a7c45e</ogc:Literal>
      Literal literalWrapper = (Literal) filter.getExpression2();
      Object geometryExpression = literalWrapper.getValue();
      WKTWriter wktWriter = new WKTWriter();

      if (geometryExpression instanceof SurfaceImpl) {
        SurfaceImpl surface = (SurfaceImpl) literalWrapper.evaluate(null);
        Polygon polygon = (Polygon) surface.getJTSGeometry();
        openSearchFilterVisitorObject.setSpatialSearch(new SpatialFilter(wktWriter.write(polygon)));
      } else if (geometryExpression instanceof Polygon) {
        Polygon polygon = (Polygon) literalWrapper.evaluate(null);
        openSearchFilterVisitorObject.setSpatialSearch(new SpatialFilter(wktWriter.write(polygon)));
      } else {
        LOGGER.debug("Only POLYGON geometry WKT for Contains/Intersects filter is supported");
      }
    } else {
      LOGGER.debug(ONLY_AND_MSG);
    }
  }

  /** TOverlaps filter maps to a Temporal (Absolute and Offset) search criteria. */
  @Override
  public Object visit(TOverlaps filter, Object data) {
    LOGGER.trace("ENTERING: TOverlaps filter");
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        getOpenSearchFilterVisitorObjectFromData(data);
    if (openSearchFilterVisitorObject == null) {
      return data;
    }

    if (openSearchFilterVisitorObject.getCurrentNest() == null
        || NestedTypes.AND.equals(openSearchFilterVisitorObject.getCurrentNest())) {
      handleTemporal(filter, openSearchFilterVisitorObject);
    } else {
      LOGGER.debug(ONLY_AND_MSG);
    }
    LOGGER.trace("EXITING: TOverlaps filter");

    return super.visit(filter, data);
  }

  /** During filter maps to a Temporal (Absolute and Offset) search criteria. */
  @Override
  public Object visit(During filter, Object data) {
    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        getOpenSearchFilterVisitorObjectFromData(data);
    if (openSearchFilterVisitorObject == null) {
      return data;
    }

    LOGGER.trace("ENTERING: TOverlaps filter");
    if (openSearchFilterVisitorObject.getCurrentNest() == null
        || NestedTypes.AND.equals(openSearchFilterVisitorObject.getCurrentNest())) {
      handleTemporal(filter, openSearchFilterVisitorObject);
    } else {
      LOGGER.debug(ONLY_AND_MSG);
    }
    LOGGER.trace("EXITING: TOverlaps filter");

    return super.visit(filter, data);
  }

  private void handleTemporal(
      BinaryTemporalOperator filter, OpenSearchFilterVisitorObject openSearchFilterVisitorObject) {

    Literal literalWrapper = (Literal) filter.getExpression2();
    LOGGER.trace("literalWrapper.getValue() = {}", literalWrapper.getValue());

    Object literal = literalWrapper.evaluate(null);
    if (literal instanceof Period) {
      Period period = (Period) literal;

      // Extract the start and end dates from the filter
      Date start = period.getBeginning().getPosition().getDate();
      Date end = period.getEnding().getPosition().getDate();

      openSearchFilterVisitorObject.setTemporalSearch(new TemporalFilter(start, end));

    } else if (literal instanceof PeriodDuration) {

      DefaultPeriodDuration duration = (DefaultPeriodDuration) literal;

      // Extract the start and end dates from the filter
      Date end = Calendar.getInstance().getTime();
      Date start = new Date(end.getTime() - duration.getTimeInMillis());

      openSearchFilterVisitorObject.setTemporalSearch(new TemporalFilter(start, end));
    }
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
      LOGGER.debug("selectors = {}", selectors);

      String searchPhrase =
          normalizePattern(
              likeFilter.getLiteral(),
              filter.getWildCard(),
              filter.getSingleChar(),
              filter.getEscape());
      LOGGER.debug("searchPhrase = [{}]", searchPhrase);
      if (openSearchFilterVisitorObject.getContextualSearch() != null) {
        Map<String, String> searchPhraseMap =
            openSearchFilterVisitorObject.getContextualSearch().getSearchPhraseMap();
        if (searchPhraseMap.containsKey(OpenSearchParserImpl.SEARCH_TERMS)) {
          searchPhraseMap.put(
              OpenSearchParserImpl.SEARCH_TERMS,
              searchPhraseMap.get(OpenSearchParserImpl.SEARCH_TERMS)
                  + " "
                  + openSearchFilterVisitorObject.getCurrentNest()
                  + " "
                  + searchPhrase);
        } else {
          searchPhraseMap.put(OpenSearchParserImpl.SEARCH_TERMS, searchPhrase);
        }
      } else {
        Map<String, String> searchPhraseMap = new HashMap<>();
        searchPhraseMap.put(OpenSearchParserImpl.SEARCH_TERMS, searchPhrase);
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
      if (filter instanceof IsEqualsToImpl) {
        IsEqualsToImpl isEqualsTo = (IsEqualsToImpl) filter;
        Expression leftValue = isEqualsTo.getLeftValue();
        if (Metacard.ID.equals(leftValue.toString())) {
          openSearchFilterVisitorObject.setId(isEqualsTo.getExpression2().toString());
        }
      } else if (filter instanceof PropertyIsEqualToLiteral) {
        PropertyIsEqualToLiteral isEqualsTo = (PropertyIsEqualToLiteral) filter;
        if (Metacard.ID.equals(isEqualsTo.getExpression1().toString())) {
          openSearchFilterVisitorObject.setId(isEqualsTo.getExpression2().toString());
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

  private OpenSearchFilterVisitorObject getOpenSearchFilterVisitorObjectFromData(Object data) {
    if (data instanceof OpenSearchFilterVisitorObject) {
      return (OpenSearchFilterVisitorObject) data;
    }
    return null;
  }
}
