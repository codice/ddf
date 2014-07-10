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
package org.codice.ddf.spatial.ogc.wfs.catalog.endpoint;

import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import ogc.schema.opengis.wfs.exception.v_1_0_0.ServiceExceptionReport;

import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsException;
import org.junit.Test;

public class TestWfsExceptionMapper {

    private static final String SERVICE_EXCEPTION_MSG = "Mock Exception";

    @Test
    public void testWfsExceptionToServiceExceptionReport() {

        WfsException wfsException = new WfsException(SERVICE_EXCEPTION_MSG,
                Status.BAD_REQUEST.getStatusCode());

        ExceptionMapper<WfsException> exceptionMapper = new WfsExceptionMapper();
        Response response = exceptionMapper.toResponse(wfsException);
        assertTrue(response.getEntity() instanceof ServiceExceptionReport);
        ServiceExceptionReport exceptionReport = (ServiceExceptionReport) response.getEntity();
        assertTrue(response.getStatus() == Status.BAD_REQUEST.getStatusCode());
        assertTrue(exceptionReport.getServiceException().get(0).getValue() == SERVICE_EXCEPTION_MSG);

    }

}
