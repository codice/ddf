package org.codice.ddf.spatial.geocoding.feature;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vividsolutions.jts.geom.Geometry;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opengis.feature.simple.SimpleFeature;

public class TestCatalogFeatureQueryable {
  private FilterBuilder filterBuilder = new GeotoolsFilterBuilder();

  private CatalogFramework catalogFramework;

  private CatalogFeatureQueryable catalogFeatureQueryable;

  static final String WKT_STRING = "POLYGON((30 10, 10 20, 20 40, 40 40, 30 10))";
  static final String WKT_TYPE = "Polygon";
  static final int WKT_NUM_POINTS = 5;

  @Before
  public void setUp() {
    catalogFramework = mock(CatalogFramework.class);
    catalogFeatureQueryable = new CatalogFeatureQueryable();
    catalogFeatureQueryable.setCatalogFramework(catalogFramework);
    catalogFeatureQueryable.setFilterBuilder(filterBuilder);
  }

  @Test
  public void testQuery()
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {

    final ArgumentCaptor<QueryRequest> requestArgument =
        ArgumentCaptor.forClass(QueryRequest.class);

    // stub query response (list of metacards), test how features are returned
    QueryResponse queryResponse = getMockQueryResponse();
    when(catalogFramework.query(any())).thenReturn(queryResponse);

    List<SimpleFeature> results = catalogFeatureQueryable.query("JAM", 1);

    verify(catalogFramework, atLeastOnce()).query(requestArgument.capture());

    SimpleFeature result = results.get(0);
    Geometry geometry = (Geometry) result.getDefaultGeometry();
    assertThat(geometry.getNumPoints(), is(WKT_NUM_POINTS));
    assertThat(geometry.getGeometryType(), is(WKT_TYPE));

    QueryImpl query = (QueryImpl) requestArgument.getValue().getQuery();
    assertThat(
        query.getFilter().toString(),
        is("[[ title = JAM ] AND [ metacard-tags is like gazetteer ]]"));
  }

  private QueryResponse getMockQueryResponse() {
    Metacard metacard = mock(Metacard.class);
    // when(metacard.getMetacardType()).thenReturn(BasicTypes.BASIC_METACARD);

    Attribute titleAttribute = mock(Attribute.class);
    when(titleAttribute.getValue()).thenReturn("JAM");
    when(metacard.getAttribute(Metacard.TITLE)).thenReturn(titleAttribute);

    Attribute geographyAttribute = mock(Attribute.class);
    when(geographyAttribute.getValue()).thenReturn(WKT_STRING);
    when(metacard.getAttribute(Metacard.GEOGRAPHY)).thenReturn(geographyAttribute);

    when(metacard.getTags()).thenReturn(Collections.singleton("gazetteer"));

    QueryResponse queryResponse = mock(QueryResponse.class);
    Result result = mock(Result.class);
    when(result.getMetacard()).thenReturn(metacard);
    when(queryResponse.getResults()).thenReturn(Collections.singletonList(result));
    return queryResponse;
  }
}
