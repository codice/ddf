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

import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.Validate.notNull;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.xml.bind.JAXBElement;
import net.opengis.filter.v_2_0.LiteralType;
import net.opengis.filter.v_2_0.ObjectFactory;

/** As more test cases are added, more support functions will be needed. */
class FilterNodeAssertionSupport {
  private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

  private FilterNodeAssertionSupport() {}

  static void assertParentNode(FilterNode node, String expectedType, int expectedChildCount) {
    assertParentNode(node, expectedType);
    assertThat(node.getChildren(), hasSize(expectedChildCount));
  }

  static void assertParentNode(FilterNode node, String expectedType) {
    assertThat(node.getOperator(), is(expectedType));
    assertThat(node.getChildren(), notNullValue());
    assertThat(node.isLeaf(), is(false));
  }

  static void assertLeafNode(
      FilterNode node, String expectedType, String expectedProperty, String expectedValue) {
    assertThat(node.getOperator(), is(expectedType));
    assertThat(node.isLeaf(), is(true));

    assertThat(node.getProperty(), is(expectedProperty));
    assertThat(node.getValue(), is(expectedValue));
    assertThat(node.isTemplated(), is(false));
  }

  static void assertTemplatedNode(
      FilterNode node,
      String expectedType,
      String expectedProperty,
      String defaultValue,
      String nodeId) {
    assertThat(node.getOperator(), is(expectedType));
    assertThat(node.isLeaf(), is(true));

    assertThat(node.getProperty(), is(expectedProperty));
    assertThat(node.getValue(), is(nullValue()));
    assertThat(node.isTemplated(), is(true));

    Map<String, Object> templateProps = node.getTemplateProperties();
    assertThat(templateProps.get("defaultValue"), is(defaultValue));
    assertThat(templateProps.get("nodeId"), is(nodeId));
  }

  @SuppressWarnings("unchecked" /* Casting since the data structure is known */)
  private static void assertExpression(
      List<JAXBElement<?>> elements, String property, Serializable value) {
    JAXBElement<String> propertyNode = (JAXBElement<String>) elements.get(0);
    JAXBElement<LiteralType> valueNode = (JAXBElement<LiteralType>) elements.get(1);
    assertThat(propertyNode.getValue(), is(property));
    assertThat(valueNode.getValue().getContent(), hasItem(value));
  }

  @SuppressWarnings("unchecked" /* Casting since the data structure is known */)
  private static void assertExpressionOrAny(
      List<Object> objects, String property, Serializable value) {
    JAXBElement<String> propertyNode = (JAXBElement<String>) objects.get(0);
    JAXBElement<LiteralType> valueNode = (JAXBElement<LiteralType>) objects.get(1);
    assertThat(propertyNode.getValue(), is(property));
    assertThat(valueNode.getValue().getContent(), hasItem(value));
  }

  /**
   * Starting point for all JAXB structure validations using the fluent assertion API below.
   *
   * @param element root {@link JAXBElement} to validate.
   * @return the next stage of validation options.
   */
  static JAXBValidationStarter forElement(JAXBElement<?> element) {
    return new JAXBValidationStarter(element);
  }

  static class JAXBValidationStarter {
    private JAXBElement<?> element;

    private JAXBValidationStarter(JAXBElement<?> element) {
      notNull(element);
      this.element = element;
    }

    /**
     * Validate the current {@link JAXBElement}'s binding to a particular class.
     *
     * @param binding the class of object within the {@link JAXBElement}.
     * @param <V> type parameter corresponding to the binding class.
     * @return assertion object used to take the next validation step.
     */
    public final <V> JAXBClassBindingAssertion<V> withBinding(Class<V> binding) {
      return new JAXBClassBindingAssertion<>(binding, element);
    }
  }

  static class JAXBClassBindingAssertion<T> {
    private final T elementValue;

    private Function<? super T, JAXBElement<?>> nextElement;

    @SuppressWarnings("unchecked" /* Assertions handle the check - if they pass, the value is T */)
    private JAXBClassBindingAssertion(final Class<T> binding, final JAXBElement<?> element) {
      notNull(binding);
      notNull(element);
      // Need to use assertTrue because assertThat cannot infer capture<?> types on generic methods
      assertTrue(binding.equals(element.getDeclaredType()));
      assertTrue(binding.isInstance(element.getValue()));
      this.elementValue = (T) element.getValue();
    }

