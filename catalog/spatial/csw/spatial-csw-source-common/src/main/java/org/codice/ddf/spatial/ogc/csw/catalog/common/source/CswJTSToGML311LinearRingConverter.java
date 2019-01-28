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
package org.codice.ddf.spatial.ogc.csw.catalog.common.source;

import java.util.ArrayList;
import java.util.List;
import net.opengis.gml.v_3_1_1.DirectPositionListType;
import net.opengis.gml.v_3_1_1.LinearRingType;
import org.jvnet.ogc.gml.v_3_1_1.ObjectFactoryInterface;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311CoordinateConverter;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311LinearRingConverter;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311SRSReferenceGroupConverterInterface;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LinearRing;

/**
 * An implementation of {@link JTSToGML311LinearRingConverter} that provides a means of customizing
 * the LinearRing GML. By default, the {@code CswJTSToGML311LinearRingConverter} behaves identically
 * to the {@code JTSToGML311LinearRingConverter}, but the output of the converter can be customized
 * via constructor argument(s).
 */
public class CswJTSToGML311LinearRingConverter extends JTSToGML311LinearRingConverter {
  boolean usePosList = false;

  /**
   * Constructs a LinearRing converter that is functionally identical to the converter constructed
   * by {@link
   * org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311LinearRingConverter#JTSToGML311LinearRingConverter(ObjectFactoryInterface,
   * JTSToGML311SRSReferenceGroupConverterInterface, JTSToGML311CoordinateConverter)}
   */
  public CswJTSToGML311LinearRingConverter(
      ObjectFactoryInterface objectFactory,
      JTSToGML311SRSReferenceGroupConverterInterface srsReferenceGroupConverter,
      JTSToGML311CoordinateConverter coordinateConverter) {
    this(objectFactory, srsReferenceGroupConverter, coordinateConverter, false);
  }

  /**
   * Constructs a LinearRing converter that is functionally identical to the converter constructed
   * by {@link
   * org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311LinearRingConverter#JTSToGML311LinearRingConverter(ObjectFactoryInterface,
   * JTSToGML311SRSReferenceGroupConverterInterface, JTSToGML311CoordinateConverter)} with the
   * exception that if usePosList is true the returned {@link LinearRingType} will have its posList
   * member variable populated and set, rather than its posOrPointPropertyOrPointRep. When converted
   * to a string, this results in the GML containing a single <posList> element, rather than a list
   * of <pos> elements.
   */
  public CswJTSToGML311LinearRingConverter(
      ObjectFactoryInterface objectFactory,
      JTSToGML311SRSReferenceGroupConverterInterface srsReferenceGroupConverter,
      JTSToGML311CoordinateConverter coordinateConverter,
      boolean usePosList) {
    super(objectFactory, srsReferenceGroupConverter, coordinateConverter);
    this.usePosList = usePosList;
  }

  /** @see {@code JTSToGML311LinearRingConverter#doCreateGeometryType} */
  @Override
  protected LinearRingType doCreateGeometryType(LinearRing linearRing) {
    final LinearRingType resultLinearRing;

    if (usePosList) {
      resultLinearRing = getObjectFactory().createLinearRingType();

      List<Double> posDoubleList = new ArrayList<Double>();
      for (Coordinate coordinate : linearRing.getCoordinates()) {
        posDoubleList.add(coordinate.x);
        posDoubleList.add(coordinate.y);
      }

      DirectPositionListType directPosListType = new DirectPositionListType();
      directPosListType.setValue(posDoubleList);
      resultLinearRing.setPosList(directPosListType);
    } else {
      resultLinearRing = super.doCreateGeometryType(linearRing);
    }

    return resultLinearRing;
  }
}
