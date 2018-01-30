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
package org.codice.ddf.spatial.ogc.wfs.v110.catalog.source.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.Wfs11Constants;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.converter.impl.GenericFeatureConverterWfs11;
import org.junit.Test;
import org.xml.sax.SAXException;

public class FeatureCollectionMessageBodyReaderWfs11Test {

  @Test
  public void testMultiLineStringSrs26713LineStringMemberLineStringPosList()
      throws IOException, SAXException {

    XmlSchemaCollection schemaCollection = new XmlSchemaCollection();

    try (InputStream inputStream = open("/example.xsd")) {

      XmlSchema schema = schemaCollection.read(new StreamSource(inputStream));

      FeatureMetacardType ftMetacard =
          new FeatureMetacardType(
              schema,
              new QName("http://example.com", "roads", "sf"),
              new ArrayList<>(),
              Wfs11Constants.GML_3_1_1_NAMESPACE);

      FeatureConverter featureConverter =
          new GenericFeatureConverterWfs11("urn:x-ogc:def:crs:EPSG:26713");

      featureConverter.setSourceId("wfs110");
      featureConverter.setMetacardType(ftMetacard);
      featureConverter.setWfsUrl("http://example.com/geoserver/wfs");

      FeatureCollectionMessageBodyReaderWfs11 reader =
          new FeatureCollectionMessageBodyReaderWfs11();

      reader.registerConverter(featureConverter);

      try (InputStream validWfsFeatureCollectionResponseXml =
          open("/multilinestring-eps26713-linestringmember-linestring-poslist.xml")) {
        WfsFeatureCollection response =
            reader.readFrom(null, null, null, null, null, validWfsFeatureCollectionResponseXml);
        validWfsFeatureCollectionResponseXml.close();
        assertThat(response, notNullValue());
      }
    }
  }

  private InputStream open(String name) {
    return new BufferedInputStream(
        FeatureCollectionMessageBodyReaderWfs11Test.class.getResourceAsStream(name));
  }
}
