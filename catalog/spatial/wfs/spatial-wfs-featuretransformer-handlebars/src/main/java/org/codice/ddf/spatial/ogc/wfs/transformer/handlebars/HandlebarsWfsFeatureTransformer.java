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
package org.codice.ddf.spatial.ogc.wfs.transformer.handlebars;

import static org.codice.ddf.libs.geo.util.GeospatialUtil.LAT_LON_ORDER;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.B;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.BYTES_PER_GB;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.BYTES_PER_KB;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.BYTES_PER_MB;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.BYTES_PER_PB;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.BYTES_PER_TB;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.GB;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.KB;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.MB;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.PB;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.TB;
import static org.codice.gsonsupport.GsonTypeAdapters.MAP_STRING_TO_OBJECT_TYPE;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.FeatureTypeType;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.platform.util.XMLUtils;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants;
import org.codice.ddf.spatial.ogc.wfs.catalog.metacardtype.registry.WfsMetacardTypeRegistry;
import org.codice.ddf.spatial.ogc.wfs.featuretransformer.FeatureTransformer;
import org.codice.ddf.spatial.ogc.wfs.featuretransformer.WfsMetadata;
import org.codice.ddf.transformer.xml.streaming.Gml3ToWkt;
import org.codice.ddf.transformer.xml.streaming.impl.Gml3ToWktImpl;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
import org.geotools.xml.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

