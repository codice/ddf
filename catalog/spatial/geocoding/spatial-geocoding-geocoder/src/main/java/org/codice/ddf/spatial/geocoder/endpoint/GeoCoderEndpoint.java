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

import ddf.catalog.util.impl.ServiceSelector;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.codice.ddf.spatial.geocoder.GeoCoder;
import org.codice.ddf.spatial.geocoder.GeoResult;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.codice.ddf.spatial.geocoding.context.NearbyLocation;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.primitive.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class GeoCoderEndpoint {

  private ServiceSelector<GeoCoder> geoCoderFactory;

  private static final Logger LOGGER = LoggerFactory.getLogger(GeoCoderEndpoint.class);

  public GeoCoderEndpoint(ServiceSelector<GeoCoder> geoCoderFactory) {

    if (geoCoderFactory == null) {
      throw new IllegalArgumentException(
          "GeoCoderEndpoint(): constructor argument 'geoCoderFactory' may not be null.");
    }

    this.geoCoderFactory = geoCoderFactory;
  }

  @GET
  public Response getLocation(
      @QueryParam("jsonp") String jsonp, @QueryParam("query") String query) {
    if (JsonpValidator.isValidJsonp(jsonp)) {
      JSONObject jsonObject = doQuery(query);
      return Response.ok(jsonp + "(" + jsonObject.toJSONString() + ")").build();
    } else {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }

  JSONObject doQuery(String query) {
    GeoCoder geoCoder = geoCoderFactory.getService();
    GeoResult geoResult = null;

    if (geoCoder != null) {
      geoResult = geoCoder.getLocation(query);
    }

    JSONObject jsonObject = new JSONObject();
    JSONArray resourceSets = new JSONArray();
    JSONObject resourceSet = new JSONObject();
    jsonObject.put("resourceSets", resourceSets);
    resourceSets.add(resourceSet);
    JSONArray resources = new JSONArray();
    resourceSet.put("resources", resources);

    if (geoResult != null) {
      transformGeoResult(geoResult, resources);
    }

    return jsonObject;
  }

  @GET
  @Path("nearby/cities/{wkt}")
  public Response getNearbyCities(@PathParam("wkt") String wkt) {

    GeoCoder geoCoder = geoCoderFactory.getService();
    try {
      NearbyLocation nearbyLocation = geoCoder.getNearbyCity(wkt);
      if (nearbyLocation != null) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("direction", nearbyLocation.getCardinalDirection());
        jsonObject.put("distance", nearbyLocation.getDistance());
        jsonObject.put("name", nearbyLocation.getName());
        return Response.ok(jsonObject.toJSONString()).build();
      } else {
        return Response.status(Response.Status.NO_CONTENT).build();
      }
    } catch (GeoEntryQueryException e) {
      LOGGER.debug("Error querying GeoNames resource with wkt:{}", wkt, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  void transformGeoResult(GeoResult geoResult, JSONArray resources) {
    DirectPosition directPosition = geoResult.getPoint().getDirectPosition();
    double[] coords = directPosition.getCoordinate();

    double longitude = coords[0];
    double latitude = coords[1];

    JSONObject resource = new JSONObject();
    JSONArray bbox = new JSONArray();
    List<Point> points = geoResult.getBbox();
    DirectPosition upperCorner = points.get(0).getDirectPosition();
    DirectPosition lowerCorner = points.get(1).getDirectPosition();
    bbox.add(upperCorner.getCoordinate()[1]);
    bbox.add(upperCorner.getCoordinate()[0]);
    bbox.add(lowerCorner.getCoordinate()[1]);
    bbox.add(lowerCorner.getCoordinate()[0]);
    resource.put("bbox", bbox);
    JSONObject point = new JSONObject();
    point.put("type", "Point");
    JSONArray coordinates = new JSONArray();
    coordinates.add(latitude);
    coordinates.add(longitude);
    point.put("coordinates", coordinates);
    resource.put("point", point);
    resource.put("name", geoResult.getFullName());
    resources.add(resource);
  }
}
