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

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import javax.xml.namespace.QName;
import org.apache.commons.lang.time.DateFormatUtils;
import org.codice.ddf.spatial.ogc.catalog.common.converter.XmlNode;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.AttributeDescriptorComparator;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsQnameBuilder;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class works in conjunction with XStream to convert a {@link Metacard} to XML according to
 * the GML 2.1.2 spec. It will also convert respective XML into a Metacard.
 */
public class GenericFeatureConverter extends AbstractFeatureConverter {

  private static final String FID = "fid";

  private static final Logger LOGGER = LoggerFactory.getLogger(GenericFeatureConverter.class);

  public GenericFeatureConverter() {}

  public GenericFeatureConverter(String srs) {
    this.setSrs(srs);
  }

  public GenericFeatureConverter(MetacardMapper metacardMapper) {
    super(metacardMapper);
  }

  /**
   * Method to determine if this converter knows how to convert the specified Class.
   *
   * @param clazz the class to check
   */
  @Override
  public boolean canConvert(Class clazz) {
    return Metacard.class.isAssignableFrom(clazz);
  }

  /**
   * This method will convert a {@link Metacard} instance into xml that will validate against the
   * GML 2.1.2 AbstractFeatureType.
   *
   * @param value the {@link Metacard} to convert
   * @param writer the stream writer responsible for writing this xml doc
   * @param context a reference back to the Xstream marshalling context. Allows you to call
   *     "convertAnother" which will lookup other registered converters.
   */
  @Override
  public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
    Metacard metacard = (Metacard) value;

    // TODO when we have a reference to the MCT we can get the namespace too
    QName qname =
        WfsQnameBuilder.buildQName(
            metacard.getMetacardType().getName(), metacard.getContentTypeName());

    writer.startNode(qname.getPrefix() + ":" + qname.getLocalPart());

    // Add the "fid" attribute if we have an ID
    String fid = (String) metacard.getAttribute(Metacard.ID).getValue();
    if (fid != null) {
      writer.addAttribute(FID, fid);
    }

    if (null != metacard.getLocation()) {
      Geometry geo = XmlNode.readGeometry(metacard.getLocation());
      if (geo != null && !geo.isEmpty()) {
        XmlNode.writeEnvelope(
            WfsConstants.GML_PREFIX + ":" + "boundedBy",
            context,
            writer,
            geo.getEnvelopeInternal());
      }
    }

    Set<AttributeDescriptor> descriptors =
        new TreeSet<AttributeDescriptor>(new AttributeDescriptorComparator());
    descriptors.addAll(metacard.getMetacardType().getAttributeDescriptors());

    for (AttributeDescriptor attributeDescriptor : descriptors) {
      Attribute attribute = metacard.getAttribute(attributeDescriptor.getName());
      if (attribute != null) {
        writeAttributeToXml(
            attribute, qname, attributeDescriptor.getType().getAttributeFormat(), context, writer);
      }
    }
    writer.endNode();
  }

  /* Helper method to convert these types to a String representation */
  private void writeAttributeToXml(
      Attribute attribute,
      QName qname,
      AttributeFormat format,
      MarshallingContext context,
      HierarchicalStreamWriter writer) {
    // Loop to handle multi-valued attributes

    String name = qname.getPrefix() + ":" + attribute.getName();
    for (Serializable value : attribute.getValues()) {
      String xmlValue = null;

      switch (format) {
        case XML:
          String cdata = (String) value;
          if (cdata != null && (writer.underlyingWriter() instanceof EnhancedStaxWriter)) {
            writer.startNode(name);
            EnhancedStaxWriter eWriter = (EnhancedStaxWriter) writer.underlyingWriter();
            eWriter.writeCdata(cdata);
            writer.endNode();
          }
          break;
        case GEOMETRY:
          XmlNode.writeGeometry(name, context, writer, XmlNode.readGeometry((String) value));
          break;
        case BINARY:
          xmlValue = Base64.getEncoder().encodeToString((byte[]) value);
          break;
        case DATE:
          Date date = (Date) value;
          xmlValue =
              DateFormatUtils.formatUTC(
                  date, DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern());
          break;
        case OBJECT:
          // Probably won't translate at all.
          break;
        default:
          xmlValue = value.toString();
          break;
      }
      // Write the node if we were able to convert it.
      if (xmlValue != null) {

        writer.startNode(name);
        writer.setValue(xmlValue);
        writer.endNode();
      }
    }
  }

  /**
   * This method will unmarshal an XML instance of a "gml:featureMember" to a {@link Metacard}.
   *
   * @param hreader the stream reader responsible for reading this xml doc
   * @param context a reference back to the Xstream unmarshalling context. Allows you to call
   *     "convertAnother" which will lookup other registered converters.
   */
  @Override
  public Object unmarshal(HierarchicalStreamReader hreader, UnmarshallingContext context) {

    LOGGER.debug("Entering: {} : unmarshal", this.getClass().getName());

    String fid = hreader.getAttribute(FID);
    MetacardImpl mc;
    if (metacardType != null) {
      mc = (MetacardImpl) createMetacardFromFeature(hreader, metacardType);
    } else {
      throw new IllegalArgumentException(
          "No MetacardType registered on the FeatureConverter.  Unable to to convert features to metacards.");
    }
    mc.setId(fid);
    mc.setSourceId(sourceId);

    // set some default values that we can't get from a generic
    // featureCollection if they are not already set
    Date genericDate = new Date();
    if (mc.getEffectiveDate() == null) {
      mc.setEffectiveDate(genericDate);
    }

    ifAttributeSet(
        mc.getAttribute(Core.CREATED),
        attribute -> mc.setAttribute(new AttributeImpl(Core.CREATED, genericDate)));
    ifAttributeSet(
        mc.getAttribute(Core.MODIFIED),
        attribute -> mc.setAttribute(new AttributeImpl(Core.MODIFIED, genericDate)));
    ifAttributeSet(
        mc.getAttribute(Core.TITLE),
        attribute -> mc.setAttribute(new AttributeImpl(Core.MODIFIED, genericDate)));

    mc.setContentTypeName(metacardType.getName());
    try {
      mc.setTargetNamespace(new URI(WfsConstants.NAMESPACE_URN_ROOT + metacardType.getName()));
    } catch (URISyntaxException e) {
      LOGGER.debug(
          "Unable to set Target Namespace on metacard: {}, Exception {}",
          WfsConstants.NAMESPACE_URN_ROOT + metacardType.getName(),
          e);
    }

    return mc;
  }

  public void setSourceId(final String sourceId) {
    this.sourceId = sourceId;
  }

  private void ifAttributeSet(Attribute attribute, Consumer<Attribute> consumer) {
    if (attribute != null && attribute.getValue() != null) {
      consumer.accept(attribute);
    }
  }
}
