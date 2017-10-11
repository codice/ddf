package org.codice.ddf.spatial.geocoding.feature;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.service.SecurityServiceException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.codice.ddf.security.common.Security;
import org.codice.ddf.spatial.geocoding.FeatureExtractionException;
import org.codice.ddf.spatial.geocoding.FeatureExtractor;
import org.codice.ddf.spatial.geocoding.FeatureIndexer;
import org.codice.ddf.spatial.geocoding.FeatureIndexingException;
import org.codice.ddf.spatial.geocoding.GazetteerConstants;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.opengis.feature.simple.SimpleFeature;

public class TestCatalogFeatureIndexer {
  private FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
  private CatalogHelper catalogHelper;
  private FeatureExtractor featureExtractor;
  private CatalogFramework catalogFramework;

  private CatalogFeatureIndexer featureIndexer;

  private final String RESOURCE_PATH = "blah";
  private final String COUNTRY_CODE = "JAM";

  private SimpleFeature exampleFeature;
  private Metacard exampleMetacard;

  @Before
  public void setUp()
      throws SecurityServiceException, InvocationTargetException, FeatureExtractionException,
          UnsupportedQueryException, SourceUnavailableException, FederationException {
    Security security = mock(Security.class);
    doAnswer(
            (Answer<Void>)
                invocation -> {
                  Callable callback = (Callable) invocation.getArguments()[0];
                  callback.call();
                  return null;
                })
        .when(security)
        .runWithSubjectOrElevate(any(Callable.class));

    featureExtractor = mock(FeatureExtractor.class);
    doAnswer(
            (Answer<Void>)
                invocation -> {
                  FeatureExtractor.ExtractionCallback callback =
                      (FeatureExtractor.ExtractionCallback) invocation.getArguments()[1];
                  callback.extracted(getExampleFeature());
                  return null;
                })
        .when(featureExtractor)
        .pushFeaturesToExtractionCallback(
            eq(RESOURCE_PATH), any(FeatureExtractor.ExtractionCallback.class));

    catalogFramework = mock(CatalogFramework.class);
    catalogHelper = new CatalogHelper(filterBuilder);
    featureIndexer = new CatalogFeatureIndexer(catalogFramework, catalogHelper);
    featureIndexer.setSecurity(security);

    QueryResponse queryResponse = mock(QueryResponse.class);
    Result result = mock(Result.class);
    when(result.getMetacard()).thenReturn(getExampleMetacard());
    when(queryResponse.getResults()).thenReturn(Collections.singletonList(result));
    when(catalogFramework.query(any())).thenReturn(queryResponse);

    exampleFeature = getExampleFeature();
    exampleMetacard = getExampleMetacard();
  }

  @Test
  public void testCreateIndex()
      throws FeatureIndexingException, FeatureExtractionException, SourceUnavailableException,
          IngestException {

    FeatureIndexer.IndexCallback indexCallback = mock(FeatureIndexer.IndexCallback.class);

    featureIndexer.updateIndex(RESOURCE_PATH, featureExtractor, true, indexCallback);

    ArgumentCaptor<DeleteRequestImpl> deleteRequestCaptor =
        ArgumentCaptor.forClass(DeleteRequestImpl.class);
    verify(catalogFramework, times(1)).delete(deleteRequestCaptor.capture());
    assertTrue(
        deleteRequestCaptor.getValue().getAttributeValues().contains(exampleMetacard.getId()));

    ArgumentCaptor<CreateRequestImpl> createRequestCaptor =
        ArgumentCaptor.forClass(CreateRequestImpl.class);
    verify(catalogFramework, times(1)).create(createRequestCaptor.capture());

    Metacard createdMetacard = createRequestCaptor.getValue().getMetacards().get(0);
    assertMetacardsAreEqual(createdMetacard, exampleMetacard);

    ArgumentCaptor<Integer> indexCallbackCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(indexCallback, times(1)).indexed(indexCallbackCaptor.capture());
    assertThat(indexCallbackCaptor.getValue(), is(1));
  }

  @Test
  public void testUpdateIndex()
      throws FeatureIndexingException, FeatureExtractionException, SourceUnavailableException,
          IngestException {

    FeatureIndexer.IndexCallback indexCallback = mock(FeatureIndexer.IndexCallback.class);

    featureIndexer.updateIndex(RESOURCE_PATH, featureExtractor, false, indexCallback);

    verify(catalogFramework, times(0)).delete(any());

    ArgumentCaptor<UpdateRequestImpl> updateRequestCaptor =
        ArgumentCaptor.forClass(UpdateRequestImpl.class);
    verify(catalogFramework, times(1)).update(updateRequestCaptor.capture());
    Map.Entry<Serializable, Metacard> update = updateRequestCaptor.getValue().getUpdates().get(0);
    assertThat(update.getKey(), is(exampleMetacard.getId()));
    assertMetacardsAreEqual(update.getValue(), exampleMetacard);

    ArgumentCaptor<Integer> indexCallbackCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(indexCallback, times(1)).indexed(indexCallbackCaptor.capture());
    assertThat(indexCallbackCaptor.getValue(), is(1));
  }

  private void assertMetacardsAreEqual(Metacard a, Metacard b) {
    assertThat(a.getTitle(), is(b.getTitle()));
    assertThat(a.getAttribute(Metacard.TAGS), is(b.getAttribute(Metacard.TAGS)));
    assertThat(a.getAttribute(Metacard.GEOGRAPHY), is(b.getAttribute(Metacard.GEOGRAPHY)));
  }

  private SimpleFeature getExampleFeature() {
    Geometry geometry = JTSFactoryFinder.getGeometryFactory(null).createPoint(new Coordinate(1, 1));
    SimpleFeatureBuilder builder = FeatureBuilder.forGeometry(geometry);
    return builder.buildFeature(COUNTRY_CODE);
  }

  private Metacard getExampleMetacard() {
    Metacard metacard = new MetacardImpl();
    List<Serializable> tags = new ArrayList<>();
    tags.add(GazetteerConstants.DEFAULT_TAG);
    tags.add(GazetteerConstants.COUNTRY_TAG);
    metacard.setAttribute(new AttributeImpl(Metacard.TAGS, tags));

    WKTWriter writer = new WKTWriter();
    String wkt = writer.write((Geometry) getExampleFeature().getDefaultGeometry());
    metacard.setAttribute(new AttributeImpl(Metacard.GEOGRAPHY, wkt));
    return metacard;
  }
}
