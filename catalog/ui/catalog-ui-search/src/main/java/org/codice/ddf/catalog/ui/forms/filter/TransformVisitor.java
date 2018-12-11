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
import javax.xml.bind.JAXBElement;
import org.apache.commons.collections4.CollectionUtils;
import org.codice.ddf.catalog.ui.forms.api.FilterNode;
import org.codice.ddf.catalog.ui.forms.api.FilterVisitor2;
import org.codice.ddf.catalog.ui.forms.api.FlatFilterBuilder;
import org.codice.ddf.catalog.ui.forms.api.VisitableElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Traverses a Filter represented by some root {@link VisitableElement} and builds a UI-compatible
 * {@link FilterNode} that can be serialized as part of a model. This allows the persistence layer
 * to contain standards-compliant Filter XML 2.0 which is transformed on-the-fly for the UI to
 * consume.
 *
 * <p>Irrespective of errors in the bindings, at the moment, this visitor makes an assumption: Value
 * references support a {@link List<Serializable>} but for our purposes we are only interested in
 * the first value. The rest are ignored.
 *
 * <p>To get started, load XML using JAXB, create a {@link VisitableElement} from the root {@link
 * JAXBElement}, and pass an instance of {@link TransformVisitor} into {@link
 * VisitableElement#accept(FilterVisitor2)}.
 *
 * <p>While trying to wrap one's head around visitors, it would be helpful to set a breakpoint on
 * {@link #visitLiteralType(VisitableElement)} and take some time analyzing the stack.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public class TransformVisitor<T> extends AbstractFilterVisitor2 {
  private static final Logger LOGGER = LoggerFactory.getLogger(TransformVisitor.class);

  private final FlatFilterBuilder<T> builder;

  public TransformVisitor(final FlatFilterBuilder<T> builder) {
    this.builder = builder;
  }

  public T getResult() {
    return builder.getResult();
  }

  // Work around for Value References not having an explicit binding
  @Override
  public void visitString(VisitableElement<String> visitable) {
    super.visitString(visitable);
    builder.setProperty(visitable.getValue());
  }

  @Override
  public void visitLiteralType(VisitableElement<List<Serializable>> visitable) {
    super.visitLiteralType(visitable);
    List<Serializable> values = visitable.getValue();
    if (CollectionUtils.isEmpty(values)) {
      LOGGER.debug("No values found on literal type");
      return;
    }

    // Assumption: we only support one literal value
    builder.setValue(values.get(0).toString());
  }

  @Override
  public void visitDistanceType(VisitableElement<Double> visitable) {
    super.visitDistanceType(visitable);
    Double value = visitable.getValue();
    if (value == null) {
      LOGGER.debug("No values found on distance type");
      return;
    }

    builder.setDistance(value);
  }

  @Override
  public void visitDistanceBufferType(VisitableElement<List<Object>> visitable) {
    traceName(visitable);
    List<Object> values = visitable.getValue();
    if (CollectionUtils.isEmpty(values)) {
      LOGGER.debug("No values found on distance buffer type");
      return;
    }

    builder.beginBinarySpatialType("DWITHIN");
    visitable
        .getValue()
        .stream()
        .map(VisitableElement.class::cast)
        .forEachOrdered(v -> v.accept(this));
    builder.endTerminalType();
  }

  @Override
  public void visitFunctionType(VisitableElement<Map<String, Object>> visitable) {
    traceName(visitable);
    Map<String, Object> args = visitable.getValue();
    if (LOGGER.isTraceEnabled()) {
      args.forEach((key, value) -> LOGGER.trace("Key: {} | Value: {}", key, value));
    }

    builder.setTemplatedValues(args);
  }

  @Override
  public void visitBinaryLogicType(VisitableElement<List<VisitableElement<?>>> visitable) {
    traceName(visitable);
    builder.beginBinaryLogicType(visitable.getName());
    visitable.getValue().forEach(v -> v.accept(this));
    builder.endBinaryLogicType();
  }

  @Override
  public void visitBinaryComparisonType(VisitableElement<List<VisitableElement<?>>> visitable) {
    traceName(visitable);
    builder.beginBinaryComparisonType(visitable.getName());
    visitable.getValue().forEach(v -> v.accept(this));
    builder.endTerminalType();
  }

  @Override
  public void visitPropertyIsLikeType(VisitableElement<List<VisitableElement<?>>> visitable) {
    traceName(visitable);
    // For now, will always choose ILIKE
    // For system templates defined in XML, matchCase="true" will be ignored
    builder.beginPropertyIsLikeType(visitable.getName(), false);
    visitable.getValue().forEach(v -> v.accept(this));
    builder.endTerminalType();
  }

  @Override
  public void visitBinaryTemporalType(VisitableElement<List<Object>> visitable) {
    traceName(visitable);
    builder.beginBinaryTemporalType(visitable.getName());
    visitable
        .getValue()
        .stream()
        .map(VisitableElement.class::cast)
        .forEachOrdered(v -> v.accept(this));
    builder.endTerminalType();
  }

  @Override
  public void visitBinarySpatialType(VisitableElement<List<Object>> visitable) {
    traceName(visitable);
    builder.beginBinarySpatialType("INTERSECTS");
    visitable
        .getValue()
        .stream()
        .map(VisitableElement.class::cast)
        .forEachOrdered(v -> v.accept(this));
    builder.endTerminalType();
  }

  private static void traceName(VisitableElement element) {
    LOGGER.trace("LocalPart: {}", element.getName());
  }
}
