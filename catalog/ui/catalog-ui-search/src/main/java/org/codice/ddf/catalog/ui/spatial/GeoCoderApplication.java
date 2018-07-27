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
package org.codice.ddf.catalog.ui.spatial;

import static spark.Spark.get;

import org.apache.http.HttpStatus;
import org.codice.ddf.spatial.geocoding.GeoCoderService;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.servlet.SparkApplication;

public class GeoCoderApplication implements SparkApplication {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeoCoderApplication.class);

  private GeoCoderService geoCoderService;

  public GeoCoderApplication(GeoCoderService geoCoderService) {
    this.geoCoderService = geoCoderService;
  }

  @Override
  public void init() {
    get(
        "/REST/v1/Locations",
        (req, res) -> {
          String jsonp = req.queryParams("jsonp");
          String jsonString = geoCoderService.getLocation(jsonp, req.queryParams("query"));

          if (jsonString != null) {
            res.status(HttpStatus.SC_OK);
            return jsonp + "(" + jsonString + ")";
          } else {
            res.status(HttpStatus.SC_BAD_REQUEST);
            return "";
          }
        });

    get(
        "/REST/v1/Locations/nearby/cities/:wkt",
        (req, res) -> {
          String wkt = req.params(":wkt");

          try {
            String jsonString = geoCoderService.getNearbyCities(wkt);

            if (jsonString != null) {
              res.status(HttpStatus.SC_OK);
              return jsonString;
            } else {
              res.status(HttpStatus.SC_NO_CONTENT);
              return "";
            }

          } catch (GeoEntryQueryException e) {
            LOGGER.debug("Error querying GeoNames resource with wkt:{}", wkt, e);
            res.status(HttpStatus.SC_BAD_REQUEST);
            return "";
          }
        });
  }
}
