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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.transformer;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.impl.filter.FuzzyFunction;
import ddf.measure.Distance;
import ddf.measure.Distance.LinearUnit;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.codice.ddf.libs.geo.GeoFormatException;
import org.codice.ddf.libs.geo.util.GeospatialUtil;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GmdConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswRecordConverter;
import org.geotools.filter.LiteralExpressionImpl;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.styling.UomOgcMapping;
import org.geotools.temporal.object.DefaultInstant;
import org.geotools.temporal.object.DefaultPeriod;
import org.geotools.temporal.object.DefaultPosition;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.temporal.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * CswRecordMapperFilterVisitor extends {@link DuplicatingFilterVisitor} to create a new filter
 * where PropertyName expressions are converted from CswRecord terminology to the framework's
 * Metacard terminology
 */
public class CswRecordMapperFilterVisitor extends DuplicatingFilterVisitor {
  protected static final String SPATIAL_QUERY_TAG = "spatialQueryExtraData";

  private static final Logger LOGGER = LoggerFactory.getLogger(CswRecordMapperFilterVisitor.class);

  private final AttributeRegistry attributeRegistry;

  private final CswRecordMap cswRecordMap;

  private Filter visitedFilter;

  public CswRecordMapperFilterVisitor(
      CswRecordMap cswRecordMap, AttributeRegistry attributeRegistry) {
    this.cswRecordMap = cswRecordMap;

    this.attributeRegistry = attributeRegistry;
  }

  private static void convertGeometryExpressionToEpsg4326(Expression expression) {
    if (expression instanceof LiteralExpressionImpl) {
      LiteralExpressionImpl literalExpression = (LiteralExpressionImpl) expression;
      Object valueObj = literalExpression.getValue();
      if (valueObj instanceof Geometry) {
        Geometry geometry = (Geometry) valueObj;
        Object userDataObj = geometry.getUserData();
        if (userDataObj instanceof CoordinateReferenceSystem) {
          CoordinateReferenceSystem sourceCRS = (CoordinateReferenceSystem) userDataObj;
          Geometry convertedGeometry = null;
          try {
            convertedGeometry = GeospatialUtil.transformToEPSG4326LonLatFormat(geometry, sourceCRS);
            literalExpression.setValue(convertedGeometry);
          } catch (GeoFormatException e) {
            LOGGER.debug("Unable to convert geometry {} to EPSG:4326 format", valueObj, e);
          }
        }
      }
    }
  }

  public Filter getVisitedFilter() {
    return visitedFilter;
  }

  public void setVisitedFilter(Filter filter) {
    visitedFilter = filter;
  }

  @Override
  // convert BBOX queries to Within filters.
  public Object visit(BBOX filter, Object extraData) {
    Expression geometry1 = visit(filter.getExpression1(), SPATIAL_QUERY_TAG);
    Expression geometry2 = visit(filter.getExpression2(), extraData);
    convertGeometryExpressionToEpsg4326(geometry1);
    convertGeometryExpressionToEpsg4326(geometry2);
    return getFactory(extraData).within(geometry1, geometry2);
  }

  @Override
  public Object visit(Beyond filter, Object extraData) {
    double distance = getDistanceInMeters(filter.getDistance(), filter.getDistanceUnits());

    Expression geometry1 = visit(filter.getExpression1(), SPATIAL_QUERY_TAG);
    Expression geometry2 = visit(filter.getExpression2(), extraData);
    convertGeometryExpressionToEpsg4326(geometry1);
    convertGeometryExpressionToEpsg4326(geometry2);

    return getFactory(extraData).beyond(geometry1, geometry2, distance, UomOgcMapping.METRE.name());
  }

  @Override
  public Object visit(DWithin filter, Object extraData) {
    double distance = getDistanceInMeters(filter.getDistance(), filter.getDistanceUnits());

    Expression geometry1 = visit(filter.getExpression1(), SPATIAL_QUERY_TAG);
    Expression geometry2 = visit(filter.getExpression2(), extraData);
    convertGeometryExpressionToEpsg4326(geometry1);
    convertGeometryExpressionToEpsg4326(geometry2);

    return getFactory(extraData)
        .dwithin(geometry1, geometry2, distance, UomOgcMapping.METRE.name());
  }

