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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import net.opengis.ows.v_1_0_0.ExceptionReport;

import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.junit.Test;

public class TestCswExceptionMapper {

    private static final String SERVICE_EXCEPTION_MSG = "Mock Exception";
    private static final String EXCEPTION_CODE = "OperationNotSupported";
    private static final String LOCATOR = "descriveRecords";

    @Test
    public void testCswExceptionToServiceExceptionReport() {

        CswException exception = new CswException(SERVICE_EXCEPTION_MSG,
                Status.BAD_REQUEST.getStatusCode());

        ExceptionMapper<CswException> exceptionMapper = new CswExceptionMapper();
        Response response = exceptionMapper.toResponse(exception);
        assertTrue(response.getEntity() instanceof ExceptionReport);
        ExceptionReport exceptionReport = (ExceptionReport) response.getEntity();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(SERVICE_EXCEPTION_MSG, exceptionReport.getException().get(0).getExceptionText().get(0));
        assertNull(exceptionReport.getException().get(0).getExceptionCode());
        assertNull(exceptionReport.getException().get(0).getLocator());
    }

    @Test
    public void testCswExceptionToServiceExceptionReportWithLocatorAndCode() {

        CswException exception = new CswException(SERVICE_EXCEPTION_MSG,
                Status.BAD_REQUEST.getStatusCode(), EXCEPTION_CODE, LOCATOR);

        ExceptionMapper<CswException> exceptionMapper = new CswExceptionMapper();
        Response response = exceptionMapper.toResponse(exception);
        assertTrue(response.getEntity() instanceof ExceptionReport);
        ExceptionReport exceptionReport = (ExceptionReport) response.getEntity();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(SERVICE_EXCEPTION_MSG, exceptionReport.getException().get(0).getExceptionText().get(0));
        assertEquals(EXCEPTION_CODE, exceptionReport.getException().get(0).getExceptionCode());
        assertEquals(LOCATOR, exceptionReport.getException().get(0).getLocator());
    }
}
