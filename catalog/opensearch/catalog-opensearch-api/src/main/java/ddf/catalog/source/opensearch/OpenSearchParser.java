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
package ddf.catalog.source.opensearch;

import ddf.catalog.impl.filter.SpatialDistanceFilter;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.Subject;
import java.util.List;
import java.util.Map;
import org.apache.cxf.jaxrs.client.WebClient;

/**
 * An interface to transform a {@link QueryRequest} into an OpenSearch URL
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface OpenSearchParser {

  /**
   * Populates general site information.
   *
   * @param client - OpenSearch URL to populate
   * @param queryRequest - The query request from which to populate the search options
   * @param subject - The subject associated with the query
   * @param parameters - the given OpenSearch parameters
   */
  void populateSearchOptions(
      WebClient client, QueryRequest queryRequest, Subject subject, List<String> parameters);

  /**
   * Fills in the OpenSearch query URL with contextual information (Note: Section 2.2 - Query: The
   * OpenSearch specification does not define a syntax for its primary query parameter, searchTerms,
   * but it is generally used to support simple keyword queries.)
   *
   * @param client - OpenSearch URL to populate
   * @param searchPhraseMap - a map of search queries
   * @param parameters - the given OpenSearch parameters
   */
  void populateContextual(
      WebClient client, Map<String, String> searchPhraseMap, List<String> parameters);

  /**
   * Fills in the OpenSearch query URL with temporal information (Start, End, and Name). Currently
   * name is empty due to incompatibility with endpoints.
   *
   * @param client - OpenSearch URL to populate
   * @param temporal - the TemporalFilter that contains the temporal information
   * @param parameters - the given OpenSearch parameters
   */
  void populateTemporal(WebClient client, TemporalFilter temporal, List<String> parameters);

  /**
   * Fills in the OpenSearch query URL with geospatial information (poly, lat, lon, and radius).
   *
   * @param client - OpenSearch URL to populate
   * @param spatial - SpatialDistanceFilter that contains the spatial data
   * @param shouldConvertToBBox - true if the SpatialFilter should be converted to a Bounding Box
   * @param parameters - the given OpenSearch parameters
   * @throws UnsupportedQueryException
   */
  void populateGeospatial(
      WebClient client,
      SpatialDistanceFilter spatial,
      boolean shouldConvertToBBox,
      List<String> parameters);

  /**
   * Fills in the OpenSearch query URL with geospatial information (poly, lat, lon, and radius).
   *
   * @param client - OpenSearch URL to populate
   * @param spatial - SpatialFilter that contains the spatial data
   * @param shouldConvertToBBox - true if the SpatialFilter should be converted to a Bounding Box
   * @param parameters - the given OpenSearch parameters
   * @throws UnsupportedQueryException
   */
  void populateGeospatial(
      WebClient client,
      SpatialFilter spatial,
      boolean shouldConvertToBBox,
      List<String> parameters);
}
