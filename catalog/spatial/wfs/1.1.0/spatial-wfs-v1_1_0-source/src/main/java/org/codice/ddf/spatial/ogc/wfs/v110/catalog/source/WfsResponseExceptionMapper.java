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
package org.codice.ddf.spatial.ogc.wfs.v110.catalog.source;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import net.opengis.ows.v_1_0_0.ExceptionReport;
import net.opengis.ows.v_1_0_0.ExceptionType;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsException;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsInvalidEntityException;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsNullResponseException;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsParseException;

public class WfsResponseExceptionMapper implements ResponseExceptionMapper<WfsException> {

  public WfsException fromResponse(Response response) {

    WfsException wfsEx = null;

    if (response != null) {
      wfsEx = fromResponseSafe(response);
    } else {
      wfsEx = new WfsNullResponseException();
    }
    return wfsEx;
  }

  private WfsException fromResponseSafe(Response response) {
    WfsException wfsEx = null;

    if (response.getEntity() instanceof InputStream) {
      String msg = null;
      try (InputStream is = (InputStream) response.getEntity()) {
        msg = IOUtils.toString(is, StandardCharsets.UTF_8);
      } catch (IOException e) {
        wfsEx = new WfsParseException(e);
      }
      if (msg != null) {
        wfsEx = unmarshalResponseEntity(msg);
      }
    } else {
      wfsEx = new WfsInvalidEntityException(response.getEntity().getClass());
    }
    if (wfsEx != null) {
      wfsEx.setHttpStatus(response.getStatus());
    }
    return wfsEx;
  }

  private WfsException unmarshalResponseEntity(String msg) {
    WfsException wfsEx;
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
      wfsEx = convertToWfsException(report);
    } catch (JAXBException | XMLStreamException e) {
      wfsEx = new WfsParseException(msg, e);
    }
    return wfsEx;
  }

  private WfsException convertToWfsException(ExceptionReport report) {

    WfsException wfsException = null;
    List<ExceptionType> list = new ArrayList<>(report.getException());

    if (!list.isEmpty()) {
      Collections.reverse(list);
      StringBuilder exceptionMsg = new StringBuilder();
      for (ExceptionType serviceException : list) {
        exceptionMsg.append(serviceException.getExceptionText());
      }
      wfsException = new WfsException(exceptionMsg.toString());
    }

    if (null == wfsException) {
      wfsException =
          new WfsException(
              "Empty Service Exception Report (version = " + report.getVersion() + ")");
    }
    return wfsException;
  }
}
