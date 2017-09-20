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

  public static final List<QName> GEOMETRY_QNAMES =
      Arrays.asList(
          POINT,
          ENVELOPE,
          POLYGON,
          LINESTRING,
          GEOMETRY_COLLECTION,
          MULTI_LINE_STRING,
          MULTI_POINT,
          MULTI_POLYGON);

  public static enum SPATIAL_OPERATORS {
    BBOX("BBOX"),
    Beyond("Beyond"),
    Contains("Contains"),
    Crosses("Crosses"),
    Disjoint("Disjoint"),
    DWithin("DWithin"),
    Intersects("Intersects"),
    Equals("Equals"),
    Overlaps("Overlaps"),
    Touches("Touches"),
    Within("Within");

    private String value;

    SPATIAL_OPERATORS(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  public static enum CONFORMANCE_CONSTRAINTS {
    ImplementsQuery("ImplementsQuery"),
    ImplementsAdHocQuery("ImplementsAdHocQuery"),
    ImplementsFunctions("ImplementsFunctions"),
    ImplementsResourceld("ImplementsResourceld"),
    ImplementsMinStandardFilter("ImplementsMinStandardFilter"),
    ImplementsStandardFilter("ImplementsStandardFilter"),
    ImplementsMinSpatialFilter("ImplementsMinSpatialFilter"),
    ImplementsSpatialFilter("ImplementsSpatialFilter"),
    ImplementsMinTemporalFilter("ImplementsMinTemporalFilter"),
    ImplementsTemporalFilter("ImplementsTemporalFilter"),
    ImplementsVersionNav("ImplementsVersionNav"),
    ImplementsSorting("ImplementsSorting"),
    ImplementsExtendedOperators("ImplementsExtendedOperators"),
    ImplementsMinimumXPath("ImplementsMinimumXPath"),
    ImplementsSchemaElementFunc("ImplementsSchemaElementFunc"),
    ImplementsResourceId("ImplementsResourceId");

    private String value;

    CONFORMANCE_CONSTRAINTS(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  public static enum COMPARISON_OPERATORS {
    PropertyIsEqualTo("PropertyIsEqualTo"),
    PropertyIsNotEqualTo("PropertyIsNotEqualTo"),
    PropertyIsLessThan("PropertyIsLessThan"),
    PropertyIsGreaterThan("PropertyIsGreaterThan"),
    PropertyIsLessThanOrEqualTo("PropertyIsLessThanOrEqualTo"),
    PropertyIsGreaterThanOrEqualTo("PropertyIsGreaterThanOrEqualTo"),
    PropertyIsLike("PropertyIsLike"),
    PropertyIsNull("PropertyIsNull"),
    PropertyIsNil("PropertyIsNil"),
    PropertyIsBetween("PropertyIsBetween");

    private String value;

    COMPARISON_OPERATORS(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  public static enum TEMPORAL_OPERATORS {
    After("After"),
    Before("Before"),
    Begins("Begins"),
    BegunBy("BegunBy"),
    TContains("TContains"),
    During("During"),
    TEquals("TEquals"),
    TOverlaps("TOverlaps"),
    Meets("Meets"),
    OverlappedBy("OverlappedBy"),
    MetBy("MetBy"),
    Ends("Ends"),
    EndedBy("EndedBy");

    private String value;

    TEMPORAL_OPERATORS(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}
