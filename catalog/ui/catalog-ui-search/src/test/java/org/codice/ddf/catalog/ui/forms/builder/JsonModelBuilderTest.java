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
package org.codice.ddf.catalog.ui.forms.builder;

import static org.codice.ddf.catalog.ui.forms.FilterNodeAssertionSupport.assertLeafNode;
import static org.codice.ddf.catalog.ui.forms.FilterNodeAssertionSupport.assertParentNode;
import static org.codice.ddf.catalog.ui.forms.FilterNodeAssertionSupport.assertTemplatedNode;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import ddf.catalog.data.AttributeRegistry;
import java.util.Optional;
import org.codice.ddf.catalog.ui.forms.api.FilterNode;
import org.junit.Before;
import org.junit.Test;

public class JsonModelBuilderTest {
  private static final String AND = "And";

  private static final String OR = "Or";

  private static final String JSON_EQUAL = "=";

  private static final String XML_EQUAL = "PropertyIsEqualTo";

  private JsonModelBuilder builder;

  @Before
  public void setup() {
    AttributeRegistry registry = mock(AttributeRegistry.class);
    when(registry.lookup(any())).thenReturn(Optional.empty());
    builder = new JsonModelBuilder(registry);
  }

  @Test
  public void testBinaryComparisonType() {
    FilterNode node =
        builder
            .beginBinaryComparisonType(XML_EQUAL)
            .setProperty("name")
            .setValue("value")
            .endTerminalType()
            .getResult();

    assertLeafNode(node, JSON_EQUAL, "name", "value", null);
  }

  @Test
  public void testBinaryComparisonTypeTemplated() {
    FilterNode node =
        builder
            .beginBinaryComparisonType(XML_EQUAL)
            .setProperty("name")
            .setTemplatedValues(
                ImmutableMap.of(
                    "defaultValue", "5", "nodeId", "id", "isVisible", true, "isReadOnly", false))
            .endTerminalType()
            .getResult();

    assertTemplatedNode(node, JSON_EQUAL, "name", "5", "id");
  }

  @Test
  public void testBinarySpatialTypeDWithin() {
    FilterNode node =
        builder
            .beginBinarySpatialType("DWITHIN")
            .setProperty("name")
            .setValue("value")
            .setDistance(10.5)
            .endTerminalType()
            .getResult();

    assertLeafNode(node, "DWITHIN", "name", "value", 10.5);
  }

  @Test
  public void testBinaryLogicTypeAnd() {
    FilterNode node =
        builder
            .beginBinaryLogicType(AND)
            .beginBinaryComparisonType(XML_EQUAL)
            .setProperty("name")
            .setValue("value")
            .endTerminalType()
            .endBinaryLogicType()
            .getResult();

    assertParentNode(node, "AND", 1);
  }

  @Test
  public void testBinaryLogicTypeAllOperators() {
    new JsonModelBuilder(null).beginBinaryLogicType(AND);
    new JsonModelBuilder(null).beginBinaryLogicType(OR);
    // No IllegalArgumentException indicates a passing test
  }

  @Test
  public void testBinaryComparisonTypeAllOperators() {
    new JsonModelBuilder(null).beginBinaryComparisonType("PropertyIsEqualTo");
    new JsonModelBuilder(null).beginBinaryComparisonType("PropertyIsGreaterThan");
    new JsonModelBuilder(null).beginBinaryComparisonType("PropertyIsGreaterThanOrEqualTo");
    new JsonModelBuilder(null).beginBinaryComparisonType("PropertyIsLessThan");
    new JsonModelBuilder(null).beginBinaryComparisonType("PropertyIsLessThanOrEqualTo");
    new JsonModelBuilder(null).beginBinaryComparisonType("PropertyIsNotEqualTo");
    // No IllegalArgumentException indicates a passing test
  }
}
