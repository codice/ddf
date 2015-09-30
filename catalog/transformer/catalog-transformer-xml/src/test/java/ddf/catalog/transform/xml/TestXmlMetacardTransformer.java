/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.transform.xml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.xml.XmlParser;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.api.MetacardMarshaller;
import ddf.catalog.transformer.xml.MetacardMarshallerImpl;
import ddf.catalog.transformer.xml.PrintWriterProviderImpl;
import ddf.catalog.transformer.xml.XmlMetacardTransformer;

public class TestXmlMetacardTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestXmlMetacardTransformer.class);

    private XmlMetacardTransformer transformer;

    private Map<String, Serializable> emptyArgs = Collections.EMPTY_MAP;

    @Before
    public void setup() {
        Parser parser = new XmlParser();
        MetacardMarshaller metacardMarshaller = new MetacardMarshallerImpl(parser,
                new PrintWriterProviderImpl());
        transformer = new XmlMetacardTransformer(metacardMarshaller);
    }

    /*
    */
    @Test
    public void testMetacardTypeNameEmpty()
            throws CatalogTransformerException, IOException, XpathException, SAXException {
        Metacard mc = mock(Metacard.class);

        MetacardType mct = mock(MetacardType.class);
        when(mct.getName()).thenReturn("");
        when(mct.getAttributeDescriptors()).thenReturn(Collections.<AttributeDescriptor>emptySet());

        when(mc.getMetacardType()).thenReturn(mct);
        when(mc.getId()).thenReturn(null);
        when(mc.getSourceId()).thenReturn(null);

        BinaryContent bc = transformer.transform(mc, emptyArgs);
        String outputXml = new String(bc.getByteArray());
        //LOGGER.info(outputXml);
        Map<String, String> m = new HashMap<>();
        m.put("m", "urn:catalog:metacard");
        NamespaceContext ctx = new SimpleNamespaceContext(m);
        XMLUnit.setXpathNamespaceContext(ctx);
        assertXpathEvaluatesTo(MetacardType.DEFAULT_METACARD_TYPE_NAME, "/m:metacard/m:type",
                outputXml);
    }

    /*
    */
    @Test
    public void testMetacardTypeNameNull()
            throws CatalogTransformerException, SAXException, IOException, XpathException {
        Metacard mc = mock(Metacard.class);

        MetacardType mct = mock(MetacardType.class);
        when(mct.getName()).thenReturn(null);
        when(mct.getAttributeDescriptors()).thenReturn(Collections.<AttributeDescriptor>emptySet());

        when(mc.getMetacardType()).thenReturn(mct);
        when(mc.getId()).thenReturn(null);
        when(mc.getSourceId()).thenReturn(null);

        BinaryContent bc = transformer.transform(mc, emptyArgs);
        String outputXml = new String(bc.getByteArray());
        //LOGGER.info(outputXml);
        Map<String, String> m = new HashMap<>();
        m.put("m", "urn:catalog:metacard");
        NamespaceContext ctx = new SimpleNamespaceContext(m);
        XMLUnit.setXpathNamespaceContext(ctx);
        assertXpathEvaluatesTo(MetacardType.DEFAULT_METACARD_TYPE_NAME, "/m:metacard/m:type",
                outputXml);
    }

    @Test
    public void testXmlMetacardTransformerSparse() throws CatalogTransformerException {

        MetacardImpl mc = new MetacardImpl();

        mc.setId("1234567890987654321");
        mc.setSourceId("FooBarSource");
        mc.setTitle("Title!");
        mc.setExpirationDate(new Date());
        mc.setLocation(
                "POLYGON ((35 10, 10 20, 15 40, 45 45, 35 10),(20 30, 35 35, 30 20, 20 30))");
        mc.setMetadata(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><foo><bar/></foo>");
        byte[] bytes = {0, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0,
                0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1};
        mc.setThumbnail(bytes);

        Metacard mci = (Metacard) mc;

        BinaryContent bc = transformer.transform(mci, emptyArgs);

        if (bc == null) {
            fail("Binary Content is null.");
        }

        // TODO add assertions. Use XMLunit?

        BufferedReader in = new BufferedReader(new InputStreamReader(bc.getInputStream()));
        String inputLine;
        try {
            LOGGER.debug("\n* * * START XML METACARD REPRESENTATION * * * \n");
            while ((inputLine = in.readLine()) != null) {
                LOGGER.debug(inputLine);
            }
            in.close();
            LOGGER.debug("\n* * * END XML METACARD REPRESENTATION * * * \n");
        } catch (IOException e) { // TODO Auto-generated catch block
            LOGGER.error("IOException during test", e);
        }

    }

    @Test
    public void testXmlMetacardTransformer() throws Exception {

        MetacardImpl mc = new MetacardImpl();

        final String testId = "1234567890987654321";
        final String testSource = "FooBarSource";
        final String testTitle = "Title!";
        final Date testDate = new Date();
        final String testLocation = "POLYGON ((35 10, 10 20, 15 40, 45 45, 35 10),(20 30, 35 35, 30 20, 20 30))";
        final byte[] testThumbnail = {0, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1,
                1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1,
                1};

        mc.setId(testId);
        mc.setSourceId(testSource);
        mc.setTitle(testTitle);
        mc.setExpirationDate(testDate);
        mc.setLocation(testLocation);
        mc.setThumbnail(testThumbnail);

        String metadata = null;
        FileInputStream stream = new FileInputStream(
                new File("src/test/resources/extensibleMetacard.xml"));
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            /* Instead of using default, pass in a decoder. */
            metadata = Charset.defaultCharset().decode(bb).toString();
        } finally {
            stream.close();
        }

        mc.setMetadata(metadata);

        Metacard mci = (Metacard) mc;

        BinaryContent bc = transformer.transform(mci, emptyArgs);

        if (bc == null) {
            fail("Binary Content is null.");
        }

        String outputXml = new String(bc.getByteArray());

        LOGGER.debug("\n* * * START XML METACARD REPRESENTATION * * * \n");
        LOGGER.debug(outputXml);
        LOGGER.debug("\n* * * END XML METACARD REPRESENTATION * * * \n");

        Map<String, String> m = new HashMap<String, String>();
        m.put("gml", "http://www.opengis.net/gml");
        m.put("m", "urn:catalog:metacard");
        m.put("", "urn:catalog:metacard");
        NamespaceContext ctx = new SimpleNamespaceContext(m);
        XMLUnit.setXpathNamespaceContext(ctx);

        assertXpathEvaluatesTo(testId, "/m:metacard/@gml:id", outputXml);
        assertXpathEvaluatesTo(testSource, "/m:metacard/m:source", outputXml);
        assertXpathEvaluatesTo(testTitle, "/m:metacard/m:string[@name='title']/m:value", outputXml);

        // TODO convert GML representation?
        // assertXpathEvaluatesTo(testLocation,"/m:metacard/m:geometry[@name='location']/m:value",
        // outputXml);
        assertXpathExists("/m:metacard/m:geometry[@name='location']/m:value", outputXml);

        // TODO Base64 check?
        // assertXpathEvaluatesTo(testThumbnail,
        // "/metacard/base64Binary[@id='thumbnail']", outputXml);
        assertXpathExists("/m:metacard/m:base64Binary[@name='thumbnail']/m:value", outputXml);

        // TODO XML Date representation?
        assertXpathExists("/m:metacard/m:dateTime[@name='expiration']/m:value", outputXml);

    }
}
