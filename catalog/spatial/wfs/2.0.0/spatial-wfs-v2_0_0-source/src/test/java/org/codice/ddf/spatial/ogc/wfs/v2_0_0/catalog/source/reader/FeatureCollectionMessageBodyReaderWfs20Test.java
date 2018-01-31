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
package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.source.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.WebApplicationException;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20FeatureCollection;
import org.junit.Test;

public class FeatureCollectionMessageBodyReaderWfs20Test {
  /** Positive test case to assure valid objects are unmarshalled */
  @Test
  public void testAllowedDeserialization() throws WebApplicationException, IOException {
    FeatureCollectionMessageBodyReaderWfs20 reader = new FeatureCollectionMessageBodyReaderWfs20();
    InputStream validWfsFeatureCollectionResponseXml =
        open("/validWfsFeatureCollectionResponse.xml");
    Wfs20FeatureCollection response =
        reader.readFrom(null, null, null, null, null, validWfsFeatureCollectionResponseXml);
    validWfsFeatureCollectionResponseXml.close();
    assertThat(response, notNullValue());
  }

  /** Negative test case to assure invalid objects are not unmarshalled */
  @Test
  public void testForbiddenDeserialization() throws WebApplicationException, IOException {
    FeatureCollectionMessageBodyReaderWfs20 reader = new FeatureCollectionMessageBodyReaderWfs20();
    InputStream dynamicProxySerializedXml = open("/dynamicProxySerialized.xml");
    Wfs20FeatureCollection response =
        reader.readFrom(null, null, null, null, null, dynamicProxySerializedXml);
    dynamicProxySerializedXml.close();
    assertThat(response, nullValue());
  }
  /** Helper method to open streams from resource */
  private InputStream open(String name) {
    return new BufferedInputStream(
        FeatureCollectionMessageBodyReaderWfs20Test.class.getResourceAsStream(name));
  }
}
