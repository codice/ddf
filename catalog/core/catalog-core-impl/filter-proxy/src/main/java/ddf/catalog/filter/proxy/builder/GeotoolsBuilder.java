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
package ddf.catalog.filter.proxy.builder;

import ddf.catalog.data.Metacard;
import ddf.catalog.impl.filter.FuzzyFunction;
import ddf.catalog.impl.filter.JTSGeometryWrapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.geometry.GeometryBuilder;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.PrimitiveFactoryImpl;
import org.geotools.geometry.text.WKTParser;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.UomOgcMapping;
import org.geotools.temporal.object.DefaultInstant;
import org.geotools.temporal.object.DefaultPeriod;
import org.geotools.temporal.object.DefaultPeriodDuration;
import org.geotools.temporal.object.DefaultPosition;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.geometry.Geometry;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GeotoolsBuilder {

  // Use the OGC SE standard mapping for meters so that units assigned here
  // will
  // match units assigned elsewhere throughout DDF
  private static final String METERS = UomOgcMapping.METRE.name();

  private static final Logger LOGGER = LoggerFactory.getLogger(GeotoolsBuilder.class);

  private static WKTReader reader = new WKTReader();

  private static WKTParser parser = new WKTParser(new GeometryBuilder(DefaultGeographicCRS.WGS84));

  private FilterFactory factory = new FilterFactoryImpl();

  private String attribute;

  private Operator operator;

  private Object value;

  private Object secondaryValue;

  private String functionName;

  private List<Expression> arguments = Collections.emptyList();

  GeotoolsBuilder() {
    this(null, null, null, Collections.emptyList());
  }

  protected GeotoolsBuilder(GeotoolsBuilder builder) {
    this(
        builder.getAttribute(),
        builder.getOperator(),
        builder.getFunctionName(),
        builder.getArguments());
  }

  private GeotoolsBuilder(
      String attribute, Operator operator, String functionName, List<Expression> arguments) {
    this.attribute = attribute;
    this.operator = operator;
    this.functionName = functionName;
    this.arguments = arguments;
    parser.setFactory(new PrimitiveFactoryImpl(DefaultGeographicCRS.WGS84));
  }

  //
  // /**
  // * @param expression the expression to set
  // */
  // void setExpression(Expression expression) {
  // this.expression = expression;
  // }

  protected Filter build() {

    LOGGER.debug(
        "BUILDING attribute = {}, operator = {}, value = {}, secondaryValue = {}",
        attribute,
        operator,
        value,
        secondaryValue);

    Filter filter = null;
    String wkt = null;
    Date date = null;
    double distance = 0;
    Expression expression = null;

    switch (operator) {
      case AFTER:
        date = getValue(Date.class);
        if (date != null) {
          filter =
              factory.after(
                  factory.property(attribute),
                  factory.literal(new DefaultInstant(new DefaultPosition(date))));
        }
        break;
      case BEFORE:
        date = getValue(Date.class);
        if (date != null) {
          filter =
              factory.before(
                  factory.property(attribute),
                  factory.literal(new DefaultInstant(new DefaultPosition(date))));
        }
        break;
      case BETWEEN:
        filter =
            factory.between(
                factory.property(attribute),
                factory.literal(value),
                factory.literal(secondaryValue));
        break;
      case DURING:
        Date start = getValue(Date.class);
        Date end = getSecondaryValue(Date.class);
        if (start != null && end != null) {
          DefaultPosition defaultPosition = new DefaultPosition(start);
          Instant startInstant = new DefaultInstant(defaultPosition);
          Instant endInstant = new DefaultInstant(new DefaultPosition(end));
          Period period = new DefaultPeriod(startInstant, endInstant);
          filter = factory.during(factory.property(attribute), factory.literal(period));
        }
        break;
      case DURING_RELATIVE:
        Long longValue = getValue(Long.class);
        if (null != value) {
          filter =
              factory.during(
                  factory.property(attribute),
                  factory.literal(new DefaultPeriodDuration(longValue)));
        }
        break;
      case EQ:
        if (functionName != null) {
          expression = factory.function(functionName, arguments.toArray(new Expression[0]));
        } else {
          expression = factory.property(attribute);
        }

        filter = factory.equals(expression, factory.literal(value));
        break;
      case GT:
        filter = factory.greater(factory.property(attribute), factory.literal(value));
        break;
      case GTE:
        filter = factory.greaterOrEqual(factory.property(attribute), factory.literal(value));
        break;
      case LT:
        filter = factory.less(factory.property(attribute), factory.literal(value));
        break;
      case LTE:
        filter = factory.lessOrEqual(factory.property(attribute), factory.literal(value));
        break;
      case NEQ:
        filter = factory.notEqual(factory.property(attribute), factory.literal(value));
        break;
      case NULL:
        filter = factory.isNull(factory.property(attribute));
        break;
      case TOVERLAPS:
        filter = factory.toverlaps(factory.property(attribute), factory.literal(value));
        break;
      case BEYOND:
        wkt = getValue(String.class);
        distance = getSecondaryValue(Double.class);
        if (wkt != null && wkt.length() > 0) {
          filter = factory.beyond(attribute, toGeometry(wkt), distance, METERS);
        }
        break;
      case CONTAINS:
        wkt = getValue(String.class);
        if (wkt != null && wkt.length() > 0) {
          filter = factory.contains(attribute, toGeometry(wkt));
        }
        break;
      case DWITHIN:
        wkt = getValue(String.class);
        distance = getSecondaryValue(Double.class);
        if (wkt != null && wkt.length() > 0) {
          filter = factory.dwithin(attribute, toGeometry(wkt), distance, METERS);
        }
        break;
      case INTERSECTS:
        wkt = getValue(String.class);
        if (wkt != null && wkt.length() > 0) {
          filter = factory.intersects(attribute, toGeometry(wkt));
        }
        break;
      case WITHIN:
        wkt = getValue(String.class);
        if (wkt != null && wkt.length() > 0) {
          filter = factory.within(attribute, toGeometry(wkt));
        }
        break;
      case LIKE:
        filter =
            factory.like(
                factory.property(attribute),
                getValue(String.class),
                "*",
                "%",
                "'",
                getSecondaryValue(Boolean.class));
        break;
      case FUZZY:
        expression = factory.property(attribute);
        filter =
            factory.like(
                new FuzzyFunction(Arrays.asList(expression), factory.literal(Metacard.ANY_TEXT)),
                getValue(String.class),
                "*",
                "%",
                "'",
                getSecondaryValue(Boolean.class));
        break;
      default:
        // return null
    }

    if (filter == null) {
      throw new IllegalArgumentException(
          "Illegal argument for operation [" + operator.name() + "]");
    }

    return filter;
  }

  protected Filter build(Object arg) {
    value = arg;
    return build();
  }

  protected Filter build(Object arg0, Object arg1) {
    value = arg0;
    secondaryValue = arg1;
    return build();
  }

  private <T> T convert(Class<T> clazz, Object inputValue) {
    T convertedValue = null;

    if (inputValue != null && inputValue.getClass().isAssignableFrom(clazz)) {
      convertedValue = clazz.cast(inputValue);
    }
    return convertedValue;
  }

  String getFunctionName() {
    return functionName;
  }

  protected void setFunctionName(String functionName) {
    this.functionName = functionName;
  }

  List<Expression> getArguments() {
    return arguments;
  }

  protected void setArguments(List<Expression> arguments) {
    this.arguments = arguments;
  }

  /** @return the attribute */
  String getAttribute() {
    return attribute;
  }

  /** @param attribute the attribute to set */
  protected void setAttribute(String attribute) {
    this.attribute = attribute;
  }

  // public Expression(String attribute, Operator operator) {
  // this.attribute = attribute;
  // this.operator = operator;
  // }
  //
  // public Expression() {
  // attribute = null;
  // operator = null;
  // value = null;
  // secondaryValue = null;
  // }

  /** @return the factory */
  FilterFactory getFactory() {
    return factory;
  }

  /** @param factory the factory to set */
  void setFactory(FilterFactory factory) {
    this.factory = factory;
  }

  protected Operator getOperator() {
    return operator;
  }

  /** @param operator the operator to set */
  protected void setOperator(Operator operator) {
    LOGGER.debug("setting operator to {}", operator);
    this.operator = operator;
  }

  <T> T getSecondaryValue(Class<T> clazz) {
    return convert(clazz, secondaryValue);
  }

  protected Object getSecondaryValue() {
    return secondaryValue;
  }

  protected void setSecondaryValue(Object arg1) {
    this.secondaryValue = arg1;
  }

  /** @return the value */
  <T> T getValue(Class<T> clazz) {
    return convert(clazz, value);
  }

  protected Object getValue() {
    return value;
  }

  /** @param value the value to set */
  protected void setValue(Object value) {
    this.value = value;
  }

  public Geometry toGeometry(String wkt) {
    Geometry geometry = null;
    try {
      if (wkt.toLowerCase(Locale.US).startsWith("multi")
          || wkt.toLowerCase(Locale.US).trim().indexOf("geometrycollection") != -1) {
        // WKTParser does not currently support MultiPolygon,
        // MultiLineString, or MultiPoint
        org.locationtech.jts.geom.Geometry geo = reader.read(wkt);

        geometry = new JTSGeometryWrapper(geo);
      } else {
        geometry = parser.parse(wkt);
      }

    } catch (ParseException | java.text.ParseException e) {
      LOGGER.debug("Unable to compute geometry for WKT = {}", wkt, e);
    }

    return geometry;
  }
}
