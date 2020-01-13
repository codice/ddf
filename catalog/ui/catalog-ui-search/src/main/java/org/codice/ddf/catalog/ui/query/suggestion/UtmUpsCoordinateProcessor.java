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

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.Validate.notNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.codice.ddf.spatial.geocoding.Suggestion;
import org.codice.usng4j.CoordinateSystemTranslator;
import org.codice.usng4j.DecimalDegreesCoordinate;
import org.codice.usng4j.NSIndicator;
import org.codice.usng4j.UsngCoordinate;
import org.codice.usng4j.UtmUpsCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A regex-based processor for identifying potential UTM/UPS coordinate literals in a given input
 * string despite deviations from the official spec. The general policy is to be as forgiving as
 * possible without sacrificing determinism.
 */
public class UtmUpsCoordinateProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(UtmUpsCoordinateProcessor.class);

  private static final Character SPACE_CHAR = ' ';

  private static final Pattern PATTERN_UTM_COORDINATE =
      Pattern.compile(
          "("
              + UsngCoordinate.ZONE_REGEX_STRING
              + ")"
              + UsngCoordinate.LATITUDE_BAND_PART_ONE_REGEX_STRING
              + "?( )+(\\d){1,6}(me)?( )+(\\d{1,7}|10000000)(mn)?",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern PATTERN_UPS_COORDINATE =
      Pattern.compile("0?[ABYZ]( )+(\\d){1,7}(me)?( )+(\\d){1,7}(mn)?", Pattern.CASE_INSENSITIVE);

  private static final Pattern PATTERN_UTM_OR_UPS_COORDINATE =
      Pattern.compile(
          String.format(
              "(%s)|(%s)", PATTERN_UTM_COORDINATE.pattern(), PATTERN_UPS_COORDINATE.pattern()),
          Pattern.CASE_INSENSITIVE);

  // This key tells the UI that the geo is on the suggestion itself
  private static final String LITERAL_SUGGESTION_ID = "LITERAL-UTM-UPS";

  private final CoordinateSystemTranslator translator;

  public UtmUpsCoordinateProcessor(CoordinateSystemTranslator translator) {
    this.translator = translator;
  }

  /**
   * Given a list of {@link Suggestion}s and the query that yielded them, enhance the list with
   * additional suggestions based upon the presence of coordinate literals in the query string.
   *
   * @param results the list of results to enhance.
   * @param query the user-provided gazetteer query text, which may contain coordinate literals.
   */
  public void enhanceResults(final List<Suggestion> results, final String query) {
    LOGGER.trace("(UTM/UPS) Adding result for query [{}]", query);
    final List<LiteralSuggestion> literals = getUtmUpsSuggestions(query);
    literals
        .stream()
        .filter(Objects::nonNull)
        .filter(LiteralSuggestion::hasGeo)
        .forEach(
            literal -> {
              LOGGER.trace("Adding the UTM/UPS suggestion to results [{}]", literal);
              results.add(0, literal);
            });
    LOGGER.trace("(UTM/UPS) Done");
  }

  private List<LiteralSuggestion> getUtmUpsSuggestions(final String query) {
    final Matcher matcher = PATTERN_UTM_OR_UPS_COORDINATE.matcher(query);
    final List<String> utmUpsMatches = new ArrayList<>();
    while (matcher.find()) {
      final String group = matcher.group();
      LOGGER.trace("Match found [{}]", group);
      utmUpsMatches.add(capitalizeLatBand(group));
    }

    if (utmUpsMatches.isEmpty()) {
      LOGGER.trace("No valid UTM or UPS strings could be inferred from query [{}]", query);
      return Collections.emptyList();
    }

    return utmUpsMatches.size() == 1
        ? suggestionsForSinglePoint(utmUpsMatches.get(0))
        : Collections.singletonList(suggestionForMultiplePoints(utmUpsMatches));
  }

  /**
   * Generates a list of {@link LiteralSuggestion}s based on the given {@code utmUpsCoord} string.
   * This method is only used to generate suggestions when the user enters a single UTM/UPS
   * coordinate. The following suggestions are returned depending on the information provided in the
   * coordinate:
   *
   * <ul>
   *   <li>If the {@code utmUpsCoord} does not have a latitude band, then two suggestions are
   *       returned - one for each hemisphere - and the hemisphere is denoted by a "N" or "S"
   *       suffix. Example: "18 631054mE 4776851mN" -> "UTM/UPS: [ 18N 631054mE 4776851mN N ]",
   *       "UTM/UPS: [ 18S 631054mE 4776851mN S ]"
   *   <li>If the {@code utmpUpsCoord} has an S latitude band, then two suggestions are also
   *       returned: one for the S latitude band coordinate and another for the Southern hemisphere
   *       coordinate. This is because the "S" is ambiguous - it can either refer to the latitude
   *       band or the Southern hemisphere. Example: "15S 533427mE 3796272mN" -> "UTM/UPS: [ 15S
   *       533427mE 3796272mN ]", "UTM/UPS: [ 15S 533427mE 3796272mN S ]"
   *   <li>If the {@code utmUpsCoord} has an N latitude band, then two suggestions are returned: one
   *       for the N latitude band and another for the Northern hemisphere. The "N" isn't ambiguous
   *       in this case, but the Northern hemisphere suggestion is included for consistency and to
   *       teach users about the N/S hemisphere syntax. Example: "9N 365103mE 659568mN" -> "UTM/UPS:
   *       [ 9N 365103mE 659568mN ]", "UTM/UPS: [ 9N 365103mE 659568mN N ]"
   *   <li>Otherwise, one suggestion is returned. Example: "20M 48831mE09437282mN" -> "UTM/UPS: [
   *       20M 48831mE09437282mN ]"
   * </ul>
   *
   * @param utmUpsCoord the user-provided gazetteer query text. It must match the {@code
   *     PATTERN_UTM_OR_UPS_COORDINATE} pattern.
   * @return a list of either one or two {@link LiteralSuggestion}s, depending on the latitude band
   *     of {@code utmUpsCoord}
   */
  private List<LiteralSuggestion> suggestionsForSinglePoint(String utmUpsCoord) {
    final Character latBand = getLatBand(utmUpsCoord);
    if (latBand == null) {
      // A coordinate without an NS indicator and a lat band is invalid so show two suggestions,
      // one for the northern hemisphere and another for the southern hemisphere.
      return Stream.of(parseUtmUpsString(utmUpsCoord + " S"), parseUtmUpsString(utmUpsCoord + " N"))
          .filter(Objects::nonNull)
          .map(this::suggestion)
          .collect(toList());
    } else if (latBand.equals('N')) {
      // If the coordinate has a N lat band, include the northern hemisphere suggestion for
      // convenience
      return Stream.of(
              parseUtmUpsString(removeLatBand(utmUpsCoord) + " N"), parseUtmUpsString(utmUpsCoord))
          .filter(Objects::nonNull)
          .map(this::suggestion)
          .collect(toList());
    } else if (latBand.equals('S')) {
      // Similarly, if the coordinate has an S lat band, include the southern hemisphere
      // suggestion for convenience
      return Stream.of(
              parseUtmUpsString(removeLatBand(utmUpsCoord) + " S"), parseUtmUpsString(utmUpsCoord))
          .filter(Objects::nonNull)
          .map(this::suggestion)
          .collect(toList());
    } else {
      return Stream.of(parseUtmUpsString(utmUpsCoord))
          .filter(Objects::nonNull)
          .map(this::suggestion)
          .collect(toList());
    }
  }

  private LiteralSuggestion suggestion(UtmUpsCoordinate utmUps) {
    return new LiteralSuggestion(
        LITERAL_SUGGESTION_ID,
        makeSuggestionText(utmUps),
        Collections.singletonList(latLonFromUtmUtps(utmUps)));
  }

  /**
   * Generates one {@link LiteralSuggestion} by combining the latitude/longitude values of each
   * UTM/UPS coordinate in the {@code utmUpsCoords} list. This method is used to generate a
   * suggestion when the user enters multiple UTM/UPS coordinates. No additional suggestions are
   * made for each point for simplicity since the map will pan to the extent or bounding box of all
   * the points. As a result, the N/S characters of each coordinate are always treated as latitude
   * bands and a coordinate is considered invalid if a latitude band is not included. Example: "12S
   * 241451mE 4101052mN 13R 37090mE 63394439mN" -> "UTM/UPS: [ 12S 241451mE 4101052mN ] [ 13R
   * 37090mE 63394439mN ]"
   *
   * @param utmUpsCoords a list of strings matching the {@code PATTERN_UTM_OR_UPS_COORDINATE}
   *     pattern, created from the user-provided gazetteer query text.
   * @return a singleton list with one {@link LiteralSuggestion} with the combined list of
   *     latitude/longitude values.
   */
  private LiteralSuggestion suggestionForMultiplePoints(final List<String> utmUpsCoords) {
    List<UtmUpsCoordinate> utmUpsList =
        utmUpsCoords
            .stream()
            .map(this::parseUtmUpsString)
            .filter(Objects::nonNull)
            .collect(toList());
    List<LatLon> latLonList =
        utmUpsList.stream().map(this::latLonFromUtmUtps).filter(Objects::nonNull).collect(toList());
    String suggestionText = makeSuggestionText(utmUpsList);

    return new LiteralSuggestion(LITERAL_SUGGESTION_ID, suggestionText, latLonList);
  }

  private static String makeSuggestionText(UtmUpsCoordinate utmUps) {
    final StringBuilder nameBuilder = new StringBuilder();
    if (utmUps.isUTM()) {
      nameBuilder.append("UTM: [ ").append(utmUps.toString()).append(" ]");
      if (utmUps.getLatitudeBand() == null) {
        if (utmUps.getNSIndicator() == NSIndicator.SOUTH) {
          nameBuilder.append(" (Southern)");
        } else {
          nameBuilder.append(" (Northern)");
        }
      }
    } else {
      nameBuilder.append("UPS: [ ").append(utmUps.toString()).append(" ]");
    }
    return nameBuilder.toString();
  }

  private static String makeSuggestionText(List<UtmUpsCoordinate> utmUpsList) {
    final StringBuilder nameBuilder = new StringBuilder();
    long numberOfUtmCoords = utmUpsList.stream().filter(UtmUpsCoordinate::isUTM).count();
    int total = utmUpsList.size();
    if (numberOfUtmCoords == total) {
      nameBuilder.append("UTM:");
    } else if (numberOfUtmCoords == 0) {
      nameBuilder.append("UPS:");
    } else {
      nameBuilder.append("UTM/UPS:");
    }
    for (UtmUpsCoordinate utmUps : utmUpsList) {
      nameBuilder.append(" [ ").append(utmUps.toString()).append(" ]");
    }
    return nameBuilder.toString();
  }

  @Nullable
  private LatLon latLonFromUtmUtps(UtmUpsCoordinate utmUps) {
    DecimalDegreesCoordinate d = toLatLon(utmUps);
    if (d == null) {
      return null;
    }
    return new LatLon(d.getLat(), d.getLon());
  }

  /**
   * USNG's error handling API is inconsistent. Some inputs will trigger an {@link
   * IllegalArgumentException} while some trigger a {@link java.text.ParseException}. For the
   * purposes of this class, all errors are the same if the string could not be parsed.
   *
   * @param utmUps the utm or ups string to parse.
   * @return a valid coordinate instance, or null if the parsing failed.
   */
  @Nullable
  private UtmUpsCoordinate parseUtmUpsString(final String utmUps) {
    try {
      return translator.parseUtmUpsString(utmUps);
    } catch (Exception e) {
      LOGGER.debug("Detected string [{}] was not valid UTM/UPS", utmUps, e);
      return null;
    }
  }

  @Nullable
  private DecimalDegreesCoordinate toLatLon(final UtmUpsCoordinate utmUps) {
    final DecimalDegreesCoordinate ddc = translator.toLatLon(utmUps);
    if (ddc == null) {
      LOGGER.trace("Invalid coordinate [{}], output was null", utmUps);
    }
    return ddc;
  }

  /**
   * Transform the UTM string to disambiguate multiple hemisphere clues.
   *
   * @param utmOrUps the UTM string to transform.
   * @return a UTM string with a single piece of data that defines hemisphere.
   */
  private static String capitalizeLatBand(final String utmOrUps) {
    if (!PATTERN_UTM_COORDINATE.matcher(utmOrUps).matches()) {
      LOGGER.trace("No transform necessary, coordinate [{}] was not UTM", utmOrUps);
      return utmOrUps; // Must be UPS, do nothing
    }
    final Character latBand = getLatBand(utmOrUps);
    if (latBand == null) {
      LOGGER.trace("No lat band found on input [{}], skipping conversion to upper case", utmOrUps);
      return utmOrUps;
    }
    if (Character.isLowerCase(latBand)) {
      final String asUpperCase = setLatBand(utmOrUps, Character.toUpperCase(latBand));
      LOGGER.trace(
          "Found lower case lat band on input [{}], converting to upper case [{}]",
          utmOrUps,
          asUpperCase);
      return asUpperCase;
    }
    LOGGER.trace("Found lat band on input [{}]", utmOrUps);
    return utmOrUps;
  }

  /**
   * Private utility method that detects if a latitude band is present in a UTM input string and
   * returns it, or null if it didn't exist. The provided input <b>must</b> match {@link
   * #PATTERN_UTM_COORDINATE}.
   *
   * @param input UTM string input to search.
   * @return the lat band character, or null if it wasn't on the input string.
   */
  @Nullable
  private static Character getLatBand(final String input) {
    final int indexOfLastSymbolInZoneClause = input.indexOf(SPACE_CHAR) - 1;
    final char lastSymbolInZoneClause = input.charAt(indexOfLastSymbolInZoneClause);
    if (Character.isLetter(lastSymbolInZoneClause)) {
      return lastSymbolInZoneClause;
    }
    return null;
  }

  /**
   * Private utility method for updating a UTM string in-place with a new latitude band. The
   * provided input <b>must</b> match {@link #PATTERN_UTM_COORDINATE}.
   *
   * @param input UTM string input; must match {@link #PATTERN_UTM_COORDINATE}.
   * @param newLatBand character to use as the new latitude band for the new UTM string.
   * @return a new UTM string with the provided {@code newLatBand}, regardless if the original UTM
   *     {@code input} had a lat band or not.
   */
  private static String setLatBand(final String input, final Character newLatBand) {
    notNull(newLatBand, "newLatBand cannot be null");
    final int indexOfFirstWhiteSpace = input.indexOf(SPACE_CHAR);
    final int indexOfFirstWhiteSpaceOrLatBand =
        Character.isLetter(input.charAt(indexOfFirstWhiteSpace - 1))
            ? indexOfFirstWhiteSpace - 1 // Use lat band index instead
            : indexOfFirstWhiteSpace;
    return input
        .substring(0, indexOfFirstWhiteSpaceOrLatBand)
        .concat(Character.toString(newLatBand))
        .concat(input.substring(indexOfFirstWhiteSpace));
  }

  /**
   * Private utility method for removing the latitude band in a UTM string in-place. The provided
   * input <b>must</b> match {@link #PATTERN_UTM_COORDINATE}.
   *
   * @param input UTM string input; must match {@link #PATTERN_UTM_COORDINATE}.
   * @return a new UTM string with the latitude band removed, regardless if the original UTM {@code
   *     input} had a lat band or not.
   */
  private static String removeLatBand(final String input) {
    final int indexOfFirstWhiteSpace = input.indexOf(SPACE_CHAR);
    final int indexOfFirstWhiteSpaceOrLatBand =
        Character.isLetter(input.charAt(indexOfFirstWhiteSpace - 1))
            ? indexOfFirstWhiteSpace - 1 // Use lat band index instead
            : indexOfFirstWhiteSpace;
    return input
        .substring(0, indexOfFirstWhiteSpaceOrLatBand)
        .concat(input.substring(indexOfFirstWhiteSpace));
  }
}
