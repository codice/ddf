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

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.xml.bind.JAXBElement;
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
 * Wraps a {@link JAXBElement} containing a filter component. The goal of this class is to provide a
 * mapping from the Filter 2.0 binding type to the visit method that should be invoked.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public class VisitableXmlElementImpl implements VisitableXmlElement {
  private static final Map<Class, BiConsumer<FilterVisitor2, VisitableXmlElement>> CONSUMER_MAP =
      ImmutableMap.<Class, BiConsumer<FilterVisitor2, VisitableXmlElement>>builder()
          .put(FilterType.class, FilterVisitor2::visitFilter)
          .put(String.class, FilterVisitor2::visitString)
          .put(LiteralType.class, FilterVisitor2::visitLiteralType)
          .put(FunctionType.class, FilterVisitor2::visitFunctionType)
          .put(BinaryLogicOpType.class, FilterVisitor2::visitBinaryLogicType)
          .put(UnaryLogicOpType.class, FilterVisitor2::visitUnaryLogicType)
          .put(BinaryTemporalOpType.class, FilterVisitor2::visitBinaryTemporalType)
          .put(BinarySpatialOpType.class, FilterVisitor2::visitBinarySpatialType)
          .put(DistanceBufferType.class, FilterVisitor2::visitDistanceBufferType)
          .put(BBOXType.class, FilterVisitor2::visitBoundingBoxType)
          .put(BinaryComparisonOpType.class, FilterVisitor2::visitBinaryComparisonType)
          .put(PropertyIsLikeType.class, FilterVisitor2::visitPropertyIsLikeType)
          .put(PropertyIsNullType.class, FilterVisitor2::visitPropertyIsNullType)
          .put(PropertyIsNilType.class, FilterVisitor2::visitPropertyIsNilType)
          .put(PropertyIsBetweenType.class, FilterVisitor2::visitPropertyIsBetweenType)
          .build();

  private final JAXBElement element;

  public VisitableXmlElementImpl(final JAXBElement element) {
    this.element = element;
  }

  @Override
  public JAXBElement<?> getElement() {
    return element;
  }

  @Override
  public void accept(FilterVisitor2 visitor) {
    Class clazz = element.getDeclaredType();
    BiConsumer<FilterVisitor2, VisitableXmlElement> biConsumer = CONSUMER_MAP.get(clazz);
    if (biConsumer == null) {
      throw new FilterProcessingException(
          "Could not find mapping to visit method for class " + clazz.getName());
    }
    // Actually invoking one of the "visits" on local variable "visitor"
    biConsumer.accept(visitor, this);
  }
}
