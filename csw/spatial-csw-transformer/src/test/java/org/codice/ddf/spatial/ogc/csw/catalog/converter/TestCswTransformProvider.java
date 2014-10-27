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
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.TreeMarshaller;
import com.thoughtworks.xstream.core.TreeUnmarshaller;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.WstxDriver;
import com.thoughtworks.xstream.io.xml.XppReader;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transform.MetacardTransformer;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.transformer.TransformerManager;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.xmlpull.v1.XmlPullParserFactory;

import javax.activation.MimeType;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestCswTransformProvider {

    private TransformerManager<InputTransformer> mockInputManager = mock(TransformerManager.class);

    private TransformerManager<MetacardTransformer> mockMetacardManager = mock(TransformerManager.class);

    private CswRecordConverter mockCswRecordConverter = mock(CswRecordConverter.class);

    private MetacardTransformer mockMetacardTransformer = mock(MetacardTransformer.class);

    @Test
    public void testMarshalCswRecord() throws Exception {
        when(mockMetacardManager.getTransformerBySchema(CswConstants.CSW_OUTPUT_SCHEMA)).thenReturn(
                mockCswRecordConverter);

        when(mockCswRecordConverter.transform(any(Metacard.class), any(Map.class))).thenReturn(
                new BinaryContentImpl(IOUtils.toInputStream(getRecord()),
                        new MimeType(MediaType.APPLICATION_XML)));

        StringWriter stringWriter = new StringWriter();
        HierarchicalStreamWriter writer = new WstxDriver().createWriter(stringWriter);
        CswTransformProvider provider = new CswTransformProvider(mockMetacardManager, null);
        MarshallingContext context = new TreeMarshaller(writer, null, null);

        ArgumentCaptor<String> captor = ArgumentCaptor
                .forClass(String.class);

        provider.marshal(getMetacard(), writer, context);

        // Verify the context arguments were set correctly
        verify(mockMetacardManager, times(1)).getTransformerBySchema(captor.capture());

        String outputSchema = captor.getValue();

        assertThat(outputSchema, is(CswConstants.CSW_OUTPUT_SCHEMA));
    }

    @Test
    public void testMarshalOtherSchema() throws Exception {
        final String OTHER_SCHEMA = "http://example.com/scheam.xsd";
        when(mockMetacardManager.getTransformerBySchema(OTHER_SCHEMA)).thenReturn(
                mockMetacardTransformer);

        when(mockMetacardTransformer.transform(any(Metacard.class), any(Map.class))).thenReturn(
                new BinaryContentImpl(IOUtils.toInputStream(getRecord()),
                        new MimeType(MediaType.APPLICATION_XML)));

        StringWriter stringWriter = new StringWriter();
        HierarchicalStreamWriter writer = new WstxDriver().createWriter(stringWriter);
        CswTransformProvider provider = new CswTransformProvider(mockMetacardManager, null);
        MarshallingContext context = new TreeMarshaller(writer, null, null);
        context.put(CswConstants.OUTPUT_SCHEMA_PARAMETER, OTHER_SCHEMA);

        ArgumentCaptor<String> captor = ArgumentCaptor
                .forClass(String.class);

        provider.marshal(getMetacard(), writer, context);

        // Verify the context arguments were set correctly
        verify(mockMetacardManager, times(1)).getTransformerBySchema(captor.capture());

        String outputSchema = captor.getValue();

        assertThat(outputSchema, is(OTHER_SCHEMA));
    }

    @Test (expected = ConversionException.class)
    public void testMarshalNoTransformers() throws Exception {
        when(mockMetacardManager.getTransformerBySchema(anyString())).thenReturn(null);

        StringWriter stringWriter = new StringWriter();
        HierarchicalStreamWriter writer = new WstxDriver().createWriter(stringWriter);
        CswTransformProvider provider = new CswTransformProvider(mockMetacardManager, null);
        MarshallingContext context = new TreeMarshaller(writer, null, null);

        provider.marshal(getMetacard(), writer, context);

    }

    @Test
    public void testUnmarshalCswRecord() throws Exception {
        when(mockInputManager.getTransformerBySchema(CswConstants.CSW_OUTPUT_SCHEMA)).thenReturn(
                mockCswRecordConverter);

        HierarchicalStreamReader reader = new WstxDriver().createReader(new StringReader(getRecord()));
        CswTransformProvider provider = new CswTransformProvider(null, mockInputManager);
        UnmarshallingContext context = new TreeUnmarshaller(null, null, null, null);

        ArgumentCaptor<String> captor = ArgumentCaptor
                .forClass(String.class);

        Metacard metacard = (Metacard) provider.unmarshal(reader, context);

        // Verify the context arguments were set correctly
        verify(mockInputManager, times(1)).getTransformerBySchema(captor.capture());

        String outputSchema = captor.getValue();

        assertThat(outputSchema, is(CswConstants.CSW_OUTPUT_SCHEMA));
    }

    @Test
    public void testUnmarshalOtherSchema() throws Exception {
        InputTransformer mockInputTransformer = mock(InputTransformer.class);
        final String OTHER_SCHEMA = "http://example.com/scheam.xsd";
        when(mockInputManager.getTransformerBySchema(OTHER_SCHEMA)).thenReturn(
                mockInputTransformer);

        when(mockInputTransformer.transform(any(InputStream.class))).thenReturn(getMetacard());

        // XppReader is not namespace aware so it will read the XML and ignore the namespaces
        // WstxReader is namespace aware. It may fail for XML fragments.
        HierarchicalStreamReader reader = new XppReader(new StringReader(getRecord()),
                XmlPullParserFactory.newInstance().newPullParser());
        CswTransformProvider provider = new CswTransformProvider(null, mockInputManager);
        UnmarshallingContext context = new TreeUnmarshaller(null, null, null, null);
        context.put(CswConstants.OUTPUT_SCHEMA_PARAMETER, OTHER_SCHEMA);

        ArgumentCaptor<String> captor = ArgumentCaptor
                .forClass(String.class);

        Metacard metacard = (Metacard) provider.unmarshal(reader, context);

        // Verify the context arguments were set correctly
        verify(mockInputManager, times(1)).getTransformerBySchema(captor.capture());

        String outputSchema = captor.getValue();

        assertThat(outputSchema, is(OTHER_SCHEMA));
    }

    @Test (expected = ConversionException.class)
    public void testUnmarshalNoTransformers() throws Exception {
        when(mockInputManager.getTransformerBySchema(anyString())).thenReturn(null);

        HierarchicalStreamReader reader = new WstxDriver().createReader(new StringReader(getRecord()));
        CswTransformProvider provider = new CswTransformProvider(null, mockInputManager);
        UnmarshallingContext context = new TreeUnmarshaller(null, null, null, null);

        provider.unmarshal(reader, context);

    }

    private String getRecord() {
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