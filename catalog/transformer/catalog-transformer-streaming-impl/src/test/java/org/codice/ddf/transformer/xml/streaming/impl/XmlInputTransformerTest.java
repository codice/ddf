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
package org.codice.ddf.transformer.xml.streaming.impl;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Validation;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.validation.ValidationException;
import ddf.catalog.validation.impl.ValidationExceptionImpl;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.codice.ddf.transformer.xml.streaming.Gml3ToWkt;
import org.codice.ddf.transformer.xml.streaming.SaxEventHandler;
import org.codice.ddf.transformer.xml.streaming.SaxEventHandlerFactory;
import org.codice.ddf.transformer.xml.streaming.lib.SaxEventHandlerDelegate;
import org.codice.ddf.transformer.xml.streaming.lib.XmlInputTransformer;
import org.geotools.gml3.GMLConfiguration;
import org.junit.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.gml2.GMLHandler;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

public class XmlInputTransformerTest {
  static SaxEventHandler saxEventHandler;

  static SaxEventHandlerFactory saxEventHandlerFactory;

  static GmlHandler gmlHandler;

  static Gml3ToWkt gml3ToWkt = new Gml3ToWktImpl(new GMLConfiguration());

  static SaxEventHandlerDelegate saxEventHandlerDelegate;

  static InputStream inputStream;

  XmlInputTransformer xmlInputTransformer;

  /*
     Tests a base XmlInputTransformer, CONTENT_TYPE is null because it is not in the base xmlToMetacard mapping
  */
  @Test
  public void testNormalTransform() throws FileNotFoundException, CatalogTransformerException {

    inputStream = new FileInputStream("src/test/resources/metacard2.xml");
    saxEventHandlerFactory = new XmlSaxEventHandlerFactoryImpl();
    saxEventHandler = saxEventHandlerFactory.getNewSaxEventHandler();
    assertThat(saxEventHandler.getSupportedAttributeDescriptors().size(), is(greaterThan(0)));

    GMLHandler gh = new GMLHandler(new GeometryFactory(), (ErrorHandler) null);
    gmlHandler = new GmlHandler(gh, gml3ToWkt);
    assertThat(gmlHandler.getSupportedAttributeDescriptors().size(), is(greaterThan(0)));
    saxEventHandlerDelegate =
        new SaxEventHandlerDelegate(Arrays.asList(saxEventHandler, gmlHandler));

    Metacard metacard = saxEventHandlerDelegate.read(inputStream).getMetacard(null);
    assertThat(metacard.getAttribute(Metacard.TITLE).getValues().size(), is(1));
    assertThat(metacard.getAttribute(Metacard.TITLE).getValues().get(0), is("Title!"));
    assertThat(metacard.getAttribute(Metacard.DESCRIPTION).getValues().size(), is(1));
    assertThat(metacard.getAttribute(Metacard.DESCRIPTION).getValues().get(0), is("Description!"));
    assertThat(metacard.getAttribute(Metacard.POINT_OF_CONTACT).getValues().size(), is(1));
    assertThat(metacard.getAttribute(Metacard.POINT_OF_CONTACT).getValues().get(0), is("POC!"));
    assertThat(metacard.getAttribute(Metacard.RESOURCE_URI).getValues().size(), is(1));
    assertThat(metacard.getAttribute(Metacard.RESOURCE_URI).getValues().get(0), is("foobar"));
    assertThat(metacard.getAttribute(Metacard.CONTENT_TYPE), is(nullValue()));
    assertThat(metacard.getAttribute(Metacard.GEOGRAPHY).getValues().size(), is(1));
    assertThat(metacard.getAttribute(Metacard.GEOGRAPHY).getValues().get(0), is("POINT (100 200)"));
  }

  /*
     Configures a custom xmlToMetacard mapping, CONTENT_TYPE is not null because it is in the custom xmlToMetacard mapping
  */
  @Test
  public void testConfiguredTransform() throws FileNotFoundException, CatalogTransformerException {
    Map xmlToMetacard = new HashMap<>();
    xmlToMetacard.put("title", Metacard.TITLE);
    xmlToMetacard.put("point-of-contact", Metacard.POINT_OF_CONTACT);
    xmlToMetacard.put("description", Metacard.DESCRIPTION);
    xmlToMetacard.put("source", Metacard.RESOURCE_URI);
    xmlToMetacard.put("type", Metacard.CONTENT_TYPE);

    XmlSaxEventHandlerFactoryImpl factory = new XmlSaxEventHandlerFactoryImpl();
    factory.setXmlToMetacardMapping(xmlToMetacard);
    saxEventHandler = factory.getNewSaxEventHandler();
    assertThat(factory.getXmlToMetacardMapping().equals(xmlToMetacard), is(true));
    saxEventHandlerDelegate =
        new SaxEventHandlerDelegate(Collections.singletonList(saxEventHandler));
    inputStream = new FileInputStream("src/test/resources/metacard2.xml");

    saxEventHandlerDelegate.read(inputStream);
    Metacard metacard = saxEventHandlerDelegate.getMetacard(null);
    assertThat(metacard.getAttribute(Metacard.TITLE).getValues().size(), is(1));
    assertThat(metacard.getAttribute(Metacard.TITLE).getValues().get(0), is("Title!"));
    assertThat(metacard.getAttribute(Metacard.DESCRIPTION).getValues().size(), is(1));
    assertThat(metacard.getAttribute(Metacard.DESCRIPTION).getValues().get(0), is("Description!"));
    assertThat(metacard.getAttribute(Metacard.POINT_OF_CONTACT).getValues().size(), is(1));
    assertThat(metacard.getAttribute(Metacard.POINT_OF_CONTACT).getValues().get(0), is("POC!"));
    assertThat(metacard.getAttribute(Metacard.RESOURCE_URI).getValues().size(), is(1));
    assertThat(metacard.getAttribute(Metacard.RESOURCE_URI).getValues().get(0), is("foobar"));
    assertThat(metacard.getAttribute(Metacard.CONTENT_TYPE).getValues().size(), is(1));
    assertThat(metacard.getAttribute(Metacard.CONTENT_TYPE).getValues().get(0), is("ddf.metacard"));
  }

