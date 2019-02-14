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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.codice.ddf.spatial.geocoding.Suggestion;
import org.junit.Test;

public class DmsCoordinateProcessorTest {
  DmsCoordinateProcessor processor = new DmsCoordinateProcessor();

  @Test
  public void testDmsStringSingleCoordinate() {
    assertSuggestion(
        "28°56\'26\"N 117°38\'11\"W",
        "DMS: [ 28°56'26\"N 117°38'11\"W ]",
        ImmutableList.of(new LatLon(28.940555, -117.636388)));
  }

  @Test
  public void testDmsStringPartialSeconds() {
    assertSuggestion(
        "28°56\'26.012\"N 117°38\'11.933\"W",
        "DMS: [ 28°56'26.012\"N 117°38'11.933\"W ]",
        ImmutableList.of(new LatLon(28.940559, -117.636648)));
  }

  @Test
  public void testDmsStringLongPartialSeconds() {
    assertSuggestion(
        "28°56\'26.012222222222222222222222222222222222222222222222222222222\"N 117°38\'11.92222222222222222222222222222222222222222222222222222222\"W",
        "DMS: [ 28°56'26.012\"N 117°38'11.922\"W ]",
        ImmutableList.of(new LatLon(28.940559, -117.636648)));
  }

  @Test
  public void testDmsStringUnnecessaryDecimalOmitted() {
    assertSuggestion(
        "28°56\'26.000000000000000000000\"N 117°38\'11.\"W",
        "DMS: [ 28°56'26\"N 117°38'11\"W ]",
        ImmutableList.of(new LatLon(28.940556, -117.6363889)));
  }

  @Test
  public void testDmsStringExtraneousSymbols() {
    assertSuggestion(
        "*28^&*56(*&26\\N 117°38s11:OW",
        "DMS: [ 28°56'26\"N 117°38'11\"W ]",
        ImmutableList.of(new LatLon(28.940555, -117.636388)));
  }

  @Test
  public void testDmsStringNumbersPreceding() {
    assertSuggestion(
        "44,44.28°56\'26\"N 117°38\'11\"W",
        "DMS: [ 28°56'26\"N 117°38'11\"W ]",
        ImmutableList.of(new LatLon(28.940555, -117.636388)));
  }

  @Test
  public void testDmsStringNoSymbolBetweenSecondsAndDirection() {
    assertSuggestion(
        "28°56\'26N 117°38\'11W",
        "DMS: [ 28°56'26\"N 117°38'11\"W ]",
        ImmutableList.of(new LatLon(28.940555, -117.636388)));
  }

  @Test
  public void testDmsStringNumbersFollowing() {
    assertSuggestion(
        "28°56\'26\"N 117°38\'11\"W44",
        "DMS: [ 28°56'26\"N 117°38'11\"W ]",
        ImmutableList.of(new LatLon(28.940555, -117.636388)));
  }

  @Test
  public void testDmsStringIgnoresNumbersAndSymbolsPrecedingLonDegrees() {
    assertSuggestion(
        "28°56\'26\"N 34.117°38\'11\"W44",
        "DMS: [ 28°56'26\"N 117°38'11\"W ]",
        ImmutableList.of(new LatLon(28.940555, -117.636388)));
  }

  @Test
  public void testDmsStringOnlyNumbers() {
    assertSuggestion(
        "28 56 26 N 117 38 11 W",
        "DMS: [ 28°56'26\"N 117°38'11\"W ]",
        ImmutableList.of(new LatLon(28.940555, -117.636388)));
  }

  @Test
  public void testDmsStringAtLatBoundary() {
    assertSuggestion(
        "90°00\'00\"N 117°38\'11\"W",
        "DMS: [ 90°00'00\"N 117°38'11\"W ]",
        ImmutableList.of(new LatLon(90.0, -117.636388)));
  }

  @Test
  public void testDmsStringAtLonBoundary() {
    assertSuggestion(
        "28°56\'26\"N 180°00\'00\"W",
        "DMS: [ 28°56'26\"N 180°00'00\"W ]",
        ImmutableList.of(new LatLon(28.940555, -180.0)));
  }

