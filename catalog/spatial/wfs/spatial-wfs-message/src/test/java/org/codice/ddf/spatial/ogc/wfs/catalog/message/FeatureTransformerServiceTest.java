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
package org.codice.ddf.spatial.ogc.wfs.catalog.message;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.component.bean.ProxyHelper;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.codice.ddf.spatial.ogc.wfs.catalog.message.api.FeatureTransformer;
import org.codice.ddf.spatial.ogc.wfs.catalog.message.api.WfsMetadata;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class FeatureTransformerServiceTest {
  private static final int FEATURE_MEMBER_COUNT = 10;

  private CamelContext camelContext;

  private Endpoint endpoint;

  private BiFunction<InputStream, WfsMetadata, List<Metacard>> featureTransformationService;

  private List<FeatureTransformer> transformerList;

  @Before
  public void setup() throws Exception {
    setupTransformers();
    SimpleRegistry registry = new SimpleRegistry();
    registry.put("wfsTransformerProcessor", new WfsTransformerProcessor(transformerList));

    this.camelContext = new DefaultCamelContext(registry);
    camelContext.addRoutes(new WfsRouteBuilder());
    camelContext.setTracing(true);

    endpoint = camelContext.getEndpoint(WfsRouteBuilder.ENDPOINT_URL);
    featureTransformationService = ProxyHelper.createProxy(endpoint, BiFunction.class);
    camelContext.start();
  }

  private void setupTransformers() {
    transformerList = new ArrayList<>();
    FeatureTransformer mockTransformer = mock(FeatureTransformer.class);
    Optional optional = Optional.of(mock(Metacard.class));
    when(mockTransformer.apply(any(InputStream.class), any(WfsMetadata.class)))
        .thenReturn(optional);
    transformerList.add(mockTransformer);
  }

  @Test
  public void testApply() throws Exception {
    InputStream inputStream =
        new BufferedInputStream(
            FeatureTransformerServiceTest.class.getResourceAsStream("/GeneralGIN.xml"));

    WfsMetadata wfsMetadata = mock(WfsMetadata.class);

    List<Metacard> metacards = featureTransformationService.apply(inputStream, wfsMetadata);
    ArgumentCaptor<InputStream> inputStreamArgumentCaptor =
        ArgumentCaptor.forClass(InputStream.class);
    ArgumentCaptor<WfsMetadata> wfsMetadataArgumentCaptor =
        ArgumentCaptor.forClass(WfsMetadata.class);
    verify(transformerList.get(0), times(FEATURE_MEMBER_COUNT))
        .apply(inputStreamArgumentCaptor.capture(), wfsMetadataArgumentCaptor.capture());

    for (int i = 0; i < FEATURE_MEMBER_COUNT; i++) {
      assertThat(inputStreamArgumentCaptor.getAllValues().get(i), notNullValue());
      assertThat(wfsMetadataArgumentCaptor.getAllValues().get(i), notNullValue());
    }

    assertThat(metacards, hasSize(10));
  }
}
