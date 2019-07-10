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
package ddf.catalog.transformer.xml.adapter;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.xml.binding.GeometryElement;
import ddf.catalog.transformer.xml.binding.GeometryElement.Value;
import java.io.Serializable;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import net.opengis.gml.v_3_1_1.AbstractGeometryType;
import org.jvnet.jaxb2_commons.locator.DefaultRootObjectLocator;
import org.jvnet.ogc.gml.v_3_1_1.jts.ConversionFailedException;
import org.jvnet.ogc.gml.v_3_1_1.jts.GML311ToJTSGeometryConverter;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311GeometryConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeometryAdapter extends XmlAdapter<GeometryElement, Attribute> {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeometryAdapter.class);
  private static GeometryFactory geometryFactory = new GeometryFactory();

  public static GeometryElement marshalFrom(Attribute attribute)
      throws CatalogTransformerException {
    GeometryElement element = new GeometryElement();
    element.setName(attribute.getName());
    if (attribute.getValue() != null) {
      for (Serializable value : attribute.getValues()) {
        if (!(value instanceof String)) {
          continue;
        }
        String wkt = (String) value;
        WKTReader wktReader = new WKTReader(geometryFactory);
        Geometry jtsGeometry = null;
        try {
          jtsGeometry = wktReader.read(wkt);
        } catch (ParseException e) {
          throw new CatalogTransformerException(
              "Could not transform Metacard to XML.  Invalid WKT.", e);
        }

        JTSToGML311GeometryConverter converter = new JTSToGML311GeometryConverter();

        @SuppressWarnings("unchecked")
        JAXBElement<AbstractGeometryType> gmlElement =
            (JAXBElement<AbstractGeometryType>) converter.createElement(jtsGeometry);

        GeometryElement.Value geoValue = new GeometryElement.Value();
        geoValue.setGeometry(gmlElement);
        ((GeometryElement) element).getValue().add(geoValue);
      }
    }
    return element;
  }

  public static Attribute unmarshalFrom(GeometryElement element) throws ConversionFailedException {
    AttributeImpl attribute = null;
    GML311ToJTSGeometryConverter converter = new GML311ToJTSGeometryConverter();
    WKTWriter wktWriter = new WKTWriter();

    for (Value xmlValue : element.getValue()) {
      JAXBElement<AbstractGeometryType> xmlGeometry = xmlValue.getGeometry();
      Geometry geometry = null;
      if (xmlGeometry != null && xmlGeometry.getValue() != null) {
        try {
          geometry =
              converter.createGeometry(
                  new DefaultRootObjectLocator(xmlValue), xmlGeometry.getValue());
        } catch (ConversionFailedException e) {
          LOGGER.debug("Unable to adapt goemetry. ", e);
        }
      }
      if (geometry != null && !geometry.isEmpty()) {
        String wkt = wktWriter.write(geometry);

        if (attribute == null) {
          attribute = new AttributeImpl(element.getName(), wkt);
        } else {
          attribute.addValue(wkt);
        }
      }
    }
    return attribute;
  }

  @Override
  public GeometryElement marshal(Attribute attribute) throws CatalogTransformerException {
    return marshalFrom(attribute);
  }

  @Override
  public Attribute unmarshal(GeometryElement element) throws ConversionFailedException {
    return unmarshalFrom(element);
  }
}
