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
package org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.io.ByteArrayInputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsException;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.source.WfsResponseExceptionMapper;
import org.junit.Test;

public class TestWfsResponseExceptionMapper {

    @Test
    public void testCswExceptionWithNullResponse() {
        WfsException cswException = new WfsResponseExceptionMapper().fromResponse(null);

        assertThat(cswException.getMessage(), equalTo("Error handling response, response is null"));
    }

    @Test
    public void testWfsExceptionWithInvalidEntityType() {
        String serviceExceptionReportXml = "<?xml version='1.0'?>\r\n"
                + "<ServiceExceptionReport version='1.2.0'\r\n"
                + "    xmlns='http://www.opengis.net/ogc'\r\n"
                + "    xsi:schemaLocation='http://www.opengis.net/ogc http://schemas.opengis.net/wfs/1.0.0/OGC-exception.xsd'\r\n"
                + "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\r\n"
                + "    <ServiceException code='GeneralException'>Schema\r\n"
                + "        does not exist.</ServiceException>\r\n"
                + "</ServiceExceptionReport>";
        ResponseBuilder responseBuilder = Response.ok(serviceExceptionReportXml);
        responseBuilder.type("text/xml");
        Response response = responseBuilder.build();

        WfsException wfsException = new WfsResponseExceptionMapper().fromResponse(response);

        assertThat(wfsException.getMessage(),
                equalTo("Error reading response, entity type not understood: java.lang.String"));
    }

    @Test
    public void testInvalidWfsException() {
        String serviceExceptionReportXml = "<?xml version='1.0'?>\r\n"
                + "<ServiceExceptionReport version='1.2.0'\r\n"
                + "    xmlns='http://www.opengis.net/ogc'\r\n"
                + "    xsi:schemaLocation='http://www.opengis.net/ogc http://schemas.opengis.net/wfs/1.0.0/OGC-exception.xsd'\r\n"
                + "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\r\n"
                + "    <INVALID_TAG code='GeneralException'>Schema\r\n"
                + "        does not exist.</ServiceException>\r\n"
                + "</ServiceExceptionReport>";
        WfsException wfsException = createWfsException(serviceExceptionReportXml);

        assertThat(wfsException.getMessage(), containsString("Error parsing Response"));
    }

    @Test
    public void testEmptyWfsException() {
        String serviceExceptionReportXml = "<?xml version='1.0'?>\r\n"
                + "<ServiceExceptionReport version='1.2.0'\r\n"
                + "    xmlns='http://www.opengis.net/ogc'\r\n"
                + "    xsi:schemaLocation='http://www.opengis.net/ogc http://schemas.opengis.net/wfs/1.0.0/OGC-exception.xsd'\r\n"
                + "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\r\n"
                + "</ServiceExceptionReport>";
        WfsException wfsException = createWfsException(serviceExceptionReportXml);

        assertThat(wfsException.getMessage(),
                containsString("Empty Service Exception Report (version ="));
    }

    @Test
    public void testWfsExceptionWithMultipleServiceExceptions() {
        String serviceExceptionReportXml = "<?xml version='1.0'?>\r\n"
                + "<ServiceExceptionReport version='1.2.0'\r\n"
                + "    xmlns='http://www.opengis.net/ogc'\r\n"
                + "    xsi:schemaLocation='http://www.opengis.net/ogc http://schemas.opengis.net/wfs/1.0.0/OGC-exception.xsd'\r\n"
                + "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\r\n"
                + "    <ServiceException code='GeneralException'>First exception text</ServiceException>\r\n"
                + "    <ServiceException code='GeneralException'>Second exception text</ServiceException>\r\n"
                + "</ServiceExceptionReport>";

        WfsException wfsException = createWfsException(serviceExceptionReportXml);

        assertThat(wfsException.getMessage(), containsString("First exception text"));
        assertThat(wfsException.getMessage(), containsString("Second exception text"));
    }

    // //////////////////////////////////////////////////////////////

    private WfsException createWfsException(String serviceExceptionReportXml) {
        ByteArrayInputStream bis = new ByteArrayInputStream(serviceExceptionReportXml.getBytes());
        ResponseBuilder responseBuilder = Response.ok(bis);
        responseBuilder.type("text/xml");
        Response response = responseBuilder.build();

        return new WfsResponseExceptionMapper().fromResponse(response);
    }

}
