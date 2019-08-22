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
package org.codice.ddf.spatial.ogc.wfs.transformer.xstream;

import static java.util.Collections.singletonList;
import static org.codice.ddf.libs.geo.util.GeospatialUtil.LAT_LON_ORDER;
import static org.codice.ddf.libs.geo.util.GeospatialUtil.LON_LAT_ORDER;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Optional;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import net.opengis.wfs.v_1_1_0.FeatureTypeType;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverterFactory;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.GenericFeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.codice.ddf.spatial.ogc.wfs.catalog.metacardtype.registry.WfsMetacardTypeRegistry;
import org.codice.ddf.spatial.ogc.wfs.featuretransformer.WfsMetadata;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.Wfs11Constants;
import org.junit.Before;
import org.junit.Test;

public class XStreamWfs11FeatureTransformerTest {

  private static final QName PETER_PAN_NAME =
      new QName("http://www.neverland.org/peter/pan", "PeterPan");

  private static final String MAPPED_ATTRIBUTE = "ext.PeterPan.PANAlias";

  private static final String SOURCE_ID = "testSource";

  private XStreamWfs11FeatureTransformer transformer;

  @Before
  public void setup() throws IOException {
    transformer = new XStreamWfs11FeatureTransformer();
    transformer.setMetacardTypeRegistry(mockMetacardTypeRegistry());
  }

  @Test
  public void testMetacardMappers() throws IOException {
    transformer.setMetacardMappers(singletonList(mockMetacardMapper()));

    try (InputStream inputStream =
        new BufferedInputStream(getClass().getResourceAsStream("/FeatureMember.xml"))) {
      Optional<Metacard> metacardOptional = transformer.apply(inputStream, mockWfsMetadata());

      assertThat(metacardOptional.isPresent(), equalTo(true));
      assertThat(metacardOptional.get().getAttribute("title"), notNullValue());
    }
  }

  @Test
  public void testCoordinatesAreSwappedWhenCoordinateOrderIsLatLon() throws Exception {
    transformer.setMetacardMappers(singletonList(mockMetacardMapper()));

    try (final InputStream inputStream =
        new BufferedInputStream(getClass().getResourceAsStream("/FeatureMember.xml"))) {
      final Optional<Metacard> metacardOptional =
          transformer.apply(inputStream, mockWfsMetadata(LAT_LON_ORDER));

      assertThat(
          "The feature transformer should have returned a metacard but didn't.",
          metacardOptional.isPresent(),
          is(true));
      assertThat(metacardOptional.get().getLocation(), is("POINT (-123.26 49.4)"));
    }
  }

  @Test
  public void testCoordinatesAreNotSwappedWhenCoordinateOrderIsLonLat() throws Exception {
    transformer.setMetacardMappers(singletonList(mockMetacardMapper()));

    try (final InputStream inputStream =
        new BufferedInputStream(getClass().getResourceAsStream("/FeatureMember.xml"))) {
      final Optional<Metacard> metacardOptional =
          transformer.apply(inputStream, mockWfsMetadata(LON_LAT_ORDER));

      assertThat(
          "The feature transformer should have returned a metacard but didn't.",
          metacardOptional.isPresent(),
          is(true));
      assertThat(metacardOptional.get().getLocation(), is("POINT (49.4 -123.26)"));
    }
  }

  @Test
  public void testFeatureConverters() throws IOException {
    FeatureConverterFactory featureConverterFactory = mock(FeatureConverterFactory.class);
    when(featureConverterFactory.getFeatureType()).thenReturn(PETER_PAN_NAME.toString());

    FeatureConverter featureConverter = new GenericFeatureConverter(mockMetacardMapper());
    when(featureConverterFactory.createConverter()).thenReturn(featureConverter);
    transformer.setFeatureConverterFactories(singletonList(featureConverterFactory));

    try (InputStream inputStream =
        new BufferedInputStream(getClass().getResourceAsStream("/FeatureMember.xml"))) {
      Optional<Metacard> metacardOptional = transformer.apply(inputStream, mockWfsMetadata());

      assertThat(metacardOptional.isPresent(), equalTo(true));
      assertThat(metacardOptional.get().getAttribute("title"), notNullValue());
    }
  }

  @Test
  public void testBadMetacardReturnsNull() {
    Optional<Metacard> metacardOptional =
        transformer.apply(new ByteArrayInputStream(new byte[0]), mockWfsMetadata());
    assertThat(metacardOptional.isPresent(), equalTo(false));
  }

  private WfsMetacardTypeRegistry mockMetacardTypeRegistry() throws IOException {
    WfsMetacardTypeRegistry metacardTypeRegistry = mock(WfsMetacardTypeRegistry.class);

    XmlSchemaCollection schemaCollection = new XmlSchemaCollection();

    InputStream inputStream = getClass().getResourceAsStream("/Neverland_FeatureType.xsd");
    XmlSchema schema = schemaCollection.read(new StreamSource(inputStream));
    inputStream.close();

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema,
            PETER_PAN_NAME,
            Collections.emptySet(),
            Wfs11Constants.GML_3_1_1_NAMESPACE,
            FeatureMetacardType.DEFAULT_METACARD_TYPE_ENHANCER);

    when(metacardTypeRegistry.lookupMetacardTypeBySimpleName(
            SOURCE_ID, PETER_PAN_NAME.getLocalPart()))
        .thenReturn(Optional.of(featureMetacardType));

    return metacardTypeRegistry;
  }

  private WfsMetadata<FeatureTypeType> mockWfsMetadata() {
    WfsMetadata<FeatureTypeType> wfsMetadata = mock(WfsMetadata.class);
    when(wfsMetadata.getId()).thenReturn(SOURCE_ID);
    when(wfsMetadata.getFeatureMemberNodeNames()).thenReturn(singletonList("PeterPan"));
    when(wfsMetadata.getActiveFeatureMemberNodeName()).thenReturn("PeterPan");

    when(wfsMetadata.getDescriptors()).thenReturn(singletonList(mockFeatureType()));

    return wfsMetadata;
  }

  private WfsMetadata<FeatureTypeType> mockWfsMetadata(final String coordinateOrder) {
    final WfsMetadata<FeatureTypeType> wfsMetadata = mockWfsMetadata();
    when(wfsMetadata.getCoordinateOrder()).thenReturn(coordinateOrder);
    return wfsMetadata;
  }

  private FeatureTypeType mockFeatureType() {
    FeatureTypeType featureTypeType = new FeatureTypeType();
    featureTypeType.setName(PETER_PAN_NAME);
    featureTypeType.setDefaultSRS("urn:x-ogc:def:crs:EPSG:4326");

    return featureTypeType;
  }

  private MetacardMapper mockMetacardMapper() {
    MetacardMapper metacardMapper = mock(MetacardMapper.class);
    when(metacardMapper.getFeatureType()).thenReturn(PETER_PAN_NAME.toString());
    when(metacardMapper.getMetacardAttribute(MAPPED_ATTRIBUTE)).thenReturn(Core.TITLE);
    when(metacardMapper.getMetacardAttribute("SpatialData")).thenReturn(Core.LOCATION);
    return metacardMapper;
  }
}
