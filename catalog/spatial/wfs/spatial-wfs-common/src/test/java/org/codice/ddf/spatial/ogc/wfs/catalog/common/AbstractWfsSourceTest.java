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
package org.codice.ddf.spatial.ogc.wfs.catalog.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.junit.Before;
import org.junit.Test;

public class AbstractWfsSourceTest {
  private AbstractWfsSource wfsSource;
  private static final String FEATURE_NAME = "SampleFeature";
  private static final String TEMPORAL_SORT_PROPERTY = "myTemporalSortProperty";
  private static final String RELEVANCE_SORT_PROPERTY = "myRelevanceSortProperty";
  private static final String DISTANCE_SORT_PROPERTY = "myDistanceSortProperty";
  private List<MetacardMapper> mappers;

  @Before
  public void setup() {
    wfsSource = new TestWfsSource();
    mappers = new ArrayList<>();
    MetacardMapper mockMapper = mock(MetacardMapper.class);
    QName featureName = new QName("http://example.com", FEATURE_NAME, "Prefix");

    doReturn(featureName.toString()).when(mockMapper).getFeatureType();
    doReturn(DISTANCE_SORT_PROPERTY).when(mockMapper).getSortByDistanceFeatureProperty();
    doReturn(TEMPORAL_SORT_PROPERTY).when(mockMapper).getSortByTemporalFeatureProperty();
    doReturn(RELEVANCE_SORT_PROPERTY).when(mockMapper).getSortByRelevanceFeatureProperty();
    mappers.add(mockMapper);
  }

  @Test
  public void testSortMapping() throws Exception {
    QName featureName = new QName("http://example.com", FEATURE_NAME, "Prefix");
    String mappedTemporalProperty =
        wfsSource.mapSortByPropertyName(featureName, Result.TEMPORAL, mappers);
    String mappedRelevanceProperty =
        wfsSource.mapSortByPropertyName(featureName, Result.RELEVANCE, mappers);
    String mappedDistanceProperty =
        wfsSource.mapSortByPropertyName(featureName, Result.DISTANCE, mappers);
    assertThat(mappedTemporalProperty, is(TEMPORAL_SORT_PROPERTY));
    assertThat(mappedRelevanceProperty, is(RELEVANCE_SORT_PROPERTY));
    assertThat(mappedDistanceProperty, is(DISTANCE_SORT_PROPERTY));
  }

  @Test
  public void testNullFeatureType() throws Exception {
    String mappedTemporalProperty = wfsSource.mapSortByPropertyName(null, Result.TEMPORAL, mappers);
    assertThat(mappedTemporalProperty, nullValue());
  }

  @Test
  public void testNullProperty() throws Exception {
    QName featureName = new QName("http://example.com", FEATURE_NAME, "Prefix");
    String mappedTemporalProperty = wfsSource.mapSortByPropertyName(featureName, null, mappers);
    assertThat(mappedTemporalProperty, nullValue());
  }

  @Test
  public void testNullMapper() throws Exception {
    QName featureName = new QName("http://example.com", FEATURE_NAME, "Prefix");
    String mappedTemporalProperty =
        wfsSource.mapSortByPropertyName(featureName, Result.TEMPORAL, null);
    assertThat(mappedTemporalProperty, nullValue());
  }

  @Test
  public void testSortMappingEffective() throws Exception {
    QName featureName = new QName("http://example.com", FEATURE_NAME, "Prefix");
    String mappedTemporalProperty =
        wfsSource.mapSortByPropertyName(featureName, Metacard.EFFECTIVE, mappers);
    assertThat(mappedTemporalProperty, is(TEMPORAL_SORT_PROPERTY));
  }

  private class TestWfsSource extends AbstractWfsSource {
    @Override
    public ResourceResponse retrieveResource(URI uri, Map<String, Serializable> map)
        throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
      return null;
    }

    @Override
    public Set<String> getSupportedSchemes() {
      return null;
    }

    @Override
    public Set<String> getOptions(Metacard metacard) {
      return null;
    }

    @Override
    public String getConfigurationPid() {
      return null;
    }

    @Override
    public void setConfigurationPid(String s) {}

    @Override
    public boolean isAvailable() {
      return false;
    }

    @Override
    public boolean isAvailable(SourceMonitor sourceMonitor) {
      return false;
    }

    @Override
    public SourceResponse query(QueryRequest queryRequest) throws UnsupportedQueryException {
      return null;
    }

    @Override
    public Set<ContentType> getContentTypes() {
      return null;
    }
  }
}