    /**
     * Specify an operation to perform on {@code T} that returns the next element in the JAXB data
     * structure.
     *
     * @param nextElement mapping operation for data structure traversal.
     * @return assertion object used to take the next validation step.
     */
    public JAXBClassBindingAssertion<T> forElement(
        Function<? super T, JAXBElement<?>> nextElement) {
      notNull(nextElement);
      this.nextElement = nextElement;
      return this;
    }

    /**
     * Validate the current {@link JAXBElement}'s binding to a particular class.
     *
     * @param binding the class of object within the {@link JAXBElement}.
     * @param <V> type parameter corresponding to the binding class.
     * @return assertion object used to take the next validation step.
     */
    public <V> JAXBClassBindingAssertion<V> withBinding(Class<V> binding) {
      notNull(nextElement);
      return new JAXBClassBindingAssertion<V>(binding, nextElement.apply(elementValue));
    }

    /**
     * Begin data validation on a non-terminal elements.
     *
     * @param expressionMapper mapping operation for data structure traversal.
     * @return assertion object used to take the next validation step.
     */
    public JAXBCollectionAssertion withExpression(
        Function<? super T, List<JAXBElement<?>>> expressionMapper) {
      notNull(expressionMapper);
      return new JAXBCollectionAssertion(expressionMapper.apply(elementValue));
    }

    /**
     * Begin validation on terminal elements whose values conform to {@code List<JAXBElement<?>>}.
     *
     * @param expressionMapper mapping operation for data structure traversal.
     * @return assertion object used to take the next validation step.
     */
    public JAXBPropertyValueAssertion verifyExpression(
        Function<? super T, List<JAXBElement<?>>> expressionMapper) {
      return new JAXBPropertyValueAssertion(
          (p, v) -> assertExpression(expressionMapper.apply(elementValue), p, v));
    }

    /**
     * Begin validation on terminal elements whose values conform to {@code List<Object>}.
     *
     * @param expressionOrAnyMapper mapping operation for data structure traversal.
     * @return assertion object used to take the next validation step.
     */
    public JAXBPropertyValueAssertion verifyExpressionOrAny(
        Function<? super T, List<Object>> expressionOrAnyMapper) {
      return new JAXBPropertyValueAssertion(
          (p, v) -> assertExpressionOrAny(expressionOrAnyMapper.apply(elementValue), p, v));
    }
  }

  static class JAXBPropertyValueAssertion {
    private final BiConsumer<String, Serializable> assertion;

    private JAXBPropertyValueAssertion(BiConsumer<String, Serializable> assertion) {
      this.assertion = assertion;
    }

    /**
     * Specify the data to test for. This is a terminal operation.
     *
     * @param property the expected value for the property field, also called the {@code
     *     <ValueReference/>}.
     * @param value the expected value for the value field, also called the {@code <Literal/>}.
     */
    public void withData(String property, Serializable value) {
      assertion.accept(property, value);
    }
  }

  static class JAXBCollectionAssertion {
    private final List<JAXBElement<?>> elements;

    private JAXBCollectionAssertion(List<JAXBElement<?>> elements) {
      this.elements = elements;
    }

    /**
     * Provide a set of assertions that define the collection of {@link JAXBElement}s.
     *
     * <p>The number of assertions provided should be equal to the number of {@link JAXBElement}s
     * present in the list or the test will fail. The first assertion will be executed against the
     * first {@link JAXBElement}, the second assertion against the 2nd element, and so on in this
     * fashion.
     *
     * @param assertions set of assertions to map to the {@link JAXBElement}s.
     */
    @SafeVarargs
    public final void satisfies(Consumer<JAXBElement<?>>... assertions) {
      List<Consumer<JAXBElement<?>>> assertionsList = Arrays.asList(assertions);
      assertThat(elements.size(), is(assertionsList.size()));
      for (int i = 0; i < assertionsList.size(); i++) {
        assertionsList.get(i).accept(elements.get(i));
      }
    }
  }
}
