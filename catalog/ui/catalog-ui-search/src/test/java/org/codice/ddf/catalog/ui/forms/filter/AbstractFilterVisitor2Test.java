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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import net.opengis.filter.v_2_0.ExtensionOpsType;
import net.opengis.filter.v_2_0.FilterType;
import net.opengis.filter.v_2_0.FunctionType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AbstractFilterVisitor2Test {

  // The wrapped JAXBElement at the root level, which contains the FilterType
  @Mock private VisitableXmlElement mockInput;

  @Mock private JAXBElement mockReturned;

  @Mock private FilterType mockFilterType;

  // The wrapped JAXBElement the next level down from the FilterType - accept() called on success
  @Mock private VisitableXmlElement mockNewVisitable;

  private AbstractFilterVisitor2UnderTest visitor;

  @Before
  public void setup() {
    when(mockInput.getElement()).thenReturn(mockReturned);
    when(mockReturned.getName()).thenReturn(mock(QName.class));
    when(mockReturned.getValue()).thenReturn(mockFilterType);
    visitor = new AbstractFilterVisitor2UnderTest(mockNewVisitable);
  }

  @Test
  public void testFilterHasComparisonOp() {
    when(mockFilterType.getComparisonOps()).thenReturn(mock(JAXBElement.class));
    visitor.visitFilter(mockInput);
    verify(mockNewVisitable).accept(eq(visitor));
  }

  @Test
  public void testFilterHasLogicOp() {
    when(mockFilterType.getLogicOps()).thenReturn(mock(JAXBElement.class));
    visitor.visitFilter(mockInput);
    verify(mockNewVisitable).accept(eq(visitor));
  }

  @Test
  public void testFilterHasSpatialOp() {
    when(mockFilterType.getSpatialOps()).thenReturn(mock(JAXBElement.class));
    visitor.visitFilter(mockInput);
    verify(mockNewVisitable).accept(eq(visitor));
  }

  @Test
  public void testFilterHasTemporalOp() {
    when(mockFilterType.getTemporalOps()).thenReturn(mock(JAXBElement.class));
    visitor.visitFilter(mockInput);
    verify(mockNewVisitable).accept(eq(visitor));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFilterHasIdType() {
    when(mockFilterType.getId()).thenReturn(mock(List.class));
    visitor.visitFilter(mockInput);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFilterHasExtensionOp() {
    when(mockFilterType.getExtensionOps()).thenReturn(mock(ExtensionOpsType.class));
    visitor.visitFilter(mockInput);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFilterStartsWithFunction() {
    when(mockFilterType.getFunction()).thenReturn(mock(FunctionType.class));
    visitor.visitFilter(mockInput);
  }

  @Test(expected = FilterProcessingException.class)
  public void testFilterHasUnrecognizedType() {
    // Do not set any mocks, so all results will be null
    visitor.visitFilter(mockInput);
  }

  private static class AbstractFilterVisitor2UnderTest extends AbstractFilterVisitor2 {
    private VisitableXmlElement visitable;

    AbstractFilterVisitor2UnderTest(VisitableXmlElement element) {
      this.visitable = element;
    }

    @Override
    protected VisitableXmlElement makeVisitable(JAXBElement element) {
      return visitable;
    }
  }
}