public class HandlebarsWfsFeatureTransformer implements FeatureTransformer<FeatureTypeType> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(HandlebarsWfsFeatureTransformer.class);

  private static final XMLInputFactory XML_INPUT_FACTORY =
      XMLUtils.getInstance().getSecureXmlInputFactory();

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .create();

  private static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newInstance();

  private static final XMLEventFactory XML_EVENT_FACTORY = XMLEventFactory.newInstance();

  private static final String ATTRIBUTE_NAME = "attributeName";

  private static final String FEATURE_NAME = "featureName";

  private static final String TEMPLATE = "template";

  private static final String METACARD_ID = "metacardId";

  private static final String GML_NAMESPACE = "http://www.opengis.net/gml";

  private String featureType;

  private QName featureTypeQName;

  private String dataUnit;

  private MetacardType metacardType;

  private Map<String, FeatureAttributeEntry> mappingEntries = new HashMap<>();

  private WfsMetacardTypeRegistry metacardTypeRegistry;

  private String featureCoordinateOrder;

  @Override
  public Optional<Metacard> apply(InputStream inputStream, WfsMetadata metadata) {
    if (!isStateValid(inputStream, metadata)) {
      LOGGER.debug("Transformer state is invalid: {}, {}", featureType, mappingEntries);
      return Optional.empty();
    }

    lookupMetacardType(metadata);
    if (metacardType == null) {
      return Optional.empty();
    }

    Map<String, String> contextMap = new HashMap<>();
    populateContextMap(inputStream, contextMap);
    if (CollectionUtils.isEmpty(contextMap)) {
      return Optional.empty();
    }

    return Optional.of(createMetacard(contextMap, metadata.getId()));
  }

  /**
   * Reads in the FeatureMember from the input stream, populating the contextMap with the XML tag
   * names and values
   *
   * @param inputStream the stream containing the FeatureMember xml document
   */
  private void populateContextMap(InputStream inputStream, Map<String, String> contextMap) {
    Map<String, String> namespaces = new HashMap<>();
    boolean canHandleFeatureType = false;
    try {
      XMLEventReader xmlEventReader = getXmlEventReader(inputStream);

      String elementName = null;
      while (xmlEventReader.hasNext()) {
        XMLEvent xmlEvent = xmlEventReader.nextEvent();
        if (xmlEvent.isStartElement()) {
          StartElement startElement = xmlEvent.asStartElement();
          elementName = startElement.getName().getLocalPart();
          canHandleFeatureType |=
              processStartElement(
                  xmlEventReader, startElement, namespaces, contextMap, canHandleFeatureType);
          addXmlAttributesToContextMap(startElement, contextMap);
        } else if (xmlEvent.isCharacters()) {
          contextMap.put(elementName, xmlEvent.asCharacters().getData());
        }
      }
      if (!canHandleFeatureType) {
        contextMap.clear();
      }
    } catch (XMLStreamException e) {
      LOGGER.debug("Error transforming feature to metacard.", e);
    }
  }

  private boolean processStartElement(
      XMLEventReader xmlEventReader,
      StartElement startElement,
      Map<String, String> namespaces,
      Map<String, String> contextMap,
      boolean featureTypeFound)
      throws XMLStreamException {
    mapNamespaces(startElement, namespaces);
    if (!featureTypeFound) {
      if (canHandleFeatureType(startElement)) {
        String id =
            getIdAttributeValue(
                startElement, namespaces, getNamespaceAlias(GML_NAMESPACE, namespaces));
        contextMap.put(METACARD_ID, id);
        return true;
      }
    } else {
      XMLEvent eventPeek = xmlEventReader.peek();
      if (eventPeek.isStartElement() && isGmlElement(eventPeek.asStartElement(), namespaces)) {
        readGmlData(xmlEventReader, startElement.getName().getLocalPart(), contextMap);
      }
    }
    return false;
  }

  private boolean isGmlElement(StartElement startElement, Map<String, String> namespaces) {
    String gmlNamespaceAlias = getNamespaceAlias(GML_NAMESPACE, namespaces);
    if (StringUtils.isBlank(gmlNamespaceAlias)) {
      return false;
    }
    return startElement.getName().getPrefix().equals(gmlNamespaceAlias);
  }

  private boolean canHandleFeatureType(StartElement startElement) {
    return startElement.getName().getLocalPart().equals(featureTypeQName.getLocalPart());
  }

  private String getNamespaceAlias(String namespace, Map<String, String> namespaces) {
    return namespaces
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue().contains(namespace))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse("");
  }

  private String getIdAttributeValue(
      StartElement startElement, Map<String, String> namespaces, String namespaceAlias) {
    String id = null;
    javax.xml.stream.events.Attribute idAttribute =
        startElement.getAttributeByName(new QName(namespaces.get(namespaceAlias), "id"));
    if (idAttribute != null) {
      id = idAttribute.getValue();
    }

    if (StringUtils.isBlank(id)) {
      for (Iterator i = startElement.getAttributes(); i.hasNext(); ) {
        idAttribute = (javax.xml.stream.events.Attribute) i.next();
        if (idAttribute != null && idAttribute.getName().getLocalPart().equals("id")) {
          id = idAttribute.getValue();
        }
      }
    }

    return id;
  }

  private void addXmlAttributesToContextMap(
      final StartElement startElement, final Map<String, String> contextMap) {
    for (final Iterator attributeIterator = startElement.getAttributes();
        attributeIterator.hasNext(); ) {
      final javax.xml.stream.events.Attribute attribute =
          (javax.xml.stream.events.Attribute) attributeIterator.next();
      final String attributeKey =
          startElement.getName().getLocalPart() + "@" + attribute.getName().getLocalPart();
      contextMap.put(attributeKey, attribute.getValue());
    }
  }

  private XMLEventReader getXmlEventReader(InputStream inputStream) throws XMLStreamException {
    XMLEventReader xmlEventReader = XML_INPUT_FACTORY.createXMLEventReader(inputStream);
    xmlEventReader =
        XML_INPUT_FACTORY.createFilteredReader(
            xmlEventReader,
            event -> {
              if (event.isCharacters()) {
                return event.asCharacters().getData().trim().length() > 0;
              }

              return true;
            });
    return xmlEventReader;
  }

  private void readGmlData(
      XMLEventReader xmlEventReader, String elementName, Map<String, String> contextMap)
      throws XMLStreamException {

    if (!xmlEventReader.peek().isStartElement()) {
      LOGGER.debug(
          "Problem reading gml data for element: {}. Invalid xml element provided.", elementName);
      return;
    }

    StringWriter stringWriter = new StringWriter();
    XMLEventWriter eventWriter = XML_OUTPUT_FACTORY.createXMLEventWriter(stringWriter);

    if (eventWriter == null) {
      LOGGER.debug("Problem reading gml data for element: {}. Event writer is null", elementName);
      return;
    }

    int count = 0;
    boolean addEvent = true;

    try {
      while (addEvent) {
        XMLEvent xmlEvent = xmlEventReader.nextEvent();

        // populate the start element with the namespaces
        if (xmlEvent.isStartElement()) {
          xmlEvent = addNamespacesToStartElement(xmlEvent.asStartElement());
          count++;
        } else if (xmlEvent.isEndElement()) {
          if (count == 0) {
            addEvent = false;
            eventWriter.flush();
            LOGGER.debug("String writer: {}", stringWriter);
            contextMap.put(elementName, stringWriter.toString());
          }
          count--;
        }

        if (addEvent) {
          eventWriter.add(xmlEvent);
        }
      }
    } finally {
      eventWriter.close();
    }
  }

  private void lookupMetacardType(WfsMetadata metadata) {
    Optional<MetacardType> optionalMetacardType =
        metacardTypeRegistry.lookupMetacardTypeBySimpleName(
            metadata.getId(), featureTypeQName.getLocalPart());
    if (optionalMetacardType.isPresent()) {
      metacardType = optionalMetacardType.get();
    } else {
      LOGGER.debug(
          "Error looking up metacard type for source id: '{}', and simple name: '{}'",
          metadata.getId(),
          featureTypeQName.getLocalPart());
    }
  }

  private Metacard createMetacard(Map<String, String> contextMap, String metadataId) {
    MetacardImpl metacard = new MetacardImpl(metacardType);

    List<Attribute> attributes =
        mappingEntries
            .values()
            .stream()
            .map(entry -> createAttribute(entry, contextMap))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    attributes.forEach(metacard::setAttribute);

    String id = null;
    if (StringUtils.isBlank(metacard.getId())) {
      id = contextMap.get(METACARD_ID);
      if (StringUtils.isNotBlank(id)) {
        metacard.setId(id);
      } else {
        LOGGER.debug("Feature id is blank. Unable to set metacard id.");
      }
    }

    metacard.setSourceId(metadataId);

    Date date = new Date();
    if (metacard.getEffectiveDate() == null) {
      metacard.setEffectiveDate(date);
    }
    if (metacard.getCreatedDate() == null) {
      metacard.setCreatedDate(date);
    }
    if (metacard.getModifiedDate() == null) {
      metacard.setModifiedDate(date);
    }

    if (StringUtils.isBlank(metacard.getTitle())) {
      metacard.setTitle(id);
    }
    metacard.setContentTypeName(metacardType.getName());
    try {
      metacard.setTargetNamespace(
          new URI(WfsConstants.NAMESPACE_URN_ROOT + metacardType.getName()));
    } catch (URISyntaxException e) {
      LOGGER.debug(
          "Unable to set Target Namespace on metacard: {}{}.",
          WfsConstants.NAMESPACE_URN_ROOT,
          metacardType.getName(),
          e);
    }

    return metacard;
  }

  private StartElement addNamespacesToStartElement(StartElement startElement) {
    String prefix = startElement.getName().getPrefix();
    if (StringUtils.isBlank(prefix)) {
      return startElement;
    }

    if (isPrefixBound(startElement, prefix)) {
      return startElement;
    }

    return XML_EVENT_FACTORY.createStartElement(
        prefix,
        startElement.getNamespaceURI(prefix),
        startElement.getName().getLocalPart(),
        startElement.getAttributes(),
        Collections.singletonList(
                XML_EVENT_FACTORY.createNamespace(prefix, startElement.getNamespaceURI(prefix)))
            .iterator());
  }

  private Attribute createAttribute(FeatureAttributeEntry entry, Map<String, String> contextMap) {
    String value;
    if (StringUtils.isNotBlank(entry.getTemplateText())) {
      value = entry.getMappingFunction().apply(contextMap);
    } else {
      value = contextMap.get(entry.getFeatureProperty());
    }

    if (StringUtils.isBlank(value)) {
      LOGGER.debug("No value found for feature type: {}", entry.getFeatureProperty());
      return null;
    }

    Serializable attributeValue = getMetacardAttributeValue(entry.getFeatureProperty(), value);
    if (attributeValue == null) {
      LOGGER.debug(
          "No attribute value found for feature type: {}, attribute: {}",
          entry.getFeatureProperty(),
          entry.getAttributeName());
      return null;
    }

    return new AttributeImpl(entry.getAttributeName(), attributeValue);
  }

  private Serializable getMetacardAttributeValue(String featureName, String featureValue) {
    FeatureAttributeEntry entry = mappingEntries.get(featureName);
    if (entry == null) {
      LOGGER.debug(
          "Error handling feature name: {}, with value: {}. No mapping entry found for feature name",
          featureName,
          featureValue);
      return null;
    }

    AttributeDescriptor attributeDescriptor =
        metacardType.getAttributeDescriptor(entry.getAttributeName());
    if (attributeDescriptor == null) {
      LOGGER.debug(
          "AttributeDescriptor for attribute name {} not found. The mapping is being ignored.",
          entry.getAttributeName());
      return null;
    }

    Serializable attributeValue = null;

    if (StringUtils.equals(entry.getAttributeName(), Core.RESOURCE_SIZE)) {
      String bytes = convertToBytes(featureValue, getDataUnit());

      if (StringUtils.isNotBlank(bytes)) {
        attributeValue = bytes;
      }
    } else {
      attributeValue =
          getValueForAttributeFormat(
              attributeDescriptor.getType().getAttributeFormat(), featureValue);
    }

    return attributeValue;
  }

  private Serializable getValueForAttributeFormat(
      AttributeType.AttributeFormat attributeFormat, final String value) {

    Serializable serializable = null;
    switch (attributeFormat) {
      case BOOLEAN:
        serializable = Boolean.valueOf(value);
        break;
      case DOUBLE:
        serializable = Double.valueOf(value);
        break;
      case FLOAT:
        serializable = Float.valueOf(value);
        break;
      case INTEGER:
        serializable = Integer.valueOf(value);
        break;
      case LONG:
        serializable = Long.valueOf(value);
        break;
      case SHORT:
        serializable = Short.valueOf(value);
        break;
      case XML:
      case STRING:
        serializable = value;
        break;
      case GEOMETRY:
        LOGGER.trace("Unescape the geometry: {}", value);
        String newValue = StringEscapeUtils.unescapeXml(value);
        LOGGER.debug("Geometry value after it has been xml unescaped: {}", newValue);
        String wkt = getWktFromGeometry(newValue);
        LOGGER.debug("String wkt value: {}", wkt);
        serializable = wkt;
        break;
      case BINARY:
        serializable = value.getBytes(StandardCharsets.UTF_8);
        break;
      case DATE:
        try {
          serializable = DatatypeConverter.parseDate(value).getTime();
        } catch (IllegalArgumentException e) {
          LOGGER.debug("Error converting value to a date. value: '{}'", value, e);
        }
        break;
      default:
        break;
    }
    return serializable;
  }

  private String getWktFromGeometry(String geometry) {
    String wkt = getWktFromGml(geometry, new org.geotools.gml3.GMLConfiguration());
    if (StringUtils.isNotBlank(wkt)) {
      return wkt;
    }
    LOGGER.debug("Error converting gml to wkt using gml3 configuration. Trying gml2.");
    return getWktFromGml(geometry, new org.geotools.gml2.GMLConfiguration());
  }

  private String getWktFromGml(final String geometry, final Configuration gmlConfiguration) {
    final Gml3ToWkt gml3ToWkt = new Gml3ToWktImpl(gmlConfiguration);
    try (final InputStream gmlStream =
        new ByteArrayInputStream(geometry.getBytes(StandardCharsets.UTF_8))) {
      final Object gmlObject = gml3ToWkt.parseXml(gmlStream);
      if (gmlObject instanceof Geometry) {
        final Geometry geo = (Geometry) gmlObject;
        if (LAT_LON_ORDER.equals(featureCoordinateOrder)) {
          swapCoordinates(geo);
        }
        return new WKTWriter().write(geo);
      } else {
        LOGGER.debug("{} could not be parsed to a Geometry object.", geometry);
        return null;
      }
    } catch (Exception e) {
      LOGGER.debug(
          "Error converting gml to wkt using configuration {}. GML: {}.",
          gmlConfiguration,
          geometry,
          e);
      return null;
    }
  }

  private void swapCoordinates(final Geometry geo) {
    LOGGER.trace("Swapping Lat/Lon coords to Lon/Lat for geometry: {}", geo);
    geo.apply(new CoordinateOrderTransformer());
  }

  private boolean isPrefixBound(StartElement startElement, String prefix) {
    for (Iterator i = startElement.getNamespaces(); i.hasNext(); ) {
      Namespace namespace = (Namespace) i.next();

      if (namespace.getPrefix().equals(prefix)) {
        return true;
      }
    }
    return false;
  }

  private void mapNamespaces(StartElement startElement, Map<String, String> map) {

    for (Iterator i = startElement.getNamespaces(); i.hasNext(); ) {
      Namespace namespace = (Namespace) i.next();
      map.put(namespace.getPrefix(), namespace.getNamespaceURI());
    }
  }

  private boolean isStateValid(InputStream inputStream, WfsMetadata metadata) {
    if (inputStream == null) {
      LOGGER.debug("Received a null input stream.");
      return false;
    }

    if (metadata == null) {
      LOGGER.debug("Received a null WfsMetadata object.");
      return false;
    }

    if (StringUtils.isBlank(featureType)) {
      LOGGER.debug("Feature type must contain a value: {}", featureType);
      return false;
    }

    if (CollectionUtils.isEmpty(mappingEntries.values())) {
      LOGGER.debug("There are no mappings for feature type: {}", featureType);
      return false;
    }

    return true;
  }

  private void addAttributeMapping(String attributeName, String featureName, String templateText) {
    LOGGER.trace(
        "Adding attribute mapping from: {} to: {} using: {}",
        attributeName,
        featureName,
        templateText);
    mappingEntries.put(
        featureName, new FeatureAttributeEntry(attributeName, featureName, templateText));
  }

  public String getDataUnit() {
    return dataUnit;
  }

  public void setDataUnit(String unit) {
    LOGGER.trace("Setting data unit to: {}", unit);
    dataUnit = unit;
  }

  public void setFeatureType(String featureType) {
    LOGGER.trace("Setting feature type to: {}", featureType);
    this.featureType = featureType;
    featureTypeQName = QName.valueOf(featureType);
  }

  public void setMetacardTypeRegistry(WfsMetacardTypeRegistry metacardTypeRegistry) {
    this.metacardTypeRegistry = metacardTypeRegistry;
  }

  /**
   * Sets a list of attribute mappings from a list of JSON strings.
   *
   * @param attributeMappingsList - a list of JSON-formatted `FeatureAttributeEntry` objects.
   */
  public void setAttributeMappings(/*@Nullable*/ List<String> attributeMappingsList) {
    LOGGER.trace("Setting attribute mappings to: {}", attributeMappingsList);
    if (attributeMappingsList != null) {
      mappingEntries.clear();
      attributeMappingsList
          .stream()
          .filter(StringUtils::isNotEmpty)
          .map(this::jsonToMap)
          .filter(this::validAttributeMapping)
          .forEach(
              map ->
                  addAttributeMapping(
                      (String) map.get(ATTRIBUTE_NAME),
                      (String) map.get(FEATURE_NAME),
                      (String) map.get(TEMPLATE)));
    }
  }

  private Map jsonToMap(String jsonValue) {
    try {
      return GSON.fromJson(jsonValue, MAP_STRING_TO_OBJECT_TYPE);
    } catch (JsonParseException e) {
      LOGGER.debug("Failed to parse attribute mapping json '{}'", jsonValue, e);
    }
    return null;
  }

  private boolean validAttributeMapping(Map map) {
    return map != null
        && map.get(ATTRIBUTE_NAME) instanceof String
        && map.get(FEATURE_NAME) instanceof String
        && map.get(TEMPLATE) instanceof String;
  }

  private String convertToBytes(String value, String unit) {

    BigDecimal resourceSize = new BigDecimal(value);
    resourceSize = resourceSize.setScale(1, BigDecimal.ROUND_HALF_UP);

    switch (unit) {
      case B:
        break;
      case KB:
        resourceSize = resourceSize.multiply(new BigDecimal(BYTES_PER_KB));
        break;
      case MB:
        resourceSize = resourceSize.multiply(new BigDecimal(BYTES_PER_MB));
        break;
      case GB:
        resourceSize = resourceSize.multiply(new BigDecimal(BYTES_PER_GB));
        break;
      case TB:
        resourceSize = resourceSize.multiply(new BigDecimal(BYTES_PER_TB));
        break;
      case PB:
        resourceSize = resourceSize.multiply(new BigDecimal(BYTES_PER_PB));
        break;
      default:
        break;
    }

    String resourceSizeAsString = resourceSize.toPlainString();
    LOGGER.debug("resource size in bytes: {}", resourceSizeAsString);
    return resourceSizeAsString;
  }

  public void setFeatureCoordinateOrder(final String featureCoordinateOrder) {
    this.featureCoordinateOrder = featureCoordinateOrder;
  }
}
