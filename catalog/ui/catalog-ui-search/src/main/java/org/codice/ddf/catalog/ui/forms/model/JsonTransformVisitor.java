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
package org.codice.ddf.catalog.ui.forms.model;

import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.bind.JAXBElement;
import net.opengis.filter.v_2_0.BinaryComparisonOpType;
import net.opengis.filter.v_2_0.BinaryLogicOpType;
import net.opengis.filter.v_2_0.FunctionType;
import net.opengis.filter.v_2_0.LiteralType;
import org.codice.ddf.catalog.ui.forms.filter.AbstractFilterVisitor2;
import org.codice.ddf.catalog.ui.forms.filter.FilterProcessingException;
import org.codice.ddf.catalog.ui.forms.filter.VisitableXmlElement;
import org.codice.ddf.catalog.ui.forms.model.JsonModel.FilterNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Traverses a Filter represented by some root {@link VisitableXmlElement} and builds a
 * UI-compatible {@link FilterNode} that can be serialized as part of a model. This allows the
 * persistence layer to contain standards-compliant Filter XML 2.0 which is transformed on-the-fly
 * for the UI to consume.
 *
 * <p>Irrespective of errors in the bindings, at the moment, this visitor makes an assumption: Value
 * references support a {@link List<Serializable>} but for our purposes we are only interested in
 * the first value. The rest are ignored.
 *
 * <p>To get started, load XML using JAXB, create a {@link VisitableXmlElement} from the root {@link
 * JAXBElement}, and pass an instance of {@link JsonTransformVisitor} into {@link
 * VisitableXmlElement#accept(org.codice.ddf.catalog.ui.forms.filter.FilterVisitor2)}.
 *
 * <p>While trying to wrap one's head around visitors, it would be helpful to set a breakpoint on
 * {@link #visitLiteralType(VisitableXmlElement)} and take some time analyzing the stack.
 */
public class JsonTransformVisitor extends AbstractFilterVisitor2 {
  private static final Logger LOGGER = LoggerFactory.getLogger(JsonTransformVisitor.class);

  private static final String FORMS_FUNCTION_V1 = "forms.function.1";

  private static final Map<String, String> BINARY_COMPARE_MAPPING =
      ImmutableMap.<String, String>builder()
          .put("PropertyIsEqualTo", "=")
          .put("PropertyIsNotEqualTo", "!=")
          .put("PropertyIsLessThan", "<")
          .put("PropertyIsLessThanOrEqualTo", "<=")
          .put("PropertyIsGreaterThan", ">")
          .put("PropertyIsGreaterThanOrEqualTo", ">=")
          .build();

  private final JsonModelBuilder builder = new JsonModelBuilder();

  public FilterNode getResult() {
    return builder.getResult();
  }

  // Work around for Value References not having an explicit binding
  @Override
  public void visitString(VisitableXmlElement<String> visitable) {
    JAXBElement<String> element = visitable.getElement();
    LOGGER.debug(element.getName().getLocalPart());
    LOGGER.debug(element.getValue());

    builder.setProperty(element.getValue());
  }

  @Override
  public void visitLiteralType(VisitableXmlElement<LiteralType> visitable) {
    JAXBElement<LiteralType> element = visitable.getElement();
    LOGGER.debug(element.getName().getLocalPart());

    List<Serializable> values = element.getValue().getContent();
    if (values == null || values.isEmpty()) {
      LOGGER.debug("No values found on literal type");
      return;
    }

    // Assumption: we only support one literal value
    values.forEach(v -> LOGGER.debug((String) v));
    builder.setValue(values.get(0).toString());
  }

  @Override
  public void visitFunctionType(VisitableXmlElement<FunctionType> visitable) {
    JAXBElement<FunctionType> element = visitable.getElement();
    LOGGER.debug(element.getName().getLocalPart());

    FunctionType functionType = element.getValue();
    String functionName = functionType.getName();

    if (FORMS_FUNCTION_V1.equals(functionName)) {
      List<Optional<Serializable>> args =
          functionType
              .getExpression()
              .stream()
              .map(JAXBElement::getValue)
              .map(LiteralType.class::cast)
              .flatMap(
                  t ->
                      (t.getContent() == null || t.getContent().isEmpty())
                          ? Stream.of(Optional.<Serializable>empty())
                          : t.getContent().stream().map(Optional::of))
              .collect(Collectors.toList());

      args.forEach(o -> LOGGER.debug(o.toString()));

      Function<Serializable, Boolean> boolFunc = s -> Boolean.parseBoolean((String) s);
      builder.setTemplatedValues(
          get(args, 0, String.class), /* Default Value */
          get(args, 1, String.class), /* Node ID */
          get(args, 2, boolFunc), /* Is Visible */
          get(args, 3, boolFunc)); /* Is Read-only */

    } else {
      throw new FilterProcessingException("Could not find a valid function name");
    }
  }

  @Override
  public void visitBinaryLogicType(VisitableXmlElement<BinaryLogicOpType> visitable) {
    JAXBElement<BinaryLogicOpType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    LOGGER.debug(localPart);

    builder.beginBinaryLogicType(localPart.toUpperCase());
    element.getValue().getOps().forEach(jax -> makeVisitable(jax).accept(this));
    builder.endBinaryLogicType();
  }

  @Override
  public void visitBinaryComparisonType(VisitableXmlElement<BinaryComparisonOpType> visitable) {
    JAXBElement<BinaryComparisonOpType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    LOGGER.debug(localPart);

    String operator = BINARY_COMPARE_MAPPING.get(localPart);
    if (operator == null) {
      throw new IllegalArgumentException(
          "Cannot find mapping for binary comparison operator: " + localPart);
    }

    builder.beginBinaryComparisonType(operator);
    element.getValue().getExpression().forEach(jax -> makeVisitable(jax).accept(this));
    builder.endBinaryComparisonType();
  }

  private static <T> T get(List<Optional<Serializable>> args, int i, Class<T> expectedType) {
    return get(args, i, expectedType::cast);
  }

  private static <T> T get(
      List<Optional<Serializable>> args, int i, Function<Serializable, T> transform) {
    return transform.apply(args.get(i).orElse(null));
  }
}
