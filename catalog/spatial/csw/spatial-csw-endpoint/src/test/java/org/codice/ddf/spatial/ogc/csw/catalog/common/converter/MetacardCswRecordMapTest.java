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

import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.api.CswRecordMap;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.mappings.MetacardCswRecordMap;
import org.junit.Test;

public class MetacardCswRecordMapTest {

  CswRecordMap cswRecordMap = new MetacardCswRecordMap();

  @Test
  public void testNamespacePrefixedQueriesWithoutXpath() {
    String propertyNameWihoutXpath = "dc:title";
    String propertyNameWihoutNamespace = "title";

    assertThat(
        cswRecordMap.getDefaultMetacardFieldForPrefixedString(propertyNameWihoutXpath),
        is("title"));
    assertThat(
        cswRecordMap.getDefaultMetacardFieldForPrefixedString(propertyNameWihoutNamespace),
        is("title"));

    assertThat(
        cswRecordMap.hasDefaultMetacardFieldForPrefixedString(propertyNameWihoutXpath), is(true));
    assertThat(
        cswRecordMap.hasDefaultMetacardFieldForPrefixedString(propertyNameWihoutNamespace),
        is(true));
  }

  @Test
  public void testNamespacePrefixedQueriesWithXpath() {
    String propertyNameWithXpath = "/csw:Record/dc:title";
    String propertyNameXpathWithoutNamespace = "/Record/title";

    assertThat(
        cswRecordMap.getDefaultMetacardFieldForPrefixedString(propertyNameWithXpath),
        is("/csw:Record/dc:title"));
    assertThat(
        cswRecordMap.getDefaultMetacardFieldForPrefixedString(propertyNameXpathWithoutNamespace),
        is("/Record/title"));

    // Sortby does not support Xpath
    assertThat(
        cswRecordMap.hasDefaultMetacardFieldForPrefixedString(propertyNameWithXpath), is(false));
    assertThat(
        cswRecordMap.hasDefaultMetacardFieldForPrefixedString(propertyNameXpathWithoutNamespace),
        is(false));
  }
}
