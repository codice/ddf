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
package org.codice.ddf.catalog.ui.metacard.query.data.model;

import static ddf.catalog.data.Metacard.TITLE;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_CQL;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_FEDERATION;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_FILTER_TREE;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_SORTS;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_TYPE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.catalog.ui.metacard.QueryMetacardApplicationTest;
import org.junit.Test;

public class QueryBasicTest {

  private static final Gson GSON =
      new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

  @Test
  public void createQueryBasicFromJson() {
    String content = getFileContents("/queries/basic.json");
    QueryBasic query = GSON.fromJson(content, QueryBasic.class);

    Metacard metacard = query.getMetacard();

    assertAttribute(metacard, QUERY_CQL, "(\"anyText\" ILIKE 'foo bar')");
    assertAttribute(
        metacard,
        QUERY_FILTER_TREE,
        "{\"type\":\"AND\",\"filters\":[{\"type\":\"ILIKE\",\"property\":\"anyText\",\"value\":\"foo bar\"}]}");
    assertAttribute(metacard, QUERY_FEDERATION, "enterprise");

    Map<String, String> sorts = ImmutableMap.of("attribute", "modified", "direction", "descending");
    assertAttribute(metacard, QUERY_SORTS, sorts);

    assertAttribute(metacard, QUERY_TYPE, "advanced");
    assertAttribute(metacard, TITLE, "(\"anyText\" ILIKE 'foo bar')");
  }

  private void assertAttribute(Metacard metacard, String attrName, Object value) {
    Attribute attr = metacard.getAttribute(attrName);
    assertThat(attr.getValue(), is(value));
  }

  private static String getFileContents(String resource) {
    try (InputStream inputStream =
        QueryMetacardApplicationTest.class.getResourceAsStream(resource)) {
      return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    } catch (IOException e) {
      String message = String.format("Unable to find resource [%s]", resource);
      throw new AssertionError(message, e);
    }
  }
}
