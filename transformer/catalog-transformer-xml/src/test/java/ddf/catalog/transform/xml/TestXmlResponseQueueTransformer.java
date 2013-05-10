/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.transform.xml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BasicTypes;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardImpl;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.ResultImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.SourceResponseImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.xml.XmlMetacardTransformer;
import ddf.catalog.transformer.xml.XmlResponseQueueTransformer;

/**
 * Tests the {@link XmlResponseQueueTransformer} transformations
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 *
 */
public class TestXmlResponseQueueTransformer {
    
    private static final String DEFAULT_ID = "myID";

    private boolean verboseDebug = false;
    
    private static final String DEFAULT_TYPE_NAME = BasicTypes.BASIC_METACARD.getName();
    private static final Date DEFAULT_EXPIRATION_DATE = new DateTime(123456789).toDate();
    private static final String DEFAULT_TITLE = "myTitle";
    private static final String DEFAULT_GEO = "POLYGON ((35 10, 10 20, 15 40, 45 45, 35 10),(20 30, 35 35, 30 20, 20 30))";
    private static final String DEFAULT_METADATA = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><foo><bar/></foo>";
    private static final byte[] DEFAULT_BYTES = new byte[] {0, 0, 1};
    private static final String DEFAULT_BASE64 = "AAAB";
    private static final Logger LOGGER = Logger
            .getLogger(TestXmlResponseQueueTransformer.class);

    private static final String DEFAULT_SOURCE_ID = "mySourceId";

    static {
        BasicConfigurator.configure();
    }

    @BeforeClass
    public static void setupTestClass() {

        // makes xpaths easier to write when prefixes are declared beforehand.
        HashMap map = new HashMap();
        map.put("gml", "http://www.opengis.net/gml");
        map.put("mc", "urn:catalog:metacard");

        NamespaceContext ctx = new SimpleNamespaceContext(map);
        XMLUnit.setXpathNamespaceContext(ctx);
    }

    /**
     * Should throw exception when given {@code null} input
     * @throws CatalogTransformerException
     */
    @Test(expected=CatalogTransformerException.class)
    public void testNullSourceResponse() throws CatalogTransformerException {

        // given
        XmlResponseQueueTransformer transformer = new XmlResponseQueueTransformer();

        // when
        BinaryContent binaryContent = transformer.transform(null, null);

        // then
        // failure should occur

    }
    
    /**
     * No {@link MetacardType} name should use the default name.
     * 
     * @throws CatalogTransformerException
     * @throws IOException
     * @throws SAXException
     * @throws XpathException
     */
    @Test
    public void testMetacardTypeName_Null() throws CatalogTransformerException,
            IOException, XpathException, SAXException {

        // given
        XmlResponseQueueTransformer transformer = new XmlResponseQueueTransformer();

        SourceResponse response = givenMetacardTypeName(null);

        // when
        BinaryContent binaryContent = transformer.transform(response, null);

        // then
        assertThat(binaryContent.getMimeType(), is(XmlResponseQueueTransformer.MIME_TYPE));

        byte[] bytes = binaryContent.getByteArray();

        String output = new String(bytes);

        print(output, verboseDebug);

        assertXpathEvaluatesTo(DEFAULT_TYPE_NAME,
                "/mc:metacards/mc:metacard/mc:type", output);

    }
    
    /**
     * No {@link MetacardType} name should use the default name.
     * 
     * @throws CatalogTransformerException
     * @throws IOException
     * @throws SAXException
     * @throws XpathException
     */
    @Test
    public void testMetacardTypeName_Empty() throws CatalogTransformerException,
            IOException, XpathException, SAXException {

        // given
        XmlResponseQueueTransformer transformer = new XmlResponseQueueTransformer();

        SourceResponse response = givenMetacardTypeName("");

        // when
        BinaryContent binaryContent = transformer.transform(response, null);

        // then
        assertThat(binaryContent.getMimeType(), is(XmlResponseQueueTransformer.MIME_TYPE));

        byte[] bytes = binaryContent.getByteArray();

        String output = new String(bytes);

        print(output, verboseDebug);

        assertXpathEvaluatesTo(DEFAULT_TYPE_NAME,
                "/mc:metacards/mc:metacard/mc:type", output);

    }