  @Override
  public Object visit(Intersects filter, Object extraData) {
    convertGeometryExpressionToEpsg4326(filter.getExpression1());
    convertGeometryExpressionToEpsg4326(filter.getExpression2());
    return super.visit(filter, extraData);
  }

  @Override
  public Object visit(Contains filter, Object extraData) {
    convertGeometryExpressionToEpsg4326(filter.getExpression1());
    convertGeometryExpressionToEpsg4326(filter.getExpression2());
    return super.visit(filter, extraData);
  }

  @Override
  public Object visit(Crosses filter, Object extraData) {
    convertGeometryExpressionToEpsg4326(filter.getExpression1());
    convertGeometryExpressionToEpsg4326(filter.getExpression2());
    return super.visit(filter, extraData);
  }

  @Override
  public Object visit(Disjoint filter, Object extraData) {
    convertGeometryExpressionToEpsg4326(filter.getExpression1());
    convertGeometryExpressionToEpsg4326(filter.getExpression2());
    return super.visit(filter, extraData);
  }

  @Override
  public Object visit(Equals filter, Object extraData) {
    convertGeometryExpressionToEpsg4326(filter.getExpression1());
    convertGeometryExpressionToEpsg4326(filter.getExpression2());
    return super.visit(filter, extraData);
  }

  @Override
  public Object visit(Overlaps filter, Object extraData) {
    convertGeometryExpressionToEpsg4326(filter.getExpression1());
    convertGeometryExpressionToEpsg4326(filter.getExpression2());
    return super.visit(filter, extraData);
  }

  @Override
  public Object visit(Touches filter, Object extraData) {
    convertGeometryExpressionToEpsg4326(filter.getExpression1());
    convertGeometryExpressionToEpsg4326(filter.getExpression2());
    return super.visit(filter, extraData);
  }

  @Override
  public Object visit(Within filter, Object extraData) {
    convertGeometryExpressionToEpsg4326(filter.getExpression1());
    convertGeometryExpressionToEpsg4326(filter.getExpression2());
    return super.visit(filter, extraData);
  }

  @Override
  public Object visit(PropertyName expression, Object extraData) {
    if (expression == null) {
      LOGGER.debug("Attempting to visit a null expression");
      return null;
    }
    convertGeometryExpressionToEpsg4326(expression);
    String propertyName = expression.getPropertyName();
    String name;

    if (CswConstants.BBOX_PROP.equals(propertyName)
        || CswConstants.OWS_BOUNDING_BOX.equals(propertyName)
        || GmdConstants.APISO_BOUNDING_BOX.equals(propertyName)) {
      name = Metacard.ANY_GEO;
    } else {
      NamespaceSupport namespaceContext = expression.getNamespaceContext();
      name = cswRecordMap.getProperty(propertyName, namespaceContext);

      if (SPATIAL_QUERY_TAG.equals(extraData)) {
        Optional<AttributeDescriptor> attributeDescriptor = attributeRegistry.lookup(name);
        if (attributeDescriptor.isPresent()
            && !AttributeType.AttributeFormat.GEOMETRY.equals(
                attributeDescriptor.get().getType().getAttributeFormat())) {
          throw new UnsupportedOperationException(
              "Attempted a spatial query on a non-geometry-valued attribute ("
                  + propertyName
                  + ")");
        }
      }
    }
    LOGGER.debug("Converting \"{}\" to \"{}\"", propertyName, name);

    return getFactory(extraData).property(name);
  }

  @Override
  public Object visit(PropertyIsBetween filter, Object extraData) {
    Expression expr = visit(filter.getExpression(), extraData);

    Expression lower = visit(filter.getLowerBoundary(), expr);
    Expression upper = visit(filter.getUpperBoundary(), expr);
    return getFactory(extraData).between(expr, lower, upper);
  }

