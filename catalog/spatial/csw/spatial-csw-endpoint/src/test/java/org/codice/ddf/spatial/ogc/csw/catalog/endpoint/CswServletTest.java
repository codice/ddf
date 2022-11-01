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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thoughtworks.xstream.converters.Converter;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.MetacardType;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.opengis.cat.csw.v_2_0_2.GetCapabilitiesType;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswXmlParser;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.reader.TransactionMessageBodyReader;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.writer.CswRecordCollectionMessageBodyWriter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class CswServletTest {

  private CswEndpoint cswEndpoint = mock(CswEndpoint.class);

  private CswXmlParser xmlParser = new CswXmlParser(new XmlParser());

  private TransformerManager transformerManager = mock(TransformerManager.class);

  private Converter cswRecordConverter = mock(Converter.class);

  private MetacardType metacardType = mock(MetacardType.class);

  private AttributeRegistry registry = mock(AttributeRegistry.class);

  private final TransactionMessageBodyReader transactionReader =
      new TransactionMessageBodyReader(cswRecordConverter, metacardType, registry);

  private final CswRecordCollectionMessageBodyWriter recordCollectionWriter =
      new CswRecordCollectionMessageBodyWriter(transformerManager);

  private final CswExceptionMapper exceptionMapper = new CswExceptionMapper(xmlParser);

  private HttpServletRequest request = mock(HttpServletRequest.class);

  private ServletInputStream inputStream = mock(ServletInputStream.class);

  private HttpServletResponse response = mock(HttpServletResponse.class);

  private ServletOutputStream outputStream = mock(ServletOutputStream.class);

  private CswServlet cswServlet;

  @Before
  public void setUp() throws Exception {
    when(request.getParameter(eq("service"))).thenReturn("CSW");
    when(request.getParameter(eq("version"))).thenReturn("2.0.2");
    when(request.getParameter(eq("request"))).thenReturn("getcapabilities");
    when(response.getOutputStream()).thenReturn(outputStream);
    when(request.getInputStream()).thenReturn(inputStream);
    cswServlet =
        new CswServlet(
            cswEndpoint, xmlParser, transactionReader, recordCollectionWriter, exceptionMapper);
  }

  @Test
  public void invalidServiceGetRequest() throws Exception {
    when(request.getParameter(eq("service"))).thenReturn("foo");

    cswServlet.doGet(request, response);

    verify(response, times(1)).setStatus(SC_BAD_REQUEST);
  }

  @Test
  public void invalidRequestListGetRequest() throws Exception {
    when(request.getParameter(eq("request"))).thenReturn("invalid");

    cswServlet.doGet(request, response);

    verify(response, times(1)).setStatus(SC_BAD_REQUEST);
  }

  @Test
  public void invalidVersionListGetRequest() throws Exception {
    when(request.getParameter(eq("version"))).thenReturn("1.0.1");

    cswServlet.doGet(request, response);

    verify(response, times(1)).setStatus(SC_BAD_REQUEST);
  }

  @Test
  public void validVersionInListGetRequest() throws Exception {
    when(request.getParameter(eq("version"))).thenReturn("1.0.1,2.0.2");

    cswServlet.doGet(request, response);

    ArgumentCaptor<GetCapabilitiesRequest> endpointRequestCaptor =
        ArgumentCaptor.forClass(GetCapabilitiesRequest.class);
    verify(cswEndpoint).getCapabilities(endpointRequestCaptor.capture());
    GetCapabilitiesRequest endpointRequest = endpointRequestCaptor.getValue();

    assertThat(endpointRequest.getAcceptVersions(), is("1.0.1,2.0.2"));
  }

  @Test
  public void validGetCapabilitiesPostRequest() throws Exception {
    String getCapabilitiesRequestXml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<GetCapabilities\n"
            + "    xmlns=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
            + "    xmlns:ows=\"http://www.opengis.net/ows\"\n"
            + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "    service=\"CSW\">\n"
            + "  <ows:AcceptVersions>\n"
            + "    <ows:Version>2.0.2</ows:Version>\n"
            + "  </ows:AcceptVersions>\n"
            + "  <ows:AcceptFormats>\n"
            + "    <ows:OutputFormat>application/xml</ows:OutputFormat>\n"
            + "  </ows:AcceptFormats>\n"
            + "</GetCapabilities>";

    when(inputStream.transferTo(any()))
        .thenAnswer(
            a -> {
              a.getArgument(0, OutputStream.class)
                  .write(getCapabilitiesRequestXml.getBytes(StandardCharsets.UTF_8));
              return 0L;
            });

    cswServlet.doPost(request, response);

    ArgumentCaptor<GetCapabilitiesType> endpointRequestCaptor =
        ArgumentCaptor.forClass(GetCapabilitiesType.class);
    verify(cswEndpoint).getCapabilities(endpointRequestCaptor.capture());
    GetCapabilitiesType endpointRequest = endpointRequestCaptor.getValue();

    assertThat(endpointRequest.getAcceptVersions().getVersion().get(0), is("2.0.2"));
  }
}
