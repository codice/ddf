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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableList;
import java.util.LinkedList;
import java.util.List;
import org.codice.ddf.spatial.geocoding.Suggestion;
import org.codice.usng4j.CoordinateSystemTranslator;
import org.codice.usng4j.DecimalDegreesCoordinate;
import org.codice.usng4j.UtmUpsCoordinate;
import org.junit.Before;
import org.junit.Test;

public class UtmUpsCoordinateProcessorTest {
  private CoordinateSystemTranslator translatorMock;

  private UtmUpsCoordinateProcessor processor;

  @Before
  public void setup() {
    translatorMock = mock(CoordinateSystemTranslator.class);
    processor = new UtmUpsCoordinateProcessor(translatorMock);
  }

  @Test
  public void testUtmStringNorthernHemisphere() throws Exception {
    setupMocks("13N 234789mE 234789mN", 1.0, 2.0);
    assertSuggestion(
        "13N 234789mE 234789mN",
        "UTM/UPS: [ 13N 234789mE 234789mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0)));
  }

  @Test
  public void testUtmStringNorthernHemisphereWithMultipleSpaces() throws Exception {
    setupMocks("13N   234789mE   234789mN", 1.0, 2.0);
    assertSuggestion(
        "13N   234789mE   234789mN",
        "UTM/UPS: [ 13N   234789mE   234789mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0)));
  }

  @Test
  public void testUtmStringNorthernHemisphereWithTabs() {
    assertSuggestionDoesNotExist(String.format("%s\t%s\t%s", "13N", "234789mE", "234789mN"));
  }

  @Test
  public void testUtmStringSouthernHemisphere() throws Exception {
    setupMocks("13M 234789mE 234789mN", 1.0, 2.0);
    assertSuggestion(
        "13M 234789mE 234789mN",
        "UTM/UPS: [ 13M 234789mE 234789mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0)));
  }

  @Test
  public void testUtmStringZoneTooSmall() {
    assertSuggestionDoesNotExist("00N 234789mE 234789mN");
  }

  @Test
  public void testUtmStringZoneTooBig() throws Exception {
    setupMocks("1N 234789mE 234789mN", 1.0, 2.0);
    assertSuggestion(
        "61N 234789mE 234789mN",
        "UTM/UPS: [ 1N 234789mE 234789mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0)));
  }

  @Test
  public void testUtmStringBadLatBand() {
    assertSuggestionDoesNotExist("13I 234789mE 234789mN");
  }

  @Test
  public void testUtmStringWithoutLatBand() throws Exception {
    setupMocks("13N 234789mE 234789mN", 1.0, 2.0);
    assertSuggestion(
        "13 234789mE 234789mN",
        "UTM/UPS: [ 13N 234789mE 234789mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0)));
  }

  @Test
  public void testUtmStringWithNegativeNorthing() {
    assertSuggestionDoesNotExist("13N 234789mE -234789mN");
  }

  @Test
  public void testUtmStringWithNorthernHemisphereIndicator() throws Exception {
    setupMocks("13N 234789mE 234789mN", 1.0, 2.0);
    assertSuggestion(
        "13N 234789mE 234789mN N",
        "UTM/UPS: [ 13N 234789mE 234789mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0)));
  }

  @Test
  public void testUtmStringWithSouthernHemisphereIndicator() throws Exception {
    setupMocks("13N 234789mE 234789mN", 1.0, 2.0);
    assertSuggestion(
        "13N 234789mE 234789mN S",
        "UTM/UPS: [ 13N 234789mE 234789mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0)));
  }

  @Test
  public void testUtmStringWithInvalidNSIndicator() throws Exception {
    setupMocks("13N 234789mE 234789mN", 1.0, 2.0);
    assertSuggestion(
        "13N 234789mE 234789mN R",
        "UTM/UPS: [ 13N 234789mE 234789mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0)));
  }

  @Test
  public void testUtmStringWithLowerCaseLatBand() throws Exception {
    setupMocks("13M 234789mE 234789mN", 1.0, 2.0);
    assertSuggestion(
        "13m 234789mE 234789mN",
        "UTM/UPS: [ 13M 234789mE 234789mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0)));
  }

  @Test
  public void testUpsStringWithInvalidLatBand() {
    assertSuggestionDoesNotExist("C 2347891mE 2347891mN");
  }

  @Test
  public void testUpsString() throws Exception {
    setupMocks("A 2347891mE 2347891mN", 1.0, 2.0);
    assertSuggestion(
        "A 2347891mE 2347891mN",
        "UTM/UPS: [ A 2347891mE 2347891mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0)));
  }

  @Test
  public void testUpsStringWithoutEastingUnits() throws Exception {
    setupMocks("A 2347891 2347891mN", 1.0, 2.0);
    assertSuggestion(
        "A 2347891 2347891mN",
        "UTM/UPS: [ A 2347891 2347891mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0)));
  }

  @Test
  public void testUpsStringWithoutNorthingUnits() throws Exception {
    setupMocks("A 2347891mE 2347891", 1.0, 2.0);
    assertSuggestion(
        "A 2347891mE 2347891",
        "UTM/UPS: [ A 2347891mE 2347891 ]",
        ImmutableList.of(new LatLon(1.0, 2.0)));
  }

  @Test
  public void testUpsStringWithRedundantZoneZero() throws Exception {
    setupMocks("0A 2347891mE 2347891mN", 1.0, 2.0);
    assertSuggestion(
        "0A 2347891mE 2347891mN",
        "UTM/UPS: [ 0A 2347891mE 2347891mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0)));
  }

  @Test
  public void testUpsStringDisregardsNorthIndicator() throws Exception {
    setupMocks("A 2347891mE 2347891mN", 1.0, 2.0);
    assertSuggestion(
        "A 2347891mE 2347891mN N",
        "UTM/UPS: [ A 2347891mE 2347891mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0)));
  }

  @Test
  public void testUpsStringDisregardsSouthIndicator() throws Exception {
    setupMocks("A 2347891mE 2347891mN", 1.0, 2.0);
    assertSuggestion(
        "A 2347891mE 2347891mN S",
        "UTM/UPS: [ A 2347891mE 2347891mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0)));
  }

  @Test
  public void testUtmFollowedByUtm() throws Exception {
    setupMocks("13N 234789mE 234789mN", 1.0, 2.0);
    setupMocks("22X 234789mE 234789mN", 3.0, 4.0);
    assertSuggestion(
        "13N 234789mE 234789mN 22X 234789mE 234789mN",
        "UTM/UPS: [ 13N 234789mE 234789mN ] [ 22X 234789mE 234789mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0), new LatLon(3.0, 4.0)));
  }

  @Test
  public void testUtmWithoutUnitLabelFollowedByUtm() throws Exception {
    setupMocks("13N 234789mE 234789", 1.0, 2.0);
    setupMocks("22X 234789mE 234789mN", 3.0, 4.0);
    assertSuggestion(
        "13N 234789mE 234789 22X 234789mE 234789mN",
        "UTM/UPS: [ 13N 234789mE 234789 ] [ 22X 234789mE 234789mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0), new LatLon(3.0, 4.0)));
  }

  @Test
  public void testUpsFollowedByUps() throws Exception {
    setupMocks("A 2347891mE 2347891mN", 1.0, 2.0);
    setupMocks("B 1077891mE 1077891mN", 3.0, 4.0);
    assertSuggestion(
        "A 2347891mE 2347891mN B 1077891mE 1077891mN",
        "UTM/UPS: [ A 2347891mE 2347891mN ] [ B 1077891mE 1077891mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0), new LatLon(3.0, 4.0)));
  }

  @Test
  public void testUpsWithoutUnitLabelFollowedByUps() throws Exception {
    setupMocks("A 2347891mE 2347891", 1.0, 2.0);
    setupMocks("B 1077891mE 1077891mN", 3.0, 4.0);
    assertSuggestion(
        "A 2347891mE 2347891 B 1077891mE 1077891mN",
        "UTM/UPS: [ A 2347891mE 2347891 ] [ B 1077891mE 1077891mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0), new LatLon(3.0, 4.0)));
  }

  @Test
  public void testUtmFollowedByUps() throws Exception {
    setupMocks("13N 234789mE 234789mN", 1.0, 2.0);
    setupMocks("B 1077891mE 1077891mN", 3.0, 4.0);
    assertSuggestion(
        "13N 234789mE 234789mN B 1077891mE 1077891mN",
        "UTM/UPS: [ 13N 234789mE 234789mN ] [ B 1077891mE 1077891mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0), new LatLon(3.0, 4.0)));
  }

  @Test
  public void testUtmWithoutUnitLabelFollowedByUps() throws Exception {
    setupMocks("13N 234789mE 234789", 1.0, 2.0);
    setupMocks("B 1077891mE 1077891mN", 3.0, 4.0);
    assertSuggestion(
        "13N 234789mE 234789 B 1077891mE 1077891mN",
        "UTM/UPS: [ 13N 234789mE 234789 ] [ B 1077891mE 1077891mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0), new LatLon(3.0, 4.0)));
  }

  @Test
  public void testUpsFollowedByUtm() throws Exception {
    setupMocks("B 1077891mE 1077891mN", 1.0, 2.0);
    setupMocks("13N 234789mE 234789mN", 3.0, 4.0);
    assertSuggestion(
        "B 1077891mE 1077891mN 13N 234789mE 234789mN",
        "UTM/UPS: [ B 1077891mE 1077891mN ] [ 13N 234789mE 234789mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0), new LatLon(3.0, 4.0)));
  }

  @Test
  public void testUpsWithoutUnitLabelFollowedByUtm() throws Exception {
    setupMocks("B 1077891mE 1077891", 1.0, 2.0);
    setupMocks("13N 234789mE 234789mN", 3.0, 4.0);
    assertSuggestion(
        "B 1077891mE 1077891 13N 234789mE 234789mN",
        "UTM/UPS: [ B 1077891mE 1077891 ] [ 13N 234789mE 234789mN ]",
        ImmutableList.of(new LatLon(1.0, 2.0), new LatLon(3.0, 4.0)));
  }

  private void setupMocks(String expectedCoordString, Double expectedLat, Double expectedLon)
      throws Exception {
    UtmUpsCoordinate mockUtmUps = mock(UtmUpsCoordinate.class);
    doReturn(mockUtmUps).when(translatorMock).parseUtmUpsString(expectedCoordString);
    doReturn(expectedCoordString).when(mockUtmUps).toString();

    DecimalDegreesCoordinate ddc = mock(DecimalDegreesCoordinate.class);
    doReturn(expectedLat).when(ddc).getLat();
    doReturn(expectedLon).when(ddc).getLon();
    doReturn(ddc).when(translatorMock).toLatLon(mockUtmUps);
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
