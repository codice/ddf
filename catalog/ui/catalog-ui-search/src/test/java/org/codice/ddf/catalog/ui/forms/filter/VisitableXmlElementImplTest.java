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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class VisitableXmlElementImplTest {

  @Mock private JAXBElement mockElement;

  @Mock private FilterVisitor2 mockVisitor;

  private VisitableXmlElementImpl node;

  @Before
  public void setup() {
    node = new VisitableXmlElementImplUnderTest(mockElement);
  }

  @Test(expected = FilterProcessingException.class)
  public void testUnknownType() {
    when(mockElement.getDeclaredType()).thenReturn(Object.class);
    node.accept(mockVisitor);
  }

  @Test
  public void testFilterType() {
    parameterizedMappingVerification(FilterType.class, (v) -> verify(v).visitFilter(eq(node)));
  }

  @Test
  public void testValueReference() {
    parameterizedMappingVerification(String.class, (v) -> verify(v).visitString(eq(node)));
  }

  @Test
  public void testLiteralType() {
    parameterizedMappingVerification(
        LiteralType.class, (v) -> verify(v).visitLiteralType(eq(node)));
  }

  @Test
  public void testFunctionType() {
    parameterizedMappingVerification(
        FunctionType.class, (v) -> verify(v).visitFunctionType(eq(node)));
  }

  @Test
  public void testBinaryLogicOpType() {
    parameterizedMappingVerification(
        BinaryLogicOpType.class, (v) -> verify(v).visitBinaryLogicType(eq(node)));
  }

  @Test
  public void testUnaryLogicOpType() {
    parameterizedMappingVerification(
        UnaryLogicOpType.class, (v) -> verify(v).visitUnaryLogicType(eq(node)));
  }

  @Test
  public void testBinaryTemporalOpType() {
    parameterizedMappingVerification(
        BinaryTemporalOpType.class, (v) -> verify(v).visitBinaryTemporalType(eq(node)));
  }

  @Test
  public void testBinarySpatialOpType() {
    parameterizedMappingVerification(
        BinarySpatialOpType.class, (v) -> verify(v).visitBinarySpatialType(eq(node)));
  }

  @Test
  public void testDistanceBufferType() {
    parameterizedMappingVerification(
        DistanceBufferType.class, (v) -> verify(v).visitDistanceBufferType(eq(node)));
  }

  @Test
  public void tesetBBoxType() {
    parameterizedMappingVerification(
        BBOXType.class, (v) -> verify(v).visitBoundingBoxType(eq(node)));
  }

  @Test
  public void testBinaryComparisonOpType() {
    parameterizedMappingVerification(
        BinaryComparisonOpType.class, (v) -> verify(v).visitBinaryComparisonType(eq(node)));
  }

  @Test
  public void testPropertyIsLikeType() {
    parameterizedMappingVerification(
        PropertyIsLikeType.class, (v) -> verify(v).visitPropertyIsLikeType(eq(node)));
  }

  @Test
  public void testPropertyIsNullType() {
    parameterizedMappingVerification(
        PropertyIsNullType.class, (v) -> verify(v).visitPropertyIsNullType(eq(node)));
  }

  @Test
  public void testPropertyIsNilType() {
    parameterizedMappingVerification(
        PropertyIsNilType.class, (v) -> verify(v).visitPropertyIsNilType(eq(node)));
  }

  @Test
  public void testPropertyIsBetweenType() {
    parameterizedMappingVerification(
        PropertyIsBetweenType.class, (v) -> verify(v).visitPropertyIsBetweenType(eq(node)));
  }

  private void parameterizedMappingVerification(
      Class type, Consumer<FilterVisitor2> assertOnVisitFunction) {
    when(mockElement.getDeclaredType()).thenReturn(type);
    node.accept(mockVisitor);

    verify(mockElement).getDeclaredType();
    assertOnVisitFunction.accept(mockVisitor);
    verifyNoMoreInteractions(mockVisitor, mockElement);
  }

  private static class VisitableXmlElementImplUnderTest extends VisitableXmlElementImpl<Object> {
    VisitableXmlElementImplUnderTest(JAXBElement e) {
      super(e);
    }

    @Override
    public Object getValue() {
      return null;
    }
  }
}
