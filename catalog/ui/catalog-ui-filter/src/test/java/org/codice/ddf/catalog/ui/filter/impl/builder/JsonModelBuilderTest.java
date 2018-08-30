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
package org.codice.ddf.catalog.ui.filter.impl.builder;

import static org.codice.ddf.catalog.ui.filter.FilterNodeAssertionSupport.assertFunctionNode;
import static org.codice.ddf.catalog.ui.filter.FilterNodeAssertionSupport.assertLeafNode;
import static org.codice.ddf.catalog.ui.filter.FilterNodeAssertionSupport.assertParentNode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class JsonModelBuilderTest {
  private static final String JSON_EQUAL = "=";

  private JsonModelBuilder builder;

  @Before
  public void setup() {
    builder = new JsonModelBuilder();
  }

  @Test
  public void testBinaryComparisonType() {
    Map<String, ?> node =
        builder.isEqualTo(false).property("name").value("value").end().getResult();
    assertLeafNode(node, JSON_EQUAL, "name", "value");
  }

  @Test
  public void testBinaryComparisonTypeTemplated() {
    Map<String, ?> node =
        builder
            .isEqualTo(false)
            .property("name")
            .function("template.value.v1")
            .value(5)
            .value("id")
            .value(true)
            .value(false)
            .end()
            .end()
            .getResult();

    assertLeafNode(
        node,
        "=",
        tar -> assertThat(tar, is("name")),
        tar ->
            assertFunctionNode(tar, "template.value.v1", ImmutableList.of(5, "id", true, false)));
  }

  @Test
  public void testBinaryComparisonTypeEmbeddedFunctions() {
    Map<String, ?> node =
        builder
            .isNotEqualTo(false)
            .property("name")
            .function("function.A")
            .value(1074882733456L)
            .value("id")
            .function("function.B")
            .value(true)
            .value("hello")
            .end()
            .value(false)
            .end()
            .end()
            .getResult();

    assertLeafNode(
        node,
        "!=",
        e1 -> assertThat(e1, is("name")),
        e2 ->
            assertFunctionNode(
                e2,
                "function.A",
                arg1 -> assertThat(arg1, is(1074882733456L)),
                arg2 -> assertThat(arg2, is("id")),
                arg3 -> assertFunctionNode(arg3, "function.B", ImmutableList.of(true, "hello")),
                arg4 -> assertThat(arg4, is(false))));
  }

  @Test
  public void testBinaryLogicTypeAnd() {
    Map<String, ?> node =
        builder
            .and()
            .or()
            .intersects()
            .property("geo")
            .value("WKT (10, 5)")
            .end()
            .like(false, "", "", "")
            .property("title")
            .value("*")
            .end()
            .end()
            .isEqualTo(false)
            .property("name")
            .value("value")
            .end()
            .end()
            .getResult();

    assertParentNode(node, "AND", 2);
    List<Object> filters = (List<Object>) node.get("filters");
    assertThat(filters, notNullValue());
    assertParentNode(filters.get(0), "OR", 2);
    assertLeafNode(filters.get(1), "=", "name", "value");
  }
}
