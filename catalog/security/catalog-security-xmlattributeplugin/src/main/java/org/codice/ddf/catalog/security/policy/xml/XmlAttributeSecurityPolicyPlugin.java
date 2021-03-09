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
package org.codice.ddf.catalog.security.policy.xml;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.plugin.impl.PolicyResponseImpl;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.codehaus.stax2.XMLInputFactory2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin that parses XML metadata for elements that contain attributes with security policy
 * information
 */
public class XmlAttributeSecurityPolicyPlugin implements PolicyPlugin {
  /** Logger */
  private static final Logger LOGGER =
      LoggerFactory.getLogger(XmlAttributeSecurityPolicyPlugin.class);

  /** Input factory */
  private static volatile XMLInputFactory xmlInputFactory = null;

  static {
    XMLInputFactory xmlInputFactoryTmp = XMLInputFactory2.newInstance();
    xmlInputFactoryTmp.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
    xmlInputFactoryTmp.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
    xmlInputFactoryTmp.setProperty(
        XMLInputFactory.SUPPORT_DTD, Boolean.FALSE); // This disables DTDs entirely for that factory
    xmlInputFactoryTmp.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
    xmlInputFactory = xmlInputFactoryTmp;
  }

  /** Default XML elements to be parsed. Overridden with the metatype. */
  private List<String> xmlElements = new ArrayList<>();

  /** Default match all attribute. Overridden with the metatype. */
  private List<String> securityAttributeUnions = new ArrayList<>();

  /** Default match all attribute. Overridden with the metatype. */
  private List<String> securityAttributeIntersections = new ArrayList<>();

  /**
   * Parse XML metadata using StAX to find the security element
   *
   * @param metacard XML metadata to parse
   */
  public Map<String, Set<String>> parseSecurityMetadata(Metacard metacard) {
    Map<String, Set<String>> securityMap = new HashMap<>();
    String xmlMetadata = metacard.getMetadata();
    if (xmlMetadata == null) {
      return securityMap;
    }

    XMLStreamReader xmlStreamReader = null;
    try (StringReader reader = new StringReader(xmlMetadata)) {
      xmlStreamReader = xmlInputFactory.createXMLStreamReader(reader);
      Map<String, Set<Set<String>>> intersectionMap = new HashMap<>();
      while (xmlStreamReader.hasNext()) {
        int event = xmlStreamReader.next();
        if (event == XMLStreamConstants.START_ELEMENT) {
          String localName = xmlStreamReader.getLocalName();

          if (xmlElements.contains(localName)) {
            LOGGER.debug("Parsing security attribute.");
            parseSecurityBlock(securityMap, intersectionMap, xmlStreamReader);
          }
        }
      }
    } catch (XMLStreamException e) {
      // if this happens and message redacting is enabled, the message will be excluded from results
      LOGGER.info("Unable to parse security from XML metadata.", e);
    } finally {
      if (xmlStreamReader != null) {
        try {
          xmlStreamReader.close();
        } catch (XMLStreamException e) {
          // ignore
        }
      }
    }

    return securityMap;
  }

  /**
   * Parses the security element in the metadata
   *
   * @param xmlStreamReader xml stream
   */
  private void parseSecurityBlock(
      Map<String, Set<String>> securityMap,
      Map<String, Set<Set<String>>> intersectionMap,
      XMLStreamReader xmlStreamReader) {
    LOGGER.debug("Parsing metacard security block");
    int numAttrs = xmlStreamReader.getAttributeCount();

    for (int i = 0; i < numAttrs; i++) {
      String name = xmlStreamReader.getAttributeLocalName(i);
      if (getSecurityAttributeUnions().contains(name)) {
        LOGGER.debug("Found {} in metacard", name);
        if (!securityMap.containsKey(name)) {
          securityMap.put(name, new HashSet<>());
        }
        buildSecurityAttribute(securityMap.get(name), xmlStreamReader.getAttributeValue(i));
      } else if (getSecurityAttributeIntersections().contains(name)) {
        if (!intersectionMap.containsKey(name)) {
          intersectionMap.put(name, new HashSet<>());
        }
        Set<String> valueSet = new HashSet<>();
        buildSecurityAttribute(valueSet, xmlStreamReader.getAttributeValue(i));
        intersectionMap.get(name).add(valueSet);
      }
    }
    try {
      xmlStreamReader.close();
    } catch (Exception e) {
      LOGGER.debug("Exception closing xmlStreamReader. {}", e);
    }
    buildIntersectionAttributes(securityMap, intersectionMap);
  }

