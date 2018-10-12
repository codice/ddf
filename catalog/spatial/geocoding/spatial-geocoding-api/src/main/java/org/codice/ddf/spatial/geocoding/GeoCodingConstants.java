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
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;

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

  public static final String GEOMETRY_TYPE = "geometryType";

  public static final String GAZETTEER_METACARD_TAG = "gazetteer";

  public static final String SUGGEST_PLACE_KEY = "suggestPlace";

  public static final int COUNTRY_GAZETTEER_SORT_VALUE = Integer.MAX_VALUE;

  public static final int MAXIMUM_GAZETTEER_SORT_VALUE = Integer.MAX_VALUE - 1;

  public static final int SPECIAL_GAZETTEER_SORT_VALUE = 8;

  public static final String ADMIN_FEATURE_CLASS = "A";

  public static final String HYDROGRAPHIC_FEATURE_CLASS = "H";

  public static final String AREA_FEATURE_CLASS = "L";

  public static final String POPULATED_FEATURE_CLASS = "P";

  public static final String ROAD_FEATURE_CLASS = "R";

  public static final String SPOT_FEATURE_CLASS = "S";

  public static final String MOUNTAIN_FEATURE_CLASS = "T";

  public static final String UNDERSEA_FEATURE_CLASS = "U";

  public static final String VEGETATION_FEATURE_CLASS = "V";

  public static final String OCEAN_FEATURE_CODE = "OCN";

  public static final String SEA_FEATURE_CODE = "SEA";

  public static final String MOUNTAIN_FEATURE_CODE = "MT";

  public static final String MOUNTAIN_RANGE_FEATURE_CODE = "MTS";

  // The GeoNames feature codes for cities, excluding cities that no longer exist or that have
  // been destroyed.
  public static final List<String> CITY_FEATURE_CODES =
      ImmutableList.of(
          "PPL", "PPLA", "PPLA2", "PPLA3", "PPLA4", "PPLC", "PPLCH", "PPLF", "PPLG", "PPLL", "PPLR",
          "PPLS", "PPLX");

  public static final Map<String, Integer> FEATURE_CLASS_VALUES =
      new ImmutableMap.Builder<String, Integer>()
          .put(ADMIN_FEATURE_CLASS, MAXIMUM_GAZETTEER_SORT_VALUE)
          .put(POPULATED_FEATURE_CLASS, 8)
          .put(HYDROGRAPHIC_FEATURE_CLASS, 7)
          .put(AREA_FEATURE_CLASS, 6)
          .put(ROAD_FEATURE_CLASS, 5)
          .put(SPOT_FEATURE_CLASS, 4)
          .put(MOUNTAIN_FEATURE_CLASS, 3)
          .put(UNDERSEA_FEATURE_CLASS, 2)
          .put(VEGETATION_FEATURE_CLASS, 1)
          .build();

  public static final List<String> COUNTRY_FEATURE_CODES =
      ImmutableList.of(
          POLITICAL_ENTITY,
          POLITICAL_ENTITY_DEPENDENT,
          POLITICAL_ENTITY_FREELY_ASSOCIATED,
          POLITICAL_ENTITY_INDEPENDENT,
          POLITICAL_ENTITY_SEMI_INDEPENDENT);

  private GeoCodingConstants() {}
}
