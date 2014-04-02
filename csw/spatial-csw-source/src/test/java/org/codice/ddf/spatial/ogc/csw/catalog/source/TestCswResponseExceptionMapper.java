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
package org.codice.ddf.spatial.ogc.csw.catalog.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.io.ByteArrayInputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.junit.Test;

public class TestCswResponseExceptionMapper {

    @Test
    public void testCswExceptionWithNullResponse() {
        CswException cswException = new CswResponseExceptionMapper().fromResponse(null);

        assertThat(cswException.getMessage(), equalTo("Error handling response, response is null"));
    }

    @Test
    public void testCswExceptionWithInvalidEntityType() {
        String exceptionReportXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n"
                + "<ows:ExceptionReport version=\"1.2.0\" xmlns:ns16=\"http://www.opengis.net/ows/1.1\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:cat=\"http://www.opengis.net/cat/csw\" xmlns:gco=\"http://www.isotc211.org/2005/gco\" xmlns:gmd=\"http://www.isotc211.org/2005/gmd\" xmlns:fra=\"http://www.cnig.gouv.fr/2005/fra\" xmlns:ins=\"http://www.inspire.org\" xmlns:gmx=\"http://www.isotc211.org/2005/gmx\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:dct=\"http://purl.org/dc/terms/\" xmlns:ows=\"http://www.opengis.net/ows\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:gmi=\"http://www.isotc211.org/2005/gmi\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
                + "    <ows:Exception exceptionCode=\"INVALID_PARAMETER_VALUE\" locator=\"QueryConstraint\">\r\n"
                + "        <ows:ExceptionText>Only dc:title,dct:modified,dc:subject,dct:dateSubmitted,dct:alternative,dc:format,dct:created,dc:type,dct:abstract,dc:identifier,dc:creator  are currently supported</ows:ExceptionText>\r\n"
                + "    </ows:Exception>\r\n" + "</ows:ExceptionReport>";
        ResponseBuilder responseBuilder = Response.ok(exceptionReportXml);
        responseBuilder.type("text/xml");
        Response response = responseBuilder.build();

        CswException cswException = new CswResponseExceptionMapper().fromResponse(response);

        assertThat(cswException.getMessage(),
                equalTo("Error reading response, entity type not understood: java.lang.String"));
    }

    @Test
    public void testInvalidCswException() {
        String exceptionReportXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n"
                + "<ows:ExceptionReport version=\"1.2.0\" xmlns:ns16=\"http://www.opengis.net/ows/1.1\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:cat=\"http://www.opengis.net/cat/csw\" xmlns:gco=\"http://www.isotc211.org/2005/gco\" xmlns:gmd=\"http://www.isotc211.org/2005/gmd\" xmlns:fra=\"http://www.cnig.gouv.fr/2005/fra\" xmlns:ins=\"http://www.inspire.org\" xmlns:gmx=\"http://www.isotc211.org/2005/gmx\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:dct=\"http://purl.org/dc/terms/\" xmlns:ows=\"http://www.opengis.net/ows\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:gmi=\"http://www.isotc211.org/2005/gmi\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
                + "    <ows:Invalid exceptionCode=\"INVALID_PARAMETER_VALUE\" locator=\"QueryConstraint\">\r\n"
                + "        <ows:ExceptionText>Only dc:title,dct:modified,dc:subject,dct:dateSubmitted,dct:alternative,dc:format,dct:created,dc:type,dct:abstract,dc:identifier,dc:creator  are currently supported</ows:ExceptionText>\r\n"
                + "    </ows:Exception>\r\n" + "</ows:ExceptionReport>";

        CswException cswException = createCswException(exceptionReportXml);

        assertThat(cswException.getMessage(),
                containsString("Error received from remote Csw server"));
    }

