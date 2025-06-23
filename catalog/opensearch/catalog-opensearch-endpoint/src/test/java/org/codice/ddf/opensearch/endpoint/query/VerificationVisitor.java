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
package org.codice.ddf.opensearch.endpoint.query;

import java.util.HashMap;
import java.util.Map;
import org.geotools.api.filter.And;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.Not;
import org.geotools.api.filter.Or;
import org.geotools.api.filter.PropertyIsBetween;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.PropertyIsLike;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.spatial.Contains;
import org.geotools.api.filter.spatial.DWithin;
import org.geotools.api.filter.spatial.Intersects;
import org.geotools.api.filter.spatial.Within;
import org.geotools.api.filter.temporal.During;
import org.geotools.api.filter.temporal.TOverlaps;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerificationVisitor extends DefaultFilterVisitor {
  private static final String SEPARATOR = " - ";

  private static final Logger LOGGER = LoggerFactory.getLogger(VerificationVisitor.class);

  private int indent = 0;

  private final Map<String, FilterStatus> map = new HashMap<>();

  private static String indent(int count) {
    StringBuilder buffer = new StringBuilder();

    for (int i = 0; i < count; i++) {
      buffer.append("  ");
    }

    return buffer.toString();
  }

  @Override
  public Object visit(Function expression, Object data) {
    countOccurrence(expression);
    LOGGER.debug(
        indent(indent + 2)
            + "FUNCTION:"
            + " "
            + expression.getName()
            + SEPARATOR
            + expression.getClass().getName());
    return super.visit(expression, data);
  }

  @Override
  public Object visit(Not filter, Object data) {
    countOccurrence(filter);
    LOGGER.debug(indent(indent) + "NOT" + SEPARATOR + filter.getClass().getName());
    return super.visit(filter, data);
  }

  @Override
  public Object visit(Or filter, Object data) {
    countOccurrence(filter);

    LOGGER.debug(indent(indent) + "OR" + SEPARATOR + filter.getClass().getName());

    indent++;
    return super.visit(filter, data);
  }

  @Override
  public Object visit(And filter, Object data) {

    countOccurrence(filter);

    LOGGER.debug(indent(indent) + "AND" + SEPARATOR + filter.getClass().getName());

    indent++;

    return super.visit(filter, data);
  }

  @Override
  public Object visit(DWithin filter, Object data) {
    countOccurrence(filter);

    LOGGER.debug(indent(indent) + filter.NAME + SEPARATOR + filter.getClass().getName());

    return super.visit(filter, data);
  }

  @Override
  public Object visit(Within filter, Object data) {
    countOccurrence(filter);

    LOGGER.debug(indent(indent) + filter.NAME + SEPARATOR + filter.getClass().getName());

    return super.visit(filter, data);
  }

  @Override
  public Object visit(Intersects filter, Object data) {
    countOccurrence(filter);

    LOGGER.debug(indent(indent) + filter.NAME + SEPARATOR + filter.getClass().getName());

    return super.visit(filter, data);
  }

  @Override
  public Object visit(Contains filter, Object data) {
    countOccurrence(filter);

    LOGGER.debug(indent(indent) + filter.NAME + SEPARATOR + filter.getClass().getName());

    return super.visit(filter, data);
  }

  @Override
  public Object visit(TOverlaps filter, Object data) {
    countOccurrence(filter);

    LOGGER.debug(indent(indent) + filter.NAME + SEPARATOR + filter.getClass().getName());

    return super.visit(filter, data);
  }

  @Override
  public Object visit(During filter, Object data) {
    countOccurrence(filter);

    LOGGER.debug(indent(indent) + filter.NAME + SEPARATOR + filter.getClass().getName());

    return super.visit(filter, data);
  }

  @Override
  public Object visit(PropertyIsEqualTo filter, Object data) {
    countOccurrence(filter);

    LOGGER.debug(indent(indent) + filter.NAME + SEPARATOR + filter.getClass().getName());

    return super.visit(filter, data);
  }

  @Override
  public Object visit(PropertyIsLike filter, Object data) {
    countOccurrence(filter);
    getStatus(filter).setCaseSensitive(filter.isMatchingCase());
    getStatus(filter).setWildcard(filter.getWildCard());

    LOGGER.debug(indent(indent) + filter.NAME + SEPARATOR + filter.getClass().getName());

    LOGGER.debug(indent(indent + 2) + filter.getLiteral());

    return super.visit(filter, data);
  }

  @Override
  public Object visit(PropertyIsBetween filter, Object data) {

    countOccurrence(filter);

    LOGGER.debug(indent(indent) + filter.NAME + SEPARATOR + filter.getClass().getName());

    LOGGER.debug(indent(indent + 2) + filter.getLowerBoundary());
    LOGGER.debug(indent(indent + 2) + filter.getUpperBoundary());

    return super.visit(filter, data);
  }

  @Override
  public Object visit(PropertyName expression, Object data) {

    countOccurrence(expression);

    LOGGER.debug(
        indent(indent + 2)
            + expression.getPropertyName()
            + SEPARATOR
            + expression.getClass().getName());

    return data;
  }

  @Override
  public Object visit(Literal expression, Object data) {

    countOccurrence(expression);

    LOGGER.debug(
        indent(indent)
            + expression.getValue()
            + VerificationVisitor.SEPARATOR
            + expression.getClass().getName());
    return data;
  }

  private void countOccurrence(Filter filter) {
    if (getStatus(filter) == null) {
      FilterStatus status = new FilterStatus();
      status.increment();
      status.addFilter(filter);
      map.put(filter.getClass().getName(), status);
    } else {
      FilterStatus status = map.get(filter.getClass().getName());
      status.increment();
      status.addFilter(filter);
    }
  }

  private void countOccurrence(Expression expression) {
    if (getStatus(expression) == null) {
      FilterStatus status = new FilterStatus();
      status.increment();
      map.put(expression.getClass().getName(), status);
    } else {
      FilterStatus status = map.get(expression.getClass().getName());
      status.increment();
    }
  }

  private FilterStatus getStatus(Object filter) {
    return map.get(filter.getClass().getName());
  }

  public Map<String, FilterStatus> getMap() {
    return map;
  }
}
