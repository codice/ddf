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
package org.codice.ddf.spatial.geocoding.feature;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeatureType;

public class FeatureBuilder {
  public static SimpleFeatureBuilder forGeometry(Geometry geometry) {
    SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
    typeBuilder.setName("CountryFeatureType");
    typeBuilder.setCRS(DefaultGeographicCRS.WGS84);
    typeBuilder.add("coordinates", geometry.getClass());
    typeBuilder.add("name", String.class);
    SimpleFeatureType featureType = typeBuilder.buildFeatureType();
    SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
    builder.add(geometry);
    return builder;
  }

  private FeatureBuilder() {}
}
