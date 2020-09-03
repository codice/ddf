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
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import com.thoughtworks.xstream.io.xml.XppReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/** XStream tool to copy the contents of a HierarchicalStreamReader into another container. */
public class XStreamAttributeCopier {
  private static final Logger LOGGER = LoggerFactory.getLogger(XStreamAttributeCopier.class);

  private static final HierarchicalStreamCopier COPIER = new HierarchicalStreamCopier();

  private XStreamAttributeCopier() {}

  private static void copyElementWithAttributes(
      HierarchicalStreamReader source,
      HierarchicalStreamWriter destination,
      Map<String, String> namespaceMap) {
    destination.startNode(source.getNodeName());
    int attributeCount = source.getAttributeCount();
    for (int i = 0; i < attributeCount; i++) {
      destination.addAttribute(source.getAttributeName(i), source.getAttribute(i));
    }
    if (namespaceMap != null && !namespaceMap.isEmpty()) {
      for (Entry<String, String> entry : namespaceMap.entrySet()) {
        if (StringUtils.isBlank(source.getAttribute(entry.getKey()))) {
          destination.addAttribute(entry.getKey(), entry.getValue());
        }
      }
    }
    String value = source.getValue();
    if (value != null && value.length() > 0) {
      destination.setValue(value);
    }
    while (source.hasMoreChildren()) {
      source.moveDown();
      COPIER.copy(source, destination);
      source.moveUp();
    }
    destination.endNode();
  }

  /**
   * Copies the entire XML element {@code reader} is currently at into {@code writer} and returns a
   * new reader ready to read the copied element. After the call, {@code reader} will be at the end
   * of the element that was copied.
   *
   * <p>If {@code attributeMap} is provided, the attributes will be added to the copy.
   *
   * @param reader the reader currently at the XML element you want to copy
   * @param writer the writer that the element will be copied into
   * @param attributeMap the map of attribute names to values that will be added as attributes of
   *     the copy, may be null
   * @return a new reader ready to read the copied element
   * @throws ConversionException if a parser to use for the new reader can't be created
   */
  public static HierarchicalStreamReader copyXml(
      HierarchicalStreamReader reader, StringWriter writer, Map<String, String> attributeMap) {
    copyElementWithAttributes(reader, new CompactWriter(writer, new NoNameCoder()), attributeMap);

    XmlPullParser parser;
    try {
      parser = XmlPullParserFactory.newInstance().newPullParser();
    } catch (XmlPullParserException e) {
      throw new ConversionException("Unable to create XmlPullParser, cannot parse XML.", e);
    }

    try {
      // NOTE: must specify encoding here, otherwise the platform default
      // encoding will be used which will not always work
      return new XppReader(
          new InputStreamReader(
              IOUtils.toInputStream(writer.toString(), StandardCharsets.UTF_8.name())),
          parser);
    } catch (IOException e) {
      LOGGER.debug("Unable create reader with UTF-8 encoding, Exception {}", e);
      return new XppReader(new InputStreamReader(IOUtils.toInputStream(writer.toString())), parser);
    }
  }

  /**
   * Copies the namespace declarations on the XML element {@code reader} is currently at into {@code
   * context}. The namespace declarations will be available in {@code context} at the key {@link
   * CswConstants#NAMESPACE_DECLARATIONS}. The new namespace declarations will be added to any
   * existing ones already in {@code context}.
   *
   * @param reader the reader currently at the XML element with namespace declarations you want to
   *     copy
   * @param context the {@link UnmarshallingContext} that the namespace declarations will be copied
   *     to
   */
  public static void copyXmlNamespaceDeclarationsIntoContext(
      HierarchicalStreamReader reader, UnmarshallingContext context) {
    @SuppressWarnings("unchecked")
    Map<String, String> namespaces =
        (Map<String, String>) context.get(CswConstants.NAMESPACE_DECLARATIONS);

    if (namespaces == null) {
      namespaces = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    Iterator<String> attributeNames = reader.getAttributeNames();
    while (attributeNames.hasNext()) {
      String name = attributeNames.next();
      if (StringUtils.startsWith(name, CswConstants.XMLNS)) {
        String attributeValue = reader.getAttribute(name);
        namespaces.put(name, attributeValue);
      }
    }
    if (!namespaces.isEmpty()) {
      context.put(CswConstants.NAMESPACE_DECLARATIONS, namespaces);
    }
  }
}
