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
package org.codice.ddf.spatial.ogc.wcs.catalog.resource.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.opengis.wcs.v_1_0_0.CoverageDescription;
import net.opengis.wcs.v_1_0_0.CoverageOfferingType;
import net.opengis.wcs.v_1_0_0.DescribeCoverage;
import net.opengis.wcs.v_1_0_0.GetCapabilities;
import net.opengis.wcs.v_1_0_0.GetCoverage;
import net.opengis.wcs.v_1_0_0.WCSCapabilitiesType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.wcs.catalog.GetCoverageResponse;
import org.codice.ddf.spatial.ogc.wcs.catalog.WcsException;
import org.codice.ddf.spatial.ogc.wcs.catalog.impl.RemoteWcs;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.mime.MimeTypeMapper;

public class TestWcsResourceReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestWcsResourceReader.class);

    private static final String TEST_COVERAGE_ID = "12345";

    private static final String NITF_MIME_TYPE = "image/nitf";

    private static final String DEFAULT_WCS_ID = "My_WCS";

    private static final String DEFAULT_WCS_URL = "http://my.site.com/wcs";

    private RemoteWcs remoteWcs;

    private URI resourceURI;

    private static final JAXBContext jaxbContext = initJaxbContext();

    private static final String DEFAULT_PRODUCT_DATA = "product data goes here ...";

    private String DESCRIBE_COVERAGE_RESPONSE_TEMPLATE_XML = "<wcs:CoverageOffering \r\n"
            + "xmlns:xlink=\"http://www.w3.org/1999/xlink\" \r\n"
            + "xmlns:fn=\"http://www.w3.org/2005/xpath-functions\" \r\n"
            + "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" \r\n"
            + "xmlns:ogc=\"http://www.opengis.net/ogc\" \r\n"
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n"
            + "xmlns:wcs=\"http://www.opengis.net/wcs\"  \r\n"
            + "xmlns:gml=\"http://www.opengis.net/gml\" version=\"1.0\">\r\n"
            + "\r\n"
            + "        <wcs:metadataLink metadataType=\"other\" xlink:type=\"simple\" xlink:title=\"CSW GetRecordById\" xlink:href=\"http://my.site.com/csw?SERVICE=CSW&amp;REQUEST=getRecordById&amp;ID=999f&amp;VERSION=2.0.2\"/>\r\n"
            + "        <wcs:description>COMM-I</wcs:description>\r\n"
            + "        <wcs:name>%s</wcs:name>\r\n"
            + "        <wcs:label>18APR12QB020500012APR18063852-M1BS-052703909130_01_P001</wcs:label>\r\n"
            + "                <wcs:lonLatEnvelope srsName=\"EPSG:4326\">\r\n"
            + "            <gml:pos dimension=\"2\">%2$f %3$f</gml:pos>\r\n"
            + "            <gml:pos dimension=\"2\">%4$f %5$f</gml:pos>\r\n"
            + "        </wcs:lonLatEnvelope>\r\n"
            + "                <wcs:keywords>\r\n"
            + "            <wcs:keyword/>\r\n"
            + "        </wcs:keywords>\r\n"
            + "        <wcs:domainSet>\r\n"
            + "            <wcs:spatialDomain>\r\n"
            + "                <gml:Envelope srsName=\"EPSG:4326\">\r\n"
            + "                    <gml:pos dimension=\"2\">%2$f %3$f</gml:pos>\r\n"
            + "                    <gml:pos dimension=\"2\">%4$f %5$f</gml:pos>\r\n"
            + "                </gml:Envelope>\r\n"
            + "            </wcs:spatialDomain>\r\n"
            + "            <wcs:temporalDomain>\r\n"
            + "                <gml:timePosition>2012-04-18T06:38:52Z</gml:timePosition>\r\n"
            + "            </wcs:temporalDomain>\r\n"
            + "        </wcs:domainSet>\r\n"
            + "        <wcs:rangeSet>\r\n"
            + "            <wcs:RangeSet>\r\n"
            + "                <wcs:name>File Size</wcs:name>\r\n"
            + "                <wcs:label>Total File Size</wcs:label>\r\n"
            + "                <wcs:axisDescription>\r\n"
            + "                    <wcs:AxisDescription>\r\n"
            + "                        <wcs:name>File Size</wcs:name>\r\n"
            + "                        <wcs:label>Total File Size</wcs:label>\r\n"
            + "                        <wcs:values>\r\n"
            + "                            <wcs:singleValue>97393664</wcs:singleValue>\r\n"
            + "                        </wcs:values>\r\n"
            + "                    </wcs:AxisDescription>\r\n"
            + "                </wcs:axisDescription>\r\n"
            + "            </wcs:RangeSet>\r\n"
            + "        </wcs:rangeSet>\r\n"
            + "        <wcs:supportedCRSs>\r\n"
            + "            <wcs:requestResponseCRSs>EPSG:4326</wcs:requestResponseCRSs>\r\n"
            + "        </wcs:supportedCRSs>\r\n"
            + "        <wcs:supportedFormats nativeFormat=\"NITF02.10\">\r\n"
            + "            <wcs:formats>AS_IS;rrds:0</wcs:formats>\r\n"
            + "            <wcs:formats>AS_IS;rrds:1</wcs:formats>\r\n"
            + "            <wcs:formats>AS_IS;rrds:2</wcs:formats>\r\n"
            + "            <wcs:formats>AS_IS;rrds:3</wcs:formats>\r\n"
            + "            <wcs:formats>AS_IS;rrds:4</wcs:formats>\r\n"
            + "            <wcs:formats>AS_IS;rrds:5</wcs:formats>\r\n"
            + "            <wcs:formats>JPEG,none;bpp:24;compression:jpeg;rrds:0</wcs:formats>\r\n"
            + "            <wcs:formats>JPEG,none;bpp:8;compression:jpeg;rrds:0</wcs:formats>\r\n"
            + "            <wcs:formats>NITF,2.1;bpp:8;compression:jpeg2000;rrds:0</wcs:formats>\r\n"
            + "            <wcs:formats>NITF,2.1;bpp:8;compression:jpeg2000:NPJE;rrds:0</wcs:formats>\r\n"
            + "            <wcs:formats>NITF,2.1;bpp:8;compression:NC;rrds:0</wcs:formats>\r\n"
            + "            <wcs:formats>NITF,2.1;bpp:AS_IS;compression:jpeg2000;rrds:0</wcs:formats>\r\n"
            + "            <wcs:formats>NITF,2.1;bpp:AS_IS;compression:jpeg2000:NPJE;rrds:0</wcs:formats>\r\n"
            + "            <wcs:formats>NITF,2.1;bpp:AS_IS;compression:NC;rrds:0</wcs:formats>\r\n"
            + "            <wcs:formats>TIFF,6.0;bpp:16;compression:NC;rrds:0</wcs:formats>\r\n"
            + "            <wcs:formats>TIFF,6.0;bpp:24;compression:NC;rrds:0</wcs:formats>\r\n"
            + "            <wcs:formats>TIFF,6.0;bpp:8;compression:NC;rrds:0</wcs:formats>\r\n"
            + "        </wcs:supportedFormats>\r\n" + "        <wcs:supportedInterpolations>\r\n"
            + "            <wcs:interpolationMethod>none</wcs:interpolationMethod>\r\n"
            + "        </wcs:supportedInterpolations>\r\n" + "    </wcs:CoverageOffering>";

    private String GET_COVERAGE_REQUEST_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n"
            + "<wcs:GetCoverage service=\"WCS\" version=\"1.0.0\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:gco=\"http://www.isotc211.org/2005/gco\" xmlns:gmd=\"http://www.isotc211.org/2005/gmd\" xmlns:fra=\"http://www.cnig.gouv.fr/2005/fra\" xmlns:gmx=\"http://www.isotc211.org/2005/gmx\" xmlns:ns11=\"http://www.opengis.net/ows\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:dct=\"http://purl.org/dc/terms/\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:wcs=\"http://www.opengis.net/wcs\" xmlns:gmi=\"http://www.isotc211.org/2005/gmi\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
            + "    <wcs:sourceCoverage>0751d7a6-0fa4-4aba-8e85-63c072559762</wcs:sourceCoverage><!--3bd79c10-c127-4bd4-909f-1051970cedb7-->\r\n"
            + "    <wcs:domainSubset>\r\n"
            + "        <wcs:spatialSubset>\r\n"
            + "            <gml:Envelope srsName=\"EPSG:4326\">\r\n"
            + "                <gml:pos>51.72423713305664 33.72070539868164</gml:pos>    <!--65.769333 31.696955-->\r\n"
            + "                <gml:pos>51.73144691088867 33.727743515136716</gml:pos>    <!--65.786222 31.702889-->\r\n"
            + "            </gml:Envelope>\r\n"
            + "        </wcs:spatialSubset>\r\n"
            + "    </wcs:domainSubset>\r\n"
            + "    <wcs:output>\r\n"
            + "        <wcs:crs/>\r\n"
            + "        <wcs:format>AS_IS;rrds:0</wcs:format>\r\n"
            + "    </wcs:output>\r\n"
            + "</wcs:GetCoverage>";

    @Before
    public void setup() {
        remoteWcs = mock(RemoteWcs.class);
        InputStream stream = getClass().getResourceAsStream("/getCapabilitiesResponse.xml");
        WCSCapabilitiesType getCapabilitiesResponse = parseXml(stream, WCSCapabilitiesType.class);
        try {
            when(remoteWcs.getCapabilities(any(GetCapabilities.class))).thenReturn(
                    getCapabilitiesResponse);
        } catch (WcsException e1) {
            LOGGER.error("Could not handle GetCapabilities request");
        }

        try {
            resourceURI = new URI("wcs:" + TEST_COVERAGE_ID);
        } catch (URISyntaxException e) {
            LOGGER.error("Could not create URI");
        }
    }

    @Test
    public void testRetrieveResource() throws Exception {

        String filename = "02Aug21072624.ntf";
        String contentDisposition = String.format("Content-Disposition=attachment;filename=%s;",
                filename);

        ResourceResponse response = getWcsResourceReader(DEFAULT_PRODUCT_DATA, contentDisposition)
                .retrieveResource(resourceURI, null);

        verifyResponse(response, filename, NITF_MIME_TYPE, DEFAULT_PRODUCT_DATA);
    }

    @Test
    public void testRetrieveResourceUnknownFileExtension() throws Exception {

        String filename = "02Aug21072624.xyz";
        String contentDisposition = String.format("Content-Disposition=attachment;filename=%s;",
                filename);

        ResourceResponse response = getWcsResourceReader(DEFAULT_PRODUCT_DATA, contentDisposition)
                .retrieveResource(resourceURI, null);

        verifyResponse(response, filename, null, DEFAULT_PRODUCT_DATA);
    }

    @Test
    public void testRetrieveResourceNoFilename() throws Exception {

        String contentDisposition = "Content-Disposition=attachment;filename=;";

        ResourceResponse response = getWcsResourceReader(DEFAULT_PRODUCT_DATA, contentDisposition)
                .retrieveResource(resourceURI, null);

        verifyResponse(response, TEST_COVERAGE_ID, null, DEFAULT_PRODUCT_DATA);
    }

    @Test
    public void testRetrieveResourceQuotedFilename() throws Exception {

        String filename = "\"02Aug21072624.ntf\"";
        String contentDisposition = String.format("Content-Disposition=attachment;filename=%s;",
                filename);

        ResourceResponse response = getWcsResourceReader(DEFAULT_PRODUCT_DATA, contentDisposition)
                .retrieveResource(resourceURI, null);

        verifyResponse(response, StringUtils.strip(filename, "\""), NITF_MIME_TYPE,
                DEFAULT_PRODUCT_DATA);
    }

    @Test
    public void testRetrieveResourceNoContentDisposition() throws Exception {

        String contentDisposition = null;

        ResourceResponse response = getWcsResourceReader(DEFAULT_PRODUCT_DATA, contentDisposition)
                .retrieveResource(resourceURI, null);

        verifyResponse(response, TEST_COVERAGE_ID, null, DEFAULT_PRODUCT_DATA);
    }

    @Test
    public void testRetrieveResourceNoMimeTypeMapperAvailable() throws Exception {

        String filename = "02Aug21072624.ntf";
        String contentDisposition = String.format("Content-Disposition=attachment;filename=%s;",
                filename);

        setupDescribeCoverageResponse(TEST_COVERAGE_ID);

        setupGetCoverageResponse(DEFAULT_PRODUCT_DATA, contentDisposition);

        WcsResourceReader wcsResourceReader = new WcsResourceReader(remoteWcs, DEFAULT_WCS_ID,
                DEFAULT_WCS_URL, null);

        ResourceResponse response = wcsResourceReader.retrieveResource(resourceURI, null);

        verifyResponse(response, filename, null, DEFAULT_PRODUCT_DATA);
    }

    @Test
    public void testRetrieveResourceNoFileExtension() throws Exception {

        String filename = "02Aug21072624";
        String contentDisposition = String.format("Content-Disposition=attachment;filename=%s;",
                filename);

        ResourceResponse response = getWcsResourceReader(DEFAULT_PRODUCT_DATA, contentDisposition)
                .retrieveResource(resourceURI, null);

        verifyResponse(response, filename, null, DEFAULT_PRODUCT_DATA);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testRetrieveResourceNullResourceUri() throws Exception {

        String filename = "02Aug21072624";
        String contentDisposition = String.format("Content-Disposition=attachment;filename=%s;",
                filename);

        ResourceResponse response = getWcsResourceReader(DEFAULT_PRODUCT_DATA, contentDisposition)
                .retrieveResource(null, null);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testRetrieveResourceNonWcsResourceUri() throws Exception {

        String filename = "02Aug21072624";
        String contentDisposition = String.format("Content-Disposition=attachment;filename=%s;",
                filename);

        String uri = "http://my.site.com/" + TEST_COVERAGE_ID;
        URI nonWcsResourceURI = new URI(uri);
        ResourceResponse response = getWcsResourceReader(DEFAULT_PRODUCT_DATA, contentDisposition)
                .retrieveResource(nonWcsResourceURI, null);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testRetrieveResourceInvalidWcsResourceUri() throws Exception {

        String filename = "02Aug21072624";
        String contentDisposition = String.format("Content-Disposition=attachment;filename=%s;",
                filename);

        // URI has too many parts (should be 2, this one has 3 per ":" delimiter)
        String uri = "wcs:sourceId:" + TEST_COVERAGE_ID;
        URI invalidResourceURI = new URI(uri);
        ResourceResponse response = getWcsResourceReader(DEFAULT_PRODUCT_DATA, contentDisposition)
                .retrieveResource(invalidResourceURI, null);
    }

    @Test
    public void testRetrieveResourceInvalidDescribeCoverageResponse() throws Exception {

        String filename = "02Aug21072624";
        String contentDisposition = String.format("Content-Disposition=attachment;filename=%s;",
                filename);

        when(remoteWcs.describeCoverage(any(DescribeCoverage.class))).thenThrow(WcsException.class);

        setupGetCoverageResponse(DEFAULT_PRODUCT_DATA, contentDisposition);

        WcsResourceReader wcsResourceReader = new WcsResourceReader(remoteWcs, DEFAULT_WCS_ID,
                DEFAULT_WCS_URL, null);

        ResourceResponse response = wcsResourceReader.retrieveResource(resourceURI, null);

        assertThat(response, is(nullValue()));
    }

    @Test
    public void testRetrieveResourceNullDescribeCoverageResponse() throws Exception {

        when(remoteWcs.describeCoverage(any(DescribeCoverage.class))).thenReturn(null);

        WcsResourceReader wcsResourceReader = new WcsResourceReader(remoteWcs, DEFAULT_WCS_ID,
                DEFAULT_WCS_URL, null);

        ResourceResponse response = wcsResourceReader.retrieveResource(resourceURI, null);

        assertThat(response, is(nullValue()));
    }

    @Test
    public void testRetrieveResourceInvalidGetCoverageResponse() throws Exception {

        setupDescribeCoverageResponse(TEST_COVERAGE_ID);

        when(remoteWcs.getCoverage(any(GetCoverage.class))).thenThrow(WcsException.class);

        WcsResourceReader wcsResourceReader = new WcsResourceReader(remoteWcs, DEFAULT_WCS_ID,
                DEFAULT_WCS_URL, null);

        ResourceResponse response = wcsResourceReader.retrieveResource(resourceURI, null);

        assertThat(response, is(nullValue()));
    }

    // ///////////////////////////////////////////////////////////////////////////////////////////

    private void verifyResponse(ResourceResponse response, String expectedFilename,
            String expectedMimeType, String expectedProductData) throws Exception {

        assertThat(response, notNullValue());
        assertThat(response.getResource(), notNullValue());

        if (expectedMimeType != null) {
            assertThat(response.getResource().getMimeTypeValue(), containsString(expectedMimeType));
        } else {
            assertThat(response.getResource().getMimeTypeValue(), is(nullValue()));
        }

        if (expectedFilename != null) {
            assertThat(response.getResource().getName(), containsString(expectedFilename));
        } else {
            assertThat(response.getResource().getName(), is(nullValue()));
        }

        InputStream is = response.getResource().getInputStream();
        if (expectedProductData != null) {
            assertThat(IOUtils.toString(is), containsString(expectedProductData));
        } else {
            assertThat(IOUtils.toString(is), is(nullValue()));
        }
    }

    private WcsResourceReader getWcsResourceReader(String productData, String contentDisposition)
        throws Exception {

        setupDescribeCoverageResponse(TEST_COVERAGE_ID);

        setupGetCoverageResponse(productData, contentDisposition);

        WcsResourceReader wcsResourceReader = new WcsResourceReader(remoteWcs, DEFAULT_WCS_ID,
                DEFAULT_WCS_URL, getDefaultMimeTypeMapper());

        return wcsResourceReader;
    }

    // Uses default geometry
    private void setupDescribeCoverageResponse(String coverageId) throws Exception {
        setupDescribeCoverageResponse(coverageId, 32.41305555555555, 51.73888888888889, 32.28,
                51.5425);
    }

    private void setupDescribeCoverageResponse(String coverageId, double lowerCornerLat,
            double lowerCornerLon, double upperCornerLat, double upperCornerLon) {

        String describeCoverageResponseXml = String.format(DESCRIBE_COVERAGE_RESPONSE_TEMPLATE_XML,
                coverageId, lowerCornerLat, lowerCornerLon, upperCornerLat, upperCornerLon);

        CoverageOfferingType coverageOffering = parseXml(
                IOUtils.toInputStream(describeCoverageResponseXml), CoverageOfferingType.class);
        CoverageDescription coverageDescription = new CoverageDescription();
        coverageDescription.setCoverageOffering(Collections.singletonList(coverageOffering));

        try {
            when(remoteWcs.describeCoverage(any(DescribeCoverage.class))).thenReturn(
                    coverageDescription);
        } catch (WcsException e1) {
            LOGGER.error("Could not handle DescribeCoverage request");
        }
    }

    private void setupGetCoverageResponse(String productData, String contentDisposition)
        throws Exception {

        InputStream productDataStream = null;
        if (productData != null) {
            productDataStream = IOUtils.toInputStream(productData);
        }

        GetCoverageResponse getCoverageResponse = mock(GetCoverageResponse.class);
        when(getCoverageResponse.getInputStream()).thenReturn(productDataStream);
        when(getCoverageResponse.getContentDisposition()).thenReturn(contentDisposition);

        // POST
        when(remoteWcs.getCoverage(any(GetCoverage.class))).thenReturn(getCoverageResponse);
    }

    // Uses default file extensions-to-mime types mappings
    private MimeTypeMapper getDefaultMimeTypeMapper() throws Exception {
        Map<String, String> fileExtensionsToMimeTypes = new HashMap<String, String>();
        fileExtensionsToMimeTypes.put("ntf", NITF_MIME_TYPE);
        return getMimeTypeMapper(fileExtensionsToMimeTypes);
    }

    private MimeTypeMapper getMimeTypeMapper(Map<String, String> fileExtensionsToMimeTypes)
        throws Exception {

        MimeTypeMapper mimeTypeMapper = mock(MimeTypeMapper.class);
        for (String fileExtension : fileExtensionsToMimeTypes.keySet()) {
            String mimeType = fileExtensionsToMimeTypes.get(fileExtension);
            when(mimeTypeMapper.getMimeTypeForFileExtension(eq(fileExtension)))
                    .thenReturn(mimeType);
        }

        return mimeTypeMapper;
    }

    private static JAXBContext initJaxbContext() {

        JAXBContext jaxbContext = null;

        try {
            jaxbContext = JAXBContext
                    .newInstance(
                            "net.opengis.wcs.v_1_0_0:net.opengis.gml.profiles.gml4wcs.v_1_0_0:net.opengis.ows.v_1_0_0",
                            TestWcsResourceReader.class.getClassLoader());
        } catch (JAXBException e) {
            LOGGER.error("Failed to initialize JAXBContext", e);
        }

        return jaxbContext;
    }

    private <T1, T2> T1 parseXml(InputStream stream, T2 jaxbElementType) {
        JAXBElement<T2> jaxb = null;
        try {
            Unmarshaller u = jaxbContext.createUnmarshaller();

            Object o = u.unmarshal(stream);
            jaxb = (JAXBElement<T2>) o;
        } catch (JAXBException e) {
            LOGGER.error("failed to parse xml", e);
        }

        return (T1) jaxb.getValue();
    }

}
