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

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.leftPad;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.codice.ddf.spatial.geocoding.Suggestion;
import org.codice.usng4j.BoundingBox;
import org.codice.usng4j.CoordinateSystemTranslator;
import org.codice.usng4j.UsngCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A regex-based processor for identifying potential MGRS coordinate literals in a given input
 * string despite deviations from the official spec. The general policy is to be as forgiving as
 * possible without sacrificing determinism.
 *
 * <p>Due to the strict expectations of usng4j regarding input strings, some additional processing
 * is done to "backfill" necessary zeros for occurrences of a {@link MgrsPartType#NUMERIC} string.
 *
 * @see #processNumericType(String) for more information on this "backfilling" behavior.
 */
public class MgrsCoordinateProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(MgrsCoordinateProcessor.class);

  private static final Pattern PATTERN_MGRS_ZONE = Pattern.compile("\\d\\d?[C-HJ-NP-X]");

  private static final Pattern PATTERN_MGRS_100KM = Pattern.compile("[A-HJ-NP-Z][A-HJ-NP-V]");

  private static final Pattern PATTERN_MGRS_NUMERIC =
      Pattern.compile("((\\d){1,5}(\\h)+(\\d){1,5}(\\W)+)|((\\d){2,10})");

  private static final Pattern PATTERN_MGRS_ANYPART =
      Pattern.compile(
          format(
              "(%s)|(%s)|(%s)",
              PATTERN_MGRS_ZONE.pattern(),
              PATTERN_MGRS_100KM.pattern(),
              PATTERN_MGRS_NUMERIC.pattern()),
          Pattern.CASE_INSENSITIVE);

  // usng4j helper constant for backfilling zeros
  private static final Integer MAX_COORDINATE_LENGTH = 5;

  // This key tells the UI that the geo is on the suggestion itself
  private static final String LITERAL_SUGGESTION_ID = "LITERAL-MGRS";

  private static final String ZERO = "0";

  private static final String WHITESPACE = " ";

  /**
   * An {@link MgrsPartType} identifies one of the three different component strings that are glued
   * together to form a valid MGRS coordinate string, including a category {@link #NONE} to be used
   * when the string is unsuitable for any part of an MGRS coordinate or the type has not been
   * provided yet.
   *
   * <p>Each type corresponds to a regex defined above.
   */
  private enum MgrsPartType {
    NONE,
    ZONE,
    HUNDRED_KM,
    NUMERIC
  }

  private final CoordinateSystemTranslator translator;

  public MgrsCoordinateProcessor(CoordinateSystemTranslator translator) {
    this.translator = translator;
  }

  /**
   * Given a list of {@link Suggestion}s and the query that yielded them, enhance the list with
   * additional suggestions based upon the presence of coordinate literals in the query string.
   *
   * @param results the list of results to enhance.
   * @param query the query string to search for coordinate literals.
   */
  public void enhanceResults(List<Suggestion> results, String query) {
    final LiteralSuggestion literal = getMgrsSuggestion(query);
    if (literal != null && literal.hasGeo()) {
      LOGGER.trace("Adding the MGRS suggestion to results [{}]", literal);
      results.add(0, literal);
    }
  }

  /**
   * Build the MGRS suggestion.
   *
   * @param query the query string to search for coordinate literals.
   * @return a suggestion object with geometry attached to it; a {@link BoundingBox} if only one
   *     coordinate was detected, or a collection of {@link LatLon} center points otherwise. If no
   *     MGRS was detected in the query, {@code null} is returned.
   */
  @Nullable
  private LiteralSuggestion getMgrsSuggestion(String query) {
    final List<String> matches = getMgrsCoordinateStrings(query);
    if (matches.isEmpty()) {
      LOGGER.debug("No valid MGRS strings were found or could be inferred from query [{}]", query);
      return null;
    }

    final List<UsngCoordinate> mgrsCoordinates = new ArrayList<>();
    final StringBuilder nameBuilder = new StringBuilder("MGRS:");
    for (String match : matches) {
      final UsngCoordinate coordinate = parseMgrsString(match);
      if (coordinate == null || mgrsCoordinates.contains(coordinate)) {
        continue;
      }
      mgrsCoordinates.add(coordinate);
      nameBuilder.append(" [ ").append(coordinate.toMgrsString()).append(" ]");
    }

    if (mgrsCoordinates.size() == 1) {
      final UsngCoordinate mgrsCoord = mgrsCoordinates.get(0);
      final BoundingBox bb = translator.toBoundingBox(mgrsCoord);
      return new LiteralSuggestion(
          LITERAL_SUGGESTION_ID, nameBuilder.toString(), latLonFromBoundingBox(bb));
    }
    return new LiteralSuggestion(
        LITERAL_SUGGESTION_ID,
        nameBuilder.toString(),
        mgrsCoordinates
            .stream()
            .map(translator::toLatLon)
            .map(d -> new LatLon(d.getLat(), d.getLon()))
            .collect(Collectors.toList()));
  }

  @Nullable
  private UsngCoordinate parseMgrsString(String mgrs) {
    try {
      return translator.parseMgrsString(mgrs);
    } catch (ParseException e) {
      LOGGER.debug("Detected string [{}] was not valid MGRS", mgrs, e);
      return null;
    }
  }

  private static List<LatLon> latLonFromBoundingBox(BoundingBox bb) {
    return ImmutableList.of(
        new LatLon(bb.getSouth(), bb.getWest()),
        new LatLon(bb.getNorth(), bb.getWest()),
        new LatLon(bb.getNorth(), bb.getEast()),
        new LatLon(bb.getSouth(), bb.getEast()));
  }

  /**
   * Given the user's query, return a list of valid MGRS strings that could be inferred from the
   * query. The strings should be ready for submission to usng4j to be parsed and mathematically
   * evaluated.
   *
   * <p>The query is terminated with whitespace so the user does not have to. See the LHS of {@link
   * #PATTERN_MGRS_NUMERIC} for the cases where this is useful.
   *
   * <p>There should be no duplicate entries in the returned list. The order of the entries matters
   * so a {@link java.util.Set} cannot be used.
   *
   * @param query the input string potentially holding MGRS values.
   * @return a list of valid MGRS values that can be sent to usng4j.
   * @throws IllegalStateException if the provided query yields an {@link MgrsPartType#NONE}.
   */
  @VisibleForTesting
  List<String> getMgrsCoordinateStrings(String query) {
    final String terminatedQuery = query.concat(WHITESPACE);
    final Matcher matcher = PATTERN_MGRS_ANYPART.matcher(terminatedQuery);
    final List<String> mgrsStrings = new ArrayList<>();
    final StringBuilder mgrsBuilder = new StringBuilder();

    MgrsPartType previousType = MgrsPartType.NONE;

    while (matcher.find()) {
      final String mgrsPart = matcher.group().toUpperCase();
      final MgrsPartType type = getPartType(mgrsPart);
      switch (type) {
        case ZONE:
          handleZoneWithPreviousType(mgrsPart, previousType, mgrsBuilder, mgrsStrings);
          break;
        case HUNDRED_KM:
          handleHundredKmWithPreviousType(mgrsPart, previousType, mgrsBuilder);
          break;
        case NUMERIC:
          handleNumericWithPreviousType(mgrsPart, previousType, mgrsBuilder, mgrsStrings);
          break;
        case NONE:
          // This should not be reachable or testable; it indicates the regex contract was broken.
          throw new IllegalStateException(
              format("The provided mgrs part [ %s ] must map to a valid type", mgrsPart));
      }
      LOGGER.debug(
          "MGRS strings [{}], builder state [{}], and types [{}|{}]",
          mgrsStrings,
          mgrsBuilder,
          previousType,
          type);
      previousType = type;
    }
    LOGGER.trace("Builder's final state [{}]", mgrsBuilder);
    if (previousType != MgrsPartType.NUMERIC && mgrsBuilder.length() > 0) {
      mgrsStrings.add(mgrsBuilder.toString());
    }
    return mgrsStrings.stream().distinct().collect(Collectors.toList());
  }

  private static void handleZoneWithPreviousType(
      String mgrsPart,
      MgrsPartType previousType,
      StringBuilder mgrsBuilder,
      List<String> mgrsStrings) {
    LOGGER.trace("Found a ZONE [{}] which occurs after a {}", mgrsPart, previousType);
    switch (previousType) {
      case ZONE:
      case HUNDRED_KM:
        LOGGER.trace(
            "Storing final result coordinate, clearing the builder, and saving this part [{}]",
            mgrsPart);
        mgrsStrings.add(mgrsBuilder.toString());
        mgrsBuilder.delete(0, mgrsBuilder.length());
        mgrsBuilder.append(mgrsPart);
        break;
      case NUMERIC:
        LOGGER.trace("Clearing the builder and saving this part [{}]", mgrsPart);
        mgrsBuilder.delete(0, mgrsBuilder.length());
        mgrsBuilder.append(mgrsPart);
        break;
      case NONE:
        LOGGER.trace("Saving this part [{}]", mgrsPart);
        mgrsBuilder.append(mgrsPart);
    }
  }

  private static void handleHundredKmWithPreviousType(
      String mgrsPart, MgrsPartType previousType, StringBuilder mgrsBuilder) {
    LOGGER.trace("Found a 100KM [{}] which occurs after a {}", mgrsPart, previousType);
    switch (previousType) {
      case ZONE:
        LOGGER.trace("Saving this part [{}]", mgrsPart);
        mgrsBuilder.append(mgrsPart);
        break;
      case HUNDRED_KM:
      case NUMERIC:
      case NONE:
        LOGGER.trace("Ignoring this part [{}], since 100KM is only valid after a ZONE", mgrsPart);
    }
  }

  private static void handleNumericWithPreviousType(
      String mgrsPart,
      MgrsPartType previousType,
      StringBuilder mgrsBuilder,
      List<String> mgrsStrings) {
    LOGGER.trace("Found a NUMERIC [{}] which occurs after a {}", mgrsPart, previousType);
    switch (previousType) {
      case ZONE:
      case HUNDRED_KM:
      case NUMERIC:
        String processed = processNumericType(mgrsPart);
        LOGGER.trace(
            "NUMERIC [{}] processed = [{}], storing final result coordinate", mgrsPart, processed);
        mgrsStrings.add(mgrsBuilder.toString() + processed);
        break;
      case NONE:
        LOGGER.trace(
            "Ignoring this part [{}], since NUMERIC is only valid in a ZONE but no ZONE was provided",
            mgrsPart);
    }
  }

  /**
   * Given an MGRS {@link MgrsPartType#NUMERIC} part format the string to ensure an accurate
   * conversion, using whitespace as a hint as to the user's intentions.
   *
   * <p>The usng4j library demands a minimum of 6 symbols if the easting / northing data is not to
   * be ignored. That is, the easting component must be fully specified for the conversion to work.
   * To accomodate this, zeros are prepended onto the easting to whatever appropriate degree.
   *
   * <p>For example, an input of <span>4Q KJ 1 2</span> will become <span>4QKJ000012</span> so the
   * easting / northing is not ignored.
   *
   * <p>Refer to the unit tests for more comprehensive examples.
   *
   * <p>TODO DDF-4243 - This should be simplified if usng4j is improved
   *
   * @see #assemble(String, String)
   * @param mgrsPart an MGRS substring that contains BOTH the easting and northing.
   * @return a modified easting/northing string with zeros backfilled to ensure accurate conversion.
   */
  @SuppressWarnings("squid:S1135" /* Ticket number provided on to-do comment */)
  private static String processNumericType(String mgrsPart) {
    final String[] numericParts = mgrsPart.split("(\\W)+");
    if (numericParts.length >= 3 || numericParts.length == 0) {
      throw new IllegalArgumentException(
          "The mgrs part provided is not a valid NUMERIC type: " + mgrsPart);
    }
    if (numericParts.length == 2) {
      return assemble(numericParts[0], numericParts[1]);
    }
    String eastingAndNorthing = numericParts[0];
    if (eastingAndNorthing.length() % 2 > 0) {
      eastingAndNorthing = eastingAndNorthing.substring(0, eastingAndNorthing.length() - 1);
    }
    final int halfway = eastingAndNorthing.length() / 2;
    final String easting = eastingAndNorthing.substring(0, halfway);
    final String northing = eastingAndNorthing.substring(halfway);
    return assemble(easting, northing);
  }

  private static String assemble(String easting, String northing) {
    return leftPad(easting, MAX_COORDINATE_LENGTH, ZERO)
        + leftPad(northing, MAX_COORDINATE_LENGTH, ZERO);
  }

  private static MgrsPartType getPartType(String mgrsPart) {
    if (PATTERN_MGRS_ZONE.matcher(mgrsPart).matches()) return MgrsPartType.ZONE;
    if (PATTERN_MGRS_100KM.matcher(mgrsPart).matches()) return MgrsPartType.HUNDRED_KM;
    if (PATTERN_MGRS_NUMERIC.matcher(mgrsPart).matches()) return MgrsPartType.NUMERIC;
    return MgrsPartType.NONE;
  }
}
