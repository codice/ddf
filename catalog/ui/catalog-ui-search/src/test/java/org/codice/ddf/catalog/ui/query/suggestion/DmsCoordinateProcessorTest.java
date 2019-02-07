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
package org.codice.ddf.catalog.ui.query.suggestion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import com.google.common.collect.ImmutableList;
import java.util.LinkedList;
import java.util.List;
import org.codice.ddf.spatial.geocoding.Suggestion;
import org.junit.Before;
import org.junit.Test;

public class DmsCoordinateProcessorTest {
  private CoordinateSystemTranslator translatorMock;

  private DmsCoordinateProcessor processor;

  @Before
  public void setup() {
    processor = new DmsCoordinateProcessor();
  }

  @Test
  public void testDmsStringSingleCoordinate() {
    assertSuggestion(
        "28°56\'26\"N 117°38\'11\"W",
        "DMS: [\"28°56'26\"N 117°38'11\"W\"",
        ImmutableList.of(new LatLon(1.0, 2.0)));
  }

  @Test
  public void testDmsStringOnlyNumbers() {
    assertSuggestion(
        "28 56 26 117 38 11\"W",
        "DMS: [\"28°56'26\"N 117°38'11\"W\"",
        ImmutableList.of(new LatLon(1.0, 2.0)));
  }

  private void assertSuggestionDoesNotExist(String query) {
    List<Suggestion> list = new LinkedList<>();
    processor.enhanceResults(list, query);
    assertThat(list, is(empty()));
  }

  private void assertSuggestion(String query, String expectedName, List<LatLon> expectedGeo) {
    List<Suggestion> list = new LinkedList<>();
    processor.enhanceResults(list, query);
    assertThat(list, is(not(empty())));
    assertThat(list, hasSize(1));

    LiteralSuggestion literalSuggestion = (LiteralSuggestion) list.get(0);
    assertThat(literalSuggestion.getId(), is("LITERAL-UTM-UPS"));
    assertThat(literalSuggestion.getName(), is(expectedName));
    assertThat(literalSuggestion.hasGeo(), is(true));
    assertThat(literalSuggestion.getGeo(), is(equalTo(expectedGeo)));
  }
}
