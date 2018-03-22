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
package org.codice.ddf.endpoints;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/** OpenSearch endpoint interface. */
@Path("/")
public interface OpenSearch {

  String PHRASE = "q";

  String MAX_RESULTS = "mr";

  String SOURCES = "src";

  String MAX_TIMEOUT = "mt";

  String START_INDEX = "start";

  String COUNT = "count";

  String BBOX = "bbox";

  String POLYGON = "polygon";

  String GEOMETRY = "geometry";

  String LAT = "lat";

  String LON = "lon";

  String RADIUS = "radius";

  String DATE_START = "dtstart";

  String DATE_END = "dtend";

  String DATE_OFFSET = "dtoffset";

  String TYPE = "type";

  String VERSION = "version";

  String SELECTOR = "selector";

  String SORT = "sort";

  String FORMAT = "format";

  @GET
  Response processQuery(
      @QueryParam(PHRASE) String searchTerms,
      @QueryParam(MAX_RESULTS) String maxResults,
      @QueryParam(SOURCES) String sources,
      @QueryParam(MAX_TIMEOUT) String maxTimeout,
      @QueryParam(START_INDEX) String startIndex,
      @QueryParam(COUNT) String count,
      @QueryParam(GEOMETRY) String geometry,
      @QueryParam(BBOX) String bbox,
      @QueryParam(POLYGON) String polygon,
      @QueryParam(LAT) String lat,
      @QueryParam(LON) String lon,
      @QueryParam(RADIUS) String radius,
      @QueryParam(DATE_START) String dateStart,
      @QueryParam(DATE_END) String dateEnd,
      @QueryParam(DATE_OFFSET) String dateOffset,
      @QueryParam(SORT) String sort,
      @QueryParam(FORMAT) String format,
      @QueryParam(SELECTOR) String selector,
      @Context UriInfo ui,
      @QueryParam(TYPE) String type,
      @QueryParam(VERSION) String versions,
      @Context HttpServletRequest request);
}
