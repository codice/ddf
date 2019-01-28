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

import static org.codice.ddf.spatial.geocoding.GeoCodingConstants.GAZETTEER_METACARD_TAG;
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

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.impl.types.LocationAttributes;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Location;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.codice.ddf.security.common.Security;
import org.codice.ddf.spatial.geocoding.FeatureExtractionException;
import org.codice.ddf.spatial.geocoding.FeatureExtractor;
import org.codice.ddf.spatial.geocoding.FeatureIndexer;
import org.codice.ddf.spatial.geocoding.FeatureIndexingException;
import org.codice.ddf.spatial.geocoding.GeoCodingConstants;
import org.codice.ddf.spatial.geocoding.GeoEntryAttributes;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;
import org.mockito.ArgumentCaptor;
import org.opengis.feature.simple.SimpleFeature;

public class CatalogFeatureIndexerTest {
  private static final String RESOURCE_PATH = "blah";

  private static final String COUNTRY_CODE = "AFG";

  private static final String TITLE = "Afghanistan";

  private static final FilterBuilder FILTER_BUILDER = new GeotoolsFilterBuilder();

  private FeatureExtractor featureExtractor;

  private CatalogFramework catalogFramework;

  private CatalogFeatureIndexer featureIndexer;

  private Metacard exampleMetacard;

  @Before
  public void setUp()
      throws SecurityServiceException, InvocationTargetException, FeatureExtractionException,
          UnsupportedQueryException, SourceUnavailableException, FederationException {
    Security security = mock(Security.class);
    doAnswer(
            invocation -> {
              Callable callback = (Callable) invocation.getArguments()[0];
              callback.call();
              return null;
            })
        .when(security)
        .runWithSubjectOrElevate(any(Callable.class));

    featureExtractor = mock(FeatureExtractor.class);
    doAnswer(
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
    CatalogHelper catalogHelper = new CatalogHelper(FILTER_BUILDER);
    featureIndexer =
        new CatalogFeatureIndexer(catalogFramework, catalogHelper, generateMetacardType());
    featureIndexer.setSecurity(security);

    QueryResponse queryResponse = mock(QueryResponse.class);
    Result result = mock(Result.class);
    when(result.getMetacard()).thenReturn(getExampleMetacard());
    when(queryResponse.getResults()).thenReturn(Collections.singletonList(result));
    when(catalogFramework.query(any())).thenReturn(queryResponse);
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
    assertThat(a.getAttribute(Core.METACARD_TAGS), is(b.getAttribute(Core.METACARD_TAGS)));
    assertThat(a.getAttribute(Core.LOCATION), is(b.getAttribute(Core.LOCATION)));
    assertThat(a.getAttribute(Location.COUNTRY_CODE), is(b.getAttribute(Location.COUNTRY_CODE)));
  }

  private SimpleFeature getExampleFeature() {
    Geometry geometry = JTSFactoryFinder.getGeometryFactory(null).createPoint(new Coordinate(1, 1));
    SimpleFeatureBuilder builder = FeatureBuilder.forGeometry(geometry);
    SimpleFeature simpleFeature = builder.buildFeature(COUNTRY_CODE);
    simpleFeature.setAttribute("name", TITLE);
    return simpleFeature;
  }

  private Metacard getExampleMetacard() {
    Metacard metacard = new MetacardImpl();
    List<Serializable> tags = Arrays.asList(GAZETTEER_METACARD_TAG, GeoCodingConstants.COUNTRY_TAG);
    metacard.setAttribute(new AttributeImpl(Core.METACARD_TAGS, tags));
    WKTWriter writer = new WKTWriter();
    String wkt = writer.write((Geometry) getExampleFeature().getDefaultGeometry());
    metacard.setAttribute(new AttributeImpl(Core.LOCATION, wkt));
    metacard.setAttribute(new AttributeImpl(Core.TITLE, TITLE));
    metacard.setAttribute(new AttributeImpl(Location.COUNTRY_CODE, COUNTRY_CODE));
    return metacard;
  }

  private MetacardType generateMetacardType() {
    return new MetacardTypeImpl(
        "testType", Arrays.asList(new GeoEntryAttributes(), new LocationAttributes()));
  }
}
