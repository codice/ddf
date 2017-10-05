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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import org.apache.commons.lang.Validate;
import org.codice.ddf.spatial.geocoding.FeatureExtractionException;
import org.codice.ddf.spatial.geocoding.FeatureExtractor;
import org.codice.ddf.spatial.geocoding.FeatureIndexingException;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoJSONFeatureExtractor implements FeatureExtractor {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeoJSONFeatureExtractor.class);

  private static final int MAX_SIMPLIFY_ITERATIONS = 5;

  private static final double SIMPLIFY_DISTANCE_TOLERANCE = .1;

  private static final int MAX_POINTS_PER_FEATURE = 500;

  @Override
  public void pushFeaturesToExtractionCallback(
      String resource, ExtractionCallback extractionCallback) throws FeatureExtractionException {
    Validate.notNull(extractionCallback, "extractionCallback can't be null");

    FeatureIterator<SimpleFeature> iterator = getFeatureIteratorFromResource(resource);
    try {
      while (iterator.hasNext()) {
        SimpleFeature feature = iterator.next();
        Geometry geometry = (Geometry) feature.getDefaultGeometry();
        Geometry simplifiedGeometry = getSimplifiedGeometry(geometry);

        if (simplifiedGeometry == null) {
          LOGGER.warn("Failed to simplify geometry below {} point maximum", MAX_POINTS_PER_FEATURE);
        } else {
          feature.setDefaultGeometry(simplifiedGeometry);
          extractionCallback.extracted(feature);
        }
      }
    } catch (FeatureIndexingException e) {
      throw new FeatureExtractionException("Unable to extract feature from " + resource, e);
    } finally {
      iterator.close();
    }
  }

  private FeatureIterator<SimpleFeature> getFeatureIteratorFromResource(String resource)
      throws FeatureExtractionException {
    try {
      Reader reader = new InputStreamReader(new FileInputStream(resource), "UTF-8");
      return new FeatureJSON().streamFeatureCollection(reader);
    } catch (IOException e) {
      throw new FeatureExtractionException("Failed to load resource", e);
    }
  }

  private Geometry getSimplifiedGeometry(Geometry geometry) {
    for (int iterations = 0; iterations <= MAX_SIMPLIFY_ITERATIONS; iterations++) {
      double tolerance = SIMPLIFY_DISTANCE_TOLERANCE * Math.pow(2, iterations);
      Geometry simplifiedGeometry = DouglasPeuckerSimplifier.simplify(geometry, tolerance);
      if (simplifiedGeometry.getNumPoints() <= 0) {
        return null;
      }
      if (simplifiedGeometry.getNumPoints() < MAX_POINTS_PER_FEATURE) {
        return simplifiedGeometry;
      }
    }
    return null;
  }
}
