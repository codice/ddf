/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.endpoints;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * OpenSearch endpoint interface.
 */
@Path("/")
public interface OpenSearch {

    public static final String PHRASE = "q";

    public static final String MAX_RESULTS = "mr";

    public static final String SOURCES = "src";

    public static final String MAX_TIMEOUT = "mt";

    public static final String START_INDEX = "start";

    public static final String COUNT = "count";

    public static final String BBOX = "bbox";

    public static final String POLYGON = "polygon";

    public static final String GEOMETRY = "geometry";

    public static final String LAT = "lat";

    public static final String LON = "lon";

    public static final String RADIUS = "radius";

    public static final String DATE_START = "dtstart";

    public static final String DATE_END = "dtend";

    public static final String DATE_OFFSET = "dtoffset";

    public static final String TYPE = "type";

    public static final String VERSION = "version";

    public static final String SELECTOR = "selector";

    public static final String SORT = "sort";

    public static final String FORMAT = "format";

    @GET
    public Response processQuery(@QueryParam(PHRASE)
    String searchTerms, @QueryParam(MAX_RESULTS)
    String maxResults, @QueryParam(SOURCES)
    String sources, @QueryParam(MAX_TIMEOUT)
    String maxTimeout, @QueryParam(START_INDEX)
    String startIndex, @QueryParam(COUNT)
    String count, @QueryParam(GEOMETRY)
    String geometry, @QueryParam(BBOX)
    String bbox, @QueryParam(POLYGON)
    String polygon, @QueryParam(LAT)
    String lat, @QueryParam(LON)
    String lon, @QueryParam(RADIUS)
    String radius, @QueryParam(DATE_START)
    String dateStart, @QueryParam(DATE_END)
    String dateEnd, @QueryParam(DATE_OFFSET)
    String dateOffset, @QueryParam(SORT)
    String sort, @QueryParam(FORMAT)
    String format, @QueryParam(SELECTOR)
    String selector, @Context
    UriInfo ui, @QueryParam(TYPE)
    String type, @QueryParam(VERSION)
    String versions, @Context
    HttpServletRequest request);
}
