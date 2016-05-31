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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ddf.catalog.data.impl.BasicTypes.BASIC_METACARD;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.codice.ddf.parser.xml.XmlParser;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.MetacardTypeRegistry;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.xml.XmlInputTransformer;

public class TestXmlInputTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestXmlInputTransformer.class);

    private static final String EXTENSIBLE_METACARD_TYPE = "extensible.metacard";

    private static final String TEMPERATURE_KEY = "temperature";

    private static final String ID = "1234567890987654321";

    private static final String TITLE = "Title!";

    private static final String DESCRIPTION = "Description!";

    private static final String POINT_OF_CONTACT = "POC!";

    private static final double TEMPERATURE = 101.5;

    private XmlInputTransformer xit;

    @Before
    public void setup() {
        xit = new XmlInputTransformer(new XmlParser());

        MetacardTypeRegistry registry = mock(MetacardTypeRegistry.class);
        when(registry.lookup(anyString())).thenAnswer(invocationOnMock -> {
            String metacardTypeName = (String) invocationOnMock.getArguments()[0];
            MetacardType metacardType = metacardTypeMap().get(metacardTypeName);
            if (metacardType != null) {
                return Optional.of(metacardType);
            }
            throw new CatalogTransformerException();
        });
        xit.setMetacardTypeRegistry(registry);
    }

    private Map<String, MetacardType> metacardTypeMap() {
        Map<String, MetacardType> map = new HashMap<>();

        map.put(BASIC_METACARD.getName(), BASIC_METACARD);
        map.put(EXTENSIBLE_METACARD_TYPE, metacardTypeWithMixins(EXTENSIBLE_METACARD_TYPE));

        return map;
    }

    private MetacardType metacardTypeWithMixins(String metacardTypeName) {
        Set<AttributeDescriptor> attributeDescriptors =
                new HashSet<>(BASIC_METACARD.getAttributeDescriptors());

        attributeDescriptors.add(new AttributeDescriptorImpl(TEMPERATURE_KEY,
                true,
                true,
                false,
                false,
                BasicTypes.DOUBLE_TYPE));

        return new MetacardTypeImpl(metacardTypeName, attributeDescriptors);
    }

    @Test
    public void testTransformWithExtensibleMetacardType()
            throws IOException, CatalogTransformerException {
        Metacard metacard = xit.transform(new FileInputStream(
                "src/test/resources/extensibleMetacard.xml"));

        LOGGER.info("Type: {}",
                metacard.getMetacardType()
                        .getName());
        LOGGER.info("Source: {}", metacard.getSourceId());
        LOGGER.info("Attributes: ");
        for (AttributeDescriptor descriptor : metacard.getMetacardType()
                .getAttributeDescriptors()) {
            Attribute attribute = metacard.getAttribute(descriptor.getName());
            LOGGER.info("\t" + descriptor.getName() + ": " + ((attribute == null) ?
                    attribute :
                    attribute.getValue()));
        }

        assertThat(metacard.getMetacardType()
                .getName(), is(EXTENSIBLE_METACARD_TYPE));
        assertThat(metacard.getId(), is(ID));
        assertThat(metacard.getTitle(), is(TITLE));
        assertThat(metacard.getAttribute(TEMPERATURE_KEY)
                .getValue(), is(TEMPERATURE));
    }

    @Test
    public void testSimpleMetadata()
            throws IOException, CatalogTransformerException, ParseException {
        Metacard metacard = xit.transform(new FileInputStream("src/test/resources/metacard1.xml"));

        LOGGER.info("Attributes: ");
        for (AttributeDescriptor descriptor : metacard.getMetacardType()
                .getAttributeDescriptors()) {
            Attribute attribute = metacard.getAttribute(descriptor.getName());
            LOGGER.info("\t" + descriptor.getName() + ": " + ((attribute == null) ?
                    attribute :
                    attribute.getValue()));
        }

        LOGGER.info("Type: {}",
                metacard.getMetacardType()
                        .getName());
        LOGGER.info("Source: {}", metacard.getSourceId());

        assertEquals(ID, metacard.getId());
        assertEquals("ddf.metacard",
                metacard.getMetacardType()
                        .getName());
        assertEquals("foobar", metacard.getSourceId());

        // TODO use JTS to check for equality, not string comparison.
        assertEquals("POLYGON ((35 10, 10 20, 15 40, 45 45, 35 10), (20 30, 35 35, 30 20, 20 30))",
                metacard.getAttribute(Metacard.GEOGRAPHY)
                        .getValue());

        assertEquals(TITLE,
                metacard.getAttribute(Metacard.TITLE)
                        .getValue());

        assertArrayEquals(Base64.getDecoder()
                        .decode("AAABAAABAQEAAQAAAQEBAAEAAAEBAQABAAABAQEAAQAAAQEBAAEAAAEBAQABAAABAQE="),
                (byte[]) metacard.getAttribute(Metacard.THUMBNAIL)
                        .getValue());

        // TODO use XMLUnit to test equivalence
        assertThat(metacard.getAttribute(Metacard.METADATA)
                .getValue()
                .toString(), startsWith("<foo xmlns=\"http://foo.com\">"));

        assertEquals((new SimpleDateFormat("MMM d, yyyy HH:mm:ss.SSS z")).parse(
                "Dec 27, 2012 16:31:01.641 MST"),
                metacard.getAttribute(Metacard.EXPIRATION)
                        .getValue());

        assertEquals(DESCRIPTION,
                metacard.getAttribute("description")
                        .getValue());
        assertEquals(POINT_OF_CONTACT,
                metacard.getAttribute("point-of-contact")
                        .getValue());
    }

    @Test(expected = CatalogTransformerException.class)
    public void testUnknownMetacardType()
            throws IOException, CatalogTransformerException, ParseException {
        xit.transform(new FileInputStream("src/test/resources/unknownMetacard1.xml"));
    }
}
