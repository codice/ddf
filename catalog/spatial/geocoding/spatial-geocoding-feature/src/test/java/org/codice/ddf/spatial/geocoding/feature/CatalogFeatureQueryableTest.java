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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Location;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.codice.ddf.spatial.geocoding.FeatureQueryException;
import org.codice.ddf.spatial.geocoding.GeoCodingConstants;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.IsEqualsToImpl;
import org.geotools.filter.LikeFilterImpl;
import org.geotools.filter.LiteralExpressionImpl;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.mockito.ArgumentCaptor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.And;
import org.opengis.filter.Filter;

public class CatalogFeatureQueryableTest {

  private static final FilterBuilder FILTER_BUILDER = new GeotoolsFilterBuilder();

  private static final String COUNTRY_CODE = "JAM";

  private static final String WKT_STRING = "POLYGON((30 10, 10 20, 20 40, 40 40, 30 10))";

  private static final String WKT_TYPE = "Polygon";

  private static final int WKT_NUM_POINTS = 5;

  private CatalogHelper catalogHelper;

  private CatalogFramework catalogFramework;

  private CatalogFeatureQueryable catalogFeatureQueryable;

  @Before
  public void setUp() {
    catalogHelper = new CatalogHelper(FILTER_BUILDER);
    catalogFramework = mock(CatalogFramework.class);
    catalogFeatureQueryable = new CatalogFeatureQueryable(catalogFramework, catalogHelper);
  }

  @Test
  public void testQuery()
      throws UnsupportedQueryException, SourceUnavailableException, FederationException,
          FeatureQueryException {

    final ArgumentCaptor<QueryRequest> requestArgument =
        ArgumentCaptor.forClass(QueryRequest.class);

    QueryResponse queryResponse = getMockQueryResponse();
    when(catalogFramework.query(any())).thenReturn(queryResponse);

    List<SimpleFeature> results = catalogFeatureQueryable.query(COUNTRY_CODE, "PCL1", 1);

    verify(catalogFramework, atLeastOnce()).query(requestArgument.capture());

    SimpleFeature result = results.get(0);
    Geometry geometry = (Geometry) result.getDefaultGeometry();
    assertThat(geometry.getNumPoints(), is(WKT_NUM_POINTS));
    assertThat(geometry.getGeometryType(), is(WKT_TYPE));

    QueryImpl query = (QueryImpl) requestArgument.getValue().getQuery();

    Filter filter = query.getFilter();

    And and = (And) filter;

    List<Filter> filters = and.getChildren();
    assertThat(filters, hasSize(3));

    List<String> attributes = new ArrayList<>();
    List<String> values = new ArrayList<>();
    for (Filter compFilter : filters) {
      String attribute;
      String value;
      if (compFilter instanceof IsEqualsToImpl) {
        IsEqualsToImpl equals = (IsEqualsToImpl) compFilter;
        AttributeExpressionImpl attributeExpression =
            (AttributeExpressionImpl) equals.getExpression1();
        attribute = attributeExpression.getPropertyName();
        LiteralExpressionImpl literalExpression = (LiteralExpressionImpl) equals.getExpression2();
        value = (String) literalExpression.getValue();
      } else {
        LikeFilterImpl likeFilter = (LikeFilterImpl) compFilter;
        AttributeExpressionImpl literalExpression =
            (AttributeExpressionImpl) likeFilter.getExpression();
        attribute = literalExpression.getPropertyName();
        value = likeFilter.getLiteral();
      }
      attributes.add(attribute);
      values.add(value);
    }

    assertThat(attributes, hasSize(3));
    assertThat(attributes, hasItems(Location.COUNTRY_CODE, Core.METACARD_TAGS, Core.METACARD_TAGS));
    assertThat(values, hasSize(3));
    assertThat(
        values, hasItems(COUNTRY_CODE, GAZETTEER_METACARD_TAG, GeoCodingConstants.COUNTRY_TAG));
  }

  private QueryResponse getMockQueryResponse() {
    Metacard metacard = mock(Metacard.class);

    Attribute titleAttribute = mock(Attribute.class);
    when(titleAttribute.getValue()).thenReturn(COUNTRY_CODE);
    when(metacard.getAttribute(Core.TITLE)).thenReturn(titleAttribute);

    Attribute geographyAttribute = mock(Attribute.class);
    when(geographyAttribute.getValue()).thenReturn(WKT_STRING);
    when(metacard.getAttribute(Core.LOCATION)).thenReturn(geographyAttribute);

    when(metacard.getTags()).thenReturn(Collections.singleton(GAZETTEER_METACARD_TAG));

    QueryResponse queryResponse = mock(QueryResponse.class);
    Result result = mock(Result.class);
    when(result.getMetacard()).thenReturn(metacard);
    when(queryResponse.getResults()).thenReturn(Collections.singletonList(result));
    return queryResponse;
  }
}
