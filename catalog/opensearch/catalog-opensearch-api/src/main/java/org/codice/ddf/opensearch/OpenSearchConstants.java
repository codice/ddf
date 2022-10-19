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
package org.codice.ddf.opensearch;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;

public final class OpenSearchConstants {

  // contextual parameter
  public static final String SEARCH_TERMS = "q";
  public static final String SEARCH_TERMS_DELIMITER = " ";

  // temporal parameters
  public static final String SUPPORTED_TEMPORAL_SEARCH_TERM = Core.MODIFIED;

  public static final String DATE_START = "dtstart";

  public static final String DATE_END = "dtend";

  public static final String DATE_OFFSET = "dtoffset";

  // geospatial parameters
  public static final String SUPPORTED_SPATIAL_SEARCH_TERM = Metacard.ANY_GEO;

  public static final String BBOX = "bbox";
  public static final String BBOX_DELIMITER = ",";

  public static final String POLYGON = "polygon";
  public static final String POLYGON_LON_LAT_DELIMITER = ",";

  public static final String GEOMETRY = "geometry";

  public static final String LAT = "lat";

  public static final String LON = "lon";

  public static final String RADIUS = "radius";

  // other parameters
  public static final String MAX_RESULTS = "mr";

  public static final String SOURCES = "src";
  public static final String SOURCES_DELIMITER = ",";
  public static final String LOCAL_SOURCE = "local";

  public static final String MAX_TIMEOUT = "mt";

  public static final String START_INDEX = "start";

  public static final String COUNT = "count";

  public static final String TYPE = "type";

  public static final String VERSIONS = "version";
  public static final String VERSIONS_DELIMITER = ",";

  public static final String SELECTORS = "selector";
  public static final String SELECTORS_DELIMITER = ",";

  public static final String SORT = "sort";
  public static final String ORDER_ASCENDING = "asc";
  public static final String ORDER_DESCENDING = "desc";
  public static final String SORT_DELIMITER = ":";
  public static final String SORT_RELEVANCE = "relevance";
  public static final String SORT_TEMPORAL = "date";

  public static final String FORMAT = "format";
  public static final String RECORD_IDS = "recordIds";

  private OpenSearchConstants() {}
}
