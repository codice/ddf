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
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.codice.ddf.spatial.geocoding.Suggestion;
import org.codice.usng4j.CoordinateSystemTranslator;
import org.codice.usng4j.impl.CoordinateSystemTranslatorImpl;
import org.junit.Test;

public class UtmUpsCoordinateProcessorTest {

  private CoordinateSystemTranslator translator = new CoordinateSystemTranslatorImpl();

  private UtmUpsCoordinateProcessor processor = new UtmUpsCoordinateProcessor(translator);

  @Test
  public void testUtmStringNorthernHemisphere() {
    assertSuggestion(
        "17R 522908mE 2853543mN", "UTM/UPS: [ 17R 522908mE 2853543mN ]", 25.799891, -80.771484);
  }

  @Test
  public void testUtmStringNorthernHemisphereWithMultipleSpaces() {
    assertSuggestion(
        "17R   522908mE   2853543mN", "UTM/UPS: [ 17R 522908mE 2853543mN ]", 25.799891, -80.771484);
  }

  @Test
  public void testUtmStringNorthernHemisphereWithTabs() {
    assertSuggestionDoesNotExist(String.format("%s\t%s\t%s", "17R", "522908mE", "2853543mN"));
  }

  @Test
  public void testUtmStringSouthernHemisphere() {
    assertSuggestion(
        "13M 604276mE 9805713mN", "UTM/UPS: [ 13M 604276mE 9805713mN ]", -1.757537, -104.062500);
  }

  @Test
  public void testUtmStringZoneTooSmall() {
    assertSuggestionDoesNotExist("00N 234789mE 234789mN");
  }

  @Test
  public void testUtmStringZoneTooBig() {
    assertSuggestion(
        "61P 508378mE 967744mN", "UTM/UPS: [ 1P 508378mE 967744mN ]", 8.754795, -176.923828);
  }

  @Test
  public void testUtmStringBadLatBand() {
    assertSuggestionDoesNotExist("13I 234789mE 234789mN");
  }

  @Test
  public void testUtmStringWithoutLatBand() {
    List<Suggestion> list = new ArrayList<>();
    processor.enhanceResults(list, "13 234789mE 234789mN");
    assertThat(list, hasSize(2));

    assertSuggestion(
        (LiteralSuggestion) list.get(0),
        "UTM/UPS: [ 13 234789mE 234789mN N ]",
        2.122350,
        -107.384318);
    assertSuggestion(
        (LiteralSuggestion) list.get(1),
        "UTM/UPS: [ 13 234789mE 234789mN S ]",
        -86.716923,
        -164.055897);
  }

  @Test
  public void testUtmStringWithNegativeNorthing() {
    assertSuggestionDoesNotExist("13N 234789mE -234789mN");
  }

  @Test
  public void testUtmStringWithNLatitudeBand() {
    List<Suggestion> list = new ArrayList<>();
    processor.enhanceResults(list, "13N 234789mE 234789mN");
    assertThat(list, hasSize(2));

    assertSuggestion(
        (LiteralSuggestion) list.get(0),
        "UTM/UPS: [ 13N 234789mE 234789mN ]",
        2.122350,
        -107.384318);
    assertSuggestion(
        (LiteralSuggestion) list.get(1),
        "UTM/UPS: [ 13 234789mE 234789mN N ]",
        2.122350,
        -107.384318);
  }

  @Test
  public void testUtmStringWithSLatitudeBand() {
    List<Suggestion> list = new ArrayList<>();
    processor.enhanceResults(list, "19S 634900mE 4004219mN");
    assertThat(list, hasSize(2));

    assertSuggestion(
        (LiteralSuggestion) list.get(0),
        "UTM/UPS: [ 19S 634900mE 4004219mN ]",
        36.173357,
        -67.500000);
    assertSuggestion(
        (LiteralSuggestion) list.get(1),
        "UTM/UPS: [ 19 634900mE 4004219mN S ]",
        -54.092504,
        -66.937300);
  }

  @Test
  public void testUtmStringWithNorthernHemisphereIndicator() {
    assertSuggestion(
        "17R 522908mE 2853543mN N", "UTM/UPS: [ 17R 522908mE 2853543mN ]", 25.799891, -80.771484);
  }

  @Test
  public void testUtmStringWithSouthernHemisphereIndicator() {
    assertSuggestion(
        "17R 522908mE 2853543mN S", "UTM/UPS: [ 17R 522908mE 2853543mN ]", 25.799891, -80.771484);
  }

  @Test
  public void testUtmStringWithInvalidNSIndicator() {
    assertSuggestion(
        "17R 522908mE 2853543mN R", "UTM/UPS: [ 17R 522908mE 2853543mN ]", 25.799891, -80.771484);
  }

  @Test
  public void testUtmStringWithLowerCaseLatBand() {
    assertSuggestion(
        "17r 522908mE 2853543mN", "UTM/UPS: [ 17R 522908mE 2853543mN ]", 25.799891, -80.771484);
  }

  @Test
  public void testUpsStringWithInvalidLatBand() {
    assertSuggestionDoesNotExist("C 2347891mE 2347891mN");
  }

  @Test
  public void testUpsString() {
    assertSuggestion(
        "A 2347891mE 2347891mN", "UTM/UPS: [ A 2347891mE 2347891mN ]", -85.570691, 45.0);
  }

