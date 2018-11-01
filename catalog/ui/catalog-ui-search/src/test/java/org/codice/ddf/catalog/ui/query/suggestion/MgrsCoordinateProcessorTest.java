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
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.codice.ddf.spatial.geocoding.Suggestion;
import org.codice.usng4j.impl.CoordinateSystemTranslatorImpl;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

public class MgrsCoordinateProcessorTest {
  private final MgrsCoordinateProcessor processor =
      new MgrsCoordinateProcessor(new CoordinateSystemTranslatorImpl());

  @Test
  public void testProcessorDetectsZone() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4Q")));
  }

  @Test
  public void testProcessorDetectsTwoZones() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q 10P");
    assertThat(detectedMgrsStrings, both(hasSize(2)).and(contains("4Q", "10P")));
  }

  @Test
  public void testProcessorDetectsTwoZonesNoSpace() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q10P");
    assertThat(detectedMgrsStrings, both(hasSize(2)).and(contains("4Q", "10P")));
  }

  @Test
  public void testProcessorDetectsHundredKm() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ")));
  }

  @Test
  public void testProcessorDetectsHundredKmNoSpace() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4QFJ");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ")));
  }

  @Test
  public void testProcessorDetectsTwoHundredKm() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 10P EG");
    assertThat(detectedMgrsStrings, both(hasSize(2)).and(contains("4QFJ", "10PEG")));
  }

  @Test
  public void testProcessorDetectsTwoHundredKmNoSpace() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4QFJ10PEG");
    assertThat(detectedMgrsStrings, both(hasSize(2)).and(contains("4QFJ", "10PEG")));
  }

  @Test
  public void testProcessorIgnoresExtraneousHundredKm() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ EG");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ")));
  }

  @Test
  public void testProcessorIgnoresExtraneousHundredKmNoSpace() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4QFJEG");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ")));
  }

  @Test
  public void testProcessorIgnoresExtraneousHundredKmFollowedByZone() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ EG 10P");
    assertThat(detectedMgrsStrings, both(hasSize(2)).and(contains("4QFJ", "10P")));
  }

  @Test
  public void testProcessorIgnoresExtraneousHundredKmFollowedByZoneNoSpace() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4QFJEG10P");
    assertThat(detectedMgrsStrings, both(hasSize(2)).and(contains("4QFJ", "10P")));
  }

  @Test
  public void testProcessorDoesNotUseNextZoneDigitsAsPartOfPreviousNumeric() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("14T11 7Q22");
    assertThat(
        detectedMgrsStrings, both(hasSize(2)).and(contains("14T0000100001", "7Q0000200002")));
  }

  @Test
  public void testProcessorDetectsPrecision1() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 1 6");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ0000100006")));
  }

  @Test
  public void testProcessorDetectsPrecision1NoSpace() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 16");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ0000100006")));
  }

  @Test
  public void testProcessorDetectsPrecision1NoSpaceAtAll() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4QFJ16");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ0000100006")));
  }

  @Test
  public void testProcessorDetectsPrecision2() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 12 67");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ0001200067")));
  }

  @Test
  public void testProcessorDetectsPrecision2OffsetLeft() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 126 7");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ0012600007")));
  }

  @Test
  public void testProcessorDetectsPrecision2OffsetRight() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 1 267");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ0000100267")));
  }

  @Test
  public void testProcessorDetectsPrecision2NoSpace() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 1267");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ0001200067")));
  }

  @Test
  public void testProcessorDetectsPrecision2NoSpaceAtAll() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4QFJ1267");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ0001200067")));
  }

  @Test
  public void testProcessorDetectsPrecision3() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 123 678");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ0012300678")));
  }

  @Test
  public void testProcessorDetectsPrecision3OffsetLeft() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 1236 78");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ0123600078")));
  }

  @Test
  public void testProcessorDetectsPrecision3OffsetRight() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 12 3678");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ0001203678")));
  }

  @Test
  public void testProcessorDetectsPrecision3NoSpace() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 123678");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ0012300678")));
  }

  @Test
  public void testProcessorDetectsPrecision3NoSpaceAtAll() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4QFJ123678");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ0012300678")));
  }

  @Test
  public void testProcessorDetectsPrecision4() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 1234 6789");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ0123406789")));
  }

  @Test
  public void testProcessorDetectsPrecision4OffsetLeft() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 12346 789");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ1234600789")));
  }

  @Test
  public void testProcessorDetectsPrecision4OffsetRight() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 123 46789");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ0012346789")));
  }

  @Test
  public void testProcessorDetectsPrecision4NoSpace() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 12346789");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ0123406789")));
  }

  @Test
  public void testProcessorDetectsPrecision4NoSpaceAtAll() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4QFJ12346789");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ0123406789")));
  }

  @Test
  public void testProcessorDetectsPrecision5() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 12345 67891");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ1234567891")));
  }

  @Test
  public void testProcessorDetectsPrecision5OffsetLeft() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 123456 7891");
    assertThat(
        detectedMgrsStrings, both(hasSize(2)).and(contains("4QFJ0012300456", "4QFJ0007800091")));
  }

  @Test
  public void testProcessorDetectsPrecision5OffsetRight() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 1234 567891");
    assertThat(
        detectedMgrsStrings, both(hasSize(2)).and(contains("4QFJ0001200034", "4QFJ0056700891")));
  }

  @Test
  public void testProcessorDetectsPrecision5NoSpace() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 1234567891");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ1234567891")));
  }

  @Test
  public void testProcessorDetectsPrecision5NoSpaceAtAll() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4QFJ1234567891");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ1234567891")));
  }

  @Test
  public void testProcessorHandlesAttemptedPrecision6() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 123456 789123");
    assertThat(
        detectedMgrsStrings, both(hasSize(2)).and(contains("4QFJ0012300456", "4QFJ0078900123")));
  }

  @Test
  public void testProcessorHandlesAttemptedPrecision7() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ 1234567 8912345");
    assertThat(
        detectedMgrsStrings, both(hasSize(2)).and(contains("4QFJ0012300456", "4QFJ0089100234")));
  }

  @Test
  public void testProcessorIsNotCaseSensitive() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4qFj123456");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ0012300456")));
  }

  @Test
  public void testProcessorIsNotSymbolSensitive() {
    List<String> detectedMgrsStrings =
        processor.getMgrsCoordinateStrings("39U VR 283 391, 39U VR 283 392");
    assertThat(
        detectedMgrsStrings, both(hasSize(2)).and(contains("39UVR0028300391", "39UVR0028300392")));
  }

  @Test
  public void testProcessorSymbolsOverrideWhitespace() {
    List<String> detectedMgrsStrings =
        processor.getMgrsCoordinateStrings("39U VR 283,391, 39U VR 283 392");
    assertThat(
        detectedMgrsStrings,
        both(hasSize(3)).and(contains("39UVR0000200008", "39UVR0000300009", "39UVR0028300392")));
  }

  @Test
  public void testProcessorIgnoresInvalidZoneLetters() {
    List<String> zoneWithA = processor.getMgrsCoordinateStrings("14A");
    List<String> zoneWithB = processor.getMgrsCoordinateStrings("14B");
    List<String> zoneWithI = processor.getMgrsCoordinateStrings("14I");
    List<String> zoneWithO = processor.getMgrsCoordinateStrings("14O");
    List<String> zoneWithY = processor.getMgrsCoordinateStrings("14Y");
    List<String> zoneWithZ = processor.getMgrsCoordinateStrings("14Z");

    assertThat(zoneWithA, hasSize(0));
    assertThat(zoneWithB, hasSize(0));
    assertThat(zoneWithI, hasSize(0));
    assertThat(zoneWithO, hasSize(0));
    assertThat(zoneWithY, hasSize(0));
    assertThat(zoneWithZ, hasSize(0));
  }

  @Test
  public void testProcessorIgnoresInvalid100KmLetters() {
    List<String> kmIJ = processor.getMgrsCoordinateStrings("14Q IJ");
    List<String> kmOJ = processor.getMgrsCoordinateStrings("14Q OJ");
    List<String> kmJI = processor.getMgrsCoordinateStrings("14Q JI");
    List<String> kmJO = processor.getMgrsCoordinateStrings("14Q JO");
    List<String> kmJW = processor.getMgrsCoordinateStrings("14Q JW");
    List<String> kmJX = processor.getMgrsCoordinateStrings("14Q JX");
    List<String> kmJY = processor.getMgrsCoordinateStrings("14Q JY");
    List<String> kmJZ = processor.getMgrsCoordinateStrings("14Q JZ");

    assertThat(kmIJ, both(hasSize(1)).and(contains("14Q")));
    assertThat(kmOJ, both(hasSize(1)).and(contains("14Q")));
    assertThat(kmJI, both(hasSize(1)).and(contains("14Q")));
    assertThat(kmJO, both(hasSize(1)).and(contains("14Q")));
    assertThat(kmJW, both(hasSize(1)).and(contains("14Q")));
    assertThat(kmJX, both(hasSize(1)).and(contains("14Q")));
    assertThat(kmJY, both(hasSize(1)).and(contains("14Q")));
    assertThat(kmJZ, both(hasSize(1)).and(contains("14Q")));
  }

  @Test
  public void testProcessorIgnoresDuplicateZone() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("1D 1D");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("1D")));
  }

  @Test
  public void testProcessorDetectsDuplicateZoneIfMoreSpecificsFollow() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("1D 1D FJ");
    assertThat(detectedMgrsStrings, both(hasSize(2)).and(contains("1D", "1DFJ")));
  }

  @Test
  public void testProcessorDetectsZone100kReuse() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("1D FJ 1 2 3 4 5 6");
    assertThat(
        detectedMgrsStrings,
        both(hasSize(3)).and(contains("1DFJ0000100002", "1DFJ0000300004", "1DFJ0000500006")));
  }

  @Test
  public void testProcessorDetectsOnlyZoneReuse() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("1D 1 2 3 4 5 6");
    assertThat(
        detectedMgrsStrings,
        both(hasSize(3)).and(contains("1D0000100002", "1D0000300004", "1D0000500006")));
  }

  @Test
  public void testProcessorDetectsDuplicateCoordsWithZoneReuse() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("1D FJ 1 2 1 2 1 2");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("1DFJ0000100002")));
  }

  @Test
  public void testProcessorCanInferPrecision() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4Q FJ B12345");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4QFJ0001200034")));
  }

  @Test
  public void testProcessorFallsBackToZoneWhen100KmGridIsWrong() {
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings("4QJ1");
    assertThat(detectedMgrsStrings, both(hasSize(1)).and(contains("4Q")));
  }

  @Test
  public void testProcessorDetectsMultipleMgrsCoordinatesWithDelimiter() {
    List<Suggestion> suggestions = new LinkedList<>();
    processor.enhanceResults(suggestions, "4Q, 1D");
    validateHasCoordinates(
        suggestions,
        coord(19.828721744493368, -163.77046585527026),
        coord(-67.19807978145084, -188.61742944126627));
  }

  private static void validateHasCoordinates(List<Suggestion> suggestions, LatLon... lls) {
    assertThat(suggestions, hasSize(1));
    List<LiteralSuggestion> literalSuggestions = cast(suggestions);
    assertThat(literalSuggestions.get(0).getGeo(), hasSize(lls.length));
    assertThat(literalSuggestions.get(0), hasCoordinates(lls));
  }

  private static List<LiteralSuggestion> cast(List<Suggestion> suggestions) {
    return suggestions.stream().map(LiteralSuggestion.class::cast).collect(Collectors.toList());
  }

  private static LatLon coord(Double lat, Double lon) {
    return new LatLon(lat, lon);
  }

  private static org.hamcrest.Matcher<LiteralSuggestion> hasCoordinates(LatLon... coords) {
    return new TypeSafeMatcher<LiteralSuggestion>() {
      @Override
      public void describeTo(final Description description) {
        description
            .appendText("coordinates should be on the object ")
            .appendValue(Arrays.asList(coords));
      }

      @Override
      protected void describeMismatchSafely(
          final LiteralSuggestion item, final Description mismatchDescription) {
        mismatchDescription.appendText(" was ").appendValue(item.getGeo());
      }

      @Override
      protected boolean matchesSafely(final LiteralSuggestion item) {
        return item.hasGeo() && item.getGeo().containsAll(Arrays.asList(coords));
      }
    };
  }
}