    @Test
    public void testEmptyCswException() {
        String exceptionReportXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n"
                + "<ows:ExceptionReport version=\"1.2.0\" xmlns:ns16=\"http://www.opengis.net/ows/1.1\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:cat=\"http://www.opengis.net/cat/csw\" xmlns:gco=\"http://www.isotc211.org/2005/gco\" xmlns:gmd=\"http://www.isotc211.org/2005/gmd\" xmlns:fra=\"http://www.cnig.gouv.fr/2005/fra\" xmlns:ins=\"http://www.inspire.org\" xmlns:gmx=\"http://www.isotc211.org/2005/gmx\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:dct=\"http://purl.org/dc/terms/\" xmlns:ows=\"http://www.opengis.net/ows\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:gmi=\"http://www.isotc211.org/2005/gmi\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
                + "</ows:ExceptionReport>";

        CswException cswException = createCswException(exceptionReportXml);

        assertThat(cswException.getMessage(),
                containsString("Empty Exception Report (version = 1.2.0)"));
    }

    @Test
    public void testCswExceptionWithCodeOnly() {
        String exceptionReportXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n"
                + "<ows:ExceptionReport version=\"1.2.0\" xmlns:ns16=\"http://www.opengis.net/ows/1.1\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:cat=\"http://www.opengis.net/cat/csw\" xmlns:gco=\"http://www.isotc211.org/2005/gco\" xmlns:gmd=\"http://www.isotc211.org/2005/gmd\" xmlns:fra=\"http://www.cnig.gouv.fr/2005/fra\" xmlns:ins=\"http://www.inspire.org\" xmlns:gmx=\"http://www.isotc211.org/2005/gmx\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:dct=\"http://purl.org/dc/terms/\" xmlns:ows=\"http://www.opengis.net/ows\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:gmi=\"http://www.isotc211.org/2005/gmi\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
                + "    <ows:Exception exceptionCode=\"INVALID_PARAMETER_VALUE\">\r\n"
                + "        <ows:ExceptionText>Only dc:title,dct:modified,dc:subject,dct:dateSubmitted,dct:alternative,dc:format,dct:created,dc:type,dct:abstract,dc:identifier,dc:creator  are currently supported</ows:ExceptionText>\r\n"
                + "    </ows:Exception>\r\n" + "</ows:ExceptionReport>";

        CswException cswException = createCswException(exceptionReportXml);

        assertThat(cswException.getMessage(),
                containsString("exceptionCode = INVALID_PARAMETER_VALUE"));
        assertThat(cswException.getMessage(), not(containsString("locator = ")));
        assertThat(
                cswException.getMessage(),
                containsString("Only dc:title,dct:modified,dc:subject,dct:dateSubmitted,dct:alternative,dc:format,dct:created,dc:type,dct:abstract,dc:identifier,dc:creator  are currently supported"));
    }

    @Test
    public void testCswExceptionWithLocatorOnly() {
        String exceptionReportXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n"
                + "<ows:ExceptionReport version=\"1.2.0\" xmlns:ns16=\"http://www.opengis.net/ows/1.1\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:cat=\"http://www.opengis.net/cat/csw\" xmlns:gco=\"http://www.isotc211.org/2005/gco\" xmlns:gmd=\"http://www.isotc211.org/2005/gmd\" xmlns:fra=\"http://www.cnig.gouv.fr/2005/fra\" xmlns:ins=\"http://www.inspire.org\" xmlns:gmx=\"http://www.isotc211.org/2005/gmx\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:dct=\"http://purl.org/dc/terms/\" xmlns:ows=\"http://www.opengis.net/ows\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:gmi=\"http://www.isotc211.org/2005/gmi\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
                + "    <ows:Exception locator=\"QueryConstraint\">\r\n"
                + "        <ows:ExceptionText>Only dc:title,dct:modified,dc:subject,dct:dateSubmitted,dct:alternative,dc:format,dct:created,dc:type,dct:abstract,dc:identifier,dc:creator  are currently supported</ows:ExceptionText>\r\n"
                + "    </ows:Exception>\r\n" + "</ows:ExceptionReport>";

        CswException cswException = createCswException(exceptionReportXml);

        assertThat(cswException.getMessage(), containsString("exceptionCode = UNSPECIFIED"));
        assertThat(cswException.getMessage(), containsString("locator = QueryConstraint"));
        assertThat(
                cswException.getMessage(),
                containsString("Only dc:title,dct:modified,dc:subject,dct:dateSubmitted,dct:alternative,dc:format,dct:created,dc:type,dct:abstract,dc:identifier,dc:creator  are currently supported"));
    }

