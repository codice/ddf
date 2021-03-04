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

import static org.apache.commons.lang3.Validate.notNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBElement;
import net.opengis.filter.v_2_0.AbstractIdType;
import net.opengis.filter.v_2_0.BBOXType;
import net.opengis.filter.v_2_0.BinaryComparisonOpType;
import net.opengis.filter.v_2_0.BinaryLogicOpType;
import net.opengis.filter.v_2_0.BinarySpatialOpType;
import net.opengis.filter.v_2_0.BinaryTemporalOpType;
import net.opengis.filter.v_2_0.DistanceBufferType;
import net.opengis.filter.v_2_0.FilterType;
import net.opengis.filter.v_2_0.FunctionType;
import net.opengis.filter.v_2_0.LiteralType;
import net.opengis.filter.v_2_0.ObjectFactory;
import net.opengis.filter.v_2_0.PropertyIsBetweenType;
import net.opengis.filter.v_2_0.PropertyIsLikeType;
import net.opengis.filter.v_2_0.PropertyIsNilType;
import net.opengis.filter.v_2_0.PropertyIsNullType;
import net.opengis.filter.v_2_0.UnaryLogicOpType;
import org.codice.ddf.catalog.ui.forms.api.FilterVisitor2;
import org.codice.ddf.catalog.ui.forms.api.VisitableElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a {@link JAXBElement} containing a filter component. The goal of this class is to provide a
 * mapping from the Filter 2.0 binding type to the visit method that should be invoked.
 *
 * <p>Also returns an appropriate abstraction to decouple visitable data structures from JAXB.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public abstract class VisitableXmlElementImpl<T> implements VisitableElement<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger(VisitableXmlElementImpl.class);

  private static final ObjectFactory FACTORY = new ObjectFactory();

  private static final String INVALID_INVOCATION = "Could not find valid invocation for type: ";

  private static final Map<Class, BiConsumer<FilterVisitor2, VisitableElement>> VISIT_METHODS =
      ImmutableMap.<Class, BiConsumer<FilterVisitor2, VisitableElement>>builder()
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

  @VisibleForTesting
  VisitableXmlElementImpl(final JAXBElement element) {
    notNull(element);
    this.element = element;
  }

  public static VisitableXmlElementImpl create(final JAXBElement element) {
    return SubtypeFactory.createElement(element);
  }

  @Override
  public String getName() {
    return element.getName().getLocalPart();
  }

  @Override
  public String getFunctionName() {
    return null;
  }

  @Override
  public void accept(FilterVisitor2 visitor) {
    Class clazz = element.getDeclaredType();
    BiConsumer<FilterVisitor2, VisitableElement> biConsumer = VISIT_METHODS.get(clazz);
    if (biConsumer == null) {
      throw new FilterProcessingException(
          "Encountered an unexpected or unsupported type: " + clazz.getName());
    }
    // Actually invoking one of the "visits" on local variable "visitor"
    biConsumer.accept(visitor, this);
  }

  private static JAXBElement<?> extractFromFilter(Object object) {
    FilterType filterType = (FilterType) object;
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
      LOGGER.trace("Valid root found, beginning traversal");
      return root;
    }

    FunctionType functionType = filterType.getFunction();
    if (functionType != null) {
      return FACTORY.createFunction(functionType);
    }

    // Support can be enhanced in the future, but currently these components aren't needed
    // Ticket for adding support - https://codice.atlassian.net/browse/DDF-3829
    handleUnsupported(filterType.getId());
    handleUnsupported(filterType.getExtensionOps());

    throw new FilterProcessingException("No valid starting element for the filter was found");
  }

  private static JAXBElement<?> extractFromUnary(Object object) {
    UnaryLogicOpType unaryLogicOpType = (UnaryLogicOpType) object;
    JAXBElement<?> node =
        Stream.of(
                unaryLogicOpType.getComparisonOps(),
                unaryLogicOpType.getLogicOps(),
                unaryLogicOpType.getSpatialOps(),
                unaryLogicOpType.getTemporalOps())
            .filter(Objects::nonNull)
            .findAny()
            .orElse(null);

    if (node != null) {
      LOGGER.trace("Found valid child for unary operator");
      return node;
    }

    // Support can be enhanced in the future, but currently these components aren't needed
    // Ticket for adding support - https://codice.atlassian.net/browse/DDF-3829
    handleUnsupported(unaryLogicOpType.getId());
    handleUnsupported(unaryLogicOpType.getExtensionOps());

    // Functions are supported but not immediately after a Unary operator
    handleUnsupported(unaryLogicOpType.getFunction());

    throw new FilterProcessingException("No valid starting element for the unary op was found");
  }

  private static void handleUnsupported(Object type) {
    if (type != null) {
      throw new UnsupportedOperationException(
          "Encountered XML filter with unsupported element: " + type.getClass().getName());
    }
  }

  private static void handleUnsupported(List<JAXBElement<? extends AbstractIdType>> list) {
    if (!list.isEmpty()) {
      throw new UnsupportedOperationException(
          "Encountered XML filter with unsupported element: "
              + list.get(0).getDeclaredType().getName());
    }
  }

  /**
   * Responsible for mapping the filter types to the correct instance of {@link
   * VisitableXmlElementImpl}.
   */
  private static class SubtypeFactory {
    private static final Map<Class, Function<JAXBElement, VisitableXmlElementImpl>> CTORS =
        ImmutableMap.<Class, Function<JAXBElement, VisitableXmlElementImpl>>builder()
            // Value references
            .put(String.class, ValueReferenceElement::new)
            // Literals
            .put(LiteralType.class, LiteralElement::new)
            // Functions
            .put(FunctionType.class, FunctionElement::new)
            // Singleton Expression Elements
            .put(FilterType.class, SingletonExpressionElement::new)
            .put(UnaryLogicOpType.class, SingletonExpressionElement::new)
            .put(PropertyIsNullType.class, SingletonExpressionElement::new)
            .put(PropertyIsNilType.class, SingletonExpressionElement::new)
            // Expression Elements
            .put(BinaryLogicOpType.class, ExpressionElement::new)
            .put(BinaryComparisonOpType.class, ExpressionElement::new)
            .put(PropertyIsLikeType.class, ExpressionElement::new)
            // Expression or Any Elements
            .put(BinaryTemporalOpType.class, ExpressionOrAnyElement::new)
            .put(BinarySpatialOpType.class, ExpressionOrAnyElement::new)
            .put(DistanceBufferType.class, DistanceBufferElement::new)
            .put(BBOXType.class, ExpressionOrAnyElement::new)
            .build();

    private static VisitableXmlElementImpl<?> createElement(JAXBElement element) {
      Function<JAXBElement, VisitableXmlElementImpl> creatorFunction =
          CTORS.get(element.getDeclaredType());
      if (creatorFunction == null) {
        throw new FilterProcessingException(
            "Could not find valid constructor for type: " + element.getDeclaredType().getName());
      }
      return creatorFunction.apply(element);
    }
  }

  /**
   * Represents an element that contains a Filter Function. That is, the node has children that can
   * be decomposed into a {@code Map<String, Object>} for the use of templates.
   *
   * <p>Function currying (embedded functions) is not yet supported.
   */
  private static class FunctionElement extends VisitableXmlElementImpl<List<Serializable>> {
    private final String functionName;

    private final List<Serializable> value;

    private FunctionElement(JAXBElement element) {
      super(element);
      if (!element.getDeclaredType().equals(FunctionType.class)) {
        throw new FilterProcessingException(
            INVALID_INVOCATION + element.getDeclaredType().getName());
      }
      FunctionType functionType = (FunctionType) element.getValue();
      this.functionName = functionType.getName();
      this.value =
          functionType
              .getExpression()
              .stream()
              .map(JAXBElement::getValue)
              .map(LiteralType.class::cast)
              .map(LiteralType::getContent)
              .flatMap(
                  content ->
                      (content == null || content.isEmpty())
                          ? Stream.of(Optional.<Serializable>empty())
                          : content.stream().map(Optional::of))
              .map(opt -> opt.orElse(null))
              .collect(Collectors.toList());
    }

    @Override
    public String getFunctionName() {
      return functionName;
    }

    @Override
    public List<Serializable> getValue() {
      return value;
    }
  }

  /**
   * Represents an element that contains a Filter Expression. That is, the node has children of the
   * form {@code List<VisitableElement<?>>}.
   *
   * <p>The current implementation has several limitations:
   *
   * <ul>
   *   <li>{@link PropertyIsLikeType} is not fully supported yet, and the additional attributes need
   *       to be taken into account when support is built in.
   * </ul>
   *
   * @see PropertyIsLikeType#getEscapeChar()
   * @see PropertyIsLikeType#getSingleChar()
   * @see PropertyIsLikeType#getWildCard()
   */
  private static class ExpressionElement
      extends VisitableXmlElementImpl<List<VisitableElement<?>>> {
    private static final Map<Class<?>, Function<Object, List<JAXBElement<?>>>> PROVIDERS =
        ImmutableMap.<Class<?>, Function<Object, List<JAXBElement<?>>>>builder()
            .put(BinaryLogicOpType.class, t -> ((BinaryLogicOpType) t).getOps())
            .put(BinaryComparisonOpType.class, t -> ((BinaryComparisonOpType) t).getExpression())
            .put(PropertyIsLikeType.class, t -> ((PropertyIsLikeType) t).getExpression())
            .build();

    private final List<VisitableElement<?>> value;

    private ExpressionElement(JAXBElement element) {
      super(element);
      Function<Object, List<JAXBElement<?>>> invocation = PROVIDERS.get(element.getDeclaredType());
      if (invocation == null) {
        throw new FilterProcessingException(
            INVALID_INVOCATION + element.getDeclaredType().getName());
      }
      this.value =
          Stream.of(element)
              .map(JAXBElement::getValue)
              .map(invocation)
              .flatMap(List::stream)
              .map(SubtypeFactory::createElement)
              .collect(Collectors.toList());
    }

    @Override
    public List<VisitableElement<?>> getValue() {
      return value;
    }
  }

  /**
   * Represents an element that contains a single child in it's expression. That is, the node has
   * children of the form {@code VisitableElement<?>}.
   *
   * <p>Note that types {@link FilterType} and {@link UnaryLogicOpType} have a wider range for
   * children than just a simple expression. As such, they get special static handler functions in
   * the parent class.
   */
  private static class SingletonExpressionElement
      extends VisitableXmlElementImpl<VisitableElement<?>> {
    private static final Map<Class<?>, Function<Object, JAXBElement<?>>> PROVIDERS =
        ImmutableMap.<Class<?>, Function<Object, JAXBElement<?>>>builder()
            .put(FilterType.class, VisitableXmlElementImpl::extractFromFilter)
            .put(UnaryLogicOpType.class, VisitableXmlElementImpl::extractFromUnary)
            .put(PropertyIsNullType.class, t -> ((PropertyIsNullType) t).getExpression())
            .put(PropertyIsNilType.class, t -> ((PropertyIsNilType) t).getExpression())
            .build();

    private final VisitableElement<?> value;

    private SingletonExpressionElement(JAXBElement element) {
      super(element);
      Function<Object, JAXBElement<?>> invocation = PROVIDERS.get(element.getDeclaredType());
      if (invocation == null) {
        throw new FilterProcessingException(
            INVALID_INVOCATION + element.getDeclaredType().getName());
      }
      this.value =
          Optional.of(element)
              .map(JAXBElement::getValue)
              .map(invocation)
              .map(SubtypeFactory::createElement)
              .orElseThrow(NullPointerException::new); // Should never occur - see super ctor
    }

    @Override
    public VisitableElement<?> getValue() {
      return value;
    }
  }

  /**
   * Represents an element that contains a Filter Expression or any other data structure. That is,
   * the node has children of the form {@code List<Object>}. The objects could be {@link
   * VisitableElement}s or not.
   *
   * <p>The current implementation has some limitations:
   *
   * <ul>
   *   <li>It is currently expected that all children are simple representations of temporal and
   *       spatial data, for instance, individual date strings or WKT strings in a {@code
   *       <Literal/>} block. Thus casting to a {@link JAXBElement} is a temporary stop-gap measure.
   * </ul>
   */
  private static class ExpressionOrAnyElement extends VisitableXmlElementImpl<List<Object>> {
    private static final Map<Class<?>, Function<Object, List<Object>>> PROVIDERS =
        ImmutableMap.<Class<?>, Function<Object, List<Object>>>builder()
            .put(BinaryTemporalOpType.class, t -> ((BinaryTemporalOpType) t).getExpressionOrAny())
            .put(BinarySpatialOpType.class, t -> ((BinarySpatialOpType) t).getExpressionOrAny())
            .put(DistanceBufferType.class, t -> ((DistanceBufferType) t).getExpressionOrAny())
            .put(BBOXType.class, t -> ((BBOXType) t).getExpressionOrAny())
            .build();

    private final List<Object> value;

    private ExpressionOrAnyElement(JAXBElement element) {
      super(element);
      Function<Object, List<Object>> invocation = PROVIDERS.get(element.getDeclaredType());
      if (invocation == null) {
        throw new FilterProcessingException(
            INVALID_INVOCATION + element.getDeclaredType().getName());
      }
      try {
        this.value =
            Stream.of(element)
                .map(JAXBElement::getValue)
                .map(invocation)
                .flatMap(List::stream)
                // Will throw a ClassCastException for unexpected ANY data (GML, etc)
                .map(JAXBElement.class::cast)
                .map(SubtypeFactory::createElement)
                .collect(Collectors.toList());
      } catch (ClassCastException e) {
        // Ticket to add support: https://codice.atlassian.net/browse/DDF-3830
        throw new UnsupportedOperationException("GML or ANY XML is currently not supported", e);
      }
    }

    @Override
    public List<Object> getValue() {
      return value;
    }
  }

  private static class DistanceBufferElement extends ExpressionOrAnyElement {

    private final double buffer;

    public DistanceBufferElement(JAXBElement element) {
      super(element);
      if (!element.getDeclaredType().equals(DistanceBufferType.class)) {
        throw new FilterProcessingException(
            INVALID_INVOCATION + element.getDeclaredType().getName());
      }
      this.buffer = ((DistanceBufferType) element.getValue()).getDistance().getValue();
    }

    @Override
    public List<Object> getValue() {
      List<Object> values = super.getValue();
      values.add(
          new VisitableElement<Double>() {
            @Override
            public String getName() {
              return "Distance";
            }

            @Override
            public Double getValue() {
              return buffer;
            }

            @Override
            public void accept(FilterVisitor2 visitor) {
              visitor.visitDoubleType(this);
            }

            @Nullable
            @Override
            public String getFunctionName() {
              return null;
            }
          });
      return values;
    }
  }

  /**
   * Represents a node containing the name of a property. In Filter XML it corresponds to a {@code
   * <ValueReference/>} type, but the binding does not supply a specific class for this type of
   * element.
   */
  private static class ValueReferenceElement extends VisitableXmlElementImpl<String> {
    private final String value;

    private ValueReferenceElement(JAXBElement element) {
      super(element);
      if (!element.getDeclaredType().equals(String.class)) {
        throw new FilterProcessingException(
            INVALID_INVOCATION + element.getDeclaredType().getName());
      }
      this.value =
          Optional.of(element)
              .map(JAXBElement::getValue)
              .map(String.class::cast)
              .orElseThrow(NullPointerException::new); // Should never occur - see super ctor
    }

    @Override
    public String getValue() {
      return value;
    }
  }

  /**
   * Represents a node containing the value of a property. In Filter XML it corresponds to a {@code
   * <Literal/>} type.
   *
   * <p>When a series of {@code <Literal/>} elements are encountered by the Filter JAXB binding,
   * they are each read as their own {@link LiteralType} <b>in order of appearance</b> with the data
   * of each element being populated into their own list of serializables, which can be obtained
   * using {@link LiteralType#getContent()}. So, for the following series:
   *
   * <p>{@code <Literal>0.78</Literal> <Literal>my value</Literal> <Literal>true</Literal> }
   *
   * <p>The result would be a list of {@link LiteralType}s, each with a list of serializables of
   * size one. The corresponding entry in each list on each {@link LiteralType} would be String
   * values in the order of "0.78", "my value", and "true". Additional parsing is necessary to
   * hydrate a primitive type.
   */
  private static class LiteralElement extends VisitableXmlElementImpl<List<Serializable>> {
    private final List<Serializable> value;

    private LiteralElement(JAXBElement element) {
      super(element);
      if (!element.getDeclaredType().equals(LiteralType.class)) {
        throw new FilterProcessingException(
            INVALID_INVOCATION + element.getDeclaredType().getName());
      }
      this.value =
          Optional.of(element)
              .map(JAXBElement::getValue)
              .map(LiteralType.class::cast)
              .map(LiteralType::getContent)
              .orElseThrow(NullPointerException::new); // Should never occur - see super ctor
    }

    @Override
    public List<Serializable> getValue() {
      return value;
    }
  }
}
