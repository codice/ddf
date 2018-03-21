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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Optional;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import net.opengis.wfs.v_1_1_0.FeatureTypeType;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.codice.ddf.spatial.ogc.wfs.catalog.message.api.WfsMetadata;
import org.codice.ddf.spatial.ogc.wfs.catalog.metacardtype.registry.api.WfsMetacardTypeRegistry;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.Wfs11Constants;
import org.junit.Before;
import org.junit.Test;

public class XStreamWfs11FeatureTransformerTest {

  private static final QName GENERAL_GIN_NAME =
      new QName("http://www.nga.mil/gims/gin", "GeneralGIN");

  private static final String MAPPED_ATTRIBUTE = "ext.GeneralGIN.NIPFTopic";

  private static final String SOURCE_ID = "testSource";

  private XStreamWfs11FeatureTransformer transformer;

  @Before
  public void setup() {
    MetacardMapper metacardMapper = mock(MetacardMapper.class);
    when(metacardMapper.getFeatureType()).thenReturn(GENERAL_GIN_NAME.toString());
    when(metacardMapper.getMetacardAttribute(MAPPED_ATTRIBUTE)).thenReturn("title");

    transformer = new XStreamWfs11FeatureTransformer();
    transformer.setMetacardMappers(Collections.singletonList(metacardMapper));
    transformer.setMetacardTypeRegistry(mockMetacardTypeRegistry());
  }

  @Test
  public void testRed() {
    InputStream inputStream =
        new BufferedInputStream(getClass().getResourceAsStream("/FeatureMember.xml"));
    Optional<Metacard> metacardOptional = transformer.apply(inputStream, mockWfsMetadata());

    assertThat(metacardOptional.isPresent(), equalTo(true));
    assertThat(metacardOptional.get().getAttribute("title"), notNullValue());
  }

  private WfsMetacardTypeRegistry mockMetacardTypeRegistry() {
    WfsMetacardTypeRegistry metacardTypeRegistry = mock(WfsMetacardTypeRegistry.class);

    XmlSchemaCollection schemaCollection = new XmlSchemaCollection();
    InputStream inputStream = getClass().getResourceAsStream("/GeneralGIN_FeatureType.xsd");
    XmlSchema schema = schemaCollection.read(new StreamSource(inputStream));
    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema,
            GENERAL_GIN_NAME,
            Collections.emptyList(),
            Wfs11Constants.GML_3_1_1_NAMESPACE,
            FeatureMetacardType.DEFAULT_METACARD_TYPE_ENHANCER);

    when(metacardTypeRegistry.lookupMetacardTypeBySimpleName(
            SOURCE_ID, GENERAL_GIN_NAME.getLocalPart()))
        .thenReturn(Optional.of(featureMetacardType));

    return metacardTypeRegistry;
  }

  private WfsMetadata<FeatureTypeType> mockWfsMetadata() {
    WfsMetadata<FeatureTypeType> wfsMetadata = mock(WfsMetadata.class);
    when(wfsMetadata.getId()).thenReturn(SOURCE_ID);

    FeatureTypeType featureTypeType = new FeatureTypeType();
    featureTypeType.setName(GENERAL_GIN_NAME);
    featureTypeType.setDefaultSRS("urn:x-ogc:def:crs:EPSG:4326");
    when(wfsMetadata.getDescriptors()).thenReturn(Collections.singletonList(featureTypeType));

    return wfsMetadata;
  }
}
