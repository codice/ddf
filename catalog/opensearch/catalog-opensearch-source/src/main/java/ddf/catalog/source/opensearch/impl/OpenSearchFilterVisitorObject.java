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
package ddf.catalog.source.opensearch.impl;

import com.vividsolutions.jts.geom.Polygon;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.source.opensearch.PointRadiusSearch;

public class OpenSearchFilterVisitorObject {

  private ContextualSearch contextualSearch;

  private TemporalFilter temporalSearch;

  private PointRadiusSearch pointRadiusSearch;

  private Polygon polygonSearch;

  private String id;

  private NestedTypes currentNest;

  public ContextualSearch getContextualSearch() {
    return contextualSearch;
  }

  public void setContextualSearch(ContextualSearch contextualSearch) {
    this.contextualSearch = contextualSearch;
  }

  public TemporalFilter getTemporalSearch() {
    return temporalSearch;
  }

  public void setTemporalSearch(TemporalFilter temporalSearch) {
    this.temporalSearch = temporalSearch;
  }

  public PointRadiusSearch getPointRadiusSearch() {
    return pointRadiusSearch;
  }

  public void setPointRadiusSearch(PointRadiusSearch pointRadiusSearch) {
    this.pointRadiusSearch = pointRadiusSearch;
  }

  public Polygon getPolygonSearch() {
    return polygonSearch;
  }

  public void setPolygonSearch(Polygon polygonSearch) {
    this.polygonSearch = polygonSearch;
  }

  public String getId() {
    return id;
  }

  public void setId(String idSearch) {
    this.id = idSearch;
  }

  public NestedTypes getCurrentNest() {
    return currentNest;
  }

  public void setCurrentNest(NestedTypes currentNest) {
    this.currentNest = currentNest;
  }
}
