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
package org.codice.ddf.catalog.ui.query.suggestion;

import java.util.List;
import org.codice.ddf.spatial.geocoding.Suggestion;

class LiteralSuggestion implements Suggestion {
  private final String id;
  private final String name;
  private final List<LatLon> geo;

  LiteralSuggestion(String id, String name, List<LatLon> geo) {
    this.id = id;
    this.name = name;
    this.geo = geo;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  List<LatLon> getGeo() {
    return geo;
  }

  boolean hasGeo() {
    return geo != null && !geo.isEmpty();
  }

  @Override
  public String toString() {
    return getName();
  }
}
