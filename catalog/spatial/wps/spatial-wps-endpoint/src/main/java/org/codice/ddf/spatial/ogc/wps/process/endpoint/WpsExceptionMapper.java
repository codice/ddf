/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.ogc.wps.process.endpoint;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import net.opengis.ows.v_2_0.ExceptionReport;
import net.opengis.ows.v_2_0.ExceptionType;
import org.codice.ddf.spatial.ogc.wps.process.api.WpsException;
import org.codice.ddf.spatial.process.api.BadRequestException;
import org.codice.ddf.spatial.process.api.DataNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WpsExceptionMapper implements ExceptionMapper<Throwable> {

  private static final Logger LOGGER = LoggerFactory.getLogger(WpsExceptionMapper.class);

  private static final String SERVICE_EXCEPTION_REPORT_VERSION = "2.0.0";

  @Override
  public Response toResponse(Throwable e) {

    WpsException wpsException;
    if (e instanceof WpsException) {
      wpsException = (WpsException) e;
      LOGGER.debug(
          "{}: {}. {}",
          wpsException.getExceptionCode(),
          wpsException.getMessage(),
          wpsException.getLocator(),
          e);
    } else if (e instanceof BadRequestException) {
      BadRequestException badRequestException = (BadRequestException) e;
      LOGGER.debug("Bad request for process (job ID = {}).", badRequestException.getJobId(), e);
      wpsException =
          new WpsException(
              "One or more of inputs for which the service was able to retrieve the data but could not read it.",
              "WrongInputData",
              String.join(",", badRequestException.getInputIds()),
              HTTP_BAD_REQUEST);
    } else if (e instanceof DataNotFoundException) {
      DataNotFoundException dataNotFoundException = (DataNotFoundException) e;
      LOGGER.debug(
          "One or more of the input data for the process could not be found (job ID = {}).",
          dataNotFoundException.getJobId(),
          e);
      wpsException =
          new WpsException(
              "One of the referenced input data sets was inaccessible.",
              "DataNotAccessible",
              String.join(",", dataNotFoundException.getInputIds()),
              HTTP_BAD_REQUEST);
    } else {
      LOGGER.error(e.getMessage(), e);
      wpsException = new WpsException(null, "InternalServerError", null, HTTP_INTERNAL_ERROR);
    }
    return Response.status(wpsException.getHttpStatus())
        .entity(createServiceException(wpsException))
        .type(MediaType.TEXT_XML)
        .build();
  }

  private ExceptionReport createServiceException(WpsException wpsException) {
    ExceptionReport exceptionReport = new ExceptionReport();
    exceptionReport.setVersion(SERVICE_EXCEPTION_REPORT_VERSION);
    ExceptionType exception = new ExceptionType();
    exception.setExceptionCode(wpsException.getExceptionCode());
    exception.setLocator(wpsException.getLocator());
    exception.getExceptionText().add(wpsException.getMessage());
    exceptionReport.getException().add(exception);
    return exceptionReport;
  }
}
