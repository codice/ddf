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
package org.codice.ddf.spatial.ogc.wfs.catalog.source.reader;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.WebApplicationException;
import org.codice.ddf.spatial.ogc.wfs.catalog.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.source.reader.FeatureCollectionMessageBodyReaderWfs10;
import org.junit.Test;

public class FeatureCollectionMessageBodyReaderTest {

  /** Positive test case to assure valid objects are unmarshalled */
  @Test
  public void testAllowedDeserialization() throws WebApplicationException, IOException {
    FeatureCollectionMessageBodyReaderWfs10 reader = new FeatureCollectionMessageBodyReaderWfs10();
    InputStream validWfsFeatureCollectionResponseXml =
        open("/validWfsFeatureCollectionResponse.xml");
    WfsFeatureCollection response =
        reader.readFrom(null, null, null, null, null, validWfsFeatureCollectionResponseXml);
    validWfsFeatureCollectionResponseXml.close();
    assertThat(response, notNullValue());
  }

  /** Negative test case to assure invalid objects are not unmarshalled */
  @Test(expected = WebApplicationException.class)
  public void testForbiddenDeserialization() throws WebApplicationException, IOException {
    FeatureCollectionMessageBodyReaderWfs10 reader = new FeatureCollectionMessageBodyReaderWfs10();
    try (InputStream dynamicProxySerializedXml = open("/dynamicProxySerialized.xml")) {
      reader.readFrom(null, null, null, null, null, dynamicProxySerializedXml);
    } catch (Exception e) {
      throw e;
    }
  }

  /** Helper method to open streams from resource */
  private InputStream open(String name) {
    return new BufferedInputStream(
        FeatureCollectionMessageBodyReaderTest.class.getResourceAsStream(name));
  }
}
