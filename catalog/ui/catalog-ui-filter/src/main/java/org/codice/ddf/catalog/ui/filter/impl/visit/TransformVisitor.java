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
package org.codice.ddf.catalog.ui.filter.impl.visit;

import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.xml.bind.JAXBElement;
import org.codice.ddf.catalog.ui.filter.FilterNode;
import org.codice.ddf.catalog.ui.filter.FilterVisitor2;
import org.codice.ddf.catalog.ui.filter.FlatFilterBuilder;
import org.codice.ddf.catalog.ui.filter.VisitableElement;
import org.codice.ddf.catalog.ui.filter.json.FilterJson;
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
@SuppressWarnings("squid:S4144" /* Duplication will be addressed by DDF-3926 */)
public class TransformVisitor<T> extends AbstractFilterVisitor2 {
  // This extraneous map is an intermediate step and will go away in DDF-3926
  private static final Map<String, Consumer<FlatFilterBuilder<?>>> OPERATORS =
      ImmutableMap.<String, Consumer<FlatFilterBuilder<?>>>builder()
          .put(FilterJson.Ops.NOT, FlatFilterBuilder::not)
          .put("Not", FlatFilterBuilder::not)
          .put(FilterJson.Ops.AND, FlatFilterBuilder::and)
          .put("And", FlatFilterBuilder::and)
          .put(FilterJson.Ops.OR, FlatFilterBuilder::or)
          .put("Or", FlatFilterBuilder::or)
          .put(FilterJson.Ops.AFTER, FlatFilterBuilder::after)
          .put("After", FlatFilterBuilder::after)
          .put(FilterJson.Ops.BEFORE, FlatFilterBuilder::before)
          .put("Before", FlatFilterBuilder::before)
          .put(FilterJson.Ops.ILIKE, b -> b.like(false, "%", "_", "\\"))
          .put(FilterJson.Ops.LIKE, b -> b.like(true, "%", "_", "\\"))
          .put("PropertyIsLike", b -> b.like(false, "%", "_", "\\"))
          .put(FilterJson.Ops.EQ, b -> b.isEqualTo(false))
          .put("PropertyIsEqualTo", b -> b.isEqualTo(false))
          .put(FilterJson.Ops.NOT_EQ, b -> b.isNotEqualTo(false))
          .put("PropertyIsNotEqualTo", b -> b.isNotEqualTo(false))
          .put(FilterJson.Ops.GT, b -> b.isGreaterThan(false))
          .put("PropertyIsGreaterThan", b -> b.isGreaterThan(false))
          .put(FilterJson.Ops.GT_OR_ET, b -> b.isGreaterThanOrEqualTo(false))
          .put("PropertyIsGreaterThanOrEqualTo", b -> b.isGreaterThanOrEqualTo(false))
          .put(FilterJson.Ops.LT, b -> b.isLessThan(false))
          .put("PropertyIsLessThan", b -> b.isLessThan(false))
          .put(FilterJson.Ops.LT_OR_ET, b -> b.isLessThanOrEqualTo(false))
          .put("PropertyIsLessThanOrEqualTo", b -> b.isLessThanOrEqualTo(false))
          .build();

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
    builder.property(visitable.getValue());
  }

  /**
   * {@code <Literal/>} blocks map to a list of serializable, but for our purposes there is only
   * ever one element in the list.
   *
   * @implNote See {@link VisitableXmlElementImpl.LiteralElement} for more details.
   */
  @Override
  public void visitLiteralType(VisitableElement<List<Serializable>> visitable) {
    super.visitLiteralType(visitable);
    List<Serializable> values = visitable.getValue();
    if (values == null || values.isEmpty()) {
      LOGGER.debug("No values found on literal type");
      builder.value(null);
      return;
    }
    builder.value(values.get(0));
  }

  @Override
  public void visitFunctionType(VisitableElement<Map<String, Object>> visitable) {
    traceName(visitable);
    Map<String, Object> args = visitable.getValue();
    if (LOGGER.isTraceEnabled()) {
      args.forEach((key, value) -> LOGGER.trace("Key: {} | Value: {}", key, value));
    }

    String defaultValue = (String) args.get("defaultValue");
    String nodeId = (String) args.get("nodeId");
    boolean isVisible = (boolean) args.get("isVisible");
    boolean isReadOnly = (boolean) args.get("isReadOnly");

    builder
        .function("template.value.v1")
        .value(defaultValue)
        .value(nodeId)
        .value(isVisible)
        .value(isReadOnly)
        .end();
  }

  @Override
  public void visitBinaryLogicType(VisitableElement<List<VisitableElement<?>>> visitable) {
    traceName(visitable);
    invokeBuilder(visitable);
    visitable.getValue().forEach(v -> v.accept(this));
    builder.end();
  }

  @Override
  public void visitBinaryComparisonType(VisitableElement<List<VisitableElement<?>>> visitable) {
    traceName(visitable);
    invokeBuilder(visitable);
    visitable.getValue().forEach(v -> v.accept(this));
    builder.end();
  }

  @Override
  public void visitPropertyIsLikeType(VisitableElement<List<VisitableElement<?>>> visitable) {
    traceName(visitable);
    // For now, will always choose ILIKE
    // For system templates defined in XML, matchCase="true" will be ignored
    invokeBuilder(visitable);
    visitable.getValue().forEach(v -> v.accept(this));
    builder.end();
  }

  @Override
  public void visitBinaryTemporalType(VisitableElement<List<Object>> visitable) {
    traceName(visitable);
    invokeBuilder(visitable);
    visitable
        .getValue()
        .stream()
        .map(VisitableElement.class::cast)
        .forEachOrdered(v -> v.accept(this));
    builder.end();
  }

  @Override
  public void visitBinarySpatialType(VisitableElement<List<Object>> visitable) {
    traceName(visitable);
    builder.intersects();
    visitable
        .getValue()
        .stream()
        .map(VisitableElement.class::cast)
        .forEachOrdered(v -> v.accept(this));
    builder.end();
  }

  private void invokeBuilder(VisitableElement<?> element) {
    Consumer<FlatFilterBuilder<?>> invocation = OPERATORS.get(element.getName());
    if (invocation == null) {
      throw new IllegalArgumentException("Cannot find mapping for operator " + element.getName());
    }
    invocation.accept(builder);
  }

  private static void traceName(VisitableElement element) {
    LOGGER.trace("LocalPart: {}", element.getName());
  }
}
