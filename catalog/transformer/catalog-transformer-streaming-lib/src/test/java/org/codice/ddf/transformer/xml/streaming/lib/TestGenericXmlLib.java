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

package org.codice.ddf.transformer.xml.streaming.lib;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.codice.ddf.transformer.xml.streaming.SaxEventHandler;
import org.codice.ddf.transformer.xml.streaming.SaxEventHandlerFactory;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.transform.CatalogTransformerException;

public class TestGenericXmlLib {

    MetacardTypeRegister mockMetacardTypeRegister;

    @Before
    public void setup() throws CatalogTransformerException {
        mockMetacardTypeRegister = mock(MetacardTypeRegister.class);
        doReturn(BasicTypes.BASIC_METACARD).when(mockMetacardTypeRegister)
                .getMetacardType();
    }

    @Test
    public void testNormalTransform() throws FileNotFoundException, CatalogTransformerException {

        InputStream inputStream = new FileInputStream("src/test/resources/metacard2.xml");
        SaxEventHandlerFactory saxEventHandlerFactory = mock(SaxEventHandlerFactory.class);
        when(saxEventHandlerFactory.getId()).thenReturn("test");
        SaxEventHandler handler = getNewHandler();
        when(saxEventHandlerFactory.getNewSaxEventHandler()).thenReturn(handler);
        XmlInputTransformer xmlInputTransformer = new XmlInputTransformer();
        xmlInputTransformer.setSaxEventHandlerConfiguration(Collections.singletonList("test"));
        xmlInputTransformer.setSaxEventHandlerFactories(Collections.singletonList(
                saxEventHandlerFactory));
        xmlInputTransformer.setDynamicMetacardTypeRegister(mockMetacardTypeRegister);
        assertThat(xmlInputTransformer.getDynamicMetacardTypeRegister()
                .getMetacardType(), is(notNullValue()));
        Metacard metacard = null;
        try {
            metacard = xmlInputTransformer.transform(inputStream, "test");
        } catch (IOException e) {
            fail();
        }
        assertThat(metacard.getAttribute(Metacard.METADATA)
                .getValue(), is(notNullValue()));
        assertThat(metacard.getAttribute(Metacard.ID)
                .getValue(), is("test"));

    }

    @Test(expected = CatalogTransformerException.class)
    public void testBadInputTransform() throws FileNotFoundException, CatalogTransformerException {

        InputStream inputStream = new FileInputStream("src/test/resources/metacard3InvalidXml.xml");
        SaxEventHandlerFactory saxEventHandlerFactory = mock(SaxEventHandlerFactory.class);
        when(saxEventHandlerFactory.getId()).thenReturn("test");
        SaxEventHandler handler = getNewHandler();
        when(saxEventHandlerFactory.getNewSaxEventHandler()).thenReturn(handler);
        XmlInputTransformer xmlInputTransformer = new XmlInputTransformer();
        xmlInputTransformer.setSaxEventHandlerConfiguration(Collections.singletonList("test"));
        xmlInputTransformer.setSaxEventHandlerFactories(Collections.singletonList(
                saxEventHandlerFactory));
        xmlInputTransformer.setDynamicMetacardTypeRegister(mockMetacardTypeRegister);
        try {
            xmlInputTransformer.transform(inputStream, "test");
        } catch (IOException e) {
            fail();
        }

    }

    @Test(expected = CatalogTransformerException.class)
    public void testNullInputTransform() throws FileNotFoundException, CatalogTransformerException {
        SaxEventHandlerFactory saxEventHandlerFactory = mock(SaxEventHandlerFactory.class);
        when(saxEventHandlerFactory.getId()).thenReturn("test");
        SaxEventHandler handler = getNewHandler();
        when(saxEventHandlerFactory.getNewSaxEventHandler()).thenReturn(handler);
        XmlInputTransformer xmlInputTransformer = new XmlInputTransformer();
        xmlInputTransformer.setSaxEventHandlerConfiguration(Collections.singletonList("test"));
        xmlInputTransformer.setSaxEventHandlerFactories(Collections.singletonList(
                saxEventHandlerFactory));
        try {
            xmlInputTransformer.transform(null, "test");
        } catch (IOException e) {
            fail();
        }

    }

    @Test
    public void testNoConfigTransform() throws IOException, CatalogTransformerException {
        SaxEventHandlerFactory saxEventHandlerFactory = mock(SaxEventHandlerFactory.class);
        when(saxEventHandlerFactory.getId()).thenReturn("test");
        SaxEventHandler handler = getNewHandler();
        when(saxEventHandlerFactory.getNewSaxEventHandler()).thenReturn(handler);
        XmlInputTransformer xmlInputTransformer = new XmlInputTransformer();
        xmlInputTransformer.setSaxEventHandlerFactories(Collections.singletonList(
                saxEventHandlerFactory));
        xmlInputTransformer.setDynamicMetacardTypeRegister(mockMetacardTypeRegister);
        MetacardType metacardType = xmlInputTransformer.getDynamicMetacardTypeRegister()
                .getMetacardType();
        assertThat(metacardType.getAttributeDescriptors(),
                is(BasicTypes.BASIC_METACARD.getAttributeDescriptors()));
    }

