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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import ogc.schema.opengis.wfs.exception.v_1_0_0.ServiceExceptionReport;
import ogc.schema.opengis.wfs.exception.v_1_0_0.ServiceExceptionType;

import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsException;

/**
 * Implementation of a JAX-RS Exception Mapper which maps application exceptions into a custom
 * response. In this case the custom response is a Service Exception Report per the WFS 1.0.0. See
 * {@link http ://www.opengeospatial.org/standards/wfs}
 * 
 */
@Provider
public class WfsExceptionMapper implements ExceptionMapper<WfsException> {

    // Per the WFS 1.0.0 spec, the service exception report version is fixed at
    // 1.2.0
    private static final String SERVICE_EXCEPTION_REPORT_VERSION = "1.2.0";

    @Override
    public Response toResponse(WfsException wfsException) {
        return Response.status(wfsException.getHttpStatus())
                .entity(createServiceException(wfsException)).type(MediaType.TEXT_XML).build();
    }

    private ServiceExceptionReport createServiceException(WfsException wfsException) {

        ServiceExceptionReport serviceExceptionReport = new ServiceExceptionReport();
        serviceExceptionReport.setVersion(SERVICE_EXCEPTION_REPORT_VERSION);
        ServiceExceptionType serviceException = new ServiceExceptionType();
        serviceException.setValue(wfsException.getMessage());
        serviceExceptionReport.getServiceException().add(serviceException);
        return serviceExceptionReport;
    }
}
