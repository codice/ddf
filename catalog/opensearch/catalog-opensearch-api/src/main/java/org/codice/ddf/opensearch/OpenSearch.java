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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/")
public interface OpenSearch {

  @GET
  Response processQuery(
      @QueryParam(OpenSearchConstants.SEARCH_TERMS) String searchTerms,
      @QueryParam(OpenSearchConstants.MAX_RESULTS) String maxResults,
      @QueryParam(OpenSearchConstants.SOURCES) String sources,
      @QueryParam(OpenSearchConstants.MAX_TIMEOUT) String maxTimeout,
      @QueryParam(OpenSearchConstants.START_INDEX) String startIndex,
      @QueryParam(OpenSearchConstants.COUNT) String count,
      @QueryParam(OpenSearchConstants.GEOMETRY) String geometry,
      @QueryParam(OpenSearchConstants.BBOX) String bbox,
      @QueryParam(OpenSearchConstants.POLYGON) String polygon,
      @QueryParam(OpenSearchConstants.LAT) String lat,
      @QueryParam(OpenSearchConstants.LON) String lon,
      @QueryParam(OpenSearchConstants.RADIUS) String radius,
      @QueryParam(OpenSearchConstants.DATE_START) String dateStart,
      @QueryParam(OpenSearchConstants.DATE_END) String dateEnd,
      @QueryParam(OpenSearchConstants.DATE_OFFSET) String dateOffset,
      @QueryParam(OpenSearchConstants.SORT) String sort,
      @QueryParam(OpenSearchConstants.FORMAT) String format,
      @QueryParam(OpenSearchConstants.SELECTORS) String selectors,
      @Context UriInfo ui,
      @QueryParam(OpenSearchConstants.TYPE) String type,
      @QueryParam(OpenSearchConstants.VERSIONS) String versions,
      @Context HttpServletRequest request);
}
