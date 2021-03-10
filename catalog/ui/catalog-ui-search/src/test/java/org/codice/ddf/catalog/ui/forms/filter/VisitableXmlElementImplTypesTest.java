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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import java.util.List;
import javax.xml.bind.JAXBElement;
import net.opengis.filter.v_2_0.BinaryComparisonOpType;
import net.opengis.filter.v_2_0.BinaryLogicOpType;
import net.opengis.filter.v_2_0.BinarySpatialOpType;
import net.opengis.filter.v_2_0.BinaryTemporalOpType;
import net.opengis.filter.v_2_0.ExtensionOpsType;
import net.opengis.filter.v_2_0.FilterType;
import net.opengis.filter.v_2_0.FunctionType;
import net.opengis.filter.v_2_0.LiteralType;
import net.opengis.filter.v_2_0.ObjectFactory;
import net.opengis.filter.v_2_0.PropertyIsLikeType;
import net.opengis.filter.v_2_0.ResourceIdType;
import org.junit.Test;

/**
 * Still need tests for additional types.
 *
 * @see net.opengis.filter.v_2_0.UnaryLogicOpType
 * @see net.opengis.filter.v_2_0.PropertyIsNullType
 * @see net.opengis.filter.v_2_0.PropertyIsNilType
 * @see net.opengis.filter.v_2_0.PropertyIsBetweenType
 */
public class VisitableXmlElementImplTypesTest {
  private static final ObjectFactory FACTORY = new ObjectFactory();

  private static final JAXBElement<String> VALUE_REFERENCE_NODE =
      FACTORY.createValueReference("key");

  private static final JAXBElement<LiteralType> LITERAL_NODE =
      FACTORY.createLiteral(new LiteralType().withContent("value"));

  private static final JAXBElement<PropertyIsLikeType> LIKE_NODE =
      FACTORY.createPropertyIsLike(
          new PropertyIsLikeType().withExpression(VALUE_REFERENCE_NODE, LITERAL_NODE));

  private static final JAXBElement<BinaryComparisonOpType> BINARY_COMPARISON_NODE =
      FACTORY.createPropertyIsEqualTo(
          new BinaryComparisonOpType().withExpression(VALUE_REFERENCE_NODE, LITERAL_NODE));

  private static final JAXBElement<BinarySpatialOpType> BINARY_SPATIAL_NODE =
      FACTORY.createIntersects(
          new BinarySpatialOpType().withExpressionOrAny(VALUE_REFERENCE_NODE, LITERAL_NODE));

  private static final JAXBElement<BinaryTemporalOpType> BINARY_TEMPORAL_NODE =
      FACTORY.createBefore(
          new BinaryTemporalOpType().withExpressionOrAny(VALUE_REFERENCE_NODE, LITERAL_NODE));

  private static final JAXBElement<BinaryLogicOpType> BINARY_LOGIC_NODE =
      FACTORY.createAnd(new BinaryLogicOpType().withOps(BINARY_COMPARISON_NODE));

  private static final JAXBElement<FilterType> FILTER_NODE_WITH_COMPARISON =
      FACTORY.createFilter(new FilterType().withComparisonOps(BINARY_COMPARISON_NODE));

  private static final JAXBElement<FilterType> FILTER_NODE_WITH_SPATIAL =
      FACTORY.createFilter(new FilterType().withSpatialOps(BINARY_SPATIAL_NODE));

  private static final JAXBElement<FilterType> FILTER_NODE_WITH_TEMPORAL =
      FACTORY.createFilter(new FilterType().withTemporalOps(BINARY_TEMPORAL_NODE));

  private static final JAXBElement<FilterType> FILTER_NODE_WITH_LOGIC =
      FACTORY.createFilter(new FilterType().withLogicOps(BINARY_LOGIC_NODE));

  private static final JAXBElement<FilterType> FILTER_NODE_WITH_ID =
      FACTORY.createFilter(new FilterType().withId(FACTORY.createId(new ResourceIdType())));

  private static final JAXBElement<FilterType> FILTER_NODE_WITH_EXT =
      FACTORY.createFilter(
          new FilterType()
              .withExtensionOps(
                  new ExtensionOpsType() {
                    @Override
                    public Object createNewInstance() {
                      return new Object();
                    }
                  }));

  private static final JAXBElement<FilterType> FILTER_NODE_STARTS_WITH_FUNCTION =
      FACTORY.createFilter(new FilterType().withFunction(new FunctionType()));

  @Test
  public void testExpressionForBinaryComparison() {
    validateResultList(BINARY_COMPARISON_NODE, VisitableXmlElementImpl.class);
  }

  @Test
  public void testExpressionForBinaryLogic() {
    validateResultList(BINARY_LOGIC_NODE, VisitableXmlElementImpl.class);
  }

  @Test
  public void testExpressionForLike() {
    validateResultList(LIKE_NODE, VisitableXmlElementImpl.class);
  }

  @Test
  public void testSingletonExpressionForFilterWithComparison() {
    validateResult(FILTER_NODE_WITH_COMPARISON, VisitableXmlElementImpl.class);
  }

  @Test
  public void testSingletonExpressionForFilterWithLogic() {
    validateResult(FILTER_NODE_WITH_LOGIC, VisitableXmlElementImpl.class);
  }

  @Test
  public void testSingletonExpressionForFilterWithSpatial() {
    validateResult(FILTER_NODE_WITH_SPATIAL, VisitableXmlElementImpl.class);
  }

  @Test
  public void testSingletonExpressionForFilterWithTemporal() {
    validateResult(FILTER_NODE_WITH_TEMPORAL, VisitableXmlElementImpl.class);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testSingletonExpressionForFilterWithId() {
    VisitableXmlElementImpl.create(FILTER_NODE_WITH_ID);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testSingletonExpressionForFilterWithExt() {
    VisitableXmlElementImpl.create(FILTER_NODE_WITH_EXT);
  }

  private static void validateResult(JAXBElement element, Class type) {
    VisitableXmlElementImpl visitable = VisitableXmlElementImpl.create(element);
    assertThat(visitable.getValue(), instanceOf(type));
  }

  private static void validateResultList(JAXBElement element, Class type) {
    VisitableXmlElementImpl visitable = VisitableXmlElementImpl.create(element);
    assertThat(visitable.getValue(), instanceOf(List.class));
    List<?> list = (List) visitable.getValue();
    assertThat(list, is(not(empty())));
    list.forEach(obj -> assertThat(obj, instanceOf(type)));
  }
}
