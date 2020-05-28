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
package org.codice.ddf.catalog.plugin.metacard;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.codice.ddf.catalog.plugin.metacard.util.KeyValueParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Assert that {@link MetacardCondition}s behave properly, and maintain the component separation so
 * that they can be mocked out in other tests.
 */
@RunWith(MockitoJUnitRunner.class)
public class MetacardConditionTest {

  private static final Map<String, Serializable> CRITERIA = ImmutableMap.of("name", "bob jones");

  private static final List<String> ATTRIBUTE_SETTERS = Lists.newArrayList();

  private static final Map<String, String> EXPECTED_MAP = Maps.newHashMap();

  @Mock private KeyValueParser mockParser;

  private MetacardCondition metacardCondition;

  @Test
  public void testAutoTrimmingBehavior() throws Exception {
    metacardCondition = new MetacardCondition("   key  ", " value");
    assertThat(metacardCondition.getCriteriaKey(), is("key"));
    assertThat(metacardCondition.getExpectedValue(), is("value"));
  }

  @Test
  public void testMetacardConditionCriteriaWithoutKey() throws Exception {
    metacardCondition = new MetacardCondition("address", "555 Riverside Road");
    assertThat(metacardCondition.applies(CRITERIA), is(false));
  }

  @Test
  public void testMetacardConditionEqualityFailure() throws Exception {
    metacardCondition = new MetacardCondition("name", "bob saget");
    assertThat(metacardCondition.applies(CRITERIA), is(false));
  }

  @Test
  public void testMetacardConditionSuccessful() throws Exception {
    metacardCondition = new MetacardCondition("name", "bob jones");
    assertThat(metacardCondition.applies(CRITERIA), is(true));
  }

  @Test
  public void testParserGetsCalledAndCachesMap() {
    when(mockParser.parsePairsToMap(ATTRIBUTE_SETTERS)).thenReturn(EXPECTED_MAP);
    metacardCondition = new MetacardCondition("name", "bob jones", ATTRIBUTE_SETTERS, mockParser);
    verify(mockParser).parsePairsToMap(ATTRIBUTE_SETTERS);
    assertThat(metacardCondition.getParsedAttributes(), is(EXPECTED_MAP));
  }
}
