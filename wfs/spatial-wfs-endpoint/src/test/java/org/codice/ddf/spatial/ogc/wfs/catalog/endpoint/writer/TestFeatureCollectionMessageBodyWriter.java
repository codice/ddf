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
package org.codice.ddf.spatial.ogc.wfs.catalog.endpoint.writer;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.io.IOUtils;
import org.apache.xerces.dom.DOMInputImpl;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollection;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class TestFeatureCollectionMessageBodyWriter {

    private static final String SOURCE = "src";

    private static final String CONTENT_TYPE = "Sport";

    private static final String CONTENT_TYPE_VERSION = "1.0.0";

    private static final Date DATE = new Date(1364320829000l);

    private static final String ID = "team_id";

    private static final String TITLE = "team_name";

    private static final String HOME_LOCATION = "home";

    private static final String AWAY_LOCATION = "away";

    private static final String WINS = "wins";

    private static final String DATE_CREATED = "date_created";

    private static final String TEAM1_ID = "team1";

    private static final String TEAM2_ID = "team2";

    private static final String TEAM1_LOCATION = "POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))";

    private static final String TEAM2_LOCATION = "POLYGON ((1 1, 2 2, 3 3, 4 4, 1 1))";

    private static final int WIN_COUNT = 55;

    private static final String BASIC_ID = "basic_id";

    private static final String BASIC_CONTENT_TYPE = "basic";

    private static final String BASIC_TITLE = "basic_title";

    private static final String BASIC_LOCATION = "POINT(1 1)";

    @Test
    public void testWriteToGeneratesGMLConformantXml() throws IOException, WebApplicationException,
        SAXException {

        FeatureCollectionMessageBodyWriter wtr = new FeatureCollectionMessageBodyWriter();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        wtr.writeTo(getWfsFeatureCollection(), null, null, null, null, null, stream);
        String actual = stream.toString();

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setResourceResolver(new LSResourceResolver() {

            private Map<String, String> schemaLocations;

            private Map<String, LSInput> inputs;

            {
                inputs = new HashMap<String, LSInput>();

                schemaLocations = new HashMap<String, String>();

                schemaLocations.put("xml.xsd", "/w3/1999/xml.xsd");
                schemaLocations.put("xlink.xsd", "/w3/1999/xlink.xsd");
                schemaLocations.put("geometry.xsd", "/gml/2.1.2/geometry.xsd");
                schemaLocations.put("feature.xsd", "/gml/2.1.2/feature.xsd");
                schemaLocations.put("gml.xsd", "/gml/2.1.2/gml.xsd");
                schemaLocations.put("expr.xsd", "/filter/1.0.0/expr.xsd");
                schemaLocations.put("filter.xsd", "/filter/1.0.0/filter.xsd");
                schemaLocations
                        .put("filterCapabilities.xsd", "/filter/1.0.0/filterCapabilties.xsd");
                schemaLocations.put("WFS-capabilities.xsd", "/wfs/1.0.0/WFS-capabilities.xsd");
                schemaLocations.put("OGC-exception.xsd", "/wfs/1.0.0/OGC-exception.xsd");
                schemaLocations.put("WFS-basic.xsd", "/wfs/1.0.0/WFS-basic.xsd");
                schemaLocations.put("WFS-transaction.xsd", "/wfs/1.0.0/WFS-transaction.xsd");
                schemaLocations.put("wfs.xsd", "/wfs/1.0.0/wfs.xsd");
            }

            @Override
            public LSInput resolveResource(String type, String namespaceURI, String publicId,
                    String systemId, String baseURI) {
                String fileName = new java.io.File(systemId).getName();
                if (inputs.containsKey(fileName)) {
                    return inputs.get(fileName);
                }

                LSInput input = new DOMInputImpl();
                InputStream is = getClass().getResourceAsStream(schemaLocations.get(fileName));
                input.setByteStream(is);
                input.setBaseURI(baseURI);
                input.setSystemId(systemId);
                inputs.put(fileName, input);
                return input;
            }
        });

        Source wfsSchemaSource = new StreamSource(getClass().getResourceAsStream(
                "/wfs/1.0.0/wfs.xsd"));
        Source testSchemaSource = new StreamSource(getClass().getResourceAsStream("/schema.xsd"));

        Schema schema = schemaFactory.newSchema(new Source[] {wfsSchemaSource, testSchemaSource});

        try {
            schema.newValidator().validate(new StreamSource(new StringReader(actual)));
        } catch (Exception e) {
            fail("Generated GML Response does not conform to WFS Schema" + e.getMessage());
        }
    }

    @Test
    public void testWriteToGeneratesExpectedNonBasicMetacard() throws IOException,
        WebApplicationException, SAXException {

        FeatureCollectionMessageBodyWriter wtr = new FeatureCollectionMessageBodyWriter();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        wtr.writeTo(getWfsFeatureCollection(), null, null, null, null, null, stream);

        String actual = stream.toString();
        String expected = IOUtils.toString(getClass().getResourceAsStream("/wfs.xml"), "UTF-8");

        XMLUnit.setIgnoreWhitespace(true);

        assertXMLEqual("Failed to generate correct GML", expected, actual);
    }

    @Test
    public void testWriteToGeneratesExpectedBasicMetacard() throws IOException,
        WebApplicationException, SAXException {

        FeatureCollectionMessageBodyWriter wtr = new FeatureCollectionMessageBodyWriter();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        wtr.writeTo(getBasicWfsFeatureCollection(), null, null, null, null, null, stream);

        String actual = stream.toString();
        String expected = IOUtils.toString(getClass().getResourceAsStream("/basic.xml"), "UTF-8");

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual("Failed to generate correct GML", expected, actual);
    }

    private WfsFeatureCollection getWfsFeatureCollection() {
        WfsFeatureCollection collection = new WfsFeatureCollection();
        List<Metacard> metacards = new ArrayList<Metacard>();

        metacards.add(getMetacard(TEAM1_ID, TEAM1_LOCATION, TEAM2_LOCATION));
        metacards.add(getMetacard(TEAM2_ID, TEAM2_LOCATION, TEAM1_LOCATION));

        collection.setFeatureMembers(metacards);

        return collection;
    }

    private WfsFeatureCollection getBasicWfsFeatureCollection() {
        WfsFeatureCollection collection = new WfsFeatureCollection();
        List<Metacard> metacards = new ArrayList<Metacard>();

        metacards.add(getBasicMetacard());

        collection.setFeatureMembers(metacards);

        return collection;
    }

    private Metacard getMetacard(String team, String home, String away) {
        Set<AttributeDescriptor> descriptors = new HashSet<AttributeDescriptor>();

        descriptors.addAll(BasicTypes.BASIC_METACARD.getAttributeDescriptors());
        descriptors.add(new AttributeDescriptorImpl(ID, false, false, false, false,
                BasicTypes.LONG_TYPE));
        descriptors.add(new AttributeDescriptorImpl(TITLE, false, false, false, false,
                BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(DATE_CREATED, false, false, false, false,
                BasicTypes.DATE_TYPE));
        descriptors.add(new AttributeDescriptorImpl(WINS, false, false, false, false,
                BasicTypes.INTEGER_TYPE));
        descriptors.add(new AttributeDescriptorImpl(HOME_LOCATION, false, false, false, false,
                BasicTypes.GEO_TYPE));
        descriptors.add(new AttributeDescriptorImpl(AWAY_LOCATION, false, false, false, false,
                BasicTypes.GEO_TYPE));

        MetacardTypeImpl metacardType = new MetacardTypeImpl(CONTENT_TYPE, descriptors);

        MetacardImpl metacard = new MetacardImpl();
        metacard.setType(metacardType);

        metacard.setContentTypeName(CONTENT_TYPE);
        metacard.setContentTypeVersion(CONTENT_TYPE_VERSION);
        metacard.setCreatedDate(DATE);
        metacard.setEffectiveDate(DATE);
        metacard.setId(team);
        metacard.setLocation(home);
        metacard.setSourceId(SOURCE);
        metacard.setTitle(team);

        metacard.setAttribute(ID, team);
        metacard.setAttribute(TITLE, team);
        metacard.setAttribute(DATE_CREATED, DATE);
        metacard.setAttribute(WINS, WIN_COUNT);
        metacard.setAttribute(HOME_LOCATION, home);
        metacard.setAttribute(AWAY_LOCATION, away);

        return metacard;
    }

    private Metacard getBasicMetacard() {
        MetacardImpl metacard = new MetacardImpl();
        metacard.setType(BasicTypes.BASIC_METACARD);

        metacard.setContentTypeName(BASIC_CONTENT_TYPE);
        metacard.setContentTypeVersion(CONTENT_TYPE_VERSION);
        metacard.setCreatedDate(DATE);
        metacard.setEffectiveDate(DATE);
        metacard.setId(BASIC_ID);
        metacard.setLocation(BASIC_LOCATION);
        metacard.setSourceId(SOURCE);
        metacard.setTitle(BASIC_TITLE);

        return metacard;
    }

}
