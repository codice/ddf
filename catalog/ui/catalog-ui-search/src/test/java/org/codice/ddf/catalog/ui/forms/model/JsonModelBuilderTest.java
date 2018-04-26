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

import static org.codice.ddf.catalog.ui.forms.model.FilterNodeAssertionSupport.assertLeafNode;
import static org.codice.ddf.catalog.ui.forms.model.FilterNodeAssertionSupport.assertParentNode;
import static org.codice.ddf.catalog.ui.forms.model.FilterNodeAssertionSupport.assertTemplatedNode;

import com.google.common.collect.ImmutableMap;
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
    builder = new JsonModelBuilder();
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

    assertLeafNode(node, JSON_EQUAL, "name", "value");
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
    new JsonModelBuilder().beginBinaryLogicType(AND);
    new JsonModelBuilder().beginBinaryLogicType(OR);
    // No IllegalArgumentException indicates a passing test
  }

  @Test
  public void testBinaryComparisonTypeAllOperators() {
    new JsonModelBuilder().beginBinaryComparisonType("PropertyIsEqualTo");
    new JsonModelBuilder().beginBinaryComparisonType("PropertyIsGreaterThan");
    new JsonModelBuilder().beginBinaryComparisonType("PropertyIsGreaterThanOrEqualTo");
    new JsonModelBuilder().beginBinaryComparisonType("PropertyIsLessThan");
    new JsonModelBuilder().beginBinaryComparisonType("PropertyIsLessThanOrEqualTo");
    new JsonModelBuilder().beginBinaryComparisonType("PropertyIsNotEqualTo");
    // No IllegalArgumentException indicates a passing test
  }
}
