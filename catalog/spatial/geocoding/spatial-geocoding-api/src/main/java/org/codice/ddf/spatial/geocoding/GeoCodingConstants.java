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
package org.codice.ddf.spatial.geocoding;

import com.google.common.collect.ImmutableList;
import java.util.List;

public final class GeoCodingConstants {
  public static final String ADMINISTRATIVE_DIVISION = "ADM";

  public static final String DIVISION_FIRST_ORDER = "1";

  public static final String DIVISION_SECOND_ORDER = "2";

  public static final String DIVISION_THIRD_ORDER = "3";

  public static final String DIVISION_FOURTH_ORDER = "4";

  public static final String DIVISION_FIFTH_ORDER = "5";

  public static final String POLITICAL_ENTITY = "PCL";

  public static final String POLITICAL_ENTITY_DEPENDENT = "PCLD";

  public static final String POLITICAL_ENTITY_FREELY_ASSOCIATED = "PCLF";

  public static final String POLITICAL_ENTITY_INDEPENDENT = "PCLI";

  public static final String POLITICAL_ENTITY_SEMI_INDEPENDENT = "PCLS";

  public static final String POPULATED_PLACE = "PPL";

  public static final String SEAT_FIRST_ORDER = "A";

  public static final String SEAT_SECOND_ORDER = "A2";

  public static final String SEAT_THIRD_ORDER = "A3";

  public static final String SEAT_FOURTH_ORDER = "A4";

  public static final String CAPITAL = "C";

  public static final String COUNTRY_TAG = "country-shape";

  public static final String GEONAMES_TAG = "geonames";

  public static final String GAZETTEER_METACARD_TAG = "gazetteer";

  public static final String SUGGEST_PLACE_KEY = "suggestPlace";

  // The GeoNames feature codes for cities, excluding cities that no longer exist or that have
  // been destroyed.
  public static final List<String> CITY_FEATURE_CODES =
      ImmutableList.of(
          "PPL", "PPLA", "PPLA2", "PPLA3", "PPLA4", "PPLC", "PPLCH", "PPLF", "PPLG", "PPLL", "PPLR",
          "PPLS", "PPLX");

  public static final List<String> COUNTRY_FEATURE_CODES =
      ImmutableList.of(
          POLITICAL_ENTITY,
          POLITICAL_ENTITY_DEPENDENT,
          POLITICAL_ENTITY_FREELY_ASSOCIATED,
          POLITICAL_ENTITY_INDEPENDENT,
          POLITICAL_ENTITY_SEMI_INDEPENDENT);

  private GeoCodingConstants() {}
}
