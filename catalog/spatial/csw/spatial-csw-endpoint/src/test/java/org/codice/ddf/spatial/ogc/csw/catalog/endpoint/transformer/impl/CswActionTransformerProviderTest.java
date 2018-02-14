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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.transformer.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Optional;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.transformer.CswActionTransformer;
import org.junit.Before;
import org.junit.Test;

public class CswActionTransformerProviderTest {

  private static final String TEST_TYPENAME = "some:typename";

  private static final String TEST_TYPENAME_2 = "another:typename";

  private CswActionTransformerProviderImpl cswActionTransformerProvider;

  @Before
  public void setup() {
    cswActionTransformerProvider = new CswActionTransformerProviderImpl();
  }

  @Test
  public void testBindOnNullDoesNotThrowError() {
    cswActionTransformerProvider.bind(null);
  }

  @Test
  public void testGetTransformerWithTypeName() {
    CswActionTransformer mockTransformer = getMockCswActionTransformer();
    cswActionTransformerProvider.bind(mockTransformer);

    Optional<CswActionTransformer> optional =
        cswActionTransformerProvider.getTransformer(TEST_TYPENAME);
    assertOptionalIs(optional, mockTransformer);
  }

  @Test
  public void testBindDoesNotOverrideExistingTransformersWithSameTypeName() {
    CswActionTransformer transformer1 = getMockCswActionTransformer();
    cswActionTransformerProvider.bind(transformer1);

    CswActionTransformer transformer2 = getMockCswActionTransformer();
    cswActionTransformerProvider.bind(transformer2);

    Optional<CswActionTransformer> optional =
        cswActionTransformerProvider.getTransformer(TEST_TYPENAME);
    assertOptionalIs(optional, transformer1);
  }

  @Test
  public void testBindTransformerWithNullTypeNames() {
    CswActionTransformer mockTransformer = getMockCswActionTransformer();
    when(mockTransformer.getTypeNames()).thenReturn(null);
    cswActionTransformerProvider.bind(mockTransformer);

    Optional<CswActionTransformer> optional =
        cswActionTransformerProvider.getTransformer(TEST_TYPENAME);
    assertOptionalIsEmpty(optional);
  }

  @Test
  public void testBindTransformerWithEmptyTypeNames() {
    CswActionTransformer mockTransformer = getMockCswActionTransformer();
    when(mockTransformer.getTypeNames()).thenReturn(Collections.emptySet());
    cswActionTransformerProvider.bind(mockTransformer);

    Optional<CswActionTransformer> optional =
        cswActionTransformerProvider.getTransformer(TEST_TYPENAME);
    assertOptionalIsEmpty(optional);
  }

  @Test
  public void testBindTransformerWithMultipleTypeNames() {
    CswActionTransformer mockTransformer = getMockCswActionTransformer();
    when(mockTransformer.getTypeNames())
        .thenReturn(Sets.newHashSet(TEST_TYPENAME, TEST_TYPENAME_2));
    cswActionTransformerProvider.bind(mockTransformer);

    Optional<CswActionTransformer> optional1 =
        cswActionTransformerProvider.getTransformer(TEST_TYPENAME);
    assertOptionalIs(optional1, mockTransformer);

    Optional<CswActionTransformer> optional2 =
        cswActionTransformerProvider.getTransformer(TEST_TYPENAME_2);
    assertOptionalIs(optional2, mockTransformer);
  }

  @Test
  public void testUnbindTransformerWithMultipleTypeNames() {
    CswActionTransformer mockTransformer = getMockCswActionTransformer();
    when(mockTransformer.getTypeNames())
        .thenReturn(Sets.newHashSet(TEST_TYPENAME, TEST_TYPENAME_2));
    cswActionTransformerProvider.bind(mockTransformer);

    Optional<CswActionTransformer> optional1 =
        cswActionTransformerProvider.getTransformer(TEST_TYPENAME);
    assertOptionalIs(optional1, mockTransformer);

    Optional<CswActionTransformer> optional2 =
        cswActionTransformerProvider.getTransformer(TEST_TYPENAME_2);
    assertOptionalIs(optional2, mockTransformer);

    cswActionTransformerProvider.unbind(mockTransformer);

    Optional<CswActionTransformer> empty1 =
        cswActionTransformerProvider.getTransformer(TEST_TYPENAME);
    assertOptionalIsEmpty(empty1);

    Optional<CswActionTransformer> empty2 =
        cswActionTransformerProvider.getTransformer(TEST_TYPENAME_2);
    assertOptionalIsEmpty(empty2);
  }

  @Test
  public void testUnbind() {
    CswActionTransformer mockTransformer = getMockCswActionTransformer();
    cswActionTransformerProvider.bind(mockTransformer);

    Optional<CswActionTransformer> optional =
        cswActionTransformerProvider.getTransformer(TEST_TYPENAME);
    assertOptionalIs(optional, mockTransformer);

    cswActionTransformerProvider.unbind(mockTransformer);

    Optional<CswActionTransformer> empty =
        cswActionTransformerProvider.getTransformer(TEST_TYPENAME);
    assertOptionalIsEmpty(empty);
  }

  @Test
  public void testUnbindOnNullDoesNotThrowError() {
    cswActionTransformerProvider.unbind(null);
  }

  @Test
  public void testUnbindOnUnboundTransformerDoesNotThrowError() {
    CswActionTransformer unboundTransformer = getMockCswActionTransformer();
    cswActionTransformerProvider.unbind(unboundTransformer);
  }

  private CswActionTransformer getMockCswActionTransformer() {
    CswActionTransformer mockTransformer = mock(CswActionTransformer.class);
    when(mockTransformer.getTypeNames()).thenReturn(Sets.newHashSet(TEST_TYPENAME));
    return mockTransformer;
  }

  private void assertOptionalIs(Optional op, CswActionTransformer transformer) {
    assertThat(op, notNullValue());
    assertThat(op.isPresent(), is(true));
    assertThat(op.get(), is(transformer));
  }

  private void assertOptionalIsEmpty(Optional op) {
    assertThat(op, notNullValue());
    assertThat(op.isPresent(), is(false));
  }
}
