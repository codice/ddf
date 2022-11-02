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

import com.thoughtworks.xstream.converters.Converter;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.ConnectException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import javax.net.ssl.SSLHandshakeException;
import javax.xml.bind.JAXBElement;
import net.opengis.cat.csw.v_2_0_2.CapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.GetCapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.GetRecordByIdType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.TransactionResponseType;
import net.opengis.ows.v_1_0_0.ExceptionReport;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswXmlParser;
import org.codice.ddf.spatial.ogc.csw.catalog.common.source.reader.GetRecordsMessageBodyReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CswClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(CswClient.class);

  private static final String CSW_SERVER_ERROR = "Error received from CSW server.";

  private HttpClient httpClient;

  private CswXmlParser parser;

  private ObjectFactory objectFactory;

  private Converter cswTransformConverter;

  private GetRecordsMessageBodyReader getRecordsMessageBodyReader;

  private CswSourceConfiguration cswSourceConfiguration;

  public CswClient(
      CswXmlParser parser,
      ObjectFactory objectFactory,
      Converter cswTransformConverter,
      CswSourceConfiguration cswSourceConfiguration) {
    this.parser = parser;
    this.objectFactory = objectFactory;
    this.cswTransformConverter = cswTransformConverter;
    this.cswSourceConfiguration = cswSourceConfiguration;
    init();
  }

  public void init() {
    LOGGER.debug("{}: Entering init()", cswSourceConfiguration.getId());
    httpClient = initHttpClient();
    getRecordsMessageBodyReader =
        new GetRecordsMessageBodyReader(parser, cswTransformConverter, cswSourceConfiguration);
  }

  protected HttpClient initHttpClient() {
    HttpClient.Builder clientBuilder = HttpClient.newBuilder();

    clientBuilder.connectTimeout(
        Duration.of(cswSourceConfiguration.getConnectionTimeout(), ChronoUnit.MILLIS));

    if (StringUtils.isNotBlank(cswSourceConfiguration.getUsername())
        && StringUtils.isNotBlank(cswSourceConfiguration.getPassword())) {
      clientBuilder.authenticator(
          new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
              return new PasswordAuthentication(
                  cswSourceConfiguration.getUsername(),
                  cswSourceConfiguration.getPassword().toCharArray());
            }
          });
    }

    return clientBuilder.build();
  }

  public CapabilitiesType getCapabilities(GetCapabilitiesType request)
      throws UnsupportedQueryException {
    return post(CapabilitiesType.class, objectFactory.createGetCapabilities(request));
  }

  public CswRecordCollection getRecords(GetRecordsType request) throws UnsupportedQueryException {
    return post(CswRecordCollection.class, objectFactory.createGetRecords(request));
  }

  public CswRecordCollection getRecordById(GetRecordByIdType request)
      throws UnsupportedQueryException {
    return post(CswRecordCollection.class, objectFactory.createGetRecordById(request));
  }

  public TransactionResponseType transaction(String request) throws UnsupportedQueryException {
    return post(TransactionResponseType.class, request);
  }

  private <T> T post(Class<T> clazz, Object request) throws UnsupportedQueryException {
    HttpResponse<String> clientResponse = post(request);

    if (clientResponse == null || clientResponse.statusCode() != 200) {
      throw new UnsupportedQueryException("Unable to make CSW request.");
    }

    if (clazz.equals(CswRecordCollection.class)) {
      return clazz.cast(convert(clientResponse));
    }

    JAXBElement<?> jaxbElement = unmarshal(clientResponse.body());
    if (clazz.equals(jaxbElement.getDeclaredType())) {
      return clazz.cast(jaxbElement.getValue());
    } else {
      throw new UnsupportedQueryException("Unable to unmarshal CSW response.");
    }
  }

  private HttpResponse<String> post(Object request) throws UnsupportedQueryException {
    try {
      String requestXml;
      if (String.class.equals(request.getClass())) {
        requestXml = request.toString();
      } else {
        requestXml = parser.marshal(request);
      }
      return httpClient.send(
          HttpRequest.newBuilder()
              .version(HttpClient.Version.HTTP_1_1)
              .POST(HttpRequest.BodyPublishers.ofString(requestXml))
              .header("Content-Type", "application/xml")
              .uri(new URI(cswSourceConfiguration.getCswUrl()))
              .timeout(Duration.of(cswSourceConfiguration.getReceiveTimeout(), ChronoUnit.MILLIS))
              .build(),
          HttpResponse.BodyHandlers.ofString());
    } catch (Exception ce) {
      String msg = handleClientException(ce);
      throw new UnsupportedQueryException(msg, ce);
    }
  }

  private JAXBElement<?> unmarshal(String response) throws UnsupportedQueryException {
    JAXBElement<?> jaxbElement;

    try (ByteArrayInputStream input = new ByteArrayInputStream(response.getBytes())) {
      jaxbElement = parser.unmarshal(JAXBElement.class, input);
    } catch (Exception ce) {
      String msg = handleClientException(ce);
      throw new UnsupportedQueryException(msg, ce);
    }

    if (ExceptionReport.class.equals(jaxbElement.getDeclaredType())) {
      CswException cswe =
          CswResponseExceptionMapper.convertToCswException(
              (ExceptionReport) jaxbElement.getValue());
      LOGGER.info(CSW_SERVER_ERROR, cswe);
      throw new UnsupportedQueryException(CSW_SERVER_ERROR, cswe);
    } else {
      return jaxbElement;
    }
  }

  private CswRecordCollection convert(HttpResponse<String> response)
      throws UnsupportedQueryException {
    CswRecordCollection cswRecordCollection;
    try (ByteArrayInputStream input = new ByteArrayInputStream(response.body().getBytes())) {
      cswRecordCollection = getRecordsMessageBodyReader.readFrom(response.headers().map(), input);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (cswRecordCollection == null) {
      throw new UnsupportedQueryException("Invalid results returned from server");
    }

    return cswRecordCollection;
  }

  @SuppressWarnings("squid:S1192")
  private String handleClientException(Exception ce) {
    String msg;
    Throwable cause = ce.getCause();
    String sourceId = cswSourceConfiguration.getId();
    if (cause instanceof IllegalArgumentException) {
      msg =
          CSW_SERVER_ERROR
              + " Source '"
              + sourceId
              + "'. The URI '"
              + cswSourceConfiguration.getCswUrl()
              + "' does not specify a valid protocol or could not be correctly parsed. "
              + ce.getMessage();
    } else if (cause instanceof SSLHandshakeException) {
      msg =
          CSW_SERVER_ERROR
              + " Source '"
              + sourceId
              + "' with URL '"
              + cswSourceConfiguration.getCswUrl()
              + "': "
              + cause;
    } else if (cause instanceof ConnectException) {
      msg = CSW_SERVER_ERROR + " Source '" + sourceId + "' may not be running.\n" + ce.getMessage();
    } else {
      msg = CSW_SERVER_ERROR + " Source '" + sourceId + "'\n" + ce;
    }

    LOGGER.info(msg);
    LOGGER.debug(msg, ce);
    return msg;
  }
}
