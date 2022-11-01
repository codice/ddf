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
package org.codice.ddf.spatial.ogc.csw.catalog.common.source;

import java.util.List;
import net.opengis.ows.v_1_0_0.ExceptionReport;
import net.opengis.ows.v_1_0_0.ExceptionType;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;

/**
 * Maps the exception report returned as a response from a CSW service into a {@link CswException}.
 *
 * <p>The format of the XML error response is specified by, and shall validate against, the
 * exception response schema defined in clause 8 of the OWS Common Implementation Specification. One
 * or more exceptions can be contained in the exception report. See section 10.3.7 of the CSW
 * specification for details.
 *
 * <p>An example of an exception report returned by a CSW service would be:
 *
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
 * <ows:ExceptionReport version="1.2.0" xmlns:ns16="http://www.opengis.net/ows/1.1" xmlns:dc="http://purl.org/dc/elements/1.1/"
 *   xmlns:cat="http://www.opengis.net/cat/csw" xmlns:gco="http://www.isotc211.org/2005/gco" xmlns:gmd="http://www.isotc211.org/2005/gmd"
 *   xmlns:fra="http://www.cnig.gouv.fr/2005/fra" xmlns:ins="http://www.inspire.org" xmlns:gmx="http://www.isotc211.org/2005/gmx"
 *   xmlns:ogc="http://www.opengis.net/ogc" xmlns:dct="http://purl.org/dc/terms/" xmlns:ows="http://www.opengis.net/ows"
 *   xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:gml="http://www.opengis.net/gml" xmlns:csw="http://www.opengis.net/cat/csw/2.0.2"
 *   xmlns:gmi="http://www.isotc211.org/2005/gmi" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
 *   <ows:Exception exceptionCode="OPERATION_NOT_SUPPORTED">
 *       <ows:ExceptionText>The XML request is not valid.
 * Cause:unexpected element (uri:"", local:"GetRecords"). Expected elements are ...
 *     </ows:Exception>
 * </ows:ExceptionReport>
 * }</pre>
 *
 * @author rodgersh
 */
public class CswResponseExceptionMapper {

  public static CswException convertToCswException(ExceptionReport report) {
    if (report == null) {
      return new CswException("Error handling response, response is null");
    }

    CswException cswException = null;
    List<ExceptionType> list = report.getException();

    for (ExceptionType exceptionType : list) {
      String exceptionCode = exceptionType.getExceptionCode();
      String locator = exceptionType.getLocator();
      List<String> exceptionText = exceptionType.getExceptionText();
      StringBuilder exceptionMsg = new StringBuilder();

      // Exception code is required per CSW schema, but check it anyway
      if (StringUtils.isNotBlank(exceptionCode)) {
        exceptionMsg.append("exceptionCode = " + exceptionCode + "\n");
      } else {
        exceptionMsg.append("exceptionCode = UNSPECIFIED");
      }

      // Locator and exception text(s) are both optional
      if (StringUtils.isNotBlank(locator)) {
        exceptionMsg.append("locator = " + locator + "\n");
      }

      if (exceptionText != null && !exceptionText.isEmpty()) {
        for (String text : exceptionText) {
          exceptionMsg.append(text);
        }
      }
      cswException = new CswException(exceptionMsg.toString());
    }
    if (null == cswException) {
      cswException =
          new CswException("Empty Exception Report (version = " + report.getVersion() + ")");
    }
    return cswException;
  }
}
