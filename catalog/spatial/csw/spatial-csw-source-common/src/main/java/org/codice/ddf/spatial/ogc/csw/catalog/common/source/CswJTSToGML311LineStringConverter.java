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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import java.util.ArrayList;
import java.util.List;
import net.opengis.gml.v_3_1_1.DirectPositionListType;
import net.opengis.gml.v_3_1_1.LineStringType;
import org.jvnet.ogc.gml.v_3_1_1.ObjectFactoryInterface;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311CoordinateConverter;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311LineStringConverter;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311SRSReferenceGroupConverterInterface;

/**
 * An implementation of {@link JTSToGML311LineStringConverter} that provides a means of customizing
 * the LineString GML. By default, the {@code CswJTSToGML311LineStringConverter} behaves identically
 * to the {@code JTSToGML311LineStringConverter}, but the output of the converter can be customized
 * via constructor argument(s).
 */
public class CswJTSToGML311LineStringConverter extends JTSToGML311LineStringConverter {
  private boolean usePosList = false;

  /**
   * Constructs a LineString converter that is functionally identical to the converter constructed
   * by {@link
   * org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311LineStringConverter#JTSToGML311LineStringConverter(ObjectFactoryInterface,
   * JTSToGML311SRSReferenceGroupConverterInterface, JTSToGML311CoordinateConverter)}
   */
  public CswJTSToGML311LineStringConverter(
      ObjectFactoryInterface objectFactory,
      JTSToGML311SRSReferenceGroupConverterInterface srsReferenceGroupConverter,
      JTSToGML311CoordinateConverter coordinateConverter) {
    this(objectFactory, srsReferenceGroupConverter, coordinateConverter, false);
  }

  /**
   * Constructs a LineString converter that is functionally identical to the converter constructed
   * by {@link
   * org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311LineStringConverter#JTSToGML311LineStringConverter(ObjectFactoryInterface,
   * JTSToGML311SRSReferenceGroupConverterInterface, JTSToGML311CoordinateConverter)} with the
   * exception that if usePosList is true the returned {@link LineStringType} will have its posList
   * member variable populated and set, rather than its posOrPointPropertyOrPointRep. When converted
   * to a string, this results in the GML containing a single <posList> element, rather than a list
   * of <pos> elements.
   */
  public CswJTSToGML311LineStringConverter(
      ObjectFactoryInterface objectFactory,
      JTSToGML311SRSReferenceGroupConverterInterface srsReferenceGroupConverter,
      JTSToGML311CoordinateConverter coordinateConverter,
      boolean usePosList) {
    super(objectFactory, srsReferenceGroupConverter, coordinateConverter);
    this.usePosList = usePosList;
  }

  /** @see {@code JTSToGML311LineStringConverter#doCreateGeometryType} */
  @Override
  protected LineStringType doCreateGeometryType(LineString lineString) {
    final LineStringType resultLineString;

    if (usePosList) {
      resultLineString = getObjectFactory().createLineStringType();

      List<Double> posDoubleList = new ArrayList<Double>();
      for (Coordinate coordinate : lineString.getCoordinates()) {
        posDoubleList.add(coordinate.x);
        posDoubleList.add(coordinate.y);
      }

      DirectPositionListType directPosListType = new DirectPositionListType();
      directPosListType.setValue(posDoubleList);
      resultLineString.setPosList(directPosListType);
    } else {
      resultLineString = super.doCreateGeometryType(lineString);
    }

    return resultLineString;
  }
}