  @Test(expected = NullPointerException.class)
  public void testBadGHTransform()
      throws FileNotFoundException, CatalogTransformerException, SAXException {

    inputStream = new FileInputStream("src/test/resources/metacard2.xml");

    saxEventHandler = new XmlSaxEventHandlerFactoryImpl().getNewSaxEventHandler();
    GMLHandler gh = mock(GMLHandler.class);
    doThrow(new SAXException()).when(gh).characters(any(char[].class), anyInt(), anyInt());
    doThrow(new SAXException())
        .when(gh)
        .startElement(anyString(), anyString(), anyString(), any(Attributes.class));
    doThrow(new SAXException()).when(gh).endElement(anyString(), anyString(), anyString());
    gmlHandler = new GmlHandler(gh, gml3ToWkt);
    saxEventHandlerDelegate =
        new SaxEventHandlerDelegate(Arrays.asList(saxEventHandler, gmlHandler));

    saxEventHandlerDelegate.read(inputStream);
  }

  @Test
  public void testGml3Conversion() throws FileNotFoundException, CatalogTransformerException {
    inputStream = new FileInputStream("src/test/resources/metacard1.xml");
    xmlInputTransformer = new XmlInputTransformer();
    xmlInputTransformer.setSaxEventHandlerConfiguration(Collections.singletonList("gml-handler"));
    GmlHandlerFactory factory = new GmlHandlerFactory();
    factory.setGml3ToWkt(gml3ToWkt);
    xmlInputTransformer.setSaxEventHandlerFactories(Collections.singletonList(factory));
    Metacard metacard = xmlInputTransformer.transform(inputStream);
    assertThat(
        metacard.getAttribute(Metacard.GEOGRAPHY).getValue(),
        is("POLYGON ((10 35, 20 10, 40 15, 45 45, 10 35), (30 20, 35 35, 20 30, 30 20))"));
  }

  @Test
  public void testBadGml3Converter()
      throws FileNotFoundException, CatalogTransformerException, ValidationException {
    inputStream = new FileInputStream("src/test/resources/metacard1.xml");
    xmlInputTransformer = new XmlInputTransformer();
    xmlInputTransformer.setSaxEventHandlerConfiguration(Collections.singletonList("gml-handler"));
    GmlHandlerFactory factory = new GmlHandlerFactory();
    Gml3ToWkt badGml3toWkt = mock(Gml3ToWkt.class);
    when(badGml3toWkt.convert(anyString())).thenThrow(new ValidationExceptionImpl());
    factory.setGml3ToWkt(badGml3toWkt);
    xmlInputTransformer.setSaxEventHandlerFactories(
        Collections.singletonList((SaxEventHandlerFactory) factory));
    Metacard metacard = xmlInputTransformer.transform(inputStream);
    assertThat(
        metacard.getAttribute(Validation.VALIDATION_ERRORS).getValue(), is("geospatial-handler"));
  }

  /*
   * The methods in tested in this Test are either simple getters/setters or NOOPS
   * If any of them ever get implemented, they need to have better, more descriptive tests written.
   */
  @Test
  public void testDescribableGettersSetters() throws SAXException {
    SaxEventHandlerFactory factory = new XmlSaxEventHandlerFactoryImpl();
    assertThat(factory.getDescription(), is(notNullValue()));
    assertThat(factory.getId(), is(notNullValue()));
    assertThat(factory.getOrganization(), is(notNullValue()));
    assertThat(factory.getTitle(), is(notNullValue()));
    assertThat(factory.getVersion(), is(notNullValue()));
    SaxEventHandler handler = factory.getNewSaxEventHandler();
    handler.setDocumentLocator(null);
    handler.endDocument();
    handler.startPrefixMapping(null, null);
    handler.endPrefixMapping(null);
    handler.ignorableWhitespace(null, 0, 0);
    handler.processingInstruction(null, null);
    handler.skippedEntity(null);
    factory = new GmlHandlerFactory();
    assertThat(factory.getDescription(), is(notNullValue()));
    assertThat(factory.getId(), is(notNullValue()));
    assertThat(factory.getOrganization(), is(notNullValue()));
    assertThat(factory.getTitle(), is(notNullValue()));
    assertThat(factory.getVersion(), is(notNullValue()));
    handler = factory.getNewSaxEventHandler();
    handler.setDocumentLocator(null);
    handler.endDocument();
    handler.endPrefixMapping(null);
    handler.ignorableWhitespace(null, 0, 0);
    handler.processingInstruction(null, null);
    handler.skippedEntity(null);
  }
}
