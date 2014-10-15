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
package org.codice.ddf.ui.searchui.geocoder.endpoint;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.codice.ddf.ui.searchui.geocoder.GeoCoder;
import org.codice.ddf.ui.searchui.geocoder.GeoResult;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.primitive.Point;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/")
public class GeoCoderEndpoint {

    private GeoCoder geoCoder;

    public GeoCoderEndpoint(GeoCoder geoCoder) {
        this.geoCoder = geoCoder;
    }

    @GET
    public Response getLocation(@QueryParam("jsonp") String jsonp, @QueryParam("query") String query) {
        Response response;

        GeoResult geoResult = geoCoder.getLocation(query);

        DirectPosition directPosition = geoResult.getPoint().getDirectPosition();
        double[] coords = directPosition.getCoordinate();

        double longitude = coords[0];
        double latitude = coords[1];

        JSONObject jsonObject = new JSONObject();
        JSONArray resourceSets = new JSONArray();
        JSONObject resourceSet = new JSONObject();
        jsonObject.put("resourceSets", resourceSets);
        resourceSets.add(resourceSet);
        JSONArray resources = new JSONArray();
        resourceSet.put("resources", resources);
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

        response = Response.ok(jsonp+"("+jsonObject.toJSONString()+")").build();
        return response;
    }
}
