/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
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

import javax.xml.bind.Marshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;

import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.parser.xml.domain.ChildElement;
import org.codice.ddf.parser.xml.domain.MotherElement;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableList;

public class TestXmlParser {
    private Parser parser;

    private ParserConfigurator configurator;

    private MotherElement mother;

    private ChildElement luke;

    private ChildElement leia;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        parser = new XmlParser();

        List<String> ctxPath = ImmutableList.of(MotherElement.class.getPackage().getName());
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

        mother.getChild().add(luke);
        mother.getChild().add(leia);
    }

    @Test
    public void testConfigureParser() {
        assertTrue(
                configurator.getContextPath().contains(MotherElement.class.getPackage().getName()));
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
        assertEquals(mother.getChild().size(), unmarshal.getChild().size());
        assertEquals(luke.getFirstname(), unmarshal.getChild().get(0).getFirstname());
        assertEquals(leia.getAge(), unmarshal.getChild().get(1).getAge());

        configurator.setHandler(new DefaultValidationEventHandler());
        is = new ByteArrayInputStream(os.toByteArray());
        unmarshal = parser.unmarshal(configurator, MotherElement.class, is);

        assertEquals(mother.getAge(), unmarshal.getAge());

        configurator.addProperty("UnknownProperty", Boolean.TRUE);
        is = new ByteArrayInputStream(os.toByteArray());
        thrown.expect(ParserException.class);
        unmarshal = parser.unmarshal(configurator, MotherElement.class, is);
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
        ChildElement unmarshal = parser.unmarshal(configurator, ChildElement.class, is);
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