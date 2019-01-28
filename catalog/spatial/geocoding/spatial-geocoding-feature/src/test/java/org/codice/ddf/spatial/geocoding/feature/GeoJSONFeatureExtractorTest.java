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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.stream.Collectors;
import org.codice.ddf.spatial.geocoding.FeatureExtractionException;
import org.codice.ddf.spatial.geocoding.FeatureExtractor;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.mockito.ArgumentCaptor;
import org.opengis.feature.simple.SimpleFeature;

public class GeoJSONFeatureExtractorTest {
  private static final String JAMAICA_GEOJSON_PATH =
      GeoJSONFeatureExtractorTest.class.getResource("/geojson/afghanistan.geojson").getPath();

  private static final String EMPTY_GEOJSON_PATH =
      GeoJSONFeatureExtractorTest.class.getResource("/geojson/empty.geojson").getPath();

  private static final String NA_GEOJSON_PATH =
      GeoJSONFeatureExtractorTest.class.getResource("/geojson/northamerica.geojson").getPath();

  private static final String BAD_GEOJSON_PATH =
      GeoJSONFeatureExtractorTest.class.getResource("/geojson/bad.geojson").getPath();

  private GeoJSONFeatureExtractor geoJSONFeatureExtractor;

  @Before
  public void setUp() {
    geoJSONFeatureExtractor = new GeoJSONFeatureExtractor();
  }

  @Test
  public void testExtractAndSimplifyCountry() throws Exception {
    final SimpleFeature feature = loadFeaturesFromPath(JAMAICA_GEOJSON_PATH, true).get(0);
    assertThat(feature.getID(), is("AFG"));

    Geometry geometry = (Geometry) feature.getDefaultGeometry();
    assertThat(geometry.getNumPoints(), is(58));
  }

  @Test
  public void testExtractAndSimplifyMultipleCountry() throws Exception {
    final List<SimpleFeature> simpleFeatureList = loadFeaturesFromPath(NA_GEOJSON_PATH, true);
    assertThat(simpleFeatureList.size(), is(3));
    final List<String> countryCodes =
        simpleFeatureList.stream().map(SimpleFeature::getID).collect(Collectors.toList());
    assertThat(countryCodes, hasItems("USA", "CAN", "MEX"));
  }

  @Test(expected = FeatureExtractionException.class)
  public void testExtractEmptyGeoJson() throws Exception {
    loadFeaturesFromPath(EMPTY_GEOJSON_PATH, false).get(0);
  }

  @Test
  public void testExtractBadGeoJson() throws Exception {
    final List<SimpleFeature> simpleFeatureList = loadFeaturesFromPath(BAD_GEOJSON_PATH, false);
    assertThat(simpleFeatureList.size(), is(0));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExtractNullPath() throws Exception {
    loadFeaturesFromPath(null, false).get(0);
  }

  private List<SimpleFeature> loadFeaturesFromPath(String path, boolean assertSuccessfulExtraction)
      throws Exception {
    final FeatureExtractor.ExtractionCallback extractionCallback =
        mock(FeatureExtractor.ExtractionCallback.class);

    final ArgumentCaptor<SimpleFeature> featureArgumentCaptor =
        ArgumentCaptor.forClass(SimpleFeature.class);

    geoJSONFeatureExtractor.pushFeaturesToExtractionCallback(path, extractionCallback);

    if (assertSuccessfulExtraction) {
      verify(extractionCallback, atLeastOnce()).extracted(featureArgumentCaptor.capture());
    } else {
      verify(extractionCallback, never()).extracted(featureArgumentCaptor.capture());
    }
    return featureArgumentCaptor.getAllValues();
  }
}