    @Test
    public void testNoFactoriesTransform() throws CatalogTransformerException {
        XmlInputTransformer xmlInputTransformer = new XmlInputTransformer();
        xmlInputTransformer.setSaxEventHandlerConfiguration(Collections.singletonList("test"));
        xmlInputTransformer.setSaxEventHandlerFactories(null);
        xmlInputTransformer.setDynamicMetacardTypeRegister(mockMetacardTypeRegister);
        MetacardType metacardType = xmlInputTransformer.getDynamicMetacardTypeRegister()
                .getMetacardType();
        assertThat(metacardType.getAttributeDescriptors(),
                is(BasicTypes.BASIC_METACARD.getAttributeDescriptors()));
    }

    @Test
    public void testExceptionThrowInputStream()
            throws FileNotFoundException, CatalogTransformerException {
        InputStream inputStream = new FileInputStream("src/test/resources/metacard2.xml");
        SaxEventHandlerDelegate saxEventHandlerDelegate = new SaxEventHandlerDelegate() {
            @Override
            InputTransformerErrorHandler getInputTransformerErrorHandler() {
                InputTransformerErrorHandler inputTransformerErrorHandler = mock(
                        InputTransformerErrorHandler.class);
                when(inputTransformerErrorHandler.configure(any(StringBuilder.class))).thenCallRealMethod();
                when(inputTransformerErrorHandler.getParseWarningsErrors()).thenReturn(
                        "mock-errors-and-warnings");
                return inputTransformerErrorHandler;
            }
        };
        Metacard metacard = saxEventHandlerDelegate.read(inputStream);
        assertThat(metacard.getAttribute(BasicTypes.VALIDATION_ERRORS)
                .getValue(), is("mock-errors-and-warnings"));

    }

    @Test
    public void testExceptionHappyHandler()
            throws SAXException, FileNotFoundException, CatalogTransformerException {
        InputStream inputStream = new FileInputStream("src/test/resources/metacard2.xml");
        SaxEventHandlerFactory saxEventHandlerFactory = mock(SaxEventHandlerFactory.class);
        when(saxEventHandlerFactory.getId()).thenReturn("test");
        SaxEventHandler handler = getNewHandler();

        when(saxEventHandlerFactory.getNewSaxEventHandler()).thenReturn(handler);
        XmlInputTransformer xmlInputTransformer = new XmlInputTransformer();
        xmlInputTransformer.setSaxEventHandlerConfiguration(Collections.singletonList("test"));
        xmlInputTransformer.setSaxEventHandlerFactories(Collections.singletonList(
                saxEventHandlerFactory));
        xmlInputTransformer.setDynamicMetacardTypeRegister(mockMetacardTypeRegister);
        assertThat(xmlInputTransformer.getDynamicMetacardTypeRegister()
                .getMetacardType(), is(notNullValue()));
        Metacard metacard = null;
        try {
            metacard = xmlInputTransformer.transform(inputStream, "test");
        } catch (IOException e) {
            fail();
        }
        assertThat(metacard.getAttribute(Metacard.METADATA)
                .getValue(), is(notNullValue()));
        assertThat(metacard.getAttribute(Metacard.ID)
                .getValue(), is("test"));
    }

    @Test
    public void testDescribableGettersSetters() {
        XmlInputTransformer inputTransformer = new XmlInputTransformer();
        inputTransformer.setDescription("foo");
        inputTransformer.setId("foo");
        inputTransformer.setOrganization("foo");
        inputTransformer.setTitle("foo");
        inputTransformer.setVersion("foo");
        assertThat(inputTransformer.getDescription(), is("foo"));
        assertThat(inputTransformer.getId(), is("foo"));
        assertThat(inputTransformer.getOrganization(), is("foo"));
        assertThat(inputTransformer.getTitle(), is("foo"));
        assertThat(inputTransformer.getVersion(), is("foo"));
    }

