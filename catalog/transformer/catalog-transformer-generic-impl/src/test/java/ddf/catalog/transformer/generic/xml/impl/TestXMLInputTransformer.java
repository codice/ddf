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
package ddf.catalog.transformer.generic.xml.impl;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import ddf.catalog.data.Metacard;

public class TestXMLInputTransformer {
    static XMLSaxEventHandlerImpl xmlSaxEventHandlerImpl;

    //    XMLInputTransformer xmlInputTransformer;
    static SaxEventHandlerDelegate saxEventHandlerDelegate;

    static InputStream inputStream;

    @BeforeClass
    public static void setUp() throws FileNotFoundException {
    }

    /*
        Tests a base XMLInputTransformer, CONTENT_TYPE is null because it is not in the base xmlToMetacard mapping
     */
    @Test
    public void testNormalTransform() throws FileNotFoundException {

        xmlSaxEventHandlerImpl = new XMLSaxEventHandlerImpl();
        //        xmlInputTransformer = new XMLInputTransformer();
        saxEventHandlerDelegate = new SaxEventHandlerDelegate(xmlSaxEventHandlerImpl);
        inputStream = new FileInputStream(
                "../catalog-transformer-xml/src/test/resources/metacard1.xml");
        Metacard metacard = saxEventHandlerDelegate.read(inputStream);
        assertThat(metacard.getAttribute(Metacard.TITLE).getValues().size(), is(1));
        assertThat(metacard.getAttribute(Metacard.TITLE).getValues().get(0), is("Title!"));
        assertThat(metacard.getAttribute(Metacard.DESCRIPTION).getValues().size(), is(1));
        assertThat(metacard.getAttribute(Metacard.DESCRIPTION).getValues().get(0), is("Description!"));
        assertThat(metacard.getAttribute(Metacard.POINT_OF_CONTACT).getValues().size(), is(1));
        assertThat(metacard.getAttribute(Metacard.POINT_OF_CONTACT).getValues().get(0), is("POC!"));
        assertThat(metacard.getAttribute(Metacard.RESOURCE_URI).getValues().size(), is(1));
        assertThat(metacard.getAttribute(Metacard.RESOURCE_URI).getValues().get(0), is("foobar"));
        assertThat(metacard.getAttribute(Metacard.CONTENT_TYPE), is(nullValue()));

    }

    /*
        Configures a custom xmlToMetacard mapping, CONTENT_TYPE is not null because it is custom xmlToMetacard mapping
     */
    @Test
    public void testConfiguredTransform() throws FileNotFoundException {
        Map xmlToMetacard = new HashMap<>();
        xmlToMetacard.put("title", Metacard.TITLE);
        xmlToMetacard.put("point-of-contact", Metacard.POINT_OF_CONTACT);
        xmlToMetacard.put("description", Metacard.DESCRIPTION);
        xmlToMetacard.put("source", Metacard.RESOURCE_URI);
        xmlToMetacard.put("type", Metacard.CONTENT_TYPE);

        xmlSaxEventHandlerImpl = new XMLSaxEventHandlerImpl(xmlToMetacard);
        //        xmlInputTransformer = new XMLInputTransformer();
        saxEventHandlerDelegate = new SaxEventHandlerDelegate(xmlSaxEventHandlerImpl);
        inputStream = new FileInputStream(
                "../catalog-transformer-xml/src/test/resources/metacard1.xml");
        Metacard metacard = saxEventHandlerDelegate.read(inputStream);
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
}
