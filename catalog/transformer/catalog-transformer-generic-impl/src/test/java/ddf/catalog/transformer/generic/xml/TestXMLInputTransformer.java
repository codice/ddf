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
package ddf.catalog.transformer.generic.xml;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.junit.BeforeClass;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.transformer.generic.xml.impl.SaxEventHandlerDelegate;
import ddf.catalog.transformer.generic.xml.impl.XMLSaxEventHandlerImpl;

public class TestXMLInputTransformer {
    static XMLSaxEventHandlerImpl xmlSaxEventHandlerImpl;

    //    XMLInputTransformer xmlInputTransformer;
    static SaxEventHandlerDelegate saxEventHandlerDelegate;

    static InputStream inputStream;

    @BeforeClass
    public static void setUp() throws FileNotFoundException {
        xmlSaxEventHandlerImpl = new XMLSaxEventHandlerImpl();
        //        xmlInputTransformer = new XMLInputTransformer();
        saxEventHandlerDelegate = new SaxEventHandlerDelegate(xmlSaxEventHandlerImpl);
        inputStream = new FileInputStream(
                "../catalog-transformer-xml/src/test/resources/unknownMetacard1.xml");
    }

    @Test
    public void testNormalTransform() {
        Metacard metacard = saxEventHandlerDelegate.read(inputStream);
        assertThat(metacard.getAttribute(Metacard.TITLE).getValues().size(), is(1));
        assertThat(metacard.getAttribute(Metacard.TITLE).getValues().get(0), is("Title!"));
        assertThat(metacard.getAttribute(Metacard.DESCRIPTION).getValues().size(), is(1));
        assertThat(metacard.getAttribute(Metacard.DESCRIPTION).getValues().get(0), is("Description!"));
        assertThat(metacard.getAttribute(Metacard.POINT_OF_CONTACT).getValues().size(), is(1));
        assertThat(metacard.getAttribute(Metacard.POINT_OF_CONTACT).getValues().get(0), is("POC!"));
        assertThat(metacard.getAttribute(Metacard.RESOURCE_URI).getValues().size(), is(1));
        assertThat(metacard.getAttribute(Metacard.RESOURCE_URI).getValues().get(0), is("foobar"));

    }
}