    @Test
    public void testNoIdNoSourceId() throws CatalogTransformerException, IOException, XpathException, SAXException {

        // given
        XmlResponseQueueTransformer transformer = new XmlResponseQueueTransformer();

        SourceResponse response = givenSourceResponse(null, null);

        // when
        BinaryContent binaryContent = transformer.transform(response, null);
        
        // then
        assertThat(binaryContent.getMimeType(), is(XmlResponseQueueTransformer.MIME_TYPE));

        byte[] bytes = binaryContent.getByteArray();

        String output = new String(bytes);

        print(output, verboseDebug);

        assertXpathNotExists("/mc:metacards/mc:metacard/mc:source", output);
        
        assertXpathNotExists("/mc:metacards/mc:metacard/@gml:id", output);
        
        verifyDefaults("1", output);
    }


    @Test
    public void testNoId() throws CatalogTransformerException, IOException, XpathException, SAXException {

        // given
        XmlResponseQueueTransformer transformer = new XmlResponseQueueTransformer();

        SourceResponse response = givenSourceResponse(DEFAULT_SOURCE_ID, null);

        // when
        BinaryContent binaryContent = transformer.transform(response, null);
        
        // then
        assertThat(binaryContent.getMimeType(), is(XmlResponseQueueTransformer.MIME_TYPE));

        byte[] bytes = binaryContent.getByteArray();

        String output = new String(bytes);

        print(output, verboseDebug);

        assertXpathEvaluatesTo(DEFAULT_SOURCE_ID,
                "/mc:metacards/mc:metacard/mc:source", output);
        
        assertXpathNotExists("/mc:metacards/mc:metacard/@gml:id", output);
        
        verifyDefaults("1", output);
    }
    
    @Test
    public void testStub() throws CatalogTransformerException, IOException, XpathException, SAXException {

        // given
        XmlResponseQueueTransformer transformer = new XmlResponseQueueTransformer();

        SourceResponse response = givenSourceResponse(DEFAULT_SOURCE_ID, DEFAULT_ID);

        // when
        BinaryContent binaryContent = transformer.transform(response, null);
        
        // then
        assertThat(binaryContent.getMimeType(), is(XmlResponseQueueTransformer.MIME_TYPE));

        byte[] bytes = binaryContent.getByteArray();

        String output = new String(bytes);

        print(output, verboseDebug);

        assertXpathEvaluatesTo(DEFAULT_SOURCE_ID,
                "/mc:metacards/mc:metacard/mc:source", output);
        
        assertXpathEvaluatesTo(DEFAULT_ID,
                "/mc:metacards/mc:metacard/@gml:id", output);
        
        verifyDefaults("1", output);
    }

    @Test
    public void testMultiple() throws CatalogTransformerException, IOException, XpathException, SAXException {

        // given
        XmlResponseQueueTransformer transformer = new XmlResponseQueueTransformer();

        SourceResponse response = givenSourceResponse(new MetacardStub("source1", "id1"),new MetacardStub("source2", "id2"));

        // when
        BinaryContent binaryContent = transformer.transform(response, null);
        
        // then
        assertThat(binaryContent.getMimeType(), is(XmlResponseQueueTransformer.MIME_TYPE));

        byte[] bytes = binaryContent.getByteArray();

        String output = new String(bytes);

        print(output, verboseDebug);

        assertXpathEvaluatesTo("source1",
                "/mc:metacards/mc:metacard[1]/mc:source", output);
        
        assertXpathEvaluatesTo("id1",
                "/mc:metacards/mc:metacard[1]/@gml:id", output);
        
        assertXpathEvaluatesTo("source2",
                "/mc:metacards/mc:metacard[2]/mc:source", output);
        
        assertXpathEvaluatesTo("id2",
                "/mc:metacards/mc:metacard[2]/@gml:id", output);
        
        verifyDefaults("1", output);
        verifyDefaults("2", output);
    }


