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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;

import net.opengis.ows.v_1_0_0.ExceptionReport;
import net.opengis.ows.v_1_0_0.ExceptionType;

public class CswExceptionMapper implements ExceptionMapper<Throwable> {

    // Per the CSW 2.0.2 spec, the service exception report version is fixed at
    // 1.2.0
    private static final String SERVICE_EXCEPTION_REPORT_VERSION = "1.2.0";

    @Override
    public Response toResponse(Throwable exception) {
        CswException cswException;
        if (exception instanceof CswException) {
            cswException = (CswException) exception;
        } else  {
            cswException = new CswException("Error parsing the request.  XML parameters may be missing or invalid.",
                    CswConstants.MISSING_PARAMETER_VALUE, null);
        }
        return Response.status(cswException.getHttpStatus()).entity(createServiceException(cswException))
                .type(MediaType.TEXT_XML).build();
    }

    private ExceptionReport createServiceException(CswException cswException) {
        ExceptionReport exceptionReport = new ExceptionReport();
        exceptionReport.setVersion(SERVICE_EXCEPTION_REPORT_VERSION);
        ExceptionType exception = new ExceptionType();
        exception.setExceptionCode(cswException.getExceptionCode());
        exception.setLocator(cswException.getLocator());
        exception.getExceptionText().add(cswException.getMessage());
        exceptionReport.getException().add(exception);
        return exceptionReport;
    }
}
