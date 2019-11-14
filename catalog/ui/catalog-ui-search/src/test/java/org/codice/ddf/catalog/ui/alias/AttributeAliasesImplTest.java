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
package org.codice.ddf.catalog.ui.alias;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class AttributeAliasesImplTest {

  private static final List<String> ALIAS_CONFIG = ImmutableList.of("a=b", "c=d");

  private AttributeAliasesImpl attributeAliases;

  @Before
  public void setup() {
    attributeAliases = new AttributeAliasesImpl();
    attributeAliases.setAttributeAliases(ALIAS_CONFIG);
  }

  @Test
  public void testGetAlias() {
    assertThat(attributeAliases.getAlias("a"), is("b"));
  }

  @Test
  public void testHasAlias() {
    assertThat(attributeAliases.hasAlias("a"), is(true));
    assertThat(attributeAliases.hasAlias("z"), is(false));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetAliasMapIsImmutable() {
    Map<String, String> aliasMap = attributeAliases.getAliasMap();
    aliasMap.put("y", "z");
  }

  @Test
  public void testSetAttributeAliasesTrimsStrings() {
    List<String> untrimmedConfig = ImmutableList.of(" a = b ");
    attributeAliases.setAttributeAliases(untrimmedConfig);
    assertThat(attributeAliases.getAlias("a"), is("b"));
  }

  @Test
  public void testSetAttributeAliasesOnlySplitsOnFirstEqualsSign() {
    List<String> extraEqualsConfig = ImmutableList.of("a=b=c");
    attributeAliases.setAttributeAliases(extraEqualsConfig);
    assertThat(attributeAliases.getAlias("a"), is("b=c"));
  }

  @Test
  public void testSetAttributeAliasesSkipsConfigsWithoutAtLeastOneEquals() {
    List<String> noEqualsConfig = ImmutableList.of("noequals");
    attributeAliases.setAttributeAliases(noEqualsConfig);
    assertThat(attributeAliases.getAliasMap().size(), is(0));
  }
}
