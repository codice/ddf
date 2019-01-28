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
package ddf.catalog.pubsub.predicate;

import ddf.catalog.data.Metacard;
import ddf.catalog.pubsub.criteria.geospatial.GeospatialEvaluationCriteria;
import ddf.catalog.pubsub.criteria.geospatial.GeospatialEvaluationCriteriaImpl;
import ddf.catalog.pubsub.criteria.geospatial.GeospatialEvaluator;
import ddf.catalog.pubsub.internal.PubSubConstants;
import java.util.Iterator;
import java.util.Map;
import org.geotools.geometry.jts.WKTReader2;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeospatialPredicate implements Predicate {
  private static final Logger LOGGER = LoggerFactory.getLogger(GeospatialPredicate.class);

  private Geometry geoCriteria;

  private String geoOperation;

  private double distance;

  /**
   * Instantiates a new geospatial predicate.
   *
   * @param wkt A string of well known text.
   * @param geoOperation the geo operation
   */
  public GeospatialPredicate(String wkt, String geoOperation, double distance) {
    this.geoOperation = geoOperation;
    this.distance = distance;

    try {
      WKTReader2 wktreader = new WKTReader2();
      this.geoCriteria = wktreader.read(wkt);
    } catch (Exception e) {
      LOGGER.debug("Exception reading WKT", e);
    }
  }

  public GeospatialPredicate(Geometry geo, String geoOperation, double distance) {
    this.geoOperation = geoOperation;
    this.distance = distance;

    this.geoCriteria = geo;
  }

  public static boolean isGeospatial(Map geoCriteria, String geoOperation) {
    Iterator it = geoCriteria.values().iterator();
    boolean hasCriteria = false;

    while (it.hasNext()) {
      Object item = it.next();
      if (item != null && !item.toString().equals("")) {
        hasCriteria = true;
      }
    }

    return hasCriteria && !geoCriteria.isEmpty();
  }

  public boolean matches(Event properties) {
    Metacard entry = (Metacard) properties.getProperty(PubSubConstants.HEADER_ENTRY_KEY);

    Map<String, Object> contextualMap =
        (Map<String, Object>) properties.getProperty(PubSubConstants.HEADER_CONTEXTUAL_KEY);

    String operation = (String) properties.getProperty(PubSubConstants.HEADER_OPERATION_KEY);
    LOGGER.debug("operation = {}", operation);

    if (contextualMap != null) {
      String metadata = (String) contextualMap.get("METADATA");

      // If deleting a catalog entry and the entry's location data is NULL is only the word
      // "deleted" (i.e., the
      // source is deleting the catalog entry and did not send any location data with the
      // delete event), then
      // cannot apply any geospatial filtering - just send the event on to the subscriber
      if (PubSubConstants.DELETE.equals(operation)
          && PubSubConstants.METADATA_DELETED.equals(metadata)) {
        LOGGER.debug(
            "Detected a DELETE operation where metadata is just the word 'deleted', so send event on to subscriber");
        return true;
      }
    }

    GeospatialEvaluationCriteria gec;
    try {
      gec =
          new GeospatialEvaluationCriteriaImpl(
              geoCriteria, geoOperation, entry.getLocation(), distance);
      return GeospatialEvaluator.evaluate(gec);
    } catch (ParseException e) {
      LOGGER.debug("Error parsing WKT string.  Unable to compare geos.  Returning false.");
      return false;
    }
  }

  public Geometry getGeoCriteria() {
    return geoCriteria;
  }

  public String getGeoOperation() {
    return geoOperation;
  }

  public double getDistance() {
    return distance;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("\tgeoCriteria = " + geoCriteria + "\n");
    sb.append("\tgeoOperation = " + geoOperation + "\n");
    sb.append("\tdistance = " + distance + "\n");

    return sb.toString();
  }
}
