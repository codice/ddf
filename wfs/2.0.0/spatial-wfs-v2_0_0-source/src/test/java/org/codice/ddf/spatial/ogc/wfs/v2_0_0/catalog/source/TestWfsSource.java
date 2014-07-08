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
package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import net.opengis.filter.v_2_0_0.FilterCapabilities;
import net.opengis.wfs.v_2_0_0.FeatureTypeListType;
import net.opengis.wfs.v_2_0_0.FeatureTypeType;
import net.opengis.wfs.v_2_0_0.WFSCapabilitiesType;

import org.apache.commons.lang.StringUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityTask;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsException;
import org.codice.ddf.spatial.ogc.wfs.catalog.source.reader.FeatureCollectionMessageBodyReader;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.DescribeFeatureTypeRequest;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.WfsConstants;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;

public class TestWfsSource {

    private static final String ONE_TEXT_PROPERTY_SCHEMA = "<?xml version=\"1.0\"?>"
            + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
            + "<xs:element name=\"shiporder\">" + "<xs:complexType>" + "<xs:sequence>"
            + "<xs:element name=\"orderperson\" type=\"xs:string\"/>" + "</xs:sequence>"
            + "</xs:complexType>" + "</xs:element>" + "</xs:schema>";

    private static final String SAMPLE_FEATURE_NAME = "SampleFeature";

    private RemoteWfs mockWfs = mock(RemoteWfs.class);

    private WFSCapabilitiesType mockCapabilites = new WFSCapabilitiesType();

    private BundleContext mockContext = mock(BundleContext.class);

    private AvailabilityTask mockAvailabilityTask = mock(AvailabilityTask.class);

    private FeatureCollectionMessageBodyReader mockReader = mock(FeatureCollectionMessageBodyReader.class);

    public WfsSource getWfsSource(final String schema, final FilterCapabilities filterCapabilities,
            final String srsName, final int numFeatures) throws WfsException {

        // GetCapabilities Response
        when(mockWfs.getCapabilities(any(GetCapabilitiesRequest.class)))
                .thenReturn(mockCapabilites);
        mockCapabilites.setFilterCapabilities(filterCapabilities);

        when(mockAvailabilityTask.isAvailable()).thenReturn(true);

        mockCapabilites.setFeatureTypeList(new FeatureTypeListType());
        for (int ii = 0; ii < numFeatures; ii++) {
            FeatureTypeType feature = new FeatureTypeType();
            QName qName;
            if (ii == 0) {
                qName = new QName(SAMPLE_FEATURE_NAME + ii);
            } else {
                qName = new QName("http://example.com", SAMPLE_FEATURE_NAME + ii, "Prefix" + ii);
            }
            feature.setName(qName);
            feature.setDefaultCRS(WfsConstants.EPSG_4326_URN);
            mockCapabilites.getFeatureTypeList().getFeatureType().add(feature);
        }

        XmlSchema xmlSchema = null;
        if (StringUtils.isNotBlank(schema)) {
            XmlSchemaCollection schemaCollection = new XmlSchemaCollection();
            schemaCollection.setSchemaResolver(new WfsUriResolver());
            xmlSchema = schemaCollection.read(new StreamSource(new ByteArrayInputStream(schema
                    .getBytes())));
        }

        when(mockWfs.describeFeatureType(any(DescribeFeatureTypeRequest.class))).thenReturn(
                xmlSchema);

        when(mockWfs.getFeatureCollectionReader()).thenReturn(mockReader);

        return new WfsSource(mockWfs, new GeotoolsFilterAdapterImpl(), mockContext,
                mockAvailabilityTask);
    }

    @Test
    public void testParseCapabilities() throws WfsException {
        WfsSource source = getWfsSource(ONE_TEXT_PROPERTY_SCHEMA,
                MockWfsServer.getFilterCapabilities(),
                WfsConstants.EPSG_4326_URN, 1);

        assertTrue(source.isAvailable());
        assertThat(source.featureTypeFilters.size(), is(1));
        WfsFilterDelegate delegate = source.featureTypeFilters.get(new QName(SAMPLE_FEATURE_NAME
                + "0"));
        assertThat(delegate, notNullValue());
    }

    @Test
    public void testParseCapabilitiesNoFeatures() throws WfsException {
        WfsSource source = getWfsSource("", MockWfsServer.getFilterCapabilities(),
                WfsConstants.EPSG_4326_URN, 0);

        assertTrue(source.isAvailable());
        assertThat(source.featureTypeFilters.size(), is(0));
    }

    @Test
    public void testParseCapabilitiesNoFilterCapabilities() throws WfsException {
        WfsSource source = getWfsSource(ONE_TEXT_PROPERTY_SCHEMA, null, WfsConstants.EPSG_4326_URN,
                1);

        assertTrue(source.isAvailable());
        assertThat(source.featureTypeFilters.size(), is(0));
    }


}