    @Test
    public void testDdms() throws Exception {

        MetacardImpl mc = new MetacardImpl();

        final String testId = "1234567890987654321";
        final String testSource = "FooBarSource";
        final String testTitle = "Title!";
        final Date testDate = new Date();
        final String testLocation = "POLYGON ((35 10, 10 20, 15 40, 45 45, 35 10),(20 30, 35 35, 30 20, 20 30))";
        final byte[] testThumbnail = {0, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1,
                1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1,
                1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1};

        mc.setId(testId);
        mc.setSourceId(testSource);
        mc.setTitle(testTitle);
        mc.setExpirationDate(testDate);
        mc.setLocation(testLocation);
        mc.setThumbnail(testThumbnail);

        String metadata = null;
        FileInputStream stream = new FileInputStream(new File(
                "src/test/resources/ddms.xml"));
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0,
                    fc.size());
            /* Instead of using default, pass in a decoder. */
            metadata = Charset.defaultCharset().decode(bb).toString();
        } finally {
            stream.close();
        }

        mc.setMetadata(metadata);

        Metacard mci = (Metacard) mc;

        XmlResponseQueueTransformer transformer = new XmlResponseQueueTransformer();

    
        SourceResponse response = new SourceResponseImpl(null,
                Arrays.asList((Result) new ResultImpl(mci)));
        BinaryContent bc = transformer.transform(response, null);

        if (bc == null) {
            fail("Binary Content is null.");
        }

        String outputXml = new String(bc.getByteArray());

        LOGGER.debug(outputXml);

        assertXpathEvaluatesTo(testId, "/mc:metacards/mc:metacard/@gml:id", outputXml);
        assertXpathEvaluatesTo(testSource, "/mc:metacards/mc:metacard/mc:source", outputXml);
        assertXpathEvaluatesTo(testTitle,
                "/mc:metacards/mc:metacard/mc:string[@name='title']/mc:value", outputXml);

        // TODO convert GML representation?
        // outputXml);
        assertXpathExists("/mc:metacards/mc:metacard/mc:geometry[@name='location']/mc:value",
                outputXml);

        assertXpathExists(
                "/mc:metacards/mc:metacard/mc:base64Binary[@name='thumbnail']/mc:value",
                outputXml);

        // TODO XML Date representation?
        assertXpathExists("/mc:metacards/mc:metacard/mc:dateTime[@name='expiration']/mc:value",
                outputXml);

    }

    /**
     * @return
     */
   private MetacardType getMetacardTypeStub(String name,
            Set<AttributeDescriptor> descriptors) {
    
        MetacardType type = mock(MetacardType.class);
    
        when(type.getName()).thenReturn(name);
    
        when(type.getAttributeDescriptors()).thenReturn(descriptors);
    
        return type;
    }

    /**
     * @param index TODO
     * @param output
     * @throws IOException
     * @throws SAXException
     * @throws XpathException
     */
    private void verifyDefaults(String index, String output) throws IOException, SAXException,
            XpathException {
        assertXpathEvaluatesTo(DEFAULT_TYPE_NAME,
                "/mc:metacards/mc:metacard["+index+"]/mc:type", output);
        assertXpathExists("/mc:metacards/mc:metacard["+index+"]/mc:geometry[@name='location']//gml:Polygon", output);
        assertXpathExists("/mc:metacards/mc:metacard["+index+"]/mc:dateTime[@name='expiration']", output);
        assertXpathExists("/mc:metacards/mc:metacard["+index+"]/mc:stringxml[@name='metadata']", output);
        assertXpathEvaluatesTo(DEFAULT_TITLE,"/mc:metacards/mc:metacard["+index+"]/mc:string[@name='title']/mc:value", output);
        assertXpathEvaluatesTo(DEFAULT_BASE64,"/mc:metacards/mc:metacard["+index+"]/mc:base64Binary[@name='thumbnail']/mc:value", output);
    }


    /**
     * @return
     */
    private SourceResponseImpl givenSourceResponse(String sourceId, String id) {
        return new SourceResponseImpl(null,
                Arrays.asList((Result) new ResultImpl(new MetacardStub(sourceId,
                        id))));
    }
    
    private SourceResponseImpl givenSourceResponse(Metacard... metacards) {
        
        List<Result> results = new ArrayList<Result>();
        for(Metacard m : metacards) {
            results.add(new ResultImpl(m));
        }
        
        return new SourceResponseImpl(null,results);
    }


    private SourceResponse givenMetacardTypeName(String metacardTypeName) {
        MetacardType type = getMetacardTypeStub(metacardTypeName,
                new HashSet<AttributeDescriptor>());
    
        Metacard metacard = new MetacardImpl(type);
    
        SourceResponse response = new SourceResponseImpl(null,
                Arrays.asList((Result) new ResultImpl(metacard)));
        return response;
    }

    private void print(String output, boolean inFull) {
        if (inFull) {
            LOGGER.debug(output);
        }
    }

    class MetacardStub extends MetacardImpl {

        public MetacardStub(String sourceId, String id) {
            
            super(new MetacardImpl());
            setSourceId(sourceId);
            setId(id);

            setTitle(DEFAULT_TITLE);
            setExpirationDate(DEFAULT_EXPIRATION_DATE);
            setLocation(DEFAULT_GEO);
            setMetadata(DEFAULT_METADATA);
            setThumbnail(DEFAULT_BYTES);
        }
    }
}
