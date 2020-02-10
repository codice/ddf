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
package org.codice.ddf.opensearch.source;

import ddf.catalog.impl.filter.TemporalFilter;
import java.util.LinkedList;
import java.util.Queue;
import org.locationtech.jts.geom.Geometry;

public class OpenSearchFilterVisitorObject {

  private ContextualSearch contextualSearch;

  private TemporalFilter temporalSearch;

  private final Queue<PointRadius> pointRadiusSearches;

  private final Queue<Geometry> geometrySearches;

  private String id;

  private NestedTypes currentNest;

  public OpenSearchFilterVisitorObject() {
    pointRadiusSearches = new LinkedList<>();
    geometrySearches = new LinkedList<>();
  }

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

  /** Ordered by first added. Does not contain duplicates. */
  public Queue<PointRadius> getPointRadiusSearches() {
    return pointRadiusSearches;
  }

  public void addPointRadiusSearch(PointRadius pointRadiusSearch) {
    if (!pointRadiusSearches.contains(pointRadiusSearch)) {
      pointRadiusSearches.add(pointRadiusSearch);
    }
  }

  /** Ordered by first added. Does not contain duplicates. */
  public Queue<Geometry> getGeometrySearches() {
    return geometrySearches;
  }

  public void addGeometrySearch(Geometry geometrySearch) {
    if (!geometrySearches.contains(geometrySearch)) {
      geometrySearches.add(geometrySearch);
    }
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
