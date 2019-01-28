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
package ddf.catalog.impl.filter;

import java.util.Objects;
import org.geotools.geometry.jts.spatialschema.geometry.GeometryImpl;
import org.locationtech.jts.geom.Geometry;

// TODO Temporary work around for Geotools multi geometry support
public class JTSGeometryWrapper extends GeometryImpl {

  private Geometry geo;

  public JTSGeometryWrapper(org.locationtech.jts.geom.Geometry geo) {
    this.geo = geo;
  }

  // Only implements computJTSPeer for getJTSGeometry support
  @Override
  protected Geometry computeJTSPeer() {
    return geo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof JTSGeometryWrapper)) return false;
    JTSGeometryWrapper that = (JTSGeometryWrapper) o;
    return Objects.equals(geo, that.geo);
  }

  @Override
  public int hashCode() {

    return Objects.hash(geo);
  }
}