    @Test
    public void testCswExceptionWithCodeAndLocator() {
        String exceptionReportXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n"
                + "<ows:ExceptionReport version=\"1.2.0\" xmlns:ns16=\"http://www.opengis.net/ows/1.1\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:cat=\"http://www.opengis.net/cat/csw\" xmlns:gco=\"http://www.isotc211.org/2005/gco\" xmlns:gmd=\"http://www.isotc211.org/2005/gmd\" xmlns:fra=\"http://www.cnig.gouv.fr/2005/fra\" xmlns:ins=\"http://www.inspire.org\" xmlns:gmx=\"http://www.isotc211.org/2005/gmx\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:dct=\"http://purl.org/dc/terms/\" xmlns:ows=\"http://www.opengis.net/ows\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:gmi=\"http://www.isotc211.org/2005/gmi\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
                + "    <ows:Exception exceptionCode=\"INVALID_PARAMETER_VALUE\" locator=\"QueryConstraint\">\r\n"
                + "        <ows:ExceptionText>Only dc:title,dct:modified,dc:subject,dct:dateSubmitted,dct:alternative,dc:format,dct:created,dc:type,dct:abstract,dc:identifier,dc:creator  are currently supported</ows:ExceptionText>\r\n"
                + "    </ows:Exception>\r\n" + "</ows:ExceptionReport>";

        CswException cswException = createCswException(exceptionReportXml);

        assertThat(cswException.getMessage(),
                containsString("exceptionCode = INVALID_PARAMETER_VALUE"));
        assertThat(cswException.getMessage(), containsString("locator = QueryConstraint"));
        assertThat(
                cswException.getMessage(),
                containsString("Only dc:title,dct:modified,dc:subject,dct:dateSubmitted,dct:alternative,dc:format,dct:created,dc:type,dct:abstract,dc:identifier,dc:creator  are currently supported"));
    }

    @Test
    public void testCswExceptionWithMultipleExceptionTexts() {
        String exceptionReportXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n"
                + "<ows:ExceptionReport version=\"1.2.0\" xmlns:ns16=\"http://www.opengis.net/ows/1.1\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:cat=\"http://www.opengis.net/cat/csw\" xmlns:gco=\"http://www.isotc211.org/2005/gco\" xmlns:gmd=\"http://www.isotc211.org/2005/gmd\" xmlns:fra=\"http://www.cnig.gouv.fr/2005/fra\" xmlns:ins=\"http://www.inspire.org\" xmlns:gmx=\"http://www.isotc211.org/2005/gmx\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:dct=\"http://purl.org/dc/terms/\" xmlns:ows=\"http://www.opengis.net/ows\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:gmi=\"http://www.isotc211.org/2005/gmi\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
                + "    <ows:Exception exceptionCode=\"INVALID_PARAMETER_VALUE\" locator=\"QueryConstraint\">\r\n"
                + "        <ows:ExceptionText>Only dc:title,dct:modified,dc:subject,dct:dateSubmitted,dct:alternative,dc:format,dct:created,dc:type,dct:abstract,dc:identifier,dc:creator  are currently supported</ows:ExceptionText>\r\n"
                + "        <ows:ExceptionText>Second exception text</ows:ExceptionText>\r\n"
                + "    </ows:Exception>\r\n" + "</ows:ExceptionReport>";

        CswException cswException = createCswException(exceptionReportXml);

        assertThat(cswException.getMessage(),
                containsString("exceptionCode = INVALID_PARAMETER_VALUE"));
        assertThat(cswException.getMessage(), containsString("locator = QueryConstraint"));
        assertThat(
                cswException.getMessage(),
                containsString("Only dc:title,dct:modified,dc:subject,dct:dateSubmitted,dct:alternative,dc:format,dct:created,dc:type,dct:abstract,dc:identifier,dc:creator  are currently supported"));
        assertThat(cswException.getMessage(), containsString("Second exception text"));
    }

    // //////////////////////////////////////////////////////////////

    private CswException createCswException(String exceptionReportXml) {
        ByteArrayInputStream bis = new ByteArrayInputStream(exceptionReportXml.getBytes());
        ResponseBuilder responseBuilder = Response.ok(bis);
        responseBuilder.type("text/xml");
        Response response = responseBuilder.build();

        return new CswResponseExceptionMapper().fromResponse(response);
    }

}