  @Override
  public Object visit(PropertyIsEqualTo filter, Object extraData) {
    FilterFactory2 factory = getFactory(extraData);

    if (filter.getExpression1() instanceof Function) {
      Expression function = (Expression) visit((Function) filter.getExpression1(), extraData);
      // given a function we can use its return value to infer the type of expression2
      Expression typedExpression =
          factory.literal(
              filter
                  .getExpression2()
                  .evaluate(
                      null,
                      ((Function) filter.getExpression1())
                          .getFunctionName()
                          .getReturn()
                          .getType()));
      return factory.equals(function, visit(typedExpression, function));
    }

    Expression expr1 = visit(filter.getExpression1(), extraData);
    Expression expr2 = visit(filter.getExpression2(), expr1);

    return factory.equal(expr1, expr2, filter.isMatchingCase());
  }

  @Override
  public Object visit(PropertyIsNotEqualTo filter, Object extraData) {
    Expression expr1 = visit(filter.getExpression1(), extraData);
    Expression expr2 = visit(filter.getExpression2(), expr1);

    return getFactory(extraData).notEqual(expr1, expr2, filter.isMatchingCase());
  }

  @Override
  public Object visit(PropertyIsGreaterThan filter, Object extraData) {
    Expression expr1 = visit(filter.getExpression1(), extraData);
    Expression expr2 = visit(filter.getExpression2(), expr1);

    return getFactory(extraData).greater(expr1, expr2);
  }

  @Override
  public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {
    Expression expr1 = visit(filter.getExpression1(), extraData);
    Expression expr2 = visit(filter.getExpression2(), expr1);

    // work around since Solr Provider doesn't support greaterOrEqual on temporal (DDF-311)
    if (isTemporalQuery(expr1, expr2)) {
      // also not supported by provider (DDF-311)
      // TEquals tEquals = getFactory(extraData).tequals(expr1, expr2);
      // After after = getFactory(extraData).after(expr1, expr2);
      // return getFactory(extraData).or(tEquals, after);

      Object val = null;
      Expression other = null;
      if (expr2 instanceof Literal) {
        val = ((Literal) expr2).getValue();
        other = expr1;
      } else if (expr1 instanceof Literal) {
        val = ((Literal) expr1).getValue();
        other = expr2;
      }

      if (val != null) {
        Date orig = (Date) val;
        Instant start = new DefaultInstant(new DefaultPosition(orig));
        Instant end = new DefaultInstant(new DefaultPosition(new Date()));
        DefaultPeriod period = new DefaultPeriod(start, end);
        Literal literal = getFactory(extraData).literal(period);
        return getFactory(extraData).during(other, literal);
      }
    }

    return getFactory(extraData).greaterOrEqual(expr1, expr2);
  }

  @Override
  public Object visit(PropertyIsLessThan filter, Object extraData) {
    Expression expr1 = visit(filter.getExpression1(), extraData);
    Expression expr2 = visit(filter.getExpression2(), expr1);

    // work around since solr provider doesn't support lessthan on temporal (DDF-311)
    if (isTemporalQuery(expr1, expr2)) {
      return getFactory(extraData).before(expr1, expr2);
    }

    return getFactory(extraData).less(expr1, expr2);
  }

  @Override
  public Object visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {
    Expression expr1 = visit(filter.getExpression1(), extraData);
    Expression expr2 = visit(filter.getExpression2(), expr1);

    // work around since solr provider doesn't support lessOrEqual on temporal (DDF-311)
    if (isTemporalQuery(expr1, expr2)) {
      // work around #1 fails, solr provider doesn't support tEquals either (DDF-311)
      // TEquals tEquals = getFactory(extraData).tequals(expr1, expr2);
      // Before before = getFactory(extraData).before(expr1, expr2);
      // return getFactory(extraData).or(tEquals, before);

      Object val = null;
      Expression other = null;

      if (expr2 instanceof Literal) {
        val = ((Literal) expr2).getValue();
        other = expr1;
      } else if (expr1 instanceof Literal) {
        val = ((Literal) expr1).getValue();
        other = expr2;
      }

      if (val != null) {
        Date orig = (Date) val;
        orig.setTime(orig.getTime() + 1);
        Literal literal = getFactory(extraData).literal(orig);
        return getFactory(extraData).before(other, literal);
      }
    }

    return getFactory(extraData).lessOrEqual(expr1, expr2);
  }

