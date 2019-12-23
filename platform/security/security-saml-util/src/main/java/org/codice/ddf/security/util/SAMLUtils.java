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
package org.codice.ddf.security.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.wss4j.common.util.DOM2Writer;
import org.codice.ddf.platform.util.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class SAMLUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(SAMLUtils.class);

  private static final SAMLUtils INSTANCE = new SAMLUtils();

  private static final Pattern SAML_PREFIX = Pattern.compile("<(?<prefix>\\w+?):Assertion\\s.*");

  private static final XMLUtils XML_UTILS = XMLUtils.getInstance();

  private static final String EVIDENCE =
      "<%1$s:Evidence xmlns:%1$s=\"urn:oasis:names:tc:SAML:2.0:assertion\">%2$s</%1$s:Evidence>";

  private SAMLUtils() {}

  public static SAMLUtils getInstance() {
    return INSTANCE;
  }

  public Element getSecurityTokenFromSAMLAssertion(String samlAssertion) {
    Element thisToken;

    try (StringReader stringReader = new StringReader(samlAssertion)) {
      thisToken = StaxUtils.read(stringReader).getDocumentElement();
    } catch (XMLStreamException e) {
      LOGGER.info(
          "Unexpected error converting XML string to element - proceeding without SAML token.", e);
      thisToken = parseAssertionWithoutNamespace(samlAssertion);
    }

    return thisToken;
  }

  public String getSubjectAsStringNoSignature(Element subject) {
    if (subject == null) {
      return null;
    }
    subject.normalize();
    Node signatureElement = subject.getElementsByTagNameNS("*", "Signature").item(0);
    if (signatureElement != null) {
      subject.removeChild(signatureElement);
    }
    return DOM2Writer.nodeToString(subject);
  }

  public Element parseAssertionWithoutNamespace(String assertion) {
    Element result = null;

    Matcher prefix = SAML_PREFIX.matcher(assertion);
    if (prefix.find()) {

      Thread thread = Thread.currentThread();
      ClassLoader loader = thread.getContextClassLoader();
      thread.setContextClassLoader(SAMLUtils.class.getClassLoader());

      String evidence = String.format(EVIDENCE, prefix.group("prefix"), assertion);
      try (InputStream is = new ByteArrayInputStream(evidence.getBytes(StandardCharsets.UTF_8))) {
        DocumentBuilderFactory dbf = XML_UTILS.getSecureDocumentBuilderFactory();
        dbf.setNamespaceAware(true);

        Element root = dbf.newDocumentBuilder().parse(is).getDocumentElement();

        result = ((Element) root.getChildNodes().item(0));
      } catch (ParserConfigurationException | SAXException | IOException ex) {
        LOGGER.info("Unable to parse SAML assertion", ex);
      } finally {
        thread.setContextClassLoader(loader);
      }
    }

    return result;
  }
}
