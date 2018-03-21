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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import net.opengis.ows.v_1_0_0.ExceptionReport;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.junit.Test;

public class CswExceptionMapperTest {

  private static final String SERVICE_EXCEPTION_MSG = "Mock Exception";

  private static final String EXCEPTION_CODE = "OperationNotSupported";

  private static final String LOCATOR = "describeRecords";

  private static final String XML_PARSE_FAIL_MSG =
      "Error parsing the request.  XML parameters may be missing or invalid.";

  @Test
  public void testCswExceptionToServiceExceptionReport() {

    CswException exception =
        new CswException(SERVICE_EXCEPTION_MSG, Status.BAD_REQUEST.getStatusCode());

    ExceptionMapper<Throwable> exceptionMapper = new CswExceptionMapper();
    Response response = exceptionMapper.toResponse(exception);
    assertThat(response.getEntity(), is(instanceOf(ExceptionReport.class)));
    ExceptionReport exceptionReport = (ExceptionReport) response.getEntity();
    assertThat(Status.BAD_REQUEST.getStatusCode(), is(response.getStatus()));
    assertThat(
        SERVICE_EXCEPTION_MSG, is(exceptionReport.getException().get(0).getExceptionText().get(0)));
    assertThat(exceptionReport.getException().get(0).getExceptionCode(), nullValue());
    assertThat(exceptionReport.getException().get(0).getLocator(), nullValue());
  }

  @Test
  public void testCswExceptionToServiceExceptionReportWithLocatorAndCode() {

    CswException exception =
        new CswException(
            SERVICE_EXCEPTION_MSG, Status.BAD_REQUEST.getStatusCode(), EXCEPTION_CODE, LOCATOR);

    ExceptionMapper<Throwable> exceptionMapper = new CswExceptionMapper();
    Response response = exceptionMapper.toResponse(exception);
    assertThat(response.getEntity(), is(instanceOf(ExceptionReport.class)));
    ExceptionReport exceptionReport = (ExceptionReport) response.getEntity();
    assertThat(Status.BAD_REQUEST.getStatusCode(), is(response.getStatus()));
    assertThat(
        SERVICE_EXCEPTION_MSG, is(exceptionReport.getException().get(0).getExceptionText().get(0)));
    assertThat(EXCEPTION_CODE, is(exceptionReport.getException().get(0).getExceptionCode()));
    assertThat(LOCATOR, is(exceptionReport.getException().get(0).getLocator()));
  }

  @Test
  public void testThrowableExceptionToServiceExceptionReport() {

    NullPointerException npe = new NullPointerException();

    ExceptionMapper<Throwable> exceptionMapper = new CswExceptionMapper();
    Response response = exceptionMapper.toResponse(npe);
    assertThat(response.getEntity(), is(instanceOf(ExceptionReport.class)));
    ExceptionReport exceptionReport = (ExceptionReport) response.getEntity();
    assertThat(Status.BAD_REQUEST.getStatusCode(), is(response.getStatus()));
    assertThat(
        XML_PARSE_FAIL_MSG, is(exceptionReport.getException().get(0).getExceptionText().get(0)));
    assertThat(
        CswConstants.MISSING_PARAMETER_VALUE,
        is(exceptionReport.getException().get(0).getExceptionCode()));
    assertThat(exceptionReport.getException().get(0).getLocator(), nullValue());

    IllegalArgumentException iae = new IllegalArgumentException();

    exceptionMapper = new CswExceptionMapper();
    response = exceptionMapper.toResponse(iae);
    assertThat(response.getEntity(), is(instanceOf(ExceptionReport.class)));
    exceptionReport = (ExceptionReport) response.getEntity();
    assertThat(Status.BAD_REQUEST.getStatusCode(), is(response.getStatus()));
    assertThat(
        XML_PARSE_FAIL_MSG, is(exceptionReport.getException().get(0).getExceptionText().get(0)));
    assertThat(
        CswConstants.MISSING_PARAMETER_VALUE,
        is(exceptionReport.getException().get(0).getExceptionCode()));
    assertThat(exceptionReport.getException().get(0).getLocator(), nullValue());
  }
}
