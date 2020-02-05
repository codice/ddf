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
package ddf.catalog.pubsub.criteria.geospatial;

import org.geotools.geometry.jts.WKTReader2;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;

public class GeospatialEvaluationCriteriaImpl implements GeospatialEvaluationCriteria {
  private Geometry criteria;

  private String geoOperation;

  private Geometry input;

  private double distance;

  public GeospatialEvaluationCriteriaImpl(
      Geometry criteria, String geoOperation, Geometry input, double distance) {
    this.criteria = criteria;
    this.geoOperation = geoOperation;
    this.input = input;
    this.distance = distance;
  }

  public GeospatialEvaluationCriteriaImpl(
      Geometry criteria, String geoOperation, String input, double distance) throws ParseException {
    WKTReader2 wktreader = new WKTReader2();

    this.criteria = criteria;
    this.geoOperation = geoOperation;
    this.input = wktreader.read(input);
    this.distance = distance;
  }

  public GeospatialEvaluationCriteriaImpl(Geometry criteria, String operation, String input)
      throws ParseException {
    WKTReader2 wktreader = new WKTReader2();
    this.criteria = criteria;
    this.geoOperation = operation;
    this.input = wktreader.read(input);
  }

  public Geometry getCriteria() {
    return criteria;
  }

  public double getDistance() {
    return distance;
  }

  public Geometry getInput() {
    return input;
  }

  public String getOperation() {
    return geoOperation;
  }
}
