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
package org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common;

import java.io.StringReader;
import java.util.Locale;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import ogc.schema.opengis.gml.v_2_1_2.AbstractGeometryType;
import ogc.schema.opengis.gml.v_2_1_2.GeometryCollectionType;
import ogc.schema.opengis.gml.v_2_1_2.LineStringType;
import ogc.schema.opengis.gml.v_2_1_2.MultiLineStringType;
import ogc.schema.opengis.gml.v_2_1_2.MultiPointType;
import ogc.schema.opengis.gml.v_2_1_2.MultiPolygonType;
import ogc.schema.opengis.gml.v_2_1_2.ObjectFactory;
import ogc.schema.opengis.gml.v_2_1_2.PointType;
import ogc.schema.opengis.gml.v_2_1_2.PolygonType;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.gml2.GMLWriter;

public class Wfs10JTStoGML200Converter {

  private static final ObjectFactory GML_OBJECT_FACTORY = new ObjectFactory();

  private static final String MULTIGEOMETRY_GML = "multigeometry";

  private static final String GEOMETRYCOLLECTION_GML = "GeometryCollection";

  private Wfs10JTStoGML200Converter() {}

  public static String convertGeometryToGML(Geometry geometry) throws JAXBException {
    GMLWriter gmlWriter = new GMLWriter(true);
    return gmlWriter.write(geometry);
  }

  public static AbstractGeometryType convertGMLToGeometryType(String gml, QName qName)
      throws JAXBException {

    String type = qName.getLocalPart().toUpperCase(Locale.getDefault());

    switch (type) {
      case "POLYGON":
        return JAXB.unmarshal(new StringReader(gml), PolygonType.class);
      case "POINT":
        return JAXB.unmarshal(new StringReader(gml), PointType.class);
      case "LINESTRING":
        return JAXB.unmarshal(new StringReader(gml), LineStringType.class);
      case "MULTIPOINT":
        return JAXB.unmarshal(new StringReader(gml), MultiPointType.class);
      case "MULTILINESTRING":
        return JAXB.unmarshal(new StringReader(gml), MultiLineStringType.class);
      case "MULTIPOLYGON":
        return JAXB.unmarshal(new StringReader(gml), MultiPolygonType.class);
      case "GEOMETRYCOLLECTION":
        return JAXB.unmarshal(new StringReader(gml), GeometryCollectionType.class);
      default:
        break;
    }
    return null;
  }

  public static JAXBElement<? extends AbstractGeometryType> convertGeometryTypeToJAXB(
      AbstractGeometryType abstractGeometryType) {

    if (abstractGeometryType instanceof PolygonType) {
      return GML_OBJECT_FACTORY.createPolygon((PolygonType) abstractGeometryType);
    } else if (abstractGeometryType instanceof PointType) {
      return GML_OBJECT_FACTORY.createPoint((PointType) abstractGeometryType);
    } else if (abstractGeometryType instanceof LineStringType) {
      return GML_OBJECT_FACTORY.createLineString((LineStringType) abstractGeometryType);
    } else if (abstractGeometryType instanceof MultiPointType) {
      return GML_OBJECT_FACTORY.createMultiPoint((MultiPointType) abstractGeometryType);
    } else if (abstractGeometryType instanceof MultiLineStringType) {
      return GML_OBJECT_FACTORY.createMultiLineString((MultiLineStringType) abstractGeometryType);
    } else if (abstractGeometryType instanceof MultiPolygonType) {
      return GML_OBJECT_FACTORY.createMultiPolygon((MultiPolygonType) abstractGeometryType);
    } else if (abstractGeometryType instanceof GeometryCollectionType) {
      return GML_OBJECT_FACTORY.createGeometryCollection(
          (GeometryCollectionType) abstractGeometryType);
    } else {
      return null;
    }
  }

  public static String convertGeometryCollectionToGML(Geometry geometry) throws JAXBException {
    return convertGeometryToGML(geometry)
        .toLowerCase()
        .replaceAll(MULTIGEOMETRY_GML, GEOMETRYCOLLECTION_GML);
  }
}
