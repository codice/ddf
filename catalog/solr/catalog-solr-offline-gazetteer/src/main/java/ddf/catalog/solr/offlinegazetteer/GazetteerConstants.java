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
  public static final String SUGGEST_Q = "suggest.q";
  public static final String SUGGEST_DICT = "suggest.dictionary";
  public static final String SUGGEST_DICT_VALUE = "suggestPlace";

  /* Naming */
  public static final String GAZETTEER_METACARD_TAG = GeoCodingConstants.GAZETTEER_METACARD_TAG;
  public static final String STANDALONE_GAZETTEER_CORE_NAME = "standalone-solr-gazetteer";

  public static final BiMap<String, String> NAMES =
      new ImmutableBiMap.Builder<String, String>()
          .put(Core.DESCRIPTION, "description_txt")
          .put(GeoEntryAttributes.FEATURE_CODE_ATTRIBUTE_NAME, "feature-code_txt")
          .put(Core.TITLE, "title_txt")
          .put(Core.ID, "id_txt")
          .put(Location.COUNTRY_CODE, "country-code_txt")
          .put(Core.LOCATION, "location_geo")
          .put(GeoEntryAttributes.POPULATION_ATTRIBUTE_NAME, "population_lng")
          .put(GeoEntryAttributes.GAZETTEER_SORT_VALUE, "gazetteer-sort-value_int")
          .build();

  private GazetteerConstants() {}
}
