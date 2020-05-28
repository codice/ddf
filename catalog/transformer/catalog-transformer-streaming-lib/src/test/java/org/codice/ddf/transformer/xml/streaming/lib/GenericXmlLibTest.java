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
package org.codice.ddf.transformer.xml.streaming.lib;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Validation;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.codice.ddf.transformer.xml.streaming.SaxEventHandler;
import org.codice.ddf.transformer.xml.streaming.SaxEventHandlerFactory;
import org.junit.Test;
import org.xml.sax.SAXException;

public class GenericXmlLibTest {

  @Test
  public void testNormalTransform() throws FileNotFoundException, CatalogTransformerException {

    InputStream inputStream = new FileInputStream("src/test/resources/metacard2.xml");
    SaxEventHandlerFactory saxEventHandlerFactory = mock(SaxEventHandlerFactory.class);
    when(saxEventHandlerFactory.getId()).thenReturn("test");
    SaxEventHandler handler = getNewHandler();
    when(saxEventHandlerFactory.getNewSaxEventHandler()).thenReturn(handler);
    XmlInputTransformer xmlInputTransformer = new XmlInputTransformer();
    xmlInputTransformer.setSaxEventHandlerConfiguration(Collections.singletonList("test"));
    xmlInputTransformer.setSaxEventHandlerFactories(
        Collections.singletonList(saxEventHandlerFactory));
    Metacard metacard = null;
    try {
      metacard = xmlInputTransformer.transform(inputStream, "test");
    } catch (IOException e) {
      fail();
    }
    assertThat(metacard.getAttribute(Metacard.METADATA).getValue(), notNullValue());
    assertThat(metacard.getAttribute(Metacard.ID).getValue(), is("test"));
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
    xmlInputTransformer.setSaxEventHandlerFactories(
        Collections.singletonList(saxEventHandlerFactory));
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
    xmlInputTransformer.setSaxEventHandlerFactories(
        Collections.singletonList(saxEventHandlerFactory));
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
    xmlInputTransformer.setSaxEventHandlerFactories(
        Collections.singletonList(saxEventHandlerFactory));

    xmlInputTransformer.setSaxEventHandlerConfiguration(Collections.singletonList("test"));

    SaxEventHandlerDelegate delegate = xmlInputTransformer.create();
    MetacardType metacardType = delegate.getMetacardType(xmlInputTransformer.getId());
    assertThat(
        metacardType.getAttributeDescriptors(),
        is(MetacardImpl.BASIC_METACARD.getAttributeDescriptors()));
  }

  @Test
  public void testNoFactoriesTransform() {
    XmlInputTransformer xmlInputTransformer = new XmlInputTransformer();
    xmlInputTransformer.setSaxEventHandlerConfiguration(Collections.singletonList("test"));
    xmlInputTransformer.setSaxEventHandlerFactories(Collections.emptyList());

    SaxEventHandlerDelegate delegate = xmlInputTransformer.create();
    MetacardType metacardType = delegate.getMetacardType(xmlInputTransformer.getId());
    assertThat(
        metacardType.getAttributeDescriptors(),
        is(MetacardImpl.BASIC_METACARD.getAttributeDescriptors()));
  }

  @Test
  public void testExceptionThrowInputStream()
      throws FileNotFoundException, CatalogTransformerException {
    InputStream inputStream = new FileInputStream("src/test/resources/metacard2.xml");
    SaxEventHandlerDelegate saxEventHandlerDelegate =
        new SaxEventHandlerDelegate() {
          @Override
          InputTransformerErrorHandler getInputTransformerErrorHandler() {
            InputTransformerErrorHandler inputTransformerErrorHandler =
                mock(InputTransformerErrorHandler.class);
            when(inputTransformerErrorHandler.configure(any(StringBuilder.class)))
                .thenCallRealMethod();
            when(inputTransformerErrorHandler.getParseWarningsErrors())
                .thenReturn("mock-errors-and-warnings");
            return inputTransformerErrorHandler;
          }
        };
    Metacard metacard = saxEventHandlerDelegate.read(inputStream).getMetacard(null);
    assertThat(
        metacard.getAttribute(Validation.VALIDATION_ERRORS).getValue(),
        is("mock-errors-and-warnings"));
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
    xmlInputTransformer.setSaxEventHandlerFactories(
        Collections.singletonList(saxEventHandlerFactory));
    Metacard metacard = null;
    try {
      metacard = xmlInputTransformer.transform(inputStream, "test");
    } catch (IOException e) {
      fail();
    }
    assertThat(metacard.getAttribute(Metacard.METADATA).getValue(), notNullValue());
    assertThat(metacard.getAttribute(Metacard.ID).getValue(), is("test"));
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
  public void testDynamicMetacardType() {
    Set<AttributeDescriptor> attributeDescriptors = new HashSet<>();
    attributeDescriptors.add(
        new AttributeDescriptorImpl(
            Metacard.TITLE, false, false, false, false, BasicTypes.STRING_TYPE));
    DynamicMetacardType dynamicMetacardType = new DynamicMetacardType(attributeDescriptors, "Foo");
    assertThat(dynamicMetacardType.getName(), is("Foo.metacard"));
    assertThat(dynamicMetacardType.getAttributeDescriptor(Metacard.TITLE), notNullValue());
    assertThat(
        dynamicMetacardType.getAttributeDescriptors().equals(attributeDescriptors), is(true));
  }

  private SaxEventHandler getNewHandler() {
    Attribute attribute = new AttributeImpl(Metacard.TITLE, "foo");
    Attribute attribute2 = new AttributeImpl(Metacard.TITLE, "bar");
    SaxEventHandler handler = mock(SaxEventHandler.class);
    when(handler.getAttributes()).thenReturn(Arrays.asList(attribute, attribute2));
    return handler;
  }
}
