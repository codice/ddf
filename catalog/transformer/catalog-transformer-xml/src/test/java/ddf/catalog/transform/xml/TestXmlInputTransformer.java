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
package ddf.catalog.transform.xml;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.geotools.data.Base64;
import org.junit.Test;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.xml.XmlInputTransformer;

public class TestXmlInputTransformer {

    private static final Logger LOGGER = Logger.getLogger(TestXmlInputTransformer.class);

    static {
        BasicConfigurator.configure();
    }

    @Test
    public void testTransformWithInvalidMetacardType() throws IOException,
        CatalogTransformerException {
        XmlInputTransformer xit = new XmlInputTransformer();
        Metacard metacard = xit.transform(new FileInputStream(
                "src/test/resources/invalidExtensibleMetacard.xml"));

        LOGGER.info("ID: " + metacard.getId());
        LOGGER.info("Type: " + metacard.getMetacardType().getName());
        LOGGER.info("Source: " + metacard.getSourceId());
        LOGGER.info("Attributes: ");
        for (AttributeDescriptor descriptor : metacard.getMetacardType().getAttributeDescriptors()) {
            Attribute attribute = metacard.getAttribute(descriptor.getName());
            LOGGER.info("\t" + descriptor.getName() + ": "
                    + ((attribute == null) ? attribute : attribute.getValue()));
        }

        assertThat(metacard.getMetacardType().getName(), is(BasicTypes.BASIC_METACARD.getName()));
    }

    @Test
    public void testTransformWithExtensibleMetacardType() throws IOException,
        CatalogTransformerException {
        XmlInputTransformer xit = new XmlInputTransformer();
        List<MetacardType> metacardTypes = new ArrayList<MetacardType>(1);
        MetacardType extensibleType = new MetacardTypeImpl("extensible.metacard",
                BasicTypes.BASIC_METACARD.getAttributeDescriptors());
        metacardTypes.add(extensibleType);
        xit.setMetacardTypes(metacardTypes);
        Metacard metacard = xit.transform(new FileInputStream(
                "src/test/resources/extensibleMetacard.xml"));

        LOGGER.info("ID: " + metacard.getId());
        LOGGER.info("Type: " + metacard.getMetacardType().getName());
        LOGGER.info("Source: " + metacard.getSourceId());
        LOGGER.info("Attributes: ");
        for (AttributeDescriptor descriptor : metacard.getMetacardType().getAttributeDescriptors()) {
            Attribute attribute = metacard.getAttribute(descriptor.getName());
            LOGGER.info("\t" + descriptor.getName() + ": "
                    + ((attribute == null) ? attribute : attribute.getValue()));
        }

    }

    @Test
    public void testSimpleMetadata() throws IOException, CatalogTransformerException,
        ParseException {

        XmlInputTransformer xit = new XmlInputTransformer();
        Metacard metacard = xit.transform(new FileInputStream("src/test/resources/metacard1.xml"));

        LOGGER.info("Attributes: ");
        for (AttributeDescriptor descriptor : metacard.getMetacardType().getAttributeDescriptors()) {
            Attribute attribute = metacard.getAttribute(descriptor.getName());
            LOGGER.info("\t" + descriptor.getName() + ": "
                    + ((attribute == null) ? attribute : attribute.getValue()));
        }

        LOGGER.info("ID: " + metacard.getId());
        LOGGER.info("Type: " + metacard.getMetacardType().getName());
        LOGGER.info("Source: " + metacard.getSourceId());

        assertEquals("1234567890987654321", metacard.getId());
        assertEquals("ddf.metacard", metacard.getMetacardType().getName());
        assertEquals("foobar", metacard.getSourceId());

        // TODO use JTS to check for equality, not string comparison.
        assertEquals("POLYGON ((35 10, 10 20, 15 40, 45 45, 35 10), (20 30, 35 35, 30 20, 20 30))",
                metacard.getAttribute(Metacard.GEOGRAPHY).getValue());

        assertEquals("Title!", metacard.getAttribute(Metacard.TITLE).getValue());

        assertArrayEquals(
                Base64.decode("AAABAAABAQEAAQAAAQEBAAEAAAEBAQABAAABAQEAAQAAAQEBAAEAAAEBAQABAAABAQE="),
                (byte[]) metacard.getAttribute(Metacard.THUMBNAIL).getValue());

        // TODO use XMLUnit to test equivalence
        assertThat(metacard.getAttribute(Metacard.METADATA).getValue().toString(),
                startsWith("<foo xmlns=\"http://foo.com\">"));

        assertEquals(
                (new SimpleDateFormat("MMM d, yyyy HH:mm:ss.SSS z"))
                        .parse("Dec 27, 2012 16:31:01.641 MST"),
                metacard.getAttribute(Metacard.EXPIRATION).getValue());
    }

    @Test
    public void testFallbackToBasicMetacardForUnknowMetacardType() throws FileNotFoundException,
        IOException, CatalogTransformerException, ParseException {
        XmlInputTransformer xit = new XmlInputTransformer();
        List<MetacardType> metacardTypes = new ArrayList<MetacardType>(1);
        metacardTypes.add(BasicTypes.BASIC_METACARD);
        xit.setMetacardTypes(metacardTypes);

        Metacard metacard = xit.transform(new FileInputStream(
                "src/test/resources/unknownMetacard1.xml"));

        LOGGER.info("ID: " + metacard.getId());
        LOGGER.info("Type: " + metacard.getMetacardType().getName());
        LOGGER.info("Source: " + metacard.getSourceId());
        LOGGER.info("Attributes: ");
        for (AttributeDescriptor descriptor : metacard.getMetacardType().getAttributeDescriptors()) {
            Attribute attribute = metacard.getAttribute(descriptor.getName());
            LOGGER.info("\t" + descriptor.getName() + ": "
                    + ((attribute == null) ? attribute : attribute.getValue()));
        }

        assertThat(metacard.getMetacardType().getName(), is(BasicTypes.BASIC_METACARD.getName()));

        assertThat("1234567890987654321", is(metacard.getId()));
        assertThat("foobar", is(metacard.getSourceId()));

        assertThat("POLYGON ((35 10, 10 20, 15 40, 45 45, 35 10), (20 30, 35 35, 30 20, 20 30))",
                is(metacard.getAttribute(Metacard.GEOGRAPHY).getValue()));

        assertThat("Title!", is(metacard.getAttribute(Metacard.TITLE).getValue()));

        assertArrayEquals(
                Base64.decode("AAABAAABAQEAAQAAAQEBAAEAAAEBAQABAAABAQEAAQAAAQEBAAEAAAEBAQABAAABAQE="),
                (byte[]) metacard.getAttribute(Metacard.THUMBNAIL).getValue());

        assertThat(metacard.getAttribute(Metacard.METADATA).getValue().toString(),
                startsWith("<foo xmlns=\"http://foo.com\">"));

        assertThat(
                (new SimpleDateFormat("MMM d, yyyy HH:mm:ss.SSS z"))
                        .parse("Dec 27, 2012 16:31:01.641 MST"),
                is(metacard.getAttribute(Metacard.EXPIRATION).getValue()));
    }

}
