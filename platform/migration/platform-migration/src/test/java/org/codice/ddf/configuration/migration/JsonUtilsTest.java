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
package org.codice.ddf.configuration.migration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codice.ddf.migration.MigrationException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class JsonUtilsTest {
  private static final Integer JSON_INT = 1;

  private static final Long JSON_LONG = Long.MAX_VALUE;

  private static final boolean JSON_BOOL = true;

  private static final String JSON_STRING = "Some String";

  private static final Map<String, Object> JSON_MAP =
      ImmutableMap.of(
          "int",
          JsonUtilsTest.JSON_INT,
          "long",
          JsonUtilsTest.JSON_LONG,
          "string",
          JsonUtilsTest.JSON_STRING,
          "bool",
          JsonUtilsTest.JSON_BOOL);

  private static final List<Object> JSON_LIST =
      ImmutableList.of(
          JsonUtilsTest.JSON_INT,
          JsonUtilsTest.JSON_LONG,
          JsonUtilsTest.JSON_STRING,
          JsonUtilsTest.JSON_BOOL,
          JsonUtilsTest.JSON_MAP);

  private static final Map<String, Object> JSON_DEEP_MAP =
      ImmutableMap.of("list", JsonUtilsTest.JSON_LIST, "map", JsonUtilsTest.JSON_MAP);

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testConvertToMap() throws Exception {
    Assert.assertThat(
        JsonUtils.convertToMap(JsonUtilsTest.JSON_MAP),
        Matchers.sameInstance(JsonUtilsTest.JSON_MAP));
  }

  @Test
  public void testConvertToMapWhenNull() throws Exception {
    Assert.assertThat(JsonUtils.convertToMap(null), Matchers.equalTo(new HashMap<>()));
  }

  @Test
  public void testConvertToMapWhenNotMap() throws Exception {
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("expecting a Json map"));

    JsonUtils.convertToMap(JsonUtilsTest.JSON_LIST);
  }

  @Test
  public void testGetMapFrom() throws Exception {
    Assert.assertThat(
        JsonUtils.getMapFrom(JsonUtilsTest.JSON_DEEP_MAP, "map", false),
        Matchers.equalTo(JsonUtilsTest.JSON_MAP));
  }

  @Test
  public void testGetMapFromWhenNotDefined() throws Exception {
    Assert.assertThat(
        JsonUtils.getMapFrom(JsonUtilsTest.JSON_DEEP_MAP, "map2", false),
        Matchers.equalTo(new HashMap<>()));
  }

  @Test
  public void testGetMapFromWithNullKey() throws Exception {
    Assert.assertThat(
        JsonUtils.getMapFrom(JsonUtilsTest.JSON_DEEP_MAP, null, false),
        Matchers.equalTo(new HashMap<>()));
  }

  @Test
  public void testGetMapFromWithNullMap() throws Exception {
    Assert.assertThat(
        JsonUtils.getMapFrom(JsonUtilsTest.JSON_DEEP_MAP, null, false),
        Matchers.equalTo(new HashMap<>()));
  }

  @Test
  public void testGetMapFromWhenValueIsNotMap() throws Exception {
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("[list] is not a Json map"));

    JsonUtils.getMapFrom(JsonUtilsTest.JSON_DEEP_MAP, "list", false);
  }

  @Test
  public void testGetListFrom() throws Exception {
    Assert.assertThat(
        JsonUtils.getListFrom(JsonUtilsTest.JSON_DEEP_MAP, "list"),
        Matchers.equalTo(JsonUtilsTest.JSON_LIST));
  }

  @Test
  public void testGetListFromWhenNotDefined() throws Exception {
    Assert.assertThat(
        JsonUtils.getListFrom(JsonUtilsTest.JSON_DEEP_MAP, "list2"),
        Matchers.equalTo(new ArrayList<>()));
  }

  @Test
  public void testGetListFromWithNullKey() throws Exception {
    Assert.assertThat(
        JsonUtils.getListFrom(JsonUtilsTest.JSON_DEEP_MAP, null),
        Matchers.equalTo(new ArrayList<>()));
  }

  @Test
  public void testGetListFromWithNullMap() throws Exception {
    Assert.assertThat(JsonUtils.getListFrom(null, "list"), Matchers.equalTo(new ArrayList<>()));
  }

  @Test
  public void testGetListFromWhenValueIsNotList() throws Exception {
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("[map] is not a Json list"));

    JsonUtils.getListFrom(JsonUtilsTest.JSON_DEEP_MAP, "map");
  }

  @Test
  public void testGetStringFromWhenRequired() throws Exception {
    Assert.assertThat(
        JsonUtils.getStringFrom(JsonUtilsTest.JSON_MAP, "string", true),
        Matchers.equalTo(JsonUtilsTest.JSON_STRING));
  }

  @Test
  public void testGetStringFromWhenNotRequired() throws Exception {
    Assert.assertThat(
        JsonUtils.getStringFrom(JsonUtilsTest.JSON_MAP, "string", false),
        Matchers.equalTo(JsonUtilsTest.JSON_STRING));
  }

  @Test
  public void testGetStringFromWhenRequiredAndNotDefined() throws Exception {
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("missing required [string2]"));

    Assert.assertThat(
        JsonUtils.getStringFrom(JsonUtilsTest.JSON_MAP, "string2", true),
        Matchers.equalTo(JsonUtilsTest.JSON_STRING));
  }

  @Test
  public void testGetStringFromWhenNotRequiredAndNotDefined() throws Exception {
    Assert.assertThat(
        JsonUtils.getStringFrom(JsonUtilsTest.JSON_MAP, "string2", false), Matchers.nullValue());
  }

  @Test
  public void testGetStringFromWhenRequiredAndWithNullKey() throws Exception {
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("missing required [null]"));

    Assert.assertThat(
        JsonUtils.getStringFrom(JsonUtilsTest.JSON_MAP, null, true),
        Matchers.equalTo(JsonUtilsTest.JSON_STRING));
  }

  @Test
  public void testGetStringFromWhenNotRequiredAndWithNullKey() throws Exception {
    Assert.assertThat(
        JsonUtils.getStringFrom(JsonUtilsTest.JSON_MAP, null, false), Matchers.nullValue());
  }

  @Test
  public void testGetStringFromWhenRequiredAndWithNullMap() throws Exception {
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("missing required [string]"));

    Assert.assertThat(
        JsonUtils.getStringFrom(null, "string", true), Matchers.equalTo(JsonUtilsTest.JSON_STRING));
  }

  @Test
  public void testGetStringFromWhenNotRequiredAndWithNullMap() throws Exception {
    Assert.assertThat(JsonUtils.getStringFrom(null, "string", false), Matchers.nullValue());
  }

  @Test
  public void testGetStringFromWhenRequiredAndValueIsNotString() throws Exception {
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("[int] is not a Json string"));

    JsonUtils.getStringFrom(JsonUtilsTest.JSON_MAP, "int", true);
  }

  @Test
  public void testGetStringFromWhenNotRequiredAndValueIsNotString() throws Exception {
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("[int] is not a Json string"));

    JsonUtils.getStringFrom(JsonUtilsTest.JSON_MAP, "int", false);
  }

  @Test
  public void testGetLongFromWhenRequired() throws Exception {
    Assert.assertThat(
        JsonUtils.getLongFrom(JsonUtilsTest.JSON_MAP, "long", true),
        Matchers.equalTo(JsonUtilsTest.JSON_LONG));
  }

  @Test
  public void testGetLongFromWhenNotRequired() throws Exception {
    Assert.assertThat(
        JsonUtils.getLongFrom(JsonUtilsTest.JSON_MAP, "long", false),
        Matchers.equalTo(JsonUtilsTest.JSON_LONG));
  }

  @Test
  public void testGetLongFromWhenRequiredAndNotDefined() throws Exception {
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("missing required [long2]"));

    Assert.assertThat(
        JsonUtils.getLongFrom(JsonUtilsTest.JSON_MAP, "long2", true),
        Matchers.equalTo(JsonUtilsTest.JSON_LONG));
  }

  @Test
  public void testGetLongFromWhenNotRequiredAndNotDefined() throws Exception {
    Assert.assertThat(
        JsonUtils.getLongFrom(JsonUtilsTest.JSON_MAP, "long2", false), Matchers.nullValue());
  }

  @Test
  public void testGetLongFromWhenRequiredAndWithNullKey() throws Exception {
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("missing required [null]"));

    Assert.assertThat(
        JsonUtils.getLongFrom(JsonUtilsTest.JSON_MAP, null, true),
        Matchers.equalTo(JsonUtilsTest.JSON_STRING));
  }

  @Test
  public void testGetLongFromWhenNotRequiredAndWithNullKey() throws Exception {
    Assert.assertThat(
        JsonUtils.getLongFrom(JsonUtilsTest.JSON_MAP, null, false), Matchers.nullValue());
  }

  @Test
  public void testGetLongFromWhenRequiredAndWithNullMap() throws Exception {
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("missing required [long]"));

    Assert.assertThat(
        JsonUtils.getLongFrom(null, "long", true), Matchers.equalTo(JsonUtilsTest.JSON_STRING));
  }

  @Test
  public void testGetLongFromWhenNotRequiredAndWithNullMap() throws Exception {
    Assert.assertThat(JsonUtils.getLongFrom(null, "long", false), Matchers.nullValue());
  }

  @Test
  public void testGetLongFromWhenRequiredAndValueIsNotNumber() throws Exception {
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("[bool] is not a Json number"));

    JsonUtils.getLongFrom(JsonUtilsTest.JSON_MAP, "bool", true);
  }

  @Test
  public void testGetLongFromWhenNotRequiredAndValueIsNotNumber() throws Exception {
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("[bool] is not a Json number"));

    JsonUtils.getLongFrom(JsonUtilsTest.JSON_MAP, "bool", false);
  }

  @Test
  public void testGetLongFromWhenRequiredAndValueIsInt() throws Exception {
    Assert.assertThat(
        JsonUtils.getLongFrom(JsonUtilsTest.JSON_MAP, "int", true),
        Matchers.equalTo(JsonUtilsTest.JSON_INT.longValue()));
  }

  @Test
  public void testGetLongFromWhenNotRequiredAndValueIsInt() throws Exception {
    Assert.assertThat(
        JsonUtils.getLongFrom(JsonUtilsTest.JSON_MAP, "int", false),
        Matchers.equalTo(JsonUtilsTest.JSON_INT.longValue()));
  }

  @Test
  public void testGetBooleanFromWhenRequired() throws Exception {
    Assert.assertThat(
        JsonUtils.getBooleanFrom(JsonUtilsTest.JSON_MAP, "bool", true),
        Matchers.equalTo(JsonUtilsTest.JSON_BOOL));
  }

  @Test
  public void testGetBooleanFromWhenNotRequired() throws Exception {
    Assert.assertThat(
        JsonUtils.getBooleanFrom(JsonUtilsTest.JSON_MAP, "bool", false),
        Matchers.equalTo(JsonUtilsTest.JSON_BOOL));
  }

  @Test
  public void testGetBooleanFromWhenRequiredAndNotDefined() throws Exception {
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("missing required [bool2]"));

    Assert.assertThat(
        JsonUtils.getBooleanFrom(JsonUtilsTest.JSON_MAP, "bool2", true),
        Matchers.equalTo(JsonUtilsTest.JSON_BOOL));
  }

  @Test
  public void testGetBooleanFromWhenNotRequiredAndNotDefined() throws Exception {
    Assert.assertThat(
        JsonUtils.getBooleanFrom(JsonUtilsTest.JSON_MAP, "bool2", false), Matchers.equalTo(false));
  }

  @Test
  public void testGetBooleanFromWhenRequiredAndWithNullKey() throws Exception {
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("missing required [null]"));

    Assert.assertThat(
        JsonUtils.getBooleanFrom(JsonUtilsTest.JSON_MAP, null, true),
        Matchers.equalTo(JsonUtilsTest.JSON_BOOL));
  }

  @Test
  public void testGetBooleanFromWhenNotRequiredAndWithNullKey() throws Exception {
    Assert.assertThat(
        JsonUtils.getBooleanFrom(JsonUtilsTest.JSON_MAP, null, false), Matchers.equalTo(false));
  }

  @Test
  public void testGetBooleanFromWhenRequiredAndWithNullMap() throws Exception {
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("missing required [bool]"));

    Assert.assertThat(
        JsonUtils.getBooleanFrom(null, "bool", true), Matchers.equalTo(JsonUtilsTest.JSON_BOOL));
  }

  @Test
  public void testGetBooleanFromWhenNotRequiredAndWithNullMap() throws Exception {
    Assert.assertThat(JsonUtils.getBooleanFrom(null, "bool", false), Matchers.equalTo(false));
  }

  @Test
  public void testGetBooleanFromWhenRequiredAndValueIsNotString() throws Exception {
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("[int] is not a Json boolean"));

    JsonUtils.getBooleanFrom(JsonUtilsTest.JSON_MAP, "int", true);
  }

  @Test
  public void testGetBooleanFromWhenNotRequiredAndValueIsNotString() throws Exception {
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("[int] is not a Json boolean"));

    JsonUtils.getBooleanFrom(JsonUtilsTest.JSON_MAP, "int", false);
  }
}
