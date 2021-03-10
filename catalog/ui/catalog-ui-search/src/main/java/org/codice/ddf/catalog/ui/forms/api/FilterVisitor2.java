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
package org.codice.ddf.catalog.ui.forms.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import net.opengis.filter.v_2_0.PropertyIsBetweenType;

/**
 * An experimental interface to support Filter XML 2.0 and all related capabilities. Able to visit
 * implementations of {@link VisitableElement}.
 *
 * <p>Currently support is not provided for the following:
 *
 * <ul>
 *   <li>Functions as root elements in a filter structure
 *   <li>ID definitions
 *   <li>Extension ops
 * </ul>
 *
 * <p><i>This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library.</i>
 */
public interface FilterVisitor2 {

  // Note: Scope is wider than EXPRESSION - could be any type within schema
  void visitFilter(VisitableElement<VisitableElement<?>> visitable);

  // Value References bind to String instead of their own specific type
  void visitString(VisitableElement<String> visitable);

  void visitLiteralType(VisitableElement<List<Serializable>> visitable);

  void visitDoubleType(VisitableElement<Double> visitable);

  void visitTemplateType(VisitableElement<Map<String, Object>> visitable);

  void visitMapType(VisitableElement<Map<String, Object>> visitable);

  // Traversal for the time being will assume NO embedded functions / currying support
  void visitFunctionType(VisitableElement<List<Serializable>> visitable);

  void visitBinaryLogicType(VisitableElement<List<VisitableElement<?>>> visitable);

  // Note: Scope is wider than EXPRESSION - could be any type within schema
  void visitUnaryLogicType(VisitableElement<VisitableElement<?>> visitable);

  void visitBinaryTemporalType(VisitableElement<List<Object>> visitable);

  void visitBinarySpatialType(VisitableElement<List<Object>> visitable);

  void visitDistanceBufferType(VisitableElement<List<Object>> visitable);

  void visitBoundingBoxType(VisitableElement<List<Object>> visitable);

  void visitBinaryComparisonType(VisitableElement<List<VisitableElement<?>>> visitable);

  void visitPropertyIsLikeType(VisitableElement<List<VisitableElement<?>>> visitable);

  void visitPropertyIsNullType(VisitableElement<VisitableElement<?>> visitable);

  void visitPropertyIsNilType(VisitableElement<VisitableElement<?>> visitable);

  /**
   * Visit a {@link PropertyIsBetweenType}. Note that this type will be strange; it's an expression
   * with boundary components strongly typed, each yielding a single {@link
   * javax.xml.bind.JAXBElement}.
   *
   * <p>For now the expected value will be that of a traditional expression ({@code
   * List<VisitableElement<?>>} but is subject to change in the future.
   *
   * <p><b>Note: This construct is not yet fully supported and always throws {@link
   * UnsupportedOperationException}.</b>
   *
   * @param visitable the data node to visit.
   * @throws UnsupportedOperationException this type of data is currently not supported.
   * @see net.opengis.filter.v_2_0.UpperBoundaryType
   * @see net.opengis.filter.v_2_0.LowerBoundaryType
   * @see PropertyIsBetweenType#getExpression()
   * @see PropertyIsBetweenType#getLowerBoundary()
   * @see PropertyIsBetweenType#getUpperBoundary()
   */
  void visitPropertyIsBetweenType(VisitableElement<List<VisitableElement<?>>> visitable);
}
