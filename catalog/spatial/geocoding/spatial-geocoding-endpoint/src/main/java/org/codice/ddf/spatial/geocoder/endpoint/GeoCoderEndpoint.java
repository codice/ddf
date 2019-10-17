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
package org.codice.ddf.spatial.geocoder.endpoint;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.apache.commons.text.StringEscapeUtils;
import org.codice.ddf.spatial.geocoding.GeoCoderService;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class GeoCoderEndpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeoCoderEndpoint.class);

  private GeoCoderService geoCoderService;

  public GeoCoderEndpoint(GeoCoderService geoCoderService) {
    this.geoCoderService = geoCoderService;
  }

  @GET
  public Response getLocation(
      @QueryParam("jsonp") String jsonp, @QueryParam("query") String query) {

    String jsonString = geoCoderService.getLocation(jsonp, query);
    if (jsonString != null) {
      return Response.ok(String.format("%s(%s)", StringEscapeUtils.escapeHtml4(jsonp), jsonString))
          .build();
    } else {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }

  @GET
  @Path("nearby/cities/{wkt}")
  public Response getNearbyCities(@PathParam("wkt") String wkt) {
    try {
      String jsonString = geoCoderService.getNearbyCities(wkt);

      if (jsonString != null) {
        return Response.ok(jsonString).build();
      } else {
        return Response.status(Response.Status.NO_CONTENT).build();
      }

    } catch (GeoEntryQueryException e) {
      LOGGER.debug("Error querying GeoNames resource with wkt:{}", wkt, e);
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }
}
