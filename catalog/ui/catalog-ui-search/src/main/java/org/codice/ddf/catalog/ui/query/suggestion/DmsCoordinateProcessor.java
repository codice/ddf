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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.codice.ddf.spatial.geocoding.Suggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A regex-based processor for identifying potential DMS coordinate literals in a given input string
 * despite deviations from the official spec. The general policy is to be as forgiving as possible
 * without sacrificing determinism.
 */
public class DmsCoordinateProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(DmsCoordinateProcessor.class);

  private static final String DEGREES_LAT_REGEX_STRING = "90|[0-8]?[0-9]";
  private static final String DEGREES_LON_REGEX_STRING = "180|1[0-7][0-9]|0?[0-9]?[0-9]";

  private static final String MINUTES_REGEX_STRING = "[0-5]?[0-9]";
  private static final String SECONDS_REGEX_STRING = "[0-5]?[0-9](?:\\.\\d*)?";

  private static final String DMS_LAT_REGEX_STRING =
      "(?:\\d+\\D|\\D)*?("
          + DEGREES_LAT_REGEX_STRING
          + ")\\D+("
          + MINUTES_REGEX_STRING
          + ")\\D+("
          + SECONDS_REGEX_STRING
          + ")\\D*([NnSs])\\D*";

  private static final String DMS_LON_REGEX_STRING =
      "(?:\\d+\\D|\\D)*?("
          + DEGREES_LON_REGEX_STRING
          + ")\\D+("
          + MINUTES_REGEX_STRING
          + ")\\D+("
          + SECONDS_REGEX_STRING
          + ")\\D*([EeWw])\\D*";

  private static final int DEGREES_LAT_GROUP = 1;
  private static final int MINUTES_LAT_GROUP = 2;
  private static final int SECONDS_LAT_GROUP = 3;
  private static final int DIRECTION_LAT_GROUP = 4;

  private static final int DEGREES_LON_GROUP = 5;
  private static final int MINUTES_LON_GROUP = 6;
  private static final int SECONDS_LON_GROUP = 7;
  private static final int DIRECTION_LON_GROUP = 8;

  private static final Pattern PATTERN_DMS_COORDINATES =
      Pattern.compile(DMS_LAT_REGEX_STRING + DMS_LON_REGEX_STRING);

  // This key tells the UI that the geo is on the suggestion itself
  private static final String LITERAL_SUGGESTION_ID = "LITERAL-DMS";

  /**
   * Given a list of {@link Suggestion}s and the query that yielded them, enhance the list with
   * additional suggestions based upon the presence of coordinate literals in the query string.
   *
   * @param results the list of results to enhance.
   * @param query the user-provided gazetteer query text, which may contain coordinate literals.
   */
  public void enhanceResults(final List<Suggestion> results, final String query) {
    LOGGER.trace("(DMS) Adding result for query [{}]", query);
    final LiteralSuggestion dmsSuggestion = getDmsSuggestion(query);
    if (dmsSuggestion != null && dmsSuggestion.hasGeo()) {
      LOGGER.trace("Adding the DMS suggestion to results [{}]", dmsSuggestion);
      results.add(0, dmsSuggestion);
    }
    LOGGER.trace("(DMS) Done");
  }

  @Nullable
  private static LatLon dmsToLatLon(final String dmsString) {
    final DMSCoordinates dmsCoordinates = DMSCoordinates.parseDms(dmsString);
    if (dmsCoordinates == null) {
      return null;
    }

    final DMSComponent dmsLat = dmsCoordinates.lat;
    final DMSComponent dmsLon = dmsCoordinates.lon;
    final int latModifier = dmsLat.direction.equalsIgnoreCase("N") ? 1 : -1;
    final int lonModifier = dmsLon.direction.equalsIgnoreCase("E") ? 1 : -1;
    final Double lat = dmsLat.toDecimalDegrees() * latModifier;
    final Double lon = dmsLon.toDecimalDegrees() * lonModifier;

    return LatLon.createIfValid(lat, lon);
  }

  @Nullable
  private LiteralSuggestion getDmsSuggestion(final String query) {
    final List<LatLon> dmsCoordinates = new ArrayList<>();
    final Matcher matcher = PATTERN_DMS_COORDINATES.matcher(query);
    final StringBuilder nameBuilder = new StringBuilder("DMS:");

    int start = 0;
    while (matcher.find()) {
      final String dmsString = query.substring(start, matcher.end());
      final LatLon latLon = dmsToLatLon(dmsString);
      start = matcher.end();
      if (latLon != null) {
        LOGGER.trace("Match found [{}]", matcher.group());
        dmsCoordinates.add(latLon);
        nameBuilder
            .append(" [ ")
            .append(DMSCoordinates.normalizedDmsString(dmsString))
            .append(" ]");
      }
    }
    if (dmsCoordinates.isEmpty()) {
      LOGGER.trace("No valid DMS strings could be inferred from query [{}]", query);
      return null;
    }
    return new LiteralSuggestion(LITERAL_SUGGESTION_ID, nameBuilder.toString(), dmsCoordinates);
  }

  private static class DMSComponent {
    private final int degrees;
    private final int minutes;
    private final double seconds;
    private final String direction;
    private final DecimalFormat degreeFormat;

    private DMSComponent(
        String degrees,
        String minutes,
        String seconds,
        String direction,
        DecimalFormat degreeFormat) {
      this.degrees = Integer.parseInt(degrees);
      this.minutes = Integer.parseInt(minutes);
      this.seconds = Double.parseDouble(seconds);
      this.direction = direction;
      this.degreeFormat = degreeFormat;
    }

    private Double toDecimalDegrees() {
      return degrees + minutes / 60.0 + seconds / 3600.0;
    }
  }

  private static class DMSCoordinates {
    private DMSComponent lat;
    private DMSComponent lon;

    private DMSCoordinates(DMSComponent lat, DMSComponent lon) {
      this.lat = lat;
      this.lon = lon;
    }

    /**
     * Deserialize a DMS string.
     *
     * @param dmsString the DMS string to deserialize.
     * @return [dmsLat, dmsLon] The two deserialized parts of the DMS string.
     */
    @Nullable
    private static DMSCoordinates parseDms(final String dmsString) {
      final Matcher matcher = PATTERN_DMS_COORDINATES.matcher(dmsString);

      if (matcher.matches()) {
        final DMSComponent lat =
            new DMSComponent(
                matcher.group(DEGREES_LAT_GROUP),
                matcher.group(MINUTES_LAT_GROUP),
                matcher.group(SECONDS_LAT_GROUP),
                matcher.group(DIRECTION_LAT_GROUP),
                new DecimalFormat("00"));
        final DMSComponent lon =
            new DMSComponent(
                matcher.group(DEGREES_LON_GROUP),
                matcher.group(MINUTES_LON_GROUP),
                matcher.group(SECONDS_LON_GROUP),
                matcher.group(DIRECTION_LON_GROUP),
                new DecimalFormat("000"));
        return new DMSCoordinates(lat, lon);
      }
      return null;
    }

    private static String normalizedDmsString(final String dmsString) {
      final DMSCoordinates dmsCoordinates = parseDms(dmsString);
      final List<DMSComponent> dmsParts = Arrays.asList(dmsCoordinates.lat, dmsCoordinates.lon);
      final NumberFormat minutesSecondsFormat = new DecimalFormat("00.###");

      final StringBuilder dmsBuilder = new StringBuilder();
      for (DMSComponent dmsPart : dmsParts) {
        dmsBuilder
            .append(dmsPart.degreeFormat.format(dmsPart.degrees))
            .append("\u00B0")
            .append(minutesSecondsFormat.format(dmsPart.minutes))
            .append("\'")
            .append(minutesSecondsFormat.format(dmsPart.seconds))
            .append("\"")
            .append(dmsPart.direction.toUpperCase())
            .append(" ");
      }
      return dmsBuilder.toString().trim();
    }
  }
}
