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

import com.google.common.annotations.VisibleForTesting;
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
    if (numbers.isEmpty() || numbers.size() == 1) {
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
      if (isValidLatitude(latitude)) {
        if (index + 1 < numbers.size()) {
          double longitude = numbers.get(index + 1);
          if (isValidLongitude(longitude)) {
            latLons.add(LatLon.from(latitude, longitude));
            ++index;
          }
        }
      }
    }
    return latLons;
  }

  private boolean isValidLatitude(double latitude) {
    return latitude >= -90 && latitude <= 90;
  }

  private boolean isValidLongitude(double longitude) {
    return longitude >= -180 && longitude <= 180;
  }

  @VisibleForTesting
  static class LiteralSuggestion implements Suggestion {
    private final String id;
    private final String name;
    private final List<LatLon> geo;

    LiteralSuggestion(String id, String name, List<LatLon> geo) {
      this.id = id;
      this.name = name;
      this.geo = geo;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public String getName() {
      return name;
    }

    public List<LatLon> getGeo() {
      return geo;
    }

    public boolean hasGeo() {
      return geo != null && !geo.isEmpty();
    }

    @Override
    public String toString() {
      return getName();
    }
  }

  @VisibleForTesting
  static class LatLon {
    private final Double lat;
    private final Double lon;

    static LatLon from(Double lat, Double lon) {
      return new LatLon(lat, lon);
    }

    private LatLon(Double lat, Double lon) {
      notNull(lat);
      notNull(lon);
      this.lat = lat;
      this.lon = lon;
    }

    public Double getLat() {
      return lat;
    }

    public Double getLon() {
      return lon;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LatLon latLon = (LatLon) o;
      return lat.equals(latLon.lat) && lon.equals(latLon.lon);
    }

    @Override
    public int hashCode() {
      int result = lat.hashCode();
      result = 31 * result + lon.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "(" + getLat() + ", " + getLon() + ")";
    }
  }
}
