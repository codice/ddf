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
package org.codice.ddf.catalog.ui.forms.filter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.codice.ddf.catalog.ui.forms.api.FilterVisitor2;
import org.codice.ddf.catalog.ui.forms.api.VisitableElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides common filter traversal logic which can be selectively extended for only the types that
 * an implementation is interested in visiting.
 *
 * <p>Currently support is not provided for the following:
 *
 * <ul>
 *   <li>Functions as root elements in a filter structure
 *   <li>ID definitions
 *   <li>Extension ops
 * </ul>
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public abstract class AbstractFilterVisitor2 implements FilterVisitor2 {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFilterVisitor2.class);

  @Override
  public void visitFilter(VisitableElement<VisitableElement<?>> visitable) {
    traceName(visitable);
    visitable.getValue().accept(this);
  }

  @Override
  public void visitString(VisitableElement<String> visitable) {
    traceName(visitable);
    traceValue(visitable.getValue());
  }

  @Override
  public void visitLiteralType(VisitableElement<List<Serializable>> visitable) {
    if (LOGGER.isTraceEnabled()) {
      visitable.getValue().forEach(AbstractFilterVisitor2::traceValue);
    }
  }

  @Override
  public void visitDoubleType(VisitableElement<Double> visitable) {
    traceName(visitable);
    traceValue(visitable.getValue());
  }

  @Override
  public void visitTemplateType(VisitableElement<Map<String, Object>> visitable) {
    traceName(visitable);
  }

  @Override
  public void visitMapType(VisitableElement<Map<String, Object>> visitable) {
    traceName(visitable);
  }

  @Override
  public void visitFunctionType(VisitableElement<List<Serializable>> visitable) {
    traceName(visitable);
  }

  @Override
  public void visitBinaryLogicType(VisitableElement<List<VisitableElement<?>>> visitable) {
    traceName(visitable);
    visitable.getValue().forEach(v -> v.accept(this));
  }

  @Override
  public void visitUnaryLogicType(VisitableElement<VisitableElement<?>> visitable) {
    traceName(visitable);
    visitable.accept(this);
  }

  @Override
  public void visitBinaryTemporalType(VisitableElement<List<Object>> visitable) {
    traceName(visitable);
    visitObjects(visitable);
  }

  @Override
  public void visitBinarySpatialType(VisitableElement<List<Object>> visitable) {
    traceName(visitable);
    visitObjects(visitable);
  }

  @Override
  public void visitDistanceBufferType(VisitableElement<List<Object>> visitable) {
    traceName(visitable);
    visitObjects(visitable);
  }

  @Override
  public void visitBoundingBoxType(VisitableElement<List<Object>> visitable) {
    traceName(visitable);
    throw new UnsupportedOperationException("BoundingBoxType currently is not supported");
    // Ticket for adding support - https://codice.atlassian.net/browse/DDF-3829
  }

  @Override
  public void visitBinaryComparisonType(VisitableElement<List<VisitableElement<?>>> visitable) {
    traceName(visitable);
    visitable.getValue().forEach(v -> v.accept(this));
  }

  @Override
  public void visitPropertyIsLikeType(VisitableElement<List<VisitableElement<?>>> visitable) {
    traceName(visitable);
    visitable.getValue().forEach(v -> v.accept(this));
  }

  // Attribute does not exist in our universe of attributes - will remain unsupported
  @Override
  public void visitPropertyIsNullType(VisitableElement<VisitableElement<?>> visitable) {
    traceName(visitable);
    throw new UnsupportedOperationException("PropertyIsNullType currently is not supported");
    // Ticket for adding support - https://codice.atlassian.net/browse/DDF-3829
    // When support is added, verify result of visiting embedded entity (might be null)
  }

  // Attribute exists but the value is empty - will be used for 'IS EMPTY' predicates
  @Override
  public void visitPropertyIsNilType(VisitableElement<VisitableElement<?>> visitable) {
    traceName(visitable);
    visitable.getValue().accept(this);
  }

  @Override
  public void visitPropertyIsBetweenType(VisitableElement<List<VisitableElement<?>>> visitable) {
    traceName(visitable);
    throw new UnsupportedOperationException("PropertyIsBetweenType currently is not supported");
    // Ticket for adding support - https://codice.atlassian.net/browse/DDF-3829
  }

  private void visitObjects(VisitableElement<List<Object>> visitable) {
    visitable
        .getValue()
        .stream()
        .map(VisitableElement.class::cast)
        .forEachOrdered(v -> v.accept(this));
  }

  private static void traceName(VisitableElement element) {
    LOGGER.trace("LocalPart: {}", element.getName());
  }

  private static void traceValue(Serializable value) {
    LOGGER.trace("Value: {}", value);
  }
}
