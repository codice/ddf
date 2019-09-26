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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import net.opengis.ows.v_1_0_0.ExceptionReport;
import net.opengis.ows.v_1_0_0.ExceptionType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class CswResponseExceptionMapper implements ResponseExceptionMapper<CswException> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CswResponseExceptionMapper.class);

  @Override
  public CswException fromResponse(Response response) {
    CswException cswException = null;

    if (response != null) {
      if (response.getEntity() instanceof InputStream) {
        String msg = null;
        try {
          InputStream is = (InputStream) response.getEntity();
          if (is.markSupported()) {
            is.reset();
          }
          msg = IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
          LOGGER.info("Unable to parse exception report: {}", e.getMessage());
          LOGGER.debug("Unable to parse exception report: {}", e);
        }
        if (msg != null) {
          try {
            JAXBElementProvider<ExceptionReport> provider = new JAXBElementProvider<>();
            Unmarshaller um =
                provider
                    .getJAXBContext(ExceptionReport.class, ExceptionReport.class)
                    .createUnmarshaller();
            XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
            xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
            XMLStreamReader xmlStreamReader =
                xmlInputFactory.createXMLStreamReader(new StringReader(msg));
            ExceptionReport report = (ExceptionReport) um.unmarshal(xmlStreamReader);
            cswException = convertToCswException(report);
          } catch (JAXBException | XMLStreamException e) {
            cswException = new CswException("Error received from remote Csw server: " + msg, e);
            LOGGER.info("Error parsing the exception report: {}", e.getMessage());
            LOGGER.debug("Error parsing the exception report", e);
          }
        } else {
          cswException = new CswException("Error received from remote Csw server.");
        }
      } else {
        cswException =
            new CswException(
                "Error reading response, entity type not understood: "
                    + response.getEntity().getClass().getName());
      }
      cswException.setHttpStatus(response.getStatus());
    } else {
      cswException = new CswException("Error handling response, response is null");
    }

    return cswException;
  }

  private CswException convertToCswException(ExceptionReport report) {

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

      if (!CollectionUtils.isEmpty(exceptionText)) {
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