  @Test
  public void testUpsStringWithoutEastingUnits() {
    assertSuggestion("A 2347891 2347891mN", "UTM/UPS: [ A 2347891mE 2347891mN ]", -85.570691, 45.0);
  }

  @Test
  public void testUpsStringWithoutNorthingUnits() {
    assertSuggestion("A 2347891mE 2347891", "UTM/UPS: [ A 2347891mE 2347891mN ]", -85.570691, 45.0);
  }

  @Test
  public void testUpsStringWithRedundantZoneZero() {
    assertSuggestion(
        "0A 2347891mE 2347891mN", "UTM/UPS: [ A 2347891mE 2347891mN ]", -85.570691, 45.0);
  }

  @Test
  public void testUpsStringDisregardsNorthIndicator() {
    assertSuggestion(
        "A 2347891mE 2347891mN N", "UTM/UPS: [ A 2347891mE 2347891mN ]", -85.570691, 45.0);
  }

  @Test
  public void testUpsStringDisregardsSouthIndicator() {
    assertSuggestion(
        "A 2347891mE 2347891mN S", "UTM/UPS: [ A 2347891mE 2347891mN ]", -85.570691, 45.0);
  }

  @Test
  public void testUtmFollowedByUtm() {
    assertSuggestion(
        "13N 234789mE 234789mN 22X 234789mE 8592442mN",
        "UTM/UPS: [ 13N 234789mE 234789mN ] [ 22X 234789mE 8592442mN ]",
        2.122350,
        -107.384318,
        77.190666,
        -61.773164);
  }

  @Test
  public void testUtmWithoutUnitLabelFollowedByUtm() {
    assertSuggestion(
        "13N 234789mE 234789 22X 234789mE 8592442mN",
        "UTM/UPS: [ 13N 234789mE 234789mN ] [ 22X 234789mE 8592442mN ]",
        2.122350,
        -107.384318,
        77.190666,
        -61.773164);
  }

  @Test
  public void testUpsFollowedByUps() {
    assertSuggestion(
        "A 2347891mE 2347891mN B 1077891mE 1077891mN",
        "UTM/UPS: [ A 2347891mE 2347891mN ] [ B 1077891mE 1077891mN ]",
        -85.570691,
        45.0,
        -78.293449,
        -135.0);
  }

  @Test
  public void testUpsWithoutUnitLabelFollowedByUps() {
    assertSuggestion(
        "A 2347891mE 2347891 B 1077891mE 1077891mN",
        "UTM/UPS: [ A 2347891mE 2347891mN ] [ B 1077891mE 1077891mN ]",
        -85.570691,
        45.0,
        -78.293449,
        -135.0);
  }

  @Test
  public void testUtmFollowedByUps() {
    assertSuggestion(
        "13N 234789mE 234789mN B 1077891mE 1077891mN",
        "UTM/UPS: [ 13N 234789mE 234789mN ] [ B 1077891mE 1077891mN ]",
        2.122350,
        -107.384318,
        -78.293449,
        -135.0);
  }

  @Test
  public void testUtmWithoutUnitLabelFollowedByUps() {
    assertSuggestion(
        "13N 234789mE 234789 B 1077891mE 1077891mN",
        "UTM/UPS: [ 13N 234789mE 234789mN ] [ B 1077891mE 1077891mN ]",
        2.122350,
        -107.384318,
        -78.293449,
        -135.0);
  }

  @Test
  public void testUpsFollowedByUtm() {
    assertSuggestion(
        "B 1077891mE 1077891mN 13N 234789mE 234789mN",
        "UTM/UPS: [ B 1077891mE 1077891mN ] [ 13N 234789mE 234789mN ]",
        -78.293449,
        -135.0,
        2.122350,
        -107.384318);
  }

  @Test
  public void testUpsWithoutUnitLabelFollowedByUtm() {
    assertSuggestion(
        "B 1077891mE 1077891 13N 234789mE 234789mN",
        "UTM/UPS: [ B 1077891mE 1077891mN ] [ 13N 234789mE 234789mN ]",
        -78.293449,
        -135.0,
        2.122350,
        -107.384318);
  }

  private void assertSuggestionDoesNotExist(String query) {
    List<Suggestion> list = new LinkedList<>();
    processor.enhanceResults(list, query);
    assertThat(list, is(empty()));
  }

  private void assertSuggestion(String query, String expectedName, double... expectedLatLons) {
    List<Suggestion> list = new LinkedList<>();
    processor.enhanceResults(list, query);
    assertThat(list, hasSize(1));

    LiteralSuggestion literalSuggestion = (LiteralSuggestion) list.get(0);
    assertSuggestion(literalSuggestion, expectedName, expectedLatLons);
  }

  private void assertSuggestion(
      LiteralSuggestion actualSuggestion, String expectedName, double... expectedLatLons) {
    assertThat(actualSuggestion.getId(), is("LITERAL-UTM-UPS"));
    assertThat(actualSuggestion.getName(), is(expectedName));
    assertThat(actualSuggestion.hasGeo(), is(true));
    assertThat(expectedLatLons.length % 2, is(0));
    assertThat(expectedLatLons.length / 2, is(actualSuggestion.getGeo().size()));
    int i = 0;
    for (LatLon latLon : actualSuggestion.getGeo()) {
      assertThat(latLon.getLat(), is(closeTo(expectedLatLons[i], 0.00001)));
      assertThat(latLon.getLon(), is(closeTo(expectedLatLons[i + 1], 0.00001)));
      i += 2;
    }
  }
}