    @Test
    public void testSaxEventToXmlElementConverter()
            throws UnsupportedEncodingException, XMLStreamException {
        SaxEventToXmlElementConverter saxEventToXmlElementConverter =
                new SaxEventToXmlElementConverter();
        saxEventToXmlElementConverter.addNamespace("foo", "bar");
        saxEventToXmlElementConverter.addNamespace("barfoo", "foobar");
        Attributes attrs = mock(Attributes.class);
        when(attrs.getLength()).thenReturn(2);
        when(attrs.getLocalName(0)).thenReturn("foo");
        when(attrs.getLocalName(1)).thenReturn("bar");
        when(attrs.getURI(0)).thenReturn("foobar");
        when(attrs.getURI(1)).thenReturn("");
        when(attrs.getValue(anyInt())).thenReturn("test");
        saxEventToXmlElementConverter.toElement("bar", "test", attrs);
        saxEventToXmlElementConverter.toElement("&lt;chara&gt;cters".toCharArray(), 0, 16);
        saxEventToXmlElementConverter.toElement("bar", "test");
        saxEventToXmlElementConverter.toElement("bar", "test", attrs);
        saxEventToXmlElementConverter.toElement("characters".toCharArray(), 0, 8);
        saxEventToXmlElementConverter.toElement("bar", "test");
        assertThat(saxEventToXmlElementConverter.toString(),
                is("<foo:test xmlns:foo=\"bar\" xmlns:barfoo=\"foobar\" barfoo:foo=\"test\" bar=\"test\">&amp;lt;chara&amp;gt;cte</foo:test><foo:test xmlns:foo=\"bar\" xmlns:barfoo=\"foobar\" barfoo:foo=\"test\" bar=\"test\">characte</foo:test>"));
        saxEventToXmlElementConverter.reset();
        assertThat(saxEventToXmlElementConverter.toString(), is(""));
    }

    @Test
    public void testSaxEventToXmlElementConverterEdgeCase()
            throws UnsupportedEncodingException, XMLStreamException {
        // set up mock Attribute object
        Attributes attrs = mock(Attributes.class);
        when(attrs.getLength()).thenReturn(2);
        when(attrs.getLocalName(0)).thenReturn("foo");
        when(attrs.getLocalName(1)).thenReturn("bar");
        when(attrs.getURI(0)).thenReturn("ns1uri");
        when(attrs.getURI(1)).thenReturn("");
        when(attrs.getValue(anyInt())).thenReturn("test");
        // inner attrs
        Attributes attrs2 = mock(Attributes.class);
        when(attrs2.getLength()).thenReturn(2);
        when(attrs2.getLocalName(0)).thenReturn("foo");
        when(attrs2.getLocalName(1)).thenReturn("bar");
        when(attrs2.getURI(0)).thenReturn("ns1uri.v2");
        when(attrs2.getURI(1)).thenReturn("");
        when(attrs2.getValue(anyInt())).thenReturn("test");

        SaxEventToXmlElementConverter converter = new SaxEventToXmlElementConverter();

        // Simulate begin reading
        converter.addNamespace("ns1", "ns1uri");
        converter.addNamespace("ns2", "ns2uri");

        // Write parent element and characters
        converter.toElement("ns1uri", "element1", attrs);
        converter.toElement("0123456789ABCDEF".toCharArray(), 0, 16);

        // Declare new namespace, using same prefix as previous
        converter.addNamespace("ns1", "ns1uri.v2");
        converter.toElement("ns1uri.v2", "nestedElement", attrs2);
        converter.toElement("ns1uri.v2", "nestedElement");

        // Pop prefix
        converter.removeNamespace("ns1");

        converter.toElement("ns1uri", "element1", attrs);
        converter.toElement("0123456789ABCDEF".toCharArray(), 0, 16);
        converter.toElement("ns1uri", "element1");

        // Finish writing parent
        converter.toElement("ns1uri", "element1");

        converter.toElement("ns1uri", "element1", attrs);
        converter.toElement("0123456789ABCDEF".toCharArray(), 0, 16);
        converter.toElement("ns1uri", "element1");
        assertThat(converter.toString(),
                is("<ns1:element1 xmlns:ns1=\"ns1uri\" ns1:foo=\"test\" bar=\"test\">0123456789ABCDEF<ns1:nestedElement xmlns:ns1=\"ns1uri.v2\" ns1:foo=\"test\" bar=\"test\"></ns1:nestedElement><ns1:element1 ns1:foo=\"test\" bar=\"test\">0123456789ABCDEF</ns1:element1></ns1:element1><ns1:element1 xmlns:ns1=\"ns1uri\" ns1:foo=\"test\" bar=\"test\">0123456789ABCDEF</ns1:element1>"));
    }

    @Test
    public void testDynamicMetacardType() {
        Set<AttributeDescriptor> attributeDescriptors = new HashSet<>();
        attributeDescriptors.add(new AttributeDescriptorImpl(Metacard.TITLE,
                false,
                false,
                false,
                false,
                BasicTypes.STRING_TYPE));
        MetacardTypeImpl dynamicMetacardType = new MetacardTypeImpl("Foo.metacard",
                attributeDescriptors);
        assertThat(dynamicMetacardType.getName(), is("Foo.metacard"));
        assertThat(dynamicMetacardType.getAttributeDescriptor(Metacard.TITLE), is(notNullValue()));
        assertThat(dynamicMetacardType.getAttributeDescriptors()
                .equals(attributeDescriptors), is(true));
    }

    private SaxEventHandler getNewHandler() {
        Attribute attribute = new AttributeImpl(Metacard.TITLE, "foo");
        Attribute attribute2 = new AttributeImpl(Metacard.TITLE, "bar");
        SaxEventHandler handler = mock(SaxEventHandler.class);
        when(handler.getAttributes()).thenReturn(Arrays.asList(attribute, attribute2));
        return handler;
    }

}
