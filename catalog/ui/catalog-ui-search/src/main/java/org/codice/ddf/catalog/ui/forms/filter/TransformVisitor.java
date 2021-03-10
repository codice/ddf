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
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

  private static class TemplateSubstitutionV1 {
    private static final String NAME = "template.value.v1";

    private static final Integer INDEX_DEFAULT_VALUE = 0;

    private static final Integer INDEX_NODE_ID = 1;

    private static final Integer INDEX_IS_VISIBLE = 2;

    private static final Integer INDEX_IS_READONLY = 3;
  }

  private static class Proximity {
    private static final String NAME = "proximity";

    private static final Integer INDEX_PROPERTY_NAME = 0;

    private static final Integer INDEX_MAX_DIFFS = 1;

    private static final Integer INDEX_TARGET_TXT = 2;
  }

  private static class Range {
    private static final String NAME = "custom.preds.range";

    private static final Integer INDEX_PROPERTY_NAME = 0;

    private static final Integer INDEX_LOWER_LONG = 1;

    private static final Integer INDEX_UPPER_LONG = 2;
  }

  private static class Like {
    private static final String NAME = "custom.preds.like";

    private static final Integer INDEX_PROPERTY_NAME = 0;

    private static final Integer INDEX_VALUE_TXT = 1;
  }

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
  public void visitDoubleType(VisitableElement<Double> visitable) {
    super.visitDoubleType(visitable);
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
  public void visitTemplateType(VisitableElement<Map<String, Object>> visitable) {
    traceName(visitable);
    Map<String, Object> args = visitable.getValue();
    if (LOGGER.isTraceEnabled()) {
      args.forEach((key, value) -> LOGGER.trace("Key: {} | Value: {}", key, value));
    }

    builder.setTemplatedValues(args);
  }

  @Override
  public void visitMapType(VisitableElement<Map<String, Object>> visitable) {
    traceName(visitable);
    Map<String, Object> pred = visitable.getValue();
    if (LOGGER.isTraceEnabled()) {
      pred.forEach((key, value) -> LOGGER.trace("Key: {} | Value: {}", key, value));
    }

    if ("BETWEEN".equals(pred.get("type"))) {
      Map value = (Map) pred.get("value");
      builder.addFunctionType(
          Range.NAME, Arrays.asList(pred.get("property"), value.get("lower"), value.get("upper")));
    } else if ("LIKE".equals(pred.get("type"))) {
      builder.addFunctionType(Like.NAME, Arrays.asList(pred.get("property"), pred.get("value")));
    } else {
      throw new FilterProcessingException("Visiting the Map type only applies to BETWEEN and LIKE");
    }
  }

  @Override
  public void visitFunctionType(VisitableElement<List<Serializable>> visitable) {
    traceName(visitable);
    String functionName = visitable.getFunctionName();
    if (functionName == null) {
      throw new FilterProcessingException("Function name was expected but not provided");
    }

    List<Serializable> args = visitable.getValue();
    if (LOGGER.isTraceEnabled()) {
      args.forEach((arg) -> LOGGER.trace("Argument: {}", arg));
    }

    List<Object> newArgs = new ArrayList<>();
    switch (functionName) {
      case TemplateSubstitutionV1.NAME:
        LOGGER.trace("Found template function: {} with {}", functionName, args);
        Map<String, Object> argMap = new HashMap<>();
        argMap.put("defaultValue", asString(args.get(TemplateSubstitutionV1.INDEX_DEFAULT_VALUE)));
        argMap.put("nodeId", asString(args.get(TemplateSubstitutionV1.INDEX_NODE_ID)));
        argMap.put(
            "isVisible",
            Boolean.parseBoolean(asString(args.get(TemplateSubstitutionV1.INDEX_IS_VISIBLE))));
        argMap.put(
            "isReadOnly",
            Boolean.parseBoolean(asString(args.get(TemplateSubstitutionV1.INDEX_IS_READONLY))));
        if (LOGGER.isTraceEnabled()) {
          argMap.forEach((key, value) -> LOGGER.trace("Key: {} | Value: {}", key, value));
        }
        builder.setTemplatedValues(argMap);
        break;

      case Proximity.NAME:
        LOGGER.trace("Found proximity function: {} with {}", functionName, args);
        newArgs.add(asString(args.get(Proximity.INDEX_PROPERTY_NAME)));
        newArgs.add(Integer.parseInt(asString(args.get(Proximity.INDEX_MAX_DIFFS))));
        newArgs.add(asString(args.get(Proximity.INDEX_TARGET_TXT)));
        builder.setProperty(functionName, newArgs);
        break;

      case Range.NAME:
        LOGGER.trace("Found range function: {} with {}", functionName, args);
        builder.addBetweenType(
            asString(args.get(Range.INDEX_PROPERTY_NAME)),
            parseNumber(asString(args.get(Range.INDEX_LOWER_LONG))),
            parseNumber(asString(args.get(Range.INDEX_UPPER_LONG))));
        break;

      case Like.NAME:
        LOGGER.trace("Found LIKE (case sensitive) function: {} with {}", functionName, args);
        builder.beginPropertyIsLikeType("PropertyIsLike", true);
        builder.setProperty(asString(args.get(Like.INDEX_PROPERTY_NAME)));
        builder.setValue(asString(args.get(Like.INDEX_VALUE_TXT)));
        builder.endTerminalType();
        break;

      default:
        throw new FilterProcessingException(
            "Unrecognized function name on filter: " + functionName);
    }
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

  @Override
  public void visitPropertyIsNilType(VisitableElement<VisitableElement<?>> visitable) {
    traceName(visitable);
    builder.beginNilType();
    visitable.getValue().accept(this);
    builder.endTerminalType();
  }

  private static String asString(Serializable val) {
    return val == null ? null : val.toString();
  }

  private static Number parseNumber(String str) {
    NumberFormat numberFormat = NumberFormat.getInstance();
    try {
      return numberFormat.parse(str);
    } catch (ParseException e) {
      // Default to zero since this is called when reading FROM the database
      // - fail gracefully because it must be a data persistence issue
      // - throwing an exception here would make the form disappear entirely
      LOGGER.trace("Unable to parse persisted numeric value in search form, using zero instead", e);
      return 0;
    }
  }

  private static void traceName(VisitableElement element) {
    LOGGER.trace("LocalPart: {}", element.getName());
  }
}
