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
package ddf.catalog.solr.offlinegazetteer;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Location;
import org.codice.ddf.spatial.geocoding.GeoCodingConstants;
import org.codice.ddf.spatial.geocoding.GeoEntryAttributes;

public class GazetteerConstants {
  /* Solr Requests */
  public static final String GAZETTEER_REQUEST_HANDLER = "/gazetteer";
  public static final String SUGGEST_Q_KEY = "suggest.q";
  public static final String SUGGEST_BUILD_KEY = "suggest.build";
  public static final String SUGGEST_DICT_KEY = "suggest.dictionary";
  public static final String SUGGEST_DICT = "gazetteerSuggest";

  /* Naming */
  public static final String GAZETTEER_METACARD_TAG = GeoCodingConstants.GAZETTEER_METACARD_TAG;
  public static final String COLLECTION_NAME = "gazetteer";

  public static final String DESCRIPTION = "description_txt";
  public static final String FEATURE_CODE = "feature-code_txt";
  public static final String NAME = "name_txt";
  public static final String ID = "id_txt";
  public static final String COUNTRY_CODE = "country-code_txt";
  public static final String LOCATION = "location_geo";
  public static final String POPULATION = "population_lng";
  public static final String SORT_VALUE = "sort-value_int";

  public static final BiMap<String, String> GAZETTEER_TO_CATALOG =
      new ImmutableBiMap.Builder<String, String>()
          .put(DESCRIPTION, Core.DESCRIPTION)
          .put(FEATURE_CODE, GeoEntryAttributes.FEATURE_CODE_ATTRIBUTE_NAME)
          .put(NAME, Core.TITLE)
          .put(ID, Core.ID)
          .put(COUNTRY_CODE, Location.COUNTRY_CODE)
          .put(LOCATION, Core.LOCATION)
          .put(POPULATION, GeoEntryAttributes.POPULATION_ATTRIBUTE_NAME)
          .put(SORT_VALUE, GeoEntryAttributes.GAZETTEER_SORT_VALUE)
          .build();

  private GazetteerConstants() {}
}
