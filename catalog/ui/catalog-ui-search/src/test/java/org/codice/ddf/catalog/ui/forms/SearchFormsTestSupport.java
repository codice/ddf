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
package org.codice.ddf.catalog.ui.forms;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.AttributeType;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.catalog.ui.forms.filter.FilterWriter;

public class SearchFormsTestSupport {

  private static final String MSG_COULD_NOT_LOAD = "Could not load test resource";

  /**
   * Given a pretty JSON string, flatten out the string to enable string comparison operations
   * regardless of formatting.
   *
   * @param json the input JSON string to flatten to a single line.
   * @return a single line string of the given JSON.
   */
  static String removePrettyPrintingOnJson(String json) {
    return json.replaceAll("\\h|\\v", "");
  }

  /**
   * Given a pretty XML string, flatten out the string to enable string comparison operations
   * regardless of formatting.
   *
   * @param xml the input XML string to flatten to a single line.
   * @return a single line string of the given XML.
   */
  static String removePrettyPrintingOnXml(String xml) {
    try {
      TransformerFactory factory = TransformerFactory.newInstance();
      Source xslt = new StreamSource(getResourceFile("/forms/inline-xml.xslt"));
      Transformer transformer = factory.newTransformer(xslt);

      Source text = new StreamSource(new ByteArrayInputStream(xml.getBytes()));
      StringWriter destination = new StringWriter();

      transformer.transform(text, new StreamResult(destination));

      // Replace the header that the XSLT transform removed
      return destination
          .toString()
          .replace(
              "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
              "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
    } catch (TransformerException e) {
      throw new AssertionError("Could not remove pretty printing from XML, " + xml, e);
    }
  }

  static FilterWriter getWriter() {
    try {
      return new FilterWriter(true);
    } catch (JAXBException e) {
      throw new AssertionError("Could not make filter writer, " + e.getMessage());
    }
  }

  static AttributeRegistry getAttributeRegistry() {
    AttributeDescriptor dateDescriptor = mock(AttributeDescriptor.class);
    AttributeType attributeType = mock(AttributeType.class);
    when(dateDescriptor.getType()).thenReturn(attributeType);
    when(attributeType.getAttributeFormat()).thenReturn(AttributeType.AttributeFormat.DATE);

    AttributeRegistry registry = mock(AttributeRegistry.class);
    // doReturn(Optional.empty()).when(registry).lookup(any());
    doAnswer(
            invocationOnMock -> {
              String propertyName = invocationOnMock.getArgumentAt(0, String.class);
              if (!"created".equals(propertyName)) {
                return Optional.empty();
              }
              return Optional.of(dateDescriptor);
            })
        .when(registry)
        .lookup(any());

    return registry;
  }

  static File getResourceFile(String resourceRoute) {
    try {
      URL url = SearchFormsSymbolsIT.class.getResource(resourceRoute);
      if (url == null) {
        throw new AssertionError(MSG_COULD_NOT_LOAD + ", the URL came back null");
      }
      return new File(url.toURI());
    } catch (URISyntaxException e) {
      throw new AssertionError(MSG_COULD_NOT_LOAD + ", URI syntax not valid", e);
    }
  }

  static String getContentsOfFile(String resourceRoute) {
    File resourceFile = getResourceFile(resourceRoute);
    try (FileInputStream fis = new FileInputStream(resourceFile)) {
      return IOUtils.toString(fis, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new AssertionError("Could not complete test setup due to exception", e);
    }
  }

  /**
   * Return an available port number after binding and releasing it.
   *
   * <p>The discovered port should be available for another client to bind to by the time this
   * function has returned. Given this detail, running unit tests on environments that are in the
   * process of being provisioned or are otherwise in a state of flux may cause erroneous failures
   * due to port binding race conditions.
   *
   * @return a port number the caller can <b>reasonably</b> assume is available to bind to.
   * @throws AssertionError if no port was available to bind to or the binding operation failed.
   */
  static int getAvailablePort() {
    ServerSocket socket = null;
    try {
      socket = new ServerSocket(0);
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new AssertionError("Could not autobind to available port", e);
    } finally {
      tryCloseSocket(socket);
    }
  }

  private static void tryCloseSocket(@Nullable ServerSocket socket) {
    try {
      if (socket != null) {
        socket.close();
      }
    } catch (IOException e) {
      throw new AssertionError(
          "Problem while enumerating ports (specifically, port " + socket.getLocalPort() + ")", e);
    }
  }
}
