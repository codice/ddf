/*
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.spatial.kml.converter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import net.opengis.kml.v_2_2_0.DocumentType;
import org.apache.commons.collections.CollectionUtils;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

public class KmlDocumentToJtsGeometryConverter {
  private KmlDocumentToJtsGeometryConverter() {}

  public static Geometry from(DocumentType kmlDocument) {
    if (kmlDocument == null) {
      return null;
    }

    List<Geometry> jtsGeometries =
        kmlDocument.getAbstractFeatureGroup().stream()
            .map(JAXBElement::getValue)
            .map(KmlFeatureToJtsGeometryConverter::from)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

    if (CollectionUtils.isNotEmpty(jtsGeometries)) {
      return geometryFactory.createGeometryCollection(jtsGeometries.toArray(new Geometry[0]));
    }

    return null;
  }
}
