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
package org.codice.ddf.opensearch.source;

import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.operation.QueryRequest;
import ddf.security.Subject;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.cxf.jaxrs.client.WebClient;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

/**
 * An interface to transform a {@link QueryRequest} into an OpenSearch URL
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface OpenSearchParser {

  /**
   * Populates general site information if the {@link QueryRequest} parameter is not null.
   *
   * @param client - OpenSearch URL to populate
   * @param queryRequest - The query request from which to populate the search options
   * @param subject - The subject associated with the query
   * @param parameters - the given OpenSearch parameters
   */
  void populateSearchOptions(
      WebClient client, QueryRequest queryRequest, Subject subject, List<String> parameters);

  /**
   * Fills in the OpenSearch query URL with the contextual information is contained in the search
   * phrase link Map<String, String>}. (Note: Section 2.2 - Query: The OpenSearch specification does
   * not define a syntax for its primary query parameter, searchTerms, but it is generally used to
   * support simple keyword queries.)
   *
   * @param client - OpenSearch URL to populate
   * @param searchPhraseMap - a map of search queries
   * @param parameters - the given OpenSearch parameters
   */
  void populateContextual(
      WebClient client, Map<String, String> searchPhraseMap, List<String> parameters);

  /**
   * Fills in the OpenSearch query URL with temporal information (Start, End, and Name) if the
   * {@link TemporalFilter} parameter is not null.
   *
   * @param client - OpenSearch URL to populate
   * @param temporal - the TemporalFilter that contains the temporal information
   * @param parameters - the given OpenSearch parameters
   */
  void populateTemporal(WebClient client, TemporalFilter temporal, List<String> parameters);

  /**
   * Fills in the OpenSearch query URL with polygon geospatial information if one of the spatial
   * search parameters is not null.
   *
   * @param client - OpenSearch URL to populate
   * @param parameters - the given OpenSearch parameters
   * @throws IllegalArgumentException if more than one of the search parameters is not null
   */
  void populateSpatial(
      WebClient client,
      @Nullable Geometry geometry,
      @Nullable BoundingBox boundingBox,
      @Nullable Polygon polygon,
      @Nullable PointRadius pointRadius,
      List<String> parameters);
}
