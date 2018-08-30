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
package org.codice.ddf.catalog.ui.filter.json;

import java.util.Map;

/**
 * Represents a transformation between a canonical JSON form of a filter function and its simplified
 * counterpart that the UI understands. This is useful during refactoring efforts to keep cascading
 * changes from reaching the UI. It also can keep the level of filter function nesting under control
 * by enabling simpler data structures, and presenting filter function args as maps, not lists.
 *
 * <p>Since Filter 2.0 supports the nesting of functions to formally extend the data structure as an
 * application demands it, the JSON exchanged between the UI and backend can become arbitrarily
 * complex and contain redundant information. There are several cases where collapsing the JSON into
 * a friendlier format is desired. It is a lot easier to work with something like this
 *
 * <pre>
 * {
 *     "type": "INTERSECTS",
 *     "property": "anyGeo",
 *     "value": [{
 *         "type": "GEO_JSON",
 *         "data": [
 *             -68.8568115234375,
 *             0.2197260239207992,
 *             65.89462280273439,
 *             -7.117245472370016
 *         ],
 *     },
 *     {
 *         "type": "UTM",
 *         "data": {
 *             "easting": 401514.398778453,
 *             "northing": 4337961.542420644,
 *             "zone": 13,
 *             "hemisphere": "Northern"
 *         }
 *     },
 *     {
 *         "type": "UTM",
 *         "data": {
 *             "easting": 692525.4862824995,
 *             "northing": 4132713.3248771424,
 *             "zone": 13,
 *             "hemisphere": "Northern"
 *         }
 *      }]
 * }
 * </pre>
 *
 * instead of something like this
 *
 * <pre>
 * {
 *     "type": "INTERSECTS",
 *     "property": "anyGeo",
 *     "value": {
 *         "type": "FILTER_FUNCTION",
 *         "name": "meta.geo.bbox.1",
 *         "params": {
 *             "value": [
 *                 -68.8568115234375,
 *                 0.2197260239207992,
 *                 65.89462280273439,
 *                 -7.117245472370016
 *             ],
 *             "cornerOne": {
 *                 "type": "FILTER_FUNCTION",
 *                 "name": "meta.geo.coords.utm.1",
 *                 "params": {
 *                     "easting": 401514.398778453,
 *                     "northing": 4337961.542420644,
 *                     "zone": 13,
 *                     "hemisphere": "Northern"
 *                 }
 *             },
 *             "cornerTwo": {
 *                 "type": "FILTER_FUNCTION",
 *                 "name": "meta.geo.coords.utm.1",
 *                 "params": {
 *                     "easting": 692525.4862824995,
 *                     "northing": 4132713.3248771424,
 *                     "zone": 13,
 *                     "hemisphere": "Northern"
 *                 }
 *             }
 *         }
 *     }
 * }
 * </pre>
 */
public interface JsonFunctionTransform {

  /**
   * Given a piece of custom JSON within a JSON filter, return true if this transform can be
   * performed on the custom JSON to yield a valid filter predicate.
   *
   * @param customJson a JSON blob the UI is able to handle.
   * @return a valid filter predicate in filter JSON format.
   */
  boolean canApplyToFunction(Map<String, Object> customJson);

  /**
   * Given a valid filter predicate in filter JSON, return true if this transform can be performed
   * on the filter JSON to yield a valid piece of custom JSON for the UI to consume.
   *
   * @param filterJson a valid filter predicate in filter JSON format.
   * @return a JSON blob the UI is able to handle.
   */
  boolean canApplyFromFunction(Map<String, Object> filterJson);

  /**
   * Perform a transform from a filter predicate with a customized format to a filter predicate with
   * a schema-compliant filter function format.
   *
   * @param customJson the json to transform.
   * @return the result of the transform.
   */
  Map<String, Object> toFunction(Map<String, Object> customJson);

  /**
   * Perform a transform from a filter predicate with a schema-compliant filter function format to a
   * filter predicate with a customized format.
   *
   * @param filterJson the json to transform.
   * @return the result of the transform.
   */
  Map<String, Object> fromFunction(Map<String, Object> filterJson);
}
