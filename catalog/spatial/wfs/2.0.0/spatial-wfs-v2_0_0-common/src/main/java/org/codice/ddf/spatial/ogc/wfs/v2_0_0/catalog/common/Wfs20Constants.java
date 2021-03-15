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
package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common;

import java.util.Arrays;
import java.util.List;
import javax.xml.namespace.QName;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants;

/** Constants for WFS services and requests. */
public class Wfs20Constants extends WfsConstants {

  public static final String GML_MIME_TYPE = "application/gml+xml";

  /* Request Names */
  public static final String GET_CAPABILITIES = "GetCapabilities";

  public static final String GET_PROPERTY_VALUE = "GetPropertyValue";

  public static final String DESCRIBE_FEATURE_TYPE = "DescribeFeatureType";

  public static final String GET_FEATURE = "GetFeature";

  /* Version Numbers */
  public static final String VERSION_2_0_0 = "2.0.0";

  /* Service Names */

  public static final String WFS_2_0_NAMESPACE = "http://www.opengis.net/wfs/2.0";

  public static final String GML_2_1_2_NAMESPACE = "http://www.opengis.net/gml";

  public static final String GML_3_2_NAMESPACE = "http://www.opengis.net/gml/3.2";

  public static final String WFS_SCHEMA_LOCATION = "/ogc/wfs/2.0.0/wfs.xsd";

  /* Namespaces */

  public static final String OGC_WFS_PACKAGE = "net.opengis.wfs.v_2_0_0";

  public static final String OGC_FILTER_PACKAGE = "net.opengis.filter.v_2_0_0";

  public static final String OGC_GML_PACKAGE = "net.opengis.gml.v_3_2_1";

  public static final String OGC_OWS_PACKAGE = "net.opengis.ows.v_1_1_0";

  public static final String TIME_PERIOD = "TimePeriod";

  public static final String TIME_INSTANT = "TimeInstant";

  public static final QName POLYGON = new QName(GML_3_2_NAMESPACE, "Polygon");

  public static final QName ENVELOPE = new QName(GML_3_2_NAMESPACE, "Envelope");

  public static final QName BOX = new QName(GML_3_2_NAMESPACE, "Box");

  public static final QName LINESTRING = new QName(GML_3_2_NAMESPACE, "LineString");

  public static final QName POINT = new QName(GML_3_2_NAMESPACE, "Point");

  public static final QName GEOMETRY_COLLECTION =
      new QName(GML_3_2_NAMESPACE, "GeometryCollection");

  public static final QName MULTI_POINT = new QName(GML_3_2_NAMESPACE, "MultiPoint");

  public static final QName MULTI_LINE_STRING = new QName(GML_3_2_NAMESPACE, "MultiLineString");

  public static final QName MULTI_POLYGON = new QName(GML_3_2_NAMESPACE, "MultiPolygon");

  private static final String NO_ENUM_MSG = "No enum value ";

  protected static final List<QName> GEOMETRY_QNAMES =
      Arrays.asList(
          POINT,
          ENVELOPE,
          POLYGON,
          LINESTRING,
          GEOMETRY_COLLECTION,
          MULTI_LINE_STRING,
          MULTI_POINT,
          MULTI_POLYGON);

  public enum SPATIAL_OPERATORS {
    BBOX("BBOX"),
    BEYOND("Beyond"),
    CONTAINS("Contains"),
    CROSSES("Crosses"),
    DISJOINT("Disjoint"),
    D_WITHIN("DWithin"),
    INTERSECTS("Intersects"),
    EQUALS("Equals"),
    OVERLAPS("Overlaps"),
    TOUCHES("Touches"),
    WITHIN("Within");

    private final String value;

    SPATIAL_OPERATORS(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return value;
    }

    public static SPATIAL_OPERATORS getEnum(String value) {
      return Arrays.stream(values())
          .filter(spatialOperators -> spatialOperators.getValue().equals(value))
          .findAny()
          .orElseThrow(() -> new IllegalArgumentException(NO_ENUM_MSG + value));
    }
  }

