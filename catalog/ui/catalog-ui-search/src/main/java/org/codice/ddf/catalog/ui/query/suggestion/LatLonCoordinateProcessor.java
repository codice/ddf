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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.codice.ddf.spatial.geocoding.Suggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LatLonCoordinateProcessor} enables search suggestions for Lat/Lon coordinate literals
 * whenever a pair of decimal numbers is encountered within the expected bounds. The general policy
 * is to be as forgiving as possible without sacrificing determinism.
 */
public class LatLonCoordinateProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(LatLonCoordinateProcessor.class);

  private static final String REGEX_LAT_LON = "[-]?(([\\d]+(\\.[\\d]+)?)|(\\.[\\d]+))";

  private static final Pattern PATTERN_LAT_LON = Pattern.compile(REGEX_LAT_LON);

  // This key tells the UI that the geo is on the suggestion itself
  private static final String LITERAL_SUGGESTION_ID = "LITERAL-LAT-LON";

  /**
   * Given a list of {@link Suggestion}s and the query that yielded them, enhance the list with
   * additional suggestions based upon the presence of coordinate literals in the query string.
   *
   * @param results the list to add results to.
   * @param query the query string with potential coordinate literals present.
   */
  public void enhanceResults(List<Suggestion> results, String query) {
    final LiteralSuggestion literal = getLatLonSuggestions(query);
    if (literal != null && literal.hasGeo()) {
      LOGGER.trace("Adding the Lat/Lon suggestion to results [{}]", literal);
      results.add(0, literal);
    }
  }

  private LiteralSuggestion getLatLonSuggestions(String query) {
    Matcher matcher = PATTERN_LAT_LON.matcher(query);
    List<Double> numbers = new ArrayList<>();
    while (matcher.find()) {
      numbers.add(Double.parseDouble(matcher.group()));
    }
    if (numbers.size() <= 1) {
      LOGGER.trace("Not enough numbers found to suggest a Lat/Lon");
      return null;
    }
    List<LatLon> latLonList = getLatLonList(numbers);
    String name =
        "Lat/Lon: " + latLonList.stream().map(LatLon::toString).collect(Collectors.joining(", "));
    return new LiteralSuggestion(LITERAL_SUGGESTION_ID, name, latLonList);
  }

  private List<LatLon> getLatLonList(List<Double> numbers) {
    List<LatLon> latLons = new ArrayList<>();
    for (int index = 0; index < numbers.size(); index++) {
      double latitude = numbers.get(index);
      if (LatLon.isValidLatitude(latitude) && index + 1 < numbers.size()) {

        double longitude = numbers.get(index + 1);
        if (LatLon.isValidLongitude(longitude)) {
          latLons.add(new LatLon(latitude, longitude));
          ++index;
        }
      }
    }
    return latLons;
  }
}
