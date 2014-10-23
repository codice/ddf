/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.spatial.ogc.csw.catalog.converter.impl;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.TreeUnmarshaller;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.xml.XppReader;
import com.thoughtworks.xstream.io.xml.xppdom.XppFactory;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.InputTransformer;
import org.codice.ddf.spatial.ogc.csw.catalog.transformer.TransformerManager;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestCswTransformProvider {

    private TransformerManager<InputTransformer> mockManager = mock(TransformerManager.class);

    private InputTransformer mockInputTransformer = mock(InputTransformer.class);

    @Test
    public void testMarshal() throws Exception {

    }

    @Test
    public void testUnmarshal() throws Exception {
        when(mockManager.getTransformerBySchema(Matchers.any(String.class))).thenReturn(mockInputTransformer);
        when(mockInputTransformer.transform(any(InputStream.class))).thenReturn(getMetacard());
        XmlPullParser parser = XppFactory.createDefaultParser();
        HierarchicalStreamReader reader = new XppReader(new StringReader(getRecord()), parser);
        CswTransformProvider provider = new CswTransformProvider(null, mockManager);
        UnmarshallingContext context = new TreeUnmarshaller(null, null, null, null);
        Metacard metacard = (Metacard) provider.unmarshal(reader, context);

        // TODO - assert things here


    }

    public String getRecord() {
        return "<Record>\n"
                + "  <dct:created>2014-11-01T00:00:00.000-05:00</dct:created>\n"
                + "  <dc:title>This is my title</dc:title>\n"
                + "  <dct:alternative>This is my title</dct:alternative>\n"
                + "  <location>POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))</location>\n"
                + "  <dc:date>2016-11-01T00:00:00.000-05:00</dc:date>\n"
                + "  <dct:modified>2016-11-01T00:00:00.000-05:00</dct:modified>\n"
                + "  <dct:dateSubmitted>2016-11-01T00:00:00.000-05:00</dct:dateSubmitted>\n"
                + "  <dct:issued>2016-11-01T00:00:00.000-05:00</dct:issued>\n"
                + "  <dc:identifier>ID</dc:identifier>\n"
                + "  <dct:bibliographicCitation>ID</dct:bibliographicCitation>\n"
                + "  <resource-size>123TB</resource-size>\n"
                + "  <dct:dateAccepted>2015-11-01T00:00:00.000-05:00</dct:dateAccepted>\n"
                + "  <dct:dateCopyrighted>2015-11-01T00:00:00.000-05:00</dct:dateCopyrighted>\n"
                + "  <dc:type>I have some content type</dc:type>\n"
                + "  <metadata>metadata a whole bunch of metadata</metadata>\n"
                + "  <metadata-content-type-version>1.0.0</metadata-content-type-version>\n"
                + "  <dc:source>http://host:port/my/product.pdf</dc:source>\n"
                + "  <dc:publisher>sourceID</dc:publisher>\n"
                + "  <ows:BoundingBox crs=\"urn:x-ogc:def:crs:EPSG:6.11:4326\">\n"
                + "    <ows:LowerCorner>10.0 10.0</ows:LowerCorner>\n"
                + "    <ows:UpperCorner>40.0 40.0</ows:UpperCorner>\n"
                + "  </ows:BoundingBox>\n"
                + "</Record>";
    }

    private Metacard getMetacard() {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setContentTypeName("I have some content type");
        metacard.setContentTypeVersion("1.0.0");
//        metacard.setCreatedDate(CREATED_DATE.getTime());
//        metacard.setEffectiveDate(EFFECTIVE_DATE.getTime());
        metacard.setId("ID");
        metacard.setLocation("POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))");
        metacard.setMetadata("metadata a whole bunch of metadata");
//        metacard.setModifiedDate(MODIFIED_DATE.getTime());
        metacard.setResourceSize("123TB");
        metacard.setSourceId("sourceID");
        metacard.setTitle("This is my title");
//        try {
//            metacard.setResourceURI(new URI(TEST_URI));
//        } catch (URISyntaxException e) {
//            LOGGER.debug("URISyntaxException", e);
//        }

        return metacard;
    }



}