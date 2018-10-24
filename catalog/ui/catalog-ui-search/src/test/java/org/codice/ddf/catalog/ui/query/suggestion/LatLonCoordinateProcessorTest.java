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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.codice.ddf.spatial.geocoding.Suggestion;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

public class LatLonCoordinateProcessorTest {
  private final LatLonCoordinateProcessor processor = new LatLonCoordinateProcessor();

  @Test
  public void testProcessorIgnoresSingleValue() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "0");
    assertThat(suggestions, empty());
  }

  @Test
  public void testProcessorIgnoresSingleValueWithTrailingPeriod() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "0.");
    assertThat(suggestions, empty());
  }

  @Test
  public void testProcessorDetectsOrigin() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "0 0");
    assertThat(cast(suggestions).get(0), hasCoordinates(coord(0.0, 0.0)));
  }

  @Test
  public void testProcessorDetectsOriginWithLeadingDecimals() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), ".0 .0");
    assertThat(cast(suggestions).get(0), hasCoordinates(coord(0.0, 0.0)));
  }

  @Test
  public void testProcessorDetectsWholeNumbers() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "50 123");
    assertThat(cast(suggestions).get(0), hasCoordinates(coord(50.0, 123.0)));
  }

  @Test
  public void testProcessorIgnoresTooLargeLonWholeNumber() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "50 1234");
    assertThat(suggestions, empty());
  }

  @Test
  public void testProcessorIgnoresTooSmallLonWholeNumber() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "50 -1234");
    assertThat(suggestions, empty());
  }

  @Test
  public void testProcessorIgnoresTooLargeLonDecimal() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "50 1234.1234");
    assertThat(suggestions, empty());
  }

  @Test
  public void testProcessorIgnoresTooSmallLonDecimal() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "50 -1234.1234");
    assertThat(suggestions, empty());
  }

  @Test
  public void testProcessorDetectsNegativeLonWholeNumber() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "50 -123");
    assertThat(cast(suggestions).get(0), hasCoordinates(coord(50.0, -123.0)));
  }

  @Test
  public void testProcessorDetectsLonDecimalWithoutWhole() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "50 .1234");
    assertThat(cast(suggestions).get(0), hasCoordinates(coord(50.0, 0.1234)));
  }

  @Test
  public void testProcessorDetectsNegativeLonDecimalWithoutWhole() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "50 -.1234");
    assertThat(cast(suggestions).get(0), hasCoordinates(coord(50.0, -0.1234)));
  }

  @Test
  public void testProcessorDetectsLonDecimal() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "50 123.1234");
    assertThat(cast(suggestions).get(0), hasCoordinates(coord(50.0, 123.1234)));
  }

  @Test
  public void testProcessorIgnoresTooLargeLatWholeNumber() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "95, 34");
    assertThat(suggestions, empty());
  }

  @Test
  public void testProcessorIgnoresTooSmallLatWholeNumber() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "-95 34");
    assertThat(suggestions, empty());
  }

  @Test
  public void testProcessorIgnoresTooLargeLatDecimal() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "1234.1234 34");
    assertThat(suggestions, empty());
  }

  @Test
  public void testProcessorIgnoresTooSmallLatDecimal() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "-1234.1234 34");
    assertThat(suggestions, empty());
  }

  @Test
  public void testProcessorDetectsNegativeLatWholeNumber() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "-56, 34");
    assertThat(cast(suggestions).get(0), hasCoordinates(coord(-56.0, 34.0)));
  }

  @Test
  public void testProcessorDetectsLatDecimalWithoutWhole() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), ".1234 34");
    assertThat(cast(suggestions).get(0), hasCoordinates(coord(0.1234, 34.0)));
  }

  @Test
  public void testProcessorDetectsNegativeLatDecimalWithoutWhole() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "-.1234 34");
    assertThat(cast(suggestions).get(0), hasCoordinates(coord(-0.1234, 34.0)));
  }

  @Test
  public void testProcessorDetectsLatDecimal() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "56.789 34");
    assertThat(cast(suggestions).get(0), hasCoordinates(coord(56.789, 34.0)));
  }

  @Test
  public void testProcessorDetectsAndGracefullyHandlesMassiveDecimals() {
    String massiveDecimal = "50.123456789012345678901234567890123456789012345678901234567890";
    List<Suggestion> suggestions =
        enhanceAndReturn(new LinkedList<>(), massiveDecimal + " " + massiveDecimal);
    assertThat(suggestions, hasSize(1));
  }

  @Test
  public void testProcessorHandlesOddNumberOfInputs() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "89, 110, -46, 67, -120");
    assertThat(cast(suggestions).get(0), hasCoordinates(coord(89.0, 110.0), coord(-46.0, 67.0)));
  }

  @Test
  public void testProcessorHandlesOddNumberOfInputsInvalidLatitudeInMiddle() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "89 46 99 67 -120");
    assertThat(cast(suggestions).get(0), hasCoordinates(coord(89.0, 46.0), coord(67.0, -120.0)));
  }

  @Test
  public void testProcessorHandlesOddNumberOfInputsInvalidLatitudeAtBeginning() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "99, 89, 46, 67, -120");
    assertThat(cast(suggestions).get(0), hasCoordinates(coord(89.0, 46.0), coord(67.0, -120.0)));
  }

  @Test
  public void testProcessorHandlesOddNumberOfInvalidLongitudeAtEnd() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "-38, 89, 46, 189, -120");
    assertThat(cast(suggestions).get(0), hasCoordinates(coord(-38.0, 89.0)));
  }

  @Test
  public void testProcessorIgnoresInvalidLongitudeInMiddle() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "38, 189, -46, 89");
    assertThat(cast(suggestions).get(0), hasCoordinates(coord(-46.0, 89.0)));
  }

  @Test
  public void testProcessorIgnoresInvalidLongitudeAtEnd() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "89, 46, 67, -220");
    assertThat(cast(suggestions).get(0), hasCoordinates(coord(89.0, 46.0)));
  }

  @Test
  public void testRegexDetectsLatLonCommaSpaced() {
    List<Suggestion> suggestions = enhanceAndReturn(new LinkedList<>(), "12.153, -39.0352");
    assertThat(cast(suggestions).get(0), hasCoordinates(coord(12.153, -39.0352)));
  }

  @Test
  public void testRegexDetectsLatLonParentheses() {
    List<Suggestion> suggestions =
        enhanceAndReturn(new LinkedList<>(), "(12.153, -39.0352), (21.098, 19.148)");
    assertThat(
        cast(suggestions).get(0), hasCoordinates(coord(12.153, -39.0352), coord(21.098, 19.148)));
  }

  @Test
  public void testRegexDetectsLatLonBrackets() {
    List<Suggestion> suggestions =
        enhanceAndReturn(new LinkedList<>(), "[12.153, -39.0352], [21.098, 19.148]");
    assertThat(
        cast(suggestions).get(0), hasCoordinates(coord(12.153, -39.0352), coord(21.098, 19.148)));
  }

  @Test
  public void testRegexDetectsLatLonCurlyBraces() {
    List<Suggestion> suggestions =
        enhanceAndReturn(new LinkedList<>(), "{12.153, -39.0352}, {21.098, 19.148}");
    assertThat(
        cast(suggestions).get(0), hasCoordinates(coord(12.153, -39.0352), coord(21.098, 19.148)));
  }

  @Test
  public void testRegexDetectsScatteredCoords() {
    List<Suggestion> suggestions =
        enhanceAndReturn(new LinkedList<>(), "blah;;;3g-12j.098     4.1");
    assertThat(cast(suggestions).get(0), hasCoordinates(coord(3.0, -12.0), coord(0.098, 4.1)));
  }

  @Test
  // TODO DDF-4242: Future improvement ... allow whitespace between relevant symbols: "- 14" & ".12"
  public void testRegexIgnoresStandaloneSymbols() {
    List<Suggestion> suggestions =
        enhanceAndReturn(new LinkedList<>(), "blah10.19205123gagne . - -.12 . 12 - 14");
    // The below assertion's latter coord would change from 12.0 & 14.0 --> 0.12 & -14.0
    assertThat(
        cast(suggestions).get(0), hasCoordinates(coord(10.19205123, -0.12), coord(12.0, 14.0)));
  }

  @Test
  public void testRegexIgnoresStandaloneSymbolsChainedTwice() {
    List<Suggestion> suggestions =
        enhanceAndReturn(new LinkedList<>(), "blah10.19205123gagne .. -- -.12 . 12 - 14");
    assertThat(
        cast(suggestions).get(0), hasCoordinates(coord(10.19205123, -0.12), coord(12.0, 14.0)));
  }

  @Test
  public void testRegexIgnoresStandaloneSymbolsChainedThrice() {
    List<Suggestion> suggestions =
        enhanceAndReturn(new LinkedList<>(), "blah10.19205123gagne ... --- -.12 . 12 - 14");
    assertThat(
        cast(suggestions).get(0), hasCoordinates(coord(10.19205123, -0.12), coord(12.0, 14.0)));
  }

  private List<Suggestion> enhanceAndReturn(List<Suggestion> input, String query) {
    processor.enhanceResults(input, query);
    return input;
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
