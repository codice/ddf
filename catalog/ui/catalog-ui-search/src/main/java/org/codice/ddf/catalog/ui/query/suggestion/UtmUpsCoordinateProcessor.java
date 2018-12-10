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

import static org.apache.commons.lang3.Validate.notNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.codice.ddf.spatial.geocoding.Suggestion;
import org.codice.usng4j.CoordinateSystemTranslator;
import org.codice.usng4j.DecimalDegreesCoordinate;
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
    final LiteralSuggestion literal = getUtmUpsSuggestion(query);
    if (literal != null && literal.hasGeo()) {
      LOGGER.trace("Adding the UTM/UPS suggestion to results [{}]", literal);
      results.add(0, literal);
    }
    LOGGER.trace("(UTM/UPS) Done");
  }

  @Nullable
  private LiteralSuggestion getUtmUpsSuggestion(final String query) {
    final Matcher matcher = PATTERN_UTM_OR_UPS_COORDINATE.matcher(query);
    final StringBuilder nameBuilder = new StringBuilder("UTM/UPS:");
    final List<UtmUpsCoordinate> utmUpsCoords = new ArrayList<>();
    while (matcher.find()) {
      final String group = matcher.group();
      LOGGER.trace("Match found [{}]", group);
      final String utmOrUpsText = normalizeCoordinate(group);
      final UtmUpsCoordinate utmUps = parseUtmUpsString(utmOrUpsText);
      if (utmUps != null) {
        nameBuilder.append(" [ ").append(utmUps.toString()).append(" ]");
        utmUpsCoords.add(utmUps);
      }
    }
    if (utmUpsCoords.isEmpty()) {
      LOGGER.trace("No valid UTM or UPS strings could be inferred from query [{}]", query);
      return null;
    }
    return new LiteralSuggestion(
        LITERAL_SUGGESTION_ID,
        nameBuilder.toString(),
        utmUpsCoords
            .stream()
            .map(this::toLatLon)
            .filter(Objects::nonNull)
            .map(d -> new LatLon(d.getLat(), d.getLon()))
            .collect(Collectors.toList()));
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
  private static String normalizeCoordinate(final String utmOrUps) {
    if (!PATTERN_UTM_COORDINATE.matcher(utmOrUps).matches()) {
      LOGGER.trace("No transform necessary, coordinate [{}] was not UTM", utmOrUps);
      return utmOrUps; // Must be UPS, do nothing
    }
    final Character latBand = getLatBand(utmOrUps);
    if (latBand == null) {
      final String withDefaultNorthLatBand = useDefaultNorthLatBand(utmOrUps);
      LOGGER.trace(
          "No lat band found on input [{}], setting it to default for north hemisphere [{}]",
          utmOrUps,
          withDefaultNorthLatBand);
      return withDefaultNorthLatBand;
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
   * Convenience method for setting the lat band to the default north lat band. The provided input
   * <b>must</b> match {@link #PATTERN_UTM_COORDINATE}.
   *
   * @param input the UTM input with no lat band, or a lat band to replace.
   * @return the provided input UTM string with a lat band that directs calculations to the northern
   *     hemisphere.
   */
  private static String useDefaultNorthLatBand(final String input) {
    return setLatBand(input, 'N');
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
}
