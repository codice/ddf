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
package org.codice.ddf.spatial.ogc.wfs.v110.catalog.common;

import java.util.Arrays;
import java.util.List;
import javax.xml.namespace.QName;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants;

public class Wfs11Constants extends WfsConstants {

  static final String GET_CAPABILITES = "GetCapabilities";

  static final String DESCRIBE_FEATURE_TYPE = "DescribeFeatureType";

  public static final String GET_FEATURE = "GetFeature";

  public static final String VERSION_1_1_0 = "1.1.0";

  public static final String GML_3_1_1_NAMESPACE = "http://www.opengis.net/gml";

  public static final String WFS_NAMESPACE = "http://www.opengis.net/wfs";

  private static final QName LINEAR_RING = new QName(GML_3_1_1_NAMESPACE, "LinearRing");

  public static final QName POLYGON = new QName(GML_3_1_1_NAMESPACE, "Polygon");

  private static final QName ENVELOPE = new QName(GML_3_1_1_NAMESPACE, "Envelope");

  public static final QName LINESTRING = new QName(GML_3_1_1_NAMESPACE, "LineString");

  private static final QName LINESTRING_MEMBER = new QName(GML_3_1_1_NAMESPACE, "lineStringMember");

  public static final QName POINT = new QName(GML_3_1_1_NAMESPACE, "Point");

  private static final QName POINT_MEMBER = new QName(GML_3_1_1_NAMESPACE, "pointMember");

  public static final QName GEOMETRY_COLLECTION =
      new QName(GML_3_1_1_NAMESPACE, "GeometryCollection");

  public static final QName MULTI_POINT = new QName(GML_3_1_1_NAMESPACE, "MultiPoint");

  public static final QName MULTI_LINESTRING = new QName(GML_3_1_1_NAMESPACE, "MultiLineString");

  public static final QName MULTI_POLYGON = new QName(GML_3_1_1_NAMESPACE, "MultiPolygon");

  /**
   * This is a list of sort-by attribute names that were removed from the query because they are
   * known to be unsupported by the source. In practice, this value will be suffixed with
   * ".SOURCE_ID".
   */
  public static final String UNSUPPORTED_SORT_BY_REMOVED = "unsupported-sort-by-removed";

  public static List<QName> wktOperandsAsList() {
    return Arrays.asList(
        LINEAR_RING,
        POLYGON,
        ENVELOPE,
        LINESTRING,
        LINESTRING_MEMBER,
        POINT,
        POINT_MEMBER,
        MULTI_POINT,
        MULTI_LINESTRING,
        MULTI_POLYGON,
        GEOMETRY_COLLECTION);
  }

  public enum SPATIAL_OPERATORS {
    BBOX("BBOX"),
    BEYOND("Beyond"),
    CONTAINS("Contains"),
    CROSSES("Crosses"),
    DISJOINT("Disjoint"),
    DWITHIN("DWithin"),
    INTERSECTS("Intersects"),
    EQUALS("Equals"),
    OVERLAPS("Overlaps"),
    TOUCHES("Touches"),
    WITHIN("Within");

    private String value;

    SPATIAL_OPERATORS(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}
