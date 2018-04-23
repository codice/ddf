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

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class JsonModelBuilderTest {
  private static final String AND = "AND";

  private static final String OR = "OR";

  private static void setupDefaultTestValue(JsonModelBuilder builder) {
    builder
        .beginBinaryComparisonType("=")
        .setProperty("name")
        .setValue("value")
        .endTerminalType()
        .getResult();
  }

  /**
   * The below test names follow a simple convention: (builder method) + (validation method)
   *
   * <p>This way the test names clearly define a mapping between builder function and the
   * verification that occurs behind the scenes. Refer to the state checks in {@link
   * JsonModelBuilder}, their exception messages should also document their purpose.
   */
  public static class ValidateCorrectUse {
    private JsonModelBuilder builder;

    @Before
    public void setup() {
      builder = new JsonModelBuilder();
    }

    @Test(expected = IllegalStateException.class)
    public void testBeginBinaryLogicTypeCanModify() {
      setupDefaultTestValue(builder);
      builder.beginBinaryLogicType("OR");
    }

    @Test(expected = IllegalStateException.class)
    public void testBeginBinaryLogicTypeCanStartNew() {
      builder.beginBinaryComparisonType("=").beginBinaryLogicType("OR");
    }

    @Test(expected = IllegalStateException.class)
    public void testEndBinaryLogicTypeCanModify() {
      setupDefaultTestValue(builder);
      builder.endBinaryLogicType();
    }

    @Test(expected = IllegalStateException.class)
    public void testEndBinaryLogicTypeCanEnd() {
      builder.beginBinaryComparisonType("=").endBinaryLogicType();
    }

    @Test(expected = IllegalStateException.class)
    public void testEndBinaryLogicTypeCanReturn() {
      builder.endBinaryLogicType();
    }

    @Test(expected = IllegalStateException.class)
    public void testBeginBinaryComparisonTypeCanModify() {
      setupDefaultTestValue(builder);
      builder.beginBinaryComparisonType("=");
    }

    @Test(expected = IllegalStateException.class)
    public void testBeginBinaryComparisonTypeCanStartNew() {
      builder.beginBinaryComparisonType("=").beginBinaryComparisonType(">");
    }

    @Test(expected = IllegalStateException.class)
    public void testEndBinaryComparisonTypeCanModify() {
      setupDefaultTestValue(builder);
      builder.endTerminalType();
    }

    @Test(expected = IllegalStateException.class)
    public void testGetResultCanEnd() {
      builder.beginBinaryComparisonType("=").getResult();
    }

    @Test(expected = IllegalStateException.class)
    public void testGetResultCanReturn() {
      builder.getResult();
    }

    @Test(expected = IllegalStateException.class)
    public void testSetPropertyCanModify() {
      setupDefaultTestValue(builder);
      builder.setProperty("property");
    }

    @Test(expected = IllegalStateException.class)
    public void testSetPropertyCanSetField() {
      builder.setProperty("property");
    }

    @Test(expected = IllegalStateException.class)
    public void testSetValueCanModify() {
      setupDefaultTestValue(builder);
      builder.setValue("value");
    }

    @Test(expected = IllegalStateException.class)
    public void testSetValueCanSetField() {
      builder.setValue("value");
    }

    @Test(expected = IllegalStateException.class)
    public void testSetTemplatedValuesCanModify() {
      setupDefaultTestValue(builder);
      builder.setTemplatedValues("default", "id", true, false);
    }

    @Test(expected = IllegalStateException.class)
    public void testSetTemplatedValuesCanSetField() {
      builder.setTemplatedValues("default", "id", true, false);
    }
  }

  public static class ValidateExpectedResults {
    private JsonModelBuilder builder;

    @Before
    public void setup() {
      builder = new JsonModelBuilder();
    }

    @Test
    public void testBinaryComparisonType() {
      FilterNode node =
          builder
              .beginBinaryComparisonType("=")
              .setProperty("name")
              .setValue("value")
              .endTerminalType()
              .getResult();

      assertLeafNode(node, "=", "name", "value");
    }

    @Test
    public void testBinaryComparisonTypeTemplated() {
      FilterNode node =
          builder
              .beginBinaryComparisonType("=")
              .setProperty("name")
              .setTemplatedValues("5", "id", true, false)
              .endTerminalType()
              .getResult();

      assertTemplatedNode(node, "=", "name", "5", "id");
    }

    @Test
    public void testBinaryComparisonTypeAllOperators() {
      new JsonModelBuilder().beginBinaryComparisonType("=");
      new JsonModelBuilder().beginBinaryComparisonType(">");
      new JsonModelBuilder().beginBinaryComparisonType(">=");
      new JsonModelBuilder().beginBinaryComparisonType("<");
      new JsonModelBuilder().beginBinaryComparisonType("<=");
      new JsonModelBuilder().beginBinaryComparisonType("!=");
      // No IllegalArgumentException indicates a passing test
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBinaryComparisonTypeBadOperator() {
      builder.beginBinaryComparisonType("bad");
    }

    @Test
    public void testBinaryLogicTypeAnd() {
      FilterNode node =
          builder
              .beginBinaryLogicType(AND)
              .beginBinaryComparisonType("=")
              .setProperty("name")
              .setValue("value")
              .endTerminalType()
              .endBinaryLogicType()
              .getResult();

      assertParentNode(node, AND, 1);
    }

    @Test
    public void testBinaryLogicTypeAllOperators() {
      new JsonModelBuilder().beginBinaryLogicType(AND);
      new JsonModelBuilder().beginBinaryLogicType(OR);
      // No IllegalArgumentException indicates a passing test
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBinaryLogicTypeBadOperator() {
      new JsonModelBuilder().beginBinaryLogicType("bad");
    }
  }
}
