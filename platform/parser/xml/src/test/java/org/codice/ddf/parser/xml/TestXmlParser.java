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
 **/
package org.codice.ddf.parser.xml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.bind.util.JAXBSource;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;

import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.parser.xml.domain.ChildElement;
import org.codice.ddf.parser.xml.domain.MotherElement;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.google.common.collect.ImmutableList;

public class TestXmlParser {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Parser parser;

    private ParserConfigurator configurator;

    private MotherElement mother;

    private ChildElement luke;

    private ChildElement leia;

    @Before
    public void setUp() throws Exception {
        parser = new XmlParser();

        List<String> ctxPath = ImmutableList.of(MotherElement.class.getPackage()
                .getName());
        configurator = parser.configureParser(ctxPath, TestXmlParser.class.getClassLoader());

        mother = new MotherElement();
        mother.setFirstname("Padme");
        mother.setLastname("Skywalker");
        mother.setAge(25);

        luke = new ChildElement();
        luke.setAge(2);
        luke.setFirstname("Luke");
        luke.setLastname("Skywalker");

        leia = new ChildElement();
        leia.setAge(2);
        leia.setFirstname("Leia");
        leia.setLastname("Organa");

        mother.getChild()
                .add(luke);
        mother.getChild()
                .add(leia);
    }

    @Test
    public void testConfigureParser() {
        assertTrue(configurator.getContextPath()
                .contains(MotherElement.class.getPackage()
                        .getName()));
        assertEquals(TestXmlParser.class.getClassLoader(), configurator.getClassLoader());
    }

    @Test
    public void testMarshal() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        parser.marshal(configurator, mother, os);
        String outputXml = os.toString();

        assertXpathEvaluatesTo("Padme", "/mother/@firstname", outputXml);
        assertXpathEvaluatesTo("25", "/mother/@age", outputXml);

        assertXpathExists("/mother/child/@firstname", outputXml);
        assertXpathExists("/mother/child[@firstname='Luke']", outputXml);
        assertXpathNotExists("/mother/child[@firstname='Anakin']", outputXml);

        configurator.setHandler(new DefaultValidationEventHandler());
        os = new ByteArrayOutputStream();
        parser.marshal(configurator, mother, os);
        outputXml = os.toString();

        assertXpathEvaluatesTo("Padme", "/mother/@firstname", outputXml);

        configurator.addProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        os = new ByteArrayOutputStream();
        parser.marshal(configurator, mother, os);
        outputXml = os.toString();

