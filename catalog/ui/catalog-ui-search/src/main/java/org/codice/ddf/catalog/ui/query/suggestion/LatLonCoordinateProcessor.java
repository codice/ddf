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
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.codice.ddf.spatial.geocoding.Suggestion;

/**
 * The {@link LatLonCoordinateProcessor} enables search suggestions for coordinate literals in a
 * variety of formats.
 */
public class LatLonCoordinateProcessor {
  private static final String REGEX_LAT_LON = "[-]?(([\\d]+(\\.[\\d]+)?)|(\\.[\\d]+))";

  private static final Pattern PATTERN_LAT_LON = Pattern.compile(REGEX_LAT_LON);

  /**
   * Given a list of {@link Suggestion}s and the query that yielded them, enhance the list with
   * additional suggestions based upon the presence of coordinate literals in the query string.
   *
   * @param results the list to add results to.
   * @param query the query string with potential coordinate literals present.
   */
  public List<Suggestion> enhanceResults(List<Suggestion> results, String query) {
    LinkedList<Suggestion> enhanced = new LinkedList<>(results);
    LiteralSuggestion literal = getLatLonSuggestions(query);
    if (literal != null && literal.hasGeo()) {
      enhanced.addFirst(literal);
    }
    return enhanced;
  }

  private LiteralSuggestion getLatLonSuggestions(String query) {
    Matcher matcher = PATTERN_LAT_LON.matcher(query);
    List<Double> numbers = new ArrayList<>();
    while (matcher.find()) {
      numbers.add(Double.parseDouble(matcher.group()));
    }
    if (numbers.size() <= 1) {
      return null;
    }
    List<LatLon> latLonList = getLatLonList(numbers);
    String name =
        "Lat/Lon: " + latLonList.stream().map(LatLon::toString).collect(Collectors.joining(", "));
    return new LiteralSuggestion("LITERAL", name, latLonList);
  }

  private List<LatLon> getLatLonList(List<Double> numbers) {
    List<LatLon> latLons = new ArrayList<>();
    for (int index = 0; index < numbers.size(); index++) {
      double latitude = numbers.get(index);
      if (LatLon.isValidLatitude(latitude) && index + 1 < numbers.size()) {

        double longitude = numbers.get(index + 1);
        if (LatLon.isValidLongitude(longitude)) {
          latLons.add(LatLon.from(latitude, longitude));
          ++index;
        }
      }
    }
    return latLons;
  }
}