  @Test
  public void testDmsStringAllZeros() {
    assertSuggestion(
        "0°0\'0\"S 0°0\'0\"E",
        "DMS: [ 00°00'00\"S 000°00'00\"E ]",
        ImmutableList.of(new LatLon(0.0, 0.0)));
  }

  @Test
  public void testDmsStringNoDirection() {
    assertSuggestionDoesNotExist("28°56\'26\" 117°38\'11\"");
  }

  @Test
  public void testDmsStringDoesNotMatchOnPartOfLatDegrees() {
    assertSuggestionDoesNotExist("140°56\'26\"N 117°38\'11\"W");
  }

  @Test
  public void testDmsStringDoesNoSeparationBetweenDegreesMinutesSeconds() {
    assertSuggestionDoesNotExist("405626\"N 1173811\"W");
  }

  @Test
  public void testDmsStringLatDegreesMissing() {
    assertSuggestionDoesNotExist("56\'26\"N 117°38\'11\"W");
  }

  @Test
  public void testDmsStringLatDegreesTooLarge() {
    assertSuggestionDoesNotExist("91°56\'26\"N 117°38\'11\"W");
  }

  @Test
  public void testDmsStringLonDegreesTooLarge() {
    assertSuggestionDoesNotExist("28°56\'26\"N 181°38\'11\"W");
  }

  @Test
  public void testDmsStringLatMinutesTooLarge() {
    assertSuggestionDoesNotExist("28°60\'26\"N 117°38\'11\"W");
  }

  @Test
  public void testDmsStringLonMinutesTooLarge() {
    assertSuggestionDoesNotExist("28°56\'26\"N 117°60\'11\"W");
  }

  @Test
  public void testDmsStringLatSecondsTooLarge() {
    assertSuggestionDoesNotExist("28°56\'60\"N 117°38\'11\"W");
  }

  @Test
  public void testDmsStringLonSecondsTooLarge() {
    assertSuggestionDoesNotExist("28°56\'26\"N 117°38\'60\"W");
  }

  @Test
  public void testDmsStringConvertedLatTooLarge() {
    assertSuggestionDoesNotExist("90°00\'01\"N 117°38\'11\"W");
  }

  @Test
  public void testDmsStringConvertedLonTooLarge() {
    assertSuggestionDoesNotExist("28°56\'26\"N 180°00\'01\"W");
  }

  @Test
  public void testDmsStringMultipleCoordinates() {
    assertSuggestion(
        "28°56\'26\"S 117°38\'11.64564\"E28;56;26S 117°38\'11\"QQW%WWEQW@!!!!!! 28°56\'26\"N 117°38\'11\"E",
        "DMS: [ 28°56'26\"S 117°38'11.646\"E ] [ 28°56'26\"S 117°38'11\"W ] [ 28°56'26\"N 117°38'11\"E ]",
        ImmutableList.of(
            new LatLon(-28.940555, 117.636568),
            new LatLon(-28.940555, -117.636388),
            new LatLon(28.940555, 117.636388)));
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
    assertThat(literalSuggestion.getId(), is("LITERAL-DMS"));
    assertThat(literalSuggestion.getName(), is(expectedName));
    assertThat(literalSuggestion.hasGeo(), is(true));

    List<LatLon> actualGeo = literalSuggestion.getGeo();
    List<LatLon> diff = new ArrayList<>();
    assertThat(
        "Actual geo and expected geo are the same size",
        actualGeo.size(),
        is(equalTo(expectedGeo.size())));
    for (int i = 0; i < expectedGeo.size(); i++) {
      LatLon actual = actualGeo.get(i);
      LatLon expected = expectedGeo.get(i);
      Double latDiff = actual.getLat() - expected.getLat();
      Double lonDiff = actual.getLon() - expected.getLon();

      if (Math.abs(latDiff) > 0.00001 || Math.abs(lonDiff) > 0.00001) {
        diff.add(actual);
      }
    }
    assertThat("Actual geo is the same as the expected geo", diff, is(empty()));
  }
}
