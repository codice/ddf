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
package org.codice.ddf.spatial.ogc.csw.catalog.common.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Test;

public class DefaultCswRecordMapTest {

  @Test
  public void testNamespacePrefixedQueriesWithoutXpath() {
    String propertyNameWihoutXpath = "dc:title";
    String propertyNameWihoutNamespace = "title";

    assertThat(
        DefaultCswRecordMap.getDefaultMetacardFieldForPrefixedString(propertyNameWihoutXpath),
        is("title"));
    assertThat(
        DefaultCswRecordMap.getDefaultMetacardFieldForPrefixedString(propertyNameWihoutNamespace),
        is("title"));

    assertThat(
        DefaultCswRecordMap.hasDefaultMetacardFieldForPrefixedString(propertyNameWihoutXpath),
        is(true));
    assertThat(
        DefaultCswRecordMap.hasDefaultMetacardFieldForPrefixedString(propertyNameWihoutNamespace),
        is(true));
  }

  @Test
  public void testNamespacePrefixedQueriesWithXpath() {
    String propertyNameWithXpath = "/csw:Record/dc:title";
    String propertyNameXpathWithoutNamespace = "/Record/title";

    assertThat(
        DefaultCswRecordMap.getDefaultMetacardFieldForPrefixedString(propertyNameWithXpath),
        is("/csw:Record/dc:title"));
    assertThat(
        DefaultCswRecordMap.getDefaultMetacardFieldForPrefixedString(
            propertyNameXpathWithoutNamespace),
        is("/Record/title"));

    // Sortby does not support Xpath
    assertThat(
        DefaultCswRecordMap.hasDefaultMetacardFieldForPrefixedString(propertyNameWithXpath),
        is(false));
    assertThat(
        DefaultCswRecordMap.hasDefaultMetacardFieldForPrefixedString(
            propertyNameXpathWithoutNamespace),
        is(false));
  }
}
