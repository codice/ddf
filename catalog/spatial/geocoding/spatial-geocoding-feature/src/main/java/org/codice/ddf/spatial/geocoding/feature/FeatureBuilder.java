package org.codice.ddf.spatial.geocoding.feature;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeatureType;

public class FeatureBuilder {
  public static SimpleFeatureBuilder forGeometry(Geometry geometry) {
    SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
    typeBuilder.setName("testFeatureType");
    typeBuilder.setCRS(DefaultGeographicCRS.WGS84);
    typeBuilder.add("coordinates", geometry.getClass());
    SimpleFeatureType featureType = typeBuilder.buildFeatureType();
    SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
    builder.add(geometry);
    return builder;
  }
}