  public enum CONFORMANCE_CONSTRAINTS {
    IMPLEMENTS_QUERY("ImplementsQuery"),
    IMPLEMENTS_AD_HOC_QUERY("ImplementsAdHocQuery"),
    IMPLEMENTS_FUNCTIONS("ImplementsFunctions"),
    IMPLEMENTS_RESOURCE_ID("ImplementsResourceId"),
    IMPLEMENTS_MIN_STANDARD_FILTER("ImplementsMinStandardFilter"),
    IMPLEMENTS_STANDARD_FILTER("ImplementsStandardFilter"),
    IMPLEMENTS_MIN_SPATIAL_FILTER("ImplementsMinSpatialFilter"),
    IMPLEMENTS_SPATIAL_FILTER("ImplementsSpatialFilter"),
    IMPLEMENTS_MIN_TEMPORAL_FILTER("ImplementsMinTemporalFilter"),
    IMPLEMENTS_TEMPORAL_FILTER("ImplementsTemporalFilter"),
    IMPLEMENTS_VERSION_NAV("ImplementsVersionNav"),
    IMPLEMENTS_SORTING("ImplementsSorting"),
    IMPLEMENTS_EXTENDED_OPERATORS("ImplementsExtendedOperators"),
    IMPLEMENTS_MINIMUM_X_PATH("ImplementsMinimumXPath"),
    IMPLEMENTS_SCHEMA_ELEMENT_FUNC("ImplementsSchemaElementFunc");

    private final String value;

    CONFORMANCE_CONSTRAINTS(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return value;
    }

    public static CONFORMANCE_CONSTRAINTS getEnum(String value) {
      return Arrays.stream(values())
          .filter(conformanceConstraints -> conformanceConstraints.getValue().equals(value))
          .findAny()
          .orElseThrow(() -> new IllegalArgumentException(NO_ENUM_MSG + value));
    }
  }

  public enum COMPARISON_OPERATORS {
    PROPERTY_IS_EQUAL_TO("PropertyIsEqualTo"),
    PROPERTY_IS_NOT_EQUAL_TO("PropertyIsNotEqualTo"),
    PROPERTY_IS_LESS_THAN("PropertyIsLessThan"),
    PROPERTY_IS_GREATER_THAN("PropertyIsGreaterThan"),
    PROPERTY_IS_LESS_THAN_OR_EQUAL_TO("PropertyIsLessThanOrEqualTo"),
    PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO("PropertyIsGreaterThanOrEqualTo"),
    PROPERTY_IS_LIKE("PropertyIsLike"),
    PROPERTY_IS_NULL("PropertyIsNull"),
    PROPERTY_IS_NIL("PropertyIsNil"),
    PROPERTY_IS_BETWEEN("PropertyIsBetween");

    private final String value;

    COMPARISON_OPERATORS(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return value;
    }

    public static COMPARISON_OPERATORS getEnum(String value) {
      return Arrays.stream(values())
          .filter(comparisonOperators -> comparisonOperators.getValue().equals(value))
          .findAny()
          .orElseThrow(() -> new IllegalArgumentException(NO_ENUM_MSG + value));
    }
  }

  public enum TEMPORAL_OPERATORS {
    AFTER("After"),
    BEFORE("Before"),
    BEGINS("Begins"),
    BEGUN_BY("BegunBy"),
    T_CONTAINS("TContains"),
    DURING("During"),
    T_EQUALS("TEquals"),
    T_OVERLAPS("TOverlaps"),
    MEETS("Meets"),
    OVERLAPPED_BY("OverlappedBy"),
    MET_BY("MetBy"),
    ENDS("Ends"),
    ENDED_BY("EndedBy");

    private final String value;

    TEMPORAL_OPERATORS(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return value;
    }

    public static TEMPORAL_OPERATORS getEnum(String value) {
      return Arrays.stream(values())
          .filter(temporalOperators -> temporalOperators.getValue().equals(value))
          .findAny()
          .orElseThrow(() -> new IllegalArgumentException(NO_ENUM_MSG + value));
    }
  }
}
