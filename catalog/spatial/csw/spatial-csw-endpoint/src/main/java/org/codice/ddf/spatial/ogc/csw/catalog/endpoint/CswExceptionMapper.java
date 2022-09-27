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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import net.opengis.ows.v_1_0_0.ExceptionReport;
import net.opengis.ows.v_1_0_0.ExceptionType;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CswExceptionMapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(CswExceptionMapper.class);

  // Per the CSW 2.0.2 spec, the service exception report version is fixed at
  // 1.2.0
  private static final String SERVICE_EXCEPTION_REPORT_VERSION = "1.2.0";

  private final CswXmlParser parser;

  public CswExceptionMapper(CswXmlParser parser) {
    this.parser = parser;
  }

  public void sendExceptionReport(Throwable exception, HttpServletResponse response)
      throws IOException {
    CswException cswException;
    if (exception instanceof CswException) {
      cswException = (CswException) exception;
    } else {
      String message = exception.getMessage();
      if (StringUtils.isBlank(message)) {
        cswException =
            new CswException(
                "Error parsing the request.  XML parameters may be missing or invalid.", exception);
        cswException.setExceptionCode(CswConstants.MISSING_PARAMETER_VALUE);
      } else {
        cswException = new CswException("Error handling request: " + message, exception);
        cswException.setExceptionCode(CswConstants.NO_APPLICABLE_CODE);
      }
    }
    LOGGER.debug("Error in CSW processing", cswException);

    response.setStatus(cswException.getHttpStatus());
    response.setContentType("text/xml");

    ExceptionReport exceptionReport = createServiceException(cswException);
    try {
      parser.marshal(exceptionReport, response.getOutputStream());
    } catch (ParserException e) {
      response.setStatus(SC_INTERNAL_SERVER_ERROR);
      LOGGER.warn("Unable to write out CSW ExceptionReport", e);
    }
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
