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
package org.codice.ddf.spatial.geocoder.impl;

import ddf.catalog.util.impl.ServiceSelector;
import java.util.List;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.codice.ddf.spatial.geocoder.GeoCoder;
import org.codice.ddf.spatial.geocoder.GeoResult;
import org.codice.ddf.spatial.geocoding.GeoCoderService;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.codice.ddf.spatial.geocoding.context.NearbyLocation;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.primitive.Point;

public class GeoCoderServiceImpl implements GeoCoderService {

  private ServiceSelector<GeoCoder> geoCoderFactory;

  public GeoCoderServiceImpl(ServiceSelector<GeoCoder> geoCoderFactory) {

    if (geoCoderFactory == null) {
      throw new IllegalArgumentException(
          "GeoCoderServiceImpl(): constructor argument 'geoCoderFactory' may not be null.");
    }

    this.geoCoderFactory = geoCoderFactory;
  }

  @Override
  public String getLocation(String jsonp, String query) {
    if (JsonpValidator.isValidJsonp(jsonp)) {
      JSONObject jsonObject = doQuery(query);
      return jsonObject.toJSONString();
    } else {
      return null;
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

  @Override
  public String getNearbyCities(String wkt) throws GeoEntryQueryException {

    GeoCoder geoCoder = geoCoderFactory.getService();
    NearbyLocation nearbyLocation = geoCoder.getNearbyCity(wkt);
    if (nearbyLocation != null) {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("direction", nearbyLocation.getCardinalDirection());
      jsonObject.put("distance", nearbyLocation.getDistance());
      jsonObject.put("name", nearbyLocation.getName());
      return jsonObject.toJSONString();
    } else {
      return null;
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
