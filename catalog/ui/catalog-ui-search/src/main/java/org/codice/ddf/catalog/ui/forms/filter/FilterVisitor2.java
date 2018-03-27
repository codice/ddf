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

import net.opengis.filter.v_2_0.BBOXType;
import net.opengis.filter.v_2_0.BinaryComparisonOpType;
import net.opengis.filter.v_2_0.BinaryLogicOpType;
import net.opengis.filter.v_2_0.BinarySpatialOpType;
import net.opengis.filter.v_2_0.BinaryTemporalOpType;
import net.opengis.filter.v_2_0.DistanceBufferType;
import net.opengis.filter.v_2_0.FilterType;
import net.opengis.filter.v_2_0.FunctionType;
import net.opengis.filter.v_2_0.LiteralType;
import net.opengis.filter.v_2_0.PropertyIsBetweenType;
import net.opengis.filter.v_2_0.PropertyIsLikeType;
import net.opengis.filter.v_2_0.PropertyIsNilType;
import net.opengis.filter.v_2_0.PropertyIsNullType;
import net.opengis.filter.v_2_0.UnaryLogicOpType;

/**
 * An experimental interface to support Filter XML 2.0 and all related capabilities. Able to visit
 * implementations of {@link VisitableXmlElement}.
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

  void visitFilter(VisitableXmlElement<FilterType> visitable);

  // Work around for Value References not having an explicit binding
  void visitString(VisitableXmlElement<String> visitable);

  void visitLiteralType(VisitableXmlElement<LiteralType> visitable);

  void visitFunctionType(VisitableXmlElement<FunctionType> visitable);

  void visitBinaryLogicType(VisitableXmlElement<BinaryLogicOpType> visitable);

  void visitUnaryLogicType(VisitableXmlElement<UnaryLogicOpType> visitable);

  void visitBinaryTemporalType(VisitableXmlElement<BinaryTemporalOpType> visitable);

  void visitBinarySpatialType(VisitableXmlElement<BinarySpatialOpType> visitable);

  void visitDistanceBufferType(VisitableXmlElement<DistanceBufferType> visitable);

  void visitBoundingBoxType(VisitableXmlElement<BBOXType> visitable);

  void visitBinaryComparisonType(VisitableXmlElement<BinaryComparisonOpType> visitable);

  void visitPropertyIsLikeType(VisitableXmlElement<PropertyIsLikeType> visitable);

  void visitPropertyIsNullType(VisitableXmlElement<PropertyIsNullType> visitable);

  void visitPropertyIsNilType(VisitableXmlElement<PropertyIsNilType> visitable);

  void visitPropertyIsBetweenType(VisitableXmlElement<PropertyIsBetweenType> visitable);
}