  @Override
  public Object visit(Literal expression, Object extraData) {
    if (extraData != null
        && extraData instanceof PropertyName
        && expression.getValue() instanceof String) {
      String propName = ((PropertyName) extraData).getPropertyName();
      Optional<AttributeDescriptor> attributeDescriptor = attributeRegistry.lookup(propName);
      if (attributeDescriptor.isPresent()) {
        String value = (String) expression.getValue();
        Serializable convertedValue =
            CswRecordConverter.convertStringValueToMetacardValue(
                attributeDescriptor.get().getType().getAttributeFormat(), value);

        return getFactory(extraData).literal(convertedValue);
      }
    }
    return getFactory(extraData).literal(expression.getValue());
  }

  @Override
  public Object visit(And filter, Object extraData) {
    List<Filter> children = filter.getChildren();
    List<Filter> newChildren = new ArrayList<>();
    Iterator<Filter> iter = children.iterator();
    while (iter.hasNext()) {
      Filter child = iter.next();
      if (child != null) {
        Filter newChild = (Filter) child.accept(this, extraData);
        if (newChild != null) {
          newChildren.add(newChild);
        }
      }
    }
    if (newChildren.isEmpty()) {
      return null;
    }
    if (newChildren.size() == 1) {
      return newChildren.get(0);
    }
    return getFactory(extraData).and(newChildren);
  }

  @Override
  public Object visit(Or filter, Object extraData) {
    List<Filter> children = filter.getChildren();
    List<Filter> newChildren = new ArrayList<>();
    Iterator<Filter> iter = children.iterator();
    while (iter.hasNext()) {
      Filter child = iter.next();
      if (child != null) {
        Filter newChild = (Filter) child.accept(this, extraData);
        if (newChild != null) {
          newChildren.add(newChild);
        }
      }
    }
    if (newChildren.isEmpty()) {
      return null;
    }
    if (newChildren.size() == 1) {
      return newChildren.get(0);
    }
    return getFactory(extraData).or(newChildren);
  }

  private boolean isTemporalQuery(Expression expr1, Expression expr2) {
    return isTemporalProperty(expr1) || isTemporalProperty(expr2);
  }

  private boolean isTemporalProperty(Expression expr) {
    if (expr instanceof PropertyName) {
      Optional<AttributeDescriptor> attributeDescriptor =
          attributeRegistry.lookup(((PropertyName) expr).getPropertyName());
      if (attributeDescriptor.isPresent()) {
        return AttributeType.AttributeFormat.DATE.equals(
            attributeDescriptor.get().getType().getAttributeFormat());
      }
    }
    return false;
  }

  private double getDistanceInMeters(double distance, String units) {
    LinearUnit linearUnit = null;
    if ("meters".equals(units)) {
      linearUnit = LinearUnit.METER;
    } else if ("feet".equals(units)) {
      linearUnit = LinearUnit.FOOT_U_S;
    } else if ("statute miles".equals(units)) {
      linearUnit = LinearUnit.MILE;
    } else if ("nautical miles".equals(units)) {
      linearUnit = LinearUnit.NAUTICAL_MILE;
    } else if ("kilometers".equals(units)) {
      linearUnit = LinearUnit.KILOMETER;
    }
    return new Distance(distance, linearUnit).getAs(LinearUnit.METER);
  }

  @SuppressWarnings("squid:S2177" /* Doesn't override parent method */)
  protected Expression visit(Expression expression, Object extraData) {
    if (expression == null) {
      return null;
    }
    return (Expression) expression.accept(this, extraData);
  }

  @Override
  public Object visit(Function function, Object extraData) {

    if (function instanceof FuzzyFunction) {
      // FuzzyFunction has 1 parameter to visit
      Expression expr1 = visit(function.getParameters().get(0), null);
      ((FuzzyFunction) function).setParameters(Arrays.asList(expr1));
      return function;
    } else {
      return getFactory(function)
          .function(
              function.getName(),
              function
                  .getParameters()
                  .stream()
                  .map(expression -> visit(expression, extraData))
                  .toArray(Expression[]::new));
    }
  }
}
