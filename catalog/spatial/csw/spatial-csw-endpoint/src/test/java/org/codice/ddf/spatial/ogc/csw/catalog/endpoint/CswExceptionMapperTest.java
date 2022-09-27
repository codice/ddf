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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import net.opengis.ows.v_1_0_0.ExceptionReport;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.junit.Before;
import org.junit.Test;

public class CswExceptionMapperTest {

  private static final String SERVICE_EXCEPTION_MSG = "Mock Exception";

  private static final String EXCEPTION_CODE = "OperationNotSupported";

  private static final String LOCATOR = "describeRecords";

  private static final String XML_PARSE_FAIL_MSG =
      "Error parsing the request.  XML parameters may be missing or invalid.";

  private CswXmlParser xmlParser = new CswXmlParser(new XmlParser());

  private HttpServletResponse response = mock(HttpServletResponse.class);

  private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

  @Before
  public void before() throws Exception {
    when(response.getOutputStream())
        .thenReturn(
            new ServletOutputStream() {
              @Override
              public boolean isReady() {
                return true;
              }

              @Override
              public void setWriteListener(WriteListener writeListener) {
                return;
              }

              @Override
              public void write(int b) throws IOException {
                outputStream.write(b);
              }
            });
  }

  @Test
  public void testCswExceptionToServiceExceptionReport() throws IOException, ParserException {

    CswException exception = new CswException(SERVICE_EXCEPTION_MSG, SC_BAD_REQUEST);

    CswExceptionMapper exceptionMapper = new CswExceptionMapper(xmlParser);
    exceptionMapper.sendExceptionReport(exception, response);

    ExceptionReport exceptionReport = extractExceptionReport();
    assertThat(
        SERVICE_EXCEPTION_MSG, is(exceptionReport.getException().get(0).getExceptionText().get(0)));
    assertThat(exceptionReport.getException().get(0).getExceptionCode(), nullValue());
    assertThat(exceptionReport.getException().get(0).getLocator(), nullValue());
  }

  @Test
  public void testCswExceptionToServiceExceptionReportWithLocatorAndCode()
      throws IOException, ParserException {

    CswException exception =
        new CswException(SERVICE_EXCEPTION_MSG, SC_BAD_REQUEST, EXCEPTION_CODE, LOCATOR);

    CswExceptionMapper exceptionMapper = new CswExceptionMapper(xmlParser);
    exceptionMapper.sendExceptionReport(exception, response);
    ExceptionReport exceptionReport = extractExceptionReport();
    assertThat(
        SERVICE_EXCEPTION_MSG, is(exceptionReport.getException().get(0).getExceptionText().get(0)));
    assertThat(EXCEPTION_CODE, is(exceptionReport.getException().get(0).getExceptionCode()));
    assertThat(LOCATOR, is(exceptionReport.getException().get(0).getLocator()));
  }

  @Test
  public void testThrowableExceptionToServiceExceptionReport() throws ParserException, IOException {

    NullPointerException npe = new NullPointerException();

    CswExceptionMapper exceptionMapper = new CswExceptionMapper(xmlParser);
    exceptionMapper.sendExceptionReport(npe, response);
    ExceptionReport exceptionReport = extractExceptionReport();
    assertThat(
        XML_PARSE_FAIL_MSG, is(exceptionReport.getException().get(0).getExceptionText().get(0)));
    assertThat(
        CswConstants.MISSING_PARAMETER_VALUE,
        is(exceptionReport.getException().get(0).getExceptionCode()));
    assertThat(exceptionReport.getException().get(0).getLocator(), nullValue());
  }

  private ExceptionReport extractExceptionReport() throws ParserException {
    return xmlParser.unmarshal(
        ExceptionReport.class, new ByteArrayInputStream(outputStream.toByteArray()));
  }
}
