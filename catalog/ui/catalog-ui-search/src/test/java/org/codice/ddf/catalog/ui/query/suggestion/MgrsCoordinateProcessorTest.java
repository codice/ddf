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

import static org.codice.ddf.catalog.ui.query.suggestion.LatLon.from;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.codice.ddf.spatial.geocoding.Suggestion;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

public class MgrsCoordinateProcessorTest {
  private final MgrsCoordinateProcessor processor = new MgrsCoordinateProcessor();

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
  public void testProcessorIgnoresDuplicateZone() {
    String query = "1D 1D"; // Should yield 2 points
    List<Suggestion> suggestions = processor.enhanceResults(new LinkedList<>(), query);
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings(query);
    assertThat(detectedMgrsStrings, both(hasSize(2)).and(contains("1D", "1D")));
    assertThat(
        cast(suggestions).get(0).getGeo(),
        hasSize(4)); // Yields 4 since 1 MGRS match returns a bbox, proving deduplication
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
    String query = "1D FJ 1 2 1 2 1 2"; // Should yield 3 points
    List<Suggestion> suggestions = processor.enhanceResults(new LinkedList<>(), query);
    List<String> detectedMgrsStrings = processor.getMgrsCoordinateStrings(query);
    assertThat(
        detectedMgrsStrings,
        both(hasSize(3)).and(contains("1DFJ0000100002", "1DFJ0000100002", "1DFJ0000100002")));
    assertThat(
        cast(suggestions).get(0).getGeo(),
        hasSize(4)); // Yields 4 since 1 MGRS match returns a bbox, proving deduplication
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
    List<Suggestion> suggestions = processor.enhanceResults(new LinkedList<>(), "4Q, 1D");
    validateHasCoordinates(
        suggestions,
        from(19.828721744493368, -163.77046585527026),
        from(-67.19807978145084, -188.61742944126627));
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
