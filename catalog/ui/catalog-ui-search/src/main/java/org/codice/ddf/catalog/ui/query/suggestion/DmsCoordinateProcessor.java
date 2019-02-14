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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  private static final String DMS_MINUTES_SECONDS_REGEX_STRING =
      "\\D*(" + MINUTES_REGEX_STRING + ")\\D*(" + SECONDS_REGEX_STRING + ")\\D*";

  private static final String DMS_LAT_REGEX_STRING =
      "\\D*(" + DEGREES_LAT_REGEX_STRING + ")" + DMS_MINUTES_SECONDS_REGEX_STRING + "([NnSs])\\D*";

  private static final String DMS_LON_REGEX_STRING =
      "\\D*(" + DEGREES_LON_REGEX_STRING + ")" + DMS_MINUTES_SECONDS_REGEX_STRING + "([EeWw])\\D*";

  private static final int DEGREES_LAT_GROUP = 1;
  private static final int MINUTES_LAT_GROUP = 2;
  private static final int SECONDS_LAT_GROUP = 3;
  private static final int DIRECTION_LAT_GROUP = 4;

  private static final int DEGREES_LON_GROUP = 5;
  private static final int MINUTES_LON_GROUP = 6;
  private static final int SECONDS_LON_GROUP = 7;
  private static final int DIRECTION_LON_GROUP = 8;

  private static final Pattern PATTERN_DMS_COORDINATE =
      Pattern.compile(DMS_LAT_REGEX_STRING + DMS_LON_REGEX_STRING);

  private static class CoordinateTranslator {

    private static void parseDms(
        final Map<String, Object> dmsLat,
        final Map<String, Object> dmsLon,
        final String dmsString) {
      final Matcher matcher = PATTERN_DMS_COORDINATE.matcher(dmsString);
      if (matcher.matches()) {
        dmsLat.put("degrees", Integer.parseInt(matcher.group(DEGREES_LAT_GROUP)));
        dmsLat.put("minutes", Integer.parseInt(matcher.group(MINUTES_LAT_GROUP)));
        dmsLat.put("seconds", Double.parseDouble(matcher.group(SECONDS_LAT_GROUP)));
        dmsLat.put("direction", matcher.group(DIRECTION_LAT_GROUP));
        dmsLat.put("degreeFormat", new DecimalFormat("00"));

        dmsLon.put("degrees", Integer.parseInt(matcher.group(DEGREES_LON_GROUP)));
        dmsLon.put("minutes", Integer.parseInt(matcher.group(MINUTES_LON_GROUP)));
        dmsLon.put("seconds", Double.parseDouble(matcher.group(SECONDS_LON_GROUP)));
        dmsLon.put("direction", matcher.group(DIRECTION_LON_GROUP));
        dmsLon.put("degreeFormat", new DecimalFormat("000"));
      }
    }

    private static String normalizedDmsString(final String dmsString) {
      final Map<String, Object> dmsLat = new HashMap<String, Object>();
      final Map<String, Object> dmsLon = new HashMap<String, Object>();
      parseDms(dmsLat, dmsLon, dmsString);

      final NumberFormat minutesSecondsFormat = new DecimalFormat("00.###");

      final StringBuilder dmsBuilder = new StringBuilder();
      for (Map<String, Object> dmsPart : Arrays.asList(dmsLat, dmsLon)) {

        final NumberFormat degreeFormat = (NumberFormat) dmsPart.get("degreeFormat");
        final String degrees = degreeFormat.format((int) dmsPart.get("degrees"));
        final String minutes = minutesSecondsFormat.format((int) dmsPart.get("minutes"));
        final String seconds = minutesSecondsFormat.format((double) dmsPart.get("seconds"));
        final String direction = dmsPart.get("direction").toString().toUpperCase();

        dmsBuilder
            .append(degrees)
            .append("°")
            .append(minutes)
            .append("\'")
            .append(seconds)
            .append("\"")
            .append(direction)
            .append(" ");
      }
      return dmsBuilder.toString().trim();
    }

    private static LatLon dmsToLatLon(final String dmsString) {
      final Map<String, Object> dmsLat = new HashMap<String, Object>();
      final Map<String, Object> dmsLon = new HashMap<String, Object>();
      parseDms(dmsLat, dmsLon, dmsString);

      final boolean dmsLatExists =
          dmsLat.containsKey("degrees")
              && dmsLat.containsKey("minutes")
              && dmsLat.containsKey("seconds");
      final boolean dmsLonExists =
          dmsLon.containsKey("degrees")
              && dmsLon.containsKey("minutes")
              && dmsLon.containsKey("seconds");
      return (dmsLatExists && dmsLonExists) ? toLatLon(dmsLat, dmsLon) : null;
    }

    private static LatLon toLatLon(
        final Map<String, Object> dmsLat, final Map<String, Object> dmsLon) {
      final int latModifier = dmsLat.get("direction").toString().toUpperCase().equals("N") ? 1 : -1;
      final int lonModifier = dmsLon.get("direction").toString().toUpperCase().equals("E") ? 1 : -1;
      final Double lat = toDecimalDegrees(dmsLat) * latModifier;
      final Double lon = toDecimalDegrees(dmsLon) * lonModifier;

      return (LatLon.isValidLatitude(lat) && LatLon.isValidLongitude(lon))
          ? new LatLon(lat, lon)
          : null;
    }

    private static Double toDecimalDegrees(final Map<String, Object> dms) {
      final int degrees = (int) dms.get("degrees");
      final int minutes = (int) dms.get("minutes");
      final double seconds = (double) dms.get("seconds");

      return degrees + minutes / 60.0 + seconds / 3600.0;
    }
  }

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
  private LiteralSuggestion getDmsSuggestion(final String query) {
    final List<LatLon> dmsCoordinates = new ArrayList<>();
    final Matcher matcher = PATTERN_DMS_COORDINATE.matcher(query);
    final StringBuilder nameBuilder = new StringBuilder("DMS:");

    int start = 0;
    while (matcher.find()) {
      LOGGER.trace("Match found [{}]", matcher.group());
      final LatLon parsedDms = CoordinateTranslator.dmsToLatLon(query.substring(start, matcher.end()));
      start = matcher.end();
      if (parsedDms != null) {
        dmsCoordinates.add(parsedDms);
        nameBuilder
            .append(" [ ")
            .append(CoordinateTranslator.normalizedDmsString(matcher.group()))
            .append(" ]");
      }
    }
    if (dmsCoordinates.isEmpty()) {
      LOGGER.trace("No valid DMS strings could be inferred from query [{}]", query);
      return null;
    }
    return new LiteralSuggestion(LITERAL_SUGGESTION_ID, nameBuilder.toString(), dmsCoordinates);
  }
}
