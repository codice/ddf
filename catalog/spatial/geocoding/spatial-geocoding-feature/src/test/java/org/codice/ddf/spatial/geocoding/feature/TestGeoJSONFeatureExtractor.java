package org.codice.ddf.spatial.geocoding.feature;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.vividsolutions.jts.geom.Geometry;
import java.util.List;
import org.codice.ddf.spatial.geocoding.FeatureExtractionException;
import org.codice.ddf.spatial.geocoding.FeatureExtractor;
import org.codice.ddf.spatial.geocoding.FeatureIndexingException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opengis.feature.simple.SimpleFeature;

public class TestGeoJSONFeatureExtractor {
  private static final String JAMAICA_GEOJSON_PATH =
      TestGeoJSONFeatureExtractor.class.getResource("/geojson/jamaica.geojson").getPath();

  private static final String RUSSIA_GEOJSON_PATH =
      TestGeoJSONFeatureExtractor.class.getResource("/geojson/russia.geojson").getPath();

  private GeoJSONFeatureExtractor geoJSONFeatureExtractor;

  @Before
  public void setUp() {
    geoJSONFeatureExtractor = new GeoJSONFeatureExtractor();
  }

  @Test
  public void testExtractAndSimplifyCountry()
      throws FeatureExtractionException, FeatureIndexingException {
    final SimpleFeature feature = loadFeaturesFromPath(JAMAICA_GEOJSON_PATH).get(0);
    assertThat(feature.getAttribute("ISO_A3"), is("JAM"));

    Geometry geometry = (Geometry) feature.getDefaultGeometry();
    assertThat(geometry.getNumPoints(), is(12));
  }

  @Test
  public void testExtractAndSimplifyCountryMultipleIterations()
      throws FeatureExtractionException, FeatureIndexingException {
    final SimpleFeature feature = loadFeaturesFromPath(RUSSIA_GEOJSON_PATH).get(0);
    assertThat(feature.getAttribute("ISO_A3"), is("RUS"));

    Geometry geometry = (Geometry) feature.getDefaultGeometry();
    assertThat(geometry.getNumPoints(), is(258));
  }

  private List<SimpleFeature> loadFeaturesFromPath(String path)
      throws FeatureExtractionException, FeatureIndexingException {
    final FeatureExtractor.ExtractionCallback extractionCallback =
        mock(FeatureExtractor.ExtractionCallback.class);

    final ArgumentCaptor<SimpleFeature> featureArgumentCaptor =
        ArgumentCaptor.forClass(SimpleFeature.class);

    geoJSONFeatureExtractor.pushFeaturesToExtractionCallback(path, extractionCallback);

    verify(extractionCallback, atLeastOnce()).extracted(featureArgumentCaptor.capture());

    return featureArgumentCaptor.getAllValues();
  }
}