        assertXpathEvaluatesTo("Padme", "/mother/@firstname", outputXml);
    }

    @Test
    public void testUnmarshal() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        parser.marshal(configurator, mother, os);

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        MotherElement unmarshal = parser.unmarshal(configurator, MotherElement.class, is);

        assertEquals(mother.getAge(), unmarshal.getAge());
        assertEquals(mother.getFirstname(), unmarshal.getFirstname());
        assertEquals(mother.getLastname(), unmarshal.getLastname());
        assertEquals(mother.getChild()
                        .size(),
                unmarshal.getChild()
                        .size());
        assertEquals(luke.getFirstname(),
                unmarshal.getChild()
                        .get(0)
                        .getFirstname());
        assertEquals(leia.getAge(),
                unmarshal.getChild()
                        .get(1)
                        .getAge());

        configurator.setHandler(new DefaultValidationEventHandler());
        is = new ByteArrayInputStream(os.toByteArray());
        unmarshal = parser.unmarshal(configurator, MotherElement.class, is);

        assertEquals(mother.getAge(), unmarshal.getAge());

        configurator.addProperty("UnknownProperty", Boolean.TRUE);
        is = new ByteArrayInputStream(os.toByteArray());

        thrown.expect(ParserException.class);
        parser.unmarshal(configurator, MotherElement.class, is);
    }

    @Test
    public void testUnmarshalBadCast() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        parser.marshal(configurator, mother, os);

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());

        thrown.expect(ClassCastException.class);
        ChildElement unmarshal = parser.unmarshal(configurator, ChildElement.class, is);
    }

    @Test
    public void testMarshalNode() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.newDocument();

        parser.marshal(configurator, mother, doc);

        // check the root
        assertEquals(1,
                doc.getChildNodes()
                        .getLength());
        assertEquals(3,
                doc.getDocumentElement()
                        .getAttributes()
                        .getLength());
        assertEquals(mother.getAge()
                        .toString(),
                doc.getDocumentElement()
                        .getAttributes()
                        .getNamedItem("age")
                        .getNodeValue());
        assertEquals(mother.getFirstname(),
                doc.getDocumentElement()
                        .getAttributes()
                        .getNamedItem("firstname")
                        .getNodeValue());
        assertEquals(mother.getLastname(),
                doc.getDocumentElement()
                        .getAttributes()
                        .getNamedItem("lastname")
                        .getNodeValue());

        // check the child nodes
        assertEquals(mother.getChild()
                        .size(),
                doc.getFirstChild()
                        .getChildNodes()
                        .getLength());

        // first child
        assertEquals(3,
                doc.getDocumentElement()
                        .getChildNodes()
                        .item(0)
                        .getAttributes()
                        .getLength());
        assertEquals(mother.getChild()
                        .get(0)
                        .getFirstname(),
                doc.getDocumentElement()
                        .getChildNodes()
                        .item(0)
                        .getAttributes()
                        .getNamedItem("firstname")
                        .getNodeValue());
        assertEquals(mother.getChild()
                        .get(0)
                        .getLastname(),
                doc.getDocumentElement()
                        .getChildNodes()
                        .item(0)
                        .getAttributes()
                        .getNamedItem("lastname")
                        .getNodeValue());
        assertEquals(mother.getChild()
                        .get(0)
                        .getAge()
                        .toString(),
                doc.getDocumentElement()
                        .getChildNodes()
                        .item(0)
                        .getAttributes()
                        .getNamedItem("age")
                        .getNodeValue());

        // second child
        assertEquals(3,
                doc.getDocumentElement()
                        .getChildNodes()
                        .item(1)
                        .getAttributes()
                        .getLength());
        assertEquals(mother.getChild()
                        .get(1)
                        .getFirstname(),
                doc.getDocumentElement()
                        .getChildNodes()
                        .item(1)
                        .getAttributes()
                        .getNamedItem("firstname")
                        .getNodeValue());
        assertEquals(mother.getChild()
                        .get(1)
                        .getLastname(),
                doc.getDocumentElement()
                        .getChildNodes()
                        .item(1)
                        .getAttributes()
                        .getNamedItem("lastname")
                        .getNodeValue());
        assertEquals(mother.getChild()
                        .get(1)
                        .getAge()
                        .toString(),
                doc.getDocumentElement()
                        .getChildNodes()
                        .item(1)
                        .getAttributes()
                        .getNamedItem("age")
                        .getNodeValue());
    }

    @Test
    public void testMarshalNodeRunTimeException() throws Exception {
        thrown.expect(ParserException.class);
        parser.marshal(configurator, mother, (Node) null);
    }

    @Test
    public void testMarshalNodeJAXBException() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.newDocument();

        configurator.addProperty("BadKey", "BadValue");

        thrown.expect(ParserException.class);
        parser.marshal(configurator, mother, doc);
    }

    @Test
    public void testUnmarshalNode() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.newDocument();

        parser.marshal(configurator, mother, doc);

        MotherElement unmarshal = parser.unmarshal(configurator, MotherElement.class, doc);

        assertEquals(mother.getAge(), unmarshal.getAge());
        assertEquals(mother.getFirstname(), unmarshal.getFirstname());
        assertEquals(mother.getLastname(), unmarshal.getLastname());
        assertEquals(mother.getChild()
                        .size(),
                unmarshal.getChild()
                        .size());
        assertEquals(luke.getFirstname(),
                unmarshal.getChild()
                        .get(0)
                        .getFirstname());
        assertEquals(leia.getAge(),
                unmarshal.getChild()
                        .get(1)
                        .getAge());
    }

    @Test
    public void testUnmarshalNodeRunTimeException() throws Exception {
        thrown.expect(ParserException.class);
        parser.unmarshal(configurator, MotherElement.class, (Node) null);
    }

    @Test
    public void testUnmarshalNodeJAXBException() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.newDocument();

        parser.marshal(configurator, mother, doc);

        configurator.addProperty("BadKey", "BadValue");

        thrown.expect(ParserException.class);
        parser.unmarshal(configurator, MotherElement.class, doc);
    }

    @Test
    public void testUnmarshalSource() throws Exception {
        JAXBContext motherContext = JAXBContext.newInstance(MotherElement.class);
        @SuppressWarnings("unchecked")
        JAXBElement<MotherElement> motherElementJAXBElement = new JAXBElement(new QName("mother"),
                MotherElement.class,
                mother);
        JAXBSource motherSource = new JAXBSource(motherContext, motherElementJAXBElement);

        MotherElement unmarshal = parser.unmarshal(configurator, MotherElement.class, motherSource);

        assertEquals(mother.getAge(), unmarshal.getAge());
        assertEquals(mother.getFirstname(), unmarshal.getFirstname());
        assertEquals(mother.getLastname(), unmarshal.getLastname());
        assertEquals(mother.getChild()
                        .size(),
                unmarshal.getChild()
                        .size());
        assertEquals(luke.getFirstname(),
                unmarshal.getChild()
                        .get(0)
                        .getFirstname());
        assertEquals(leia.getAge(),
                unmarshal.getChild()
                        .get(1)
                        .getAge());
    }

    @Test
    public void testUnmarshalSourceRunTimeException() throws Exception {
        thrown.expect(ParserException.class);
        parser.unmarshal(configurator, MotherElement.class, (Source) null);
    }

    @Test
    public void testUnmarshalSourceJAXBException() throws Exception {
        JAXBContext motherContext = JAXBContext.newInstance(MotherElement.class);
        @SuppressWarnings("unchecked")
        JAXBElement<MotherElement> motherElementJAXBElement = new JAXBElement(new QName("mother"),
                MotherElement.class,
                mother);
        JAXBSource motherSource = new JAXBSource(motherContext, motherElementJAXBElement);

        configurator.addProperty("BadKey", "BadValue");
        thrown.expect(ParserException.class);
        parser.unmarshal(configurator, MotherElement.class, motherSource);
    }

    @Test
    public void testBadContextPath() throws Exception {
        configurator.setContextPath(ImmutableList.of(""));
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        thrown.expect(ParserException.class);
        parser.marshal(configurator, mother, os);
    }

    @Test
    public void testBadMarshal() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        thrown.expect(ParserException.class);
        parser.marshal(configurator, this, os);
    }

    @Test
    public void testBadUnmarshal() throws Exception {
        ByteArrayInputStream is = new ByteArrayInputStream(new byte[] {0, 1, 2});

        thrown.expect(ParserException.class);
        parser.unmarshal(configurator, ChildElement.class, is);
    }

    @Test
    public void testTypeAdapter() throws Exception {
        // TODO RAP 30 Jun 15: Actually need to *test* the type adapter
        //        configurator.setAdapter(new XmlAdapter() {
        //            @Override
        //            public Object unmarshal(Object v) throws Exception {
        //                return null;
        //            }
        //
        //            @Override
        //            public Object marshal(Object v) throws Exception {
        //                return null;
        //            }
        //        });
        //        parser.marshal(configurator, mother, os);
        //        String outputXml = os.toString();
        //
        //        assertXpathEvaluatesTo("Padme", "/mother/@firstname", outputXml);
        //
        //        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        //        MotherElement unmarshal = parser.unmarshal(configurator, MotherElement.class, is);
        //
        //        assertEquals(mother.getAge(), unmarshal.getAge());
    }

    @Test
    public void testCacheKey() throws Exception {
        XmlParser.CacheKey cacheKey1 = new XmlParser.CacheKey("hello:world",
                TestXmlParser.class.getClassLoader());
        XmlParser.CacheKey cacheKey2 = new XmlParser.CacheKey("here:now",
                TestXmlParser.class.getClassLoader());
        XmlParser.CacheKey cacheKey3 = new XmlParser.CacheKey("hello:world",
                TestXmlParser.class.getClassLoader());

        assertThat(cacheKey1, not(equalTo(cacheKey2)));
        assertThat(cacheKey1.hashCode(), not(equalTo(cacheKey2.hashCode())));

        assertTrue(cacheKey1.equals(cacheKey3));
        assertThat(cacheKey1.hashCode(), equalTo(cacheKey3.hashCode()));

        assertThat(cacheKey1, equalTo(cacheKey1));
        assertThat(cacheKey1, not(equalTo(null)));

        assertEquals(cacheKey1.equals(null), false);
        assertEquals(cacheKey1.equals("hello world"), false);
    }
}