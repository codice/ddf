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
package org.codice.ddf.catalog.plugin.metacard.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests the behavior of {@link KeyValueParser}. */
@RunWith(JUnit4.class)
public class KeyValueParserTest {

  private static final List<String> TEST_CASES =
      Arrays.asList(
          "name=factoryName",
          null,
          "ser=vice=3.2.1",
          "whitespace=   ",
          "   =5.4.56",
          "description = hello there",
          "=",
          "",
          " ",
          " = ",
          null,
          "etc=/hosts/be=gin",
          "etc = /hosts/stuff",
          "======",
          "= = = =    ",
          "    name = value = stuff = hello");

  private static final Map.Entry EXPECTED_MAP_ENTRY_1 =
      new AbstractMap.SimpleEntry<>("name", "factoryName");

  private static final Map.Entry EXPECTED_MAP_ENTRY_2 =
      new AbstractMap.SimpleEntry<>("description", "hello there");

  private static final Map.Entry EXPECTED_MAP_ENTRY_3 =
      new AbstractMap.SimpleEntry<>("etc", "/hosts/stuff");

  private static final int EXPECTED_MAP_SIZE = 3;

  private KeyValueParser parser;

  @Before
  public void setup() throws Exception {
    parser = new KeyValueParser();
  }

  @Test
  public void testCorrectKeyValue() throws Exception {
    assertThat(parser.validatePair("key=value"), is(true));
  }

  @Test
  public void testCorrectKeyValueWithWhiteSpace() throws Exception {
    assertThat(parser.validatePair(" key = value "), is(true));
  }

  @Test
  public void testBadKeyValueWithNoKey() throws Exception {
    assertThat(parser.validatePair(" = value "), is(false));
  }

  @Test
  public void testBadKeyValueWithNoValue() throws Exception {
    assertThat(parser.validatePair(" key = "), is(false));
  }

  @Test
  public void testBadKeyValueDoubleEquals() throws Exception {
    assertThat(parser.validatePair(" key == value "), is(false));
  }

  @Test
  public void testBadKeyValueExtraEqualsWithKey() throws Exception {
    assertThat(parser.validatePair(" k=ey = value "), is(false));
  }

  @Test
  public void testBadKeyValueExtraEqualsWithValue() throws Exception {
    assertThat(parser.validatePair(" key = val=ue "), is(false));
  }

  @Test
  public void testCreateMap() throws Exception {
    runMapTest(parser);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateMapNullList() throws Exception {
    KeyValueParser parser = new KeyValueParser();
    parser.parsePairsToMap(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateMapFailFast() throws Exception {
    runMapTest(new KeyValueParser(true));
  }

  private void runMapTest(KeyValueParser givenParser) {
    Map<String, String> pairs = givenParser.parsePairsToMap(TEST_CASES);
    assertThat(pairs.values(), hasSize(EXPECTED_MAP_SIZE));
    assertThat(pairs.get(EXPECTED_MAP_ENTRY_1.getKey()), is(EXPECTED_MAP_ENTRY_1.getValue()));
    assertThat(pairs.get(EXPECTED_MAP_ENTRY_2.getKey()), is(EXPECTED_MAP_ENTRY_2.getValue()));
    assertThat(pairs.get(EXPECTED_MAP_ENTRY_3.getKey()), is(EXPECTED_MAP_ENTRY_3.getValue()));
  }
}