  private void buildSecurityAttribute(Set<String> builderSet, String attributeValue) {
    StringTokenizer tokenizer = new StringTokenizer(attributeValue, " ", false);
    while (tokenizer.hasMoreElements()) {
      String value = tokenizer.nextToken();
      LOGGER.debug("Adding {} to set", value);
      builderSet.add(value);
    }
  }

  private void buildIntersectionAttributes(
      Map<String, Set<String>> securityMap, Map<String, Set<Set<String>>> intersectionMap) {
    if (!intersectionMap.isEmpty()) {
      for (Map.Entry<String, Set<Set<String>>> entry : intersectionMap.entrySet()) {
        Set<Set<String>> setsOfValues = entry.getValue();
        if (!setsOfValues.isEmpty()) {
          Iterator<Set<String>> iterator = setsOfValues.iterator();
          Set<String> endSet = iterator.next();
          while (iterator.hasNext()) {
            endSet.retainAll(iterator.next());
          }
          securityMap.put(entry.getKey(), endSet);
        }
      }
    }
  }

  public List<String> getXmlElements() {
    if (xmlElements == null) {
      return new ArrayList<>();
    } else {
      return xmlElements;
    }
  }

  public void setXmlElements(List<String> xmlElements) {
    this.xmlElements = xmlElements;
  }

  public List<String> getSecurityAttributeUnions() {
    return securityAttributeUnions;
  }

  public void setSecurityAttributeUnions(List<String> securityAttributeUnions) {
    this.securityAttributeUnions = securityAttributeUnions;
  }

  public List<String> getSecurityAttributeIntersections() {
    return securityAttributeIntersections;
  }

  public void setSecurityAttributeIntersections(List<String> securityAttributeIntersections) {
    this.securityAttributeIntersections = securityAttributeIntersections;
  }

  @Override
  public PolicyResponse processPreCreate(Metacard metacard, Map<String, Serializable> map)
      throws StopProcessingException {
    if (metacard != null) {
      return new PolicyResponseImpl(null, parseSecurityMetadata(metacard));
    }
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreUpdate(Metacard metacard, Map<String, Serializable> map)
      throws StopProcessingException {
    if (metacard != null) {
      return new PolicyResponseImpl(null, parseSecurityMetadata(metacard));
    }
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreDelete(List<Metacard> list, Map<String, Serializable> map)
      throws StopProcessingException {
    Map<String, Set<String>> response = new HashMap<>();
    for (Metacard metacard : list) {
      Map<String, Set<String>> parseSecurityMetadata = parseSecurityMetadata(metacard);
      for (Map.Entry<String, Set<String>> entry : parseSecurityMetadata.entrySet()) {
        if (response.containsKey(entry.getKey())) {
          response.get(entry.getKey()).addAll(entry.getValue());
        } else {
          response.put(entry.getKey(), entry.getValue());
        }
      }
    }
    return new PolicyResponseImpl(response, null);
  }

  @Override
  public PolicyResponse processPostDelete(Metacard metacard, Map<String, Serializable> map)
      throws StopProcessingException {
    return new PolicyResponseImpl(null, parseSecurityMetadata(metacard));
  }

  @Override
  public PolicyResponse processPreQuery(Query query, Map<String, Serializable> map)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPostQuery(Result result, Map<String, Serializable> map)
      throws StopProcessingException {
    return new PolicyResponseImpl(null, parseSecurityMetadata(result.getMetacard()));
  }

  @Override
  public PolicyResponse processPreResource(ResourceRequest resourceRequest)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPostResource(ResourceResponse resourceResponse, Metacard metacard)
      throws StopProcessingException {
    return new PolicyResponseImpl(null, parseSecurityMetadata(metacard));
  }
}
