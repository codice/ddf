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
package org.codice.ddf.catalog.transformer.html;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Options;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.codice.ddf.platform.util.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class RecordViewHelpers {

  private static final Logger LOGGER = LoggerFactory.getLogger(RecordViewHelpers.class);

  private static final XMLUtils XML_UTILS = XMLUtils.getInstance();

  private static DocumentBuilderFactory documentBuilderFactory;

  static {
    documentBuilderFactory = XML_UTILS.getSecureDocumentBuilderFactory();
  }

  public CharSequence buildMetadata(String metadata, Options options) {
    if (metadata == null) {
      return "";
    }
    try {
      DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
      StreamResult result = new StreamResult(new StringWriter());
      Transformer transformer = XML_UTILS.getXmlTransformer(true);
      transformer.transform(
          new DOMSource(builder.parse(new InputSource(new StringReader(metadata)))), result);
      StringBuilder sb = new StringBuilder();
      sb.append("<pre>").append(escapeHtml(result.getWriter().toString())).append("</pre>");
      return new Handlebars.SafeString(sb.toString());
    } catch (TransformerException | SAXException | ParserConfigurationException | IOException e) {
      LOGGER.debug("Failed to convert metadata to a pretty string", e);
    }
    return metadata;
  }

  public CharSequence formatGeometry(String geometry, Options options) {
    return geometry;
  }

  public CharSequence formatDate(Date date, Options options) {
    if (date != null) {
      SimpleDateFormat iso8601DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
      iso8601DateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
      return iso8601DateFormat.format(date);
    }
    return "";
  }

  public CharSequence hasServicesUrl(Options options) throws IOException {
    return options.fn(this);
  }

  public CharSequence servicesUrl(Options options) {
    return "";
  }
}
