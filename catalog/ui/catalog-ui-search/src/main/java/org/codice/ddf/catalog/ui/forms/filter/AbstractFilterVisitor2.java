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
import java.util.Objects;
import java.util.stream.Stream;
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
  public void visitFilter(VisitableXmlElement<FilterType> visitable) {
    JAXBElement<FilterType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    traceLocalPart(localPart);

    FilterType filterType = element.getValue();
    JAXBElement<?> root =
        Stream.of(
                filterType.getComparisonOps(),
                filterType.getLogicOps(),
                filterType.getSpatialOps(),
                filterType.getTemporalOps())
            .filter(Objects::nonNull)
            .findAny()
            .orElse(null);

    if (root != null) {
      LOGGER.trace("Valid root found, beginning traversal...");
      makeVisitable(root).accept(this);
      return;
    }

    // Support can be enhanced in the future, but currently these components aren't needed
    handleUnsupported(filterType.getId());
    handleUnsupported(filterType.getExtensionOps());

    // Functions are supported but not as the FIRST element of a document
    handleUnsupported(filterType.getFunction());

    throw new FilterProcessingException("No valid starting element for the filter was found");
  }

  @Override
  public void visitString(VisitableXmlElement<String> visitable) {
    JAXBElement<String> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    traceLocalPart(localPart);
  }

  @Override
  public void visitLiteralType(VisitableXmlElement<LiteralType> visitable) {
    JAXBElement<LiteralType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    traceLocalPart(localPart);
  }

  @Override
  public void visitFunctionType(VisitableXmlElement<FunctionType> visitable) {
    JAXBElement<FunctionType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    traceLocalPart(localPart);
  }

  @Override
  public void visitUnaryLogicType(VisitableXmlElement<UnaryLogicOpType> visitable) {
    JAXBElement<UnaryLogicOpType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    traceLocalPart(localPart);
  }

  @Override
  public void visitBinaryTemporalType(VisitableXmlElement<BinaryTemporalOpType> visitable) {
    JAXBElement<BinaryTemporalOpType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    traceLocalPart(localPart);
  }

  @Override
  public void visitBinarySpatialType(VisitableXmlElement<BinarySpatialOpType> visitable) {
    JAXBElement<BinarySpatialOpType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    traceLocalPart(localPart);
  }

  @Override
  public void visitDistanceBufferType(VisitableXmlElement<DistanceBufferType> visitable) {
    JAXBElement<DistanceBufferType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    traceLocalPart(localPart);
  }

  @Override
  public void visitBoundingBoxType(VisitableXmlElement<BBOXType> visitable) {
    JAXBElement<BBOXType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    traceLocalPart(localPart);
  }

  @Override
  public void visitBinaryLogicType(VisitableXmlElement<BinaryLogicOpType> visitable) {
    JAXBElement<BinaryLogicOpType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    traceLocalPart(localPart);

    element.getValue().getOps().forEach(jax -> makeVisitable(jax).accept(this));
  }

  @Override
  public void visitBinaryComparisonType(VisitableXmlElement<BinaryComparisonOpType> visitable) {
    JAXBElement<BinaryComparisonOpType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    traceLocalPart(localPart);

    element.getValue().getExpression().forEach(jax -> makeVisitable(jax).accept(this));
  }

  @Override
  public void visitPropertyIsLikeType(VisitableXmlElement<PropertyIsLikeType> visitable) {
    JAXBElement<PropertyIsLikeType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    traceLocalPart(localPart);
  }

  @Override
  public void visitPropertyIsNullType(VisitableXmlElement<PropertyIsNullType> visitable) {
    JAXBElement<PropertyIsNullType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    traceLocalPart(localPart);
  }

  @Override
  public void visitPropertyIsNilType(VisitableXmlElement<PropertyIsNilType> visitable) {
    JAXBElement<PropertyIsNilType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    traceLocalPart(localPart);
  }

  @Override
  public void visitPropertyIsBetweenType(VisitableXmlElement<PropertyIsBetweenType> visitable) {
    JAXBElement<PropertyIsBetweenType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    traceLocalPart(localPart);
  }

  private static void handleUnsupported(Object type) {
    if (type != null) {
      throw new IllegalArgumentException(
          "Encountered filter with unsupported element: " + type.getClass().getName());
    }
  }

  private static void handleUnsupported(List type) {
    if (!type.isEmpty()) {
      throw new IllegalArgumentException(
          "Encountered filter with unsupported element: " + type.getClass().getName());
    }
  }

  protected void traceLocalPart(String localPart) {
    LOGGER.trace("Local Part: {}", localPart);
  }

  protected void traceValue(Serializable value) {
    LOGGER.trace("Value: {}", value);
  }

  protected VisitableXmlElement makeVisitable(JAXBElement element) {
    return new VisitableFilterNode(element);
  }
}
