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
package org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl;

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

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.io.xml.WstxDriver;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.libs.geo.GeoFormatException;
import org.codice.ddf.libs.geo.util.GeospatialUtil;
import org.codice.ddf.spatial.ogc.catalog.common.converter.XmlNode;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.io.gml2.GMLReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public abstract class AbstractFeatureConverter implements FeatureConverter {
  protected static final String ERROR_PARSING_MESSAGE = "Error parsing Geometry from feature xml.";

  protected static final String UTF8_ENCODING = "UTF-8";

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeatureConverter.class);

  protected String sourceId;

  protected String wfsUrl;

  protected MetacardType metacardType;

  protected String coordinateOrder;

  private HierarchicalStreamCopier copier = new HierarchicalStreamCopier();

  private NoNameCoder noNameCoder = new NoNameCoder();

  private MetacardMapper metacardMapper = null;

  private String srs = GeospatialUtil.EPSG_4326;

  public AbstractFeatureConverter() {}

  public AbstractFeatureConverter(MetacardMapper metacardMapper) {
    this.metacardMapper = metacardMapper;
  }

  @Override
  public boolean canConvert(Class clazz) {
    return Metacard.class.isAssignableFrom(clazz);
  }

  @Override
  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  @Override
  public void setWfsUrl(String url) {
    this.wfsUrl = url;
  }

  @Override
  public MetacardType getMetacardType() {
    return this.metacardType;
  }

  @Override
  public void setMetacardType(MetacardType metacardType) {
    this.metacardType = metacardType;
  }

  @Override
  public void setCoordinateOrder(String coordinateOrder) {
    this.coordinateOrder = coordinateOrder;
  }

  protected HierarchicalStreamReader copyXml(
      HierarchicalStreamReader hreader, StringWriter writer) {
    copier.copy(hreader, new CompactWriter(writer, noNameCoder));

    StaxDriver driver = new WstxDriver();
    return driver.createReader(
        new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8)));
  }

  protected Metacard createMetacardFromFeature(
      HierarchicalStreamReader hreader, MetacardType metacardType) {
    StringWriter metadataWriter = new StringWriter();
    HierarchicalStreamReader reader = copyXml(hreader, metadataWriter);
    MetacardImpl mc = new MetacardImpl(metacardType);
    mc.setContentTypeName(metacardType.getName());

    while (reader.hasMoreChildren()) {
      reader.moveDown();

      handleBasicMetacardAttribute(reader, mc);

      String featureProperty = reader.getNodeName();

      String mappedMetacardAttribute = getMappedMetacardAttribute(featureProperty);

      if (mappedMetacardAttribute == null) {
        LOGGER.debug(
            "Feature property {} did not map to a metacard attribute so it will be skipped.",
            featureProperty);
        reader.moveUp();
        continue;
      }

      AttributeDescriptor attributeDescriptor =
          metacardType.getAttributeDescriptor(mappedMetacardAttribute);

      Serializable value = null;
      if (attributeDescriptor != null
          && (StringUtils.isNotBlank(reader.getValue())
              || BasicTypes.GEO_TYPE
                  .getAttributeFormat()
                  .equals(attributeDescriptor.getType().getAttributeFormat())
              || BasicTypes.DATE_TYPE
                  .getAttributeFormat()
                  .equals(attributeDescriptor.getType().getAttributeFormat()))) {
        if (StringUtils.equals(mappedMetacardAttribute, Core.RESOURCE_SIZE)) {
          String bytes = convertToBytes(reader, metacardMapper.getDataUnit());
          if (StringUtils.isNotBlank(bytes)) {
            LOGGER.debug(
                "Setting mapped metacard attribute {} with value {}",
                mappedMetacardAttribute,
                bytes);
            mc.setAttribute(mappedMetacardAttribute, bytes);
          }
        } else {
          value =
              getValueForMetacardAttribute(
                  attributeDescriptor.getType().getAttributeFormat(), reader);
          if (value != null) {
            LOGGER.debug(
                "Setting mapped metacard attribute {} with value {}",
                mappedMetacardAttribute,
                value);
            mc.setAttribute(mappedMetacardAttribute, value);
          }
        }

        if (BasicTypes.GEO_TYPE
            .getAttributeFormat()
            .equals(attributeDescriptor.getType().getAttributeFormat())) {
          mc.setLocation((String) value);
        }
      }

      reader.moveUp();
    }

    mc.setMetadata(metadataWriter.toString());

    try {
      if (metacardType instanceof FeatureMetacardType) {
        URI namespaceUri = new URI(((FeatureMetacardType) metacardType).getNamespaceURI());
        mc.setTargetNamespace(namespaceUri);
      }
    } catch (URISyntaxException e) {
      LOGGER.debug("Error setting target namespace uri on metacard.", e);
    }

    return mc;
  }

  private void handleBasicMetacardAttribute(
      final HierarchicalStreamReader reader, final MetacardImpl metacard) {
    final String featurePropertyName = reader.getNodeName();
    if (isBasicMetacardAttribute(featurePropertyName)) {
      final AttributeDescriptor attributeDescriptor =
          getBasicMetacardAttributeDescriptor(featurePropertyName);
      final Serializable value =
          getValueForMetacardAttribute(attributeDescriptor.getType().getAttributeFormat(), reader);
      LOGGER.debug("Setting metacard basic attribute: {} = {}", featurePropertyName, value);

      metacard.setAttribute(featurePropertyName, value);
    }
  }

  private boolean isBasicMetacardAttribute(String attrName) {
    return getBasicMetacardAttributeDescriptor(attrName) != null;
  }

  private AttributeDescriptor getBasicMetacardAttributeDescriptor(String attrName) {
    return MetacardImpl.BASIC_METACARD.getAttributeDescriptor(attrName);
  }

  private String getMappedMetacardAttribute(final String featurePropertyName) {
    String mappedMetacardAttribute = null;
    if (metacardMapper != null) {
      LOGGER.debug(
          "Looking up metacard attribute for feature property {} using metacard mapper",
          featurePropertyName);
      mappedMetacardAttribute = metacardMapper.getMetacardAttribute(featurePropertyName);
      LOGGER.debug(
          "Found metacard attribute {} for feature property {}",
          mappedMetacardAttribute,
          featurePropertyName);
    }
    return mappedMetacardAttribute;
  }

  protected Serializable getValueForMetacardAttribute(
      AttributeFormat attributeFormat, HierarchicalStreamReader reader) {

    Serializable ser = null;
    switch (attributeFormat) {
      case BOOLEAN:
        ser = Boolean.valueOf(reader.getValue());
        break;
      case DOUBLE:
        ser = Double.valueOf(reader.getValue());
        break;
      case FLOAT:
        ser = Float.valueOf(reader.getValue());
        break;
      case INTEGER:
        ser = Integer.valueOf(reader.getValue());
        break;
      case LONG:
        ser = Long.valueOf(reader.getValue());
        break;
      case SHORT:
        ser = Short.valueOf(reader.getValue());
        break;
      case XML:
      case STRING:
        ser = reader.getValue();
        break;
      case GEOMETRY:
        XmlNode node = new XmlNode(reader);
        String xml = node.toString();
        GMLReader gmlReader = new GMLReader();
        Geometry geo = null;
        try {
          geo = gmlReader.read(xml, null);
          if (StringUtils.isNotBlank(srs) && !srs.equals(GeospatialUtil.EPSG_4326)) {
            geo = GeospatialUtil.transformToEPSG4326LonLatFormat(geo, srs);
          }
        } catch (SAXException | IOException | ParserConfigurationException | GeoFormatException e) {
          geo = null;
          LOGGER.debug(ERROR_PARSING_MESSAGE, e);
        }
        if (geo != null) {
          WKTWriter wktWriter = new WKTWriter();
          ser = wktWriter.write(geo);
        }
        break;
      case BINARY:
        try {
          ser = reader.getValue().getBytes(UTF8_ENCODING);
        } catch (UnsupportedEncodingException e) {
          LOGGER.debug("Error encoding the binary value into the metacard.", e);
        }
        break;
      case DATE:
        ser = parseDateFromXml(reader);
        break;
      default:
        break;
    }
    return ser;
  }

  private String convertToBytes(HierarchicalStreamReader reader, String unit) {

    BigDecimal resourceSize = new BigDecimal(reader.getValue());
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

  protected Date parseDateFromXml(HierarchicalStreamReader reader) {
    Date date = null;
    boolean processingChildNode = false;

    if (reader.hasMoreChildren()) {
      reader.moveDown();
      processingChildNode = true;
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("node name: {}", reader.getNodeName());
      LOGGER.debug("value: {}", reader.getValue());
    }
    if (StringUtils.isBlank(reader.getValue())) {
      date = null;
      if (processingChildNode) {
        reader.moveUp();
      }
      return date;
    }

    try { // trying to parse xsd:date
      date = DatatypeConverter.parseDate(reader.getValue()).getTime();
    } catch (IllegalArgumentException e) {
      LOGGER.debug("Unable to parse date, attempting to parse as xsd:dateTime.", e);
      try { // try to parse it as a xsd:dateTime
        date = DatatypeConverter.parseDateTime(reader.getValue()).getTime();
      } catch (IllegalArgumentException ie) {
        LOGGER.debug(
            "Unable to parse date from XML; defaulting \"{}\" to current datetime.",
            reader.getNodeName(),
            ie);
        date = new Date();
      }
    }

    if (processingChildNode) {
      reader.moveUp();
    }
    LOGGER.debug("node name: {}", reader.getNodeName());
    return date;
  }

  public void setSrs(String srs) {
    this.srs = srs;
  }

  public String getSrs() {
    return srs;
  }
}
