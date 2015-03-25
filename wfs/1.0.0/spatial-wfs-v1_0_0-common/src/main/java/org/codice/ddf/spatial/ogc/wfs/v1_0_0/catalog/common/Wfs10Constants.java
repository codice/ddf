/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common;

import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.List;

public class Wfs10Constants extends WfsConstants {

    /* Request Names */
    public static final String GET_CAPABILITES = "GetCapabilities";

    public static final String DESCRIBE_FEATURE_TYPE = "DescribeFeatureType";

    public static final String GET_FEATURE = "GetFeature";

    /* Version Numbers */
    public static final String VERSION_1_0_0 = "1.0.0";

    /* Service Names */

    public static enum SPATIAL_OPERATORS {
        BBOX("BBOX"), Beyond("Beyond"), Contains("Contains"), Crosses("Crosses"), Disjoint(
                "Disjoint"), DWithin("DWithin"), Intersect("Intersect"), Equals("Equals"), Overlaps(
                "Overlaps"), Touches("Touches"), Within("Within");

        private String value;

        public String getValue() {
            return value;
        }

        SPATIAL_OPERATORS(String value) {
            this.value = value;
        }
    }

    /* Namespaces */
    public static final String GML_2_1_2_NAMESPACE = "http://www.opengis.net/gml";

    public static final String GML_3_2_NAMESPACE = "http://www.opengis.net/gml/3.2";

    public static final String WFS_NAMESPACE = "http://www.opengis.net/wfs";

    public static final String GML_NAMESPACE = "http://www.opengis.net/gml";

    public static final String GML_SCHEMA_LOCATION = "/ogc/gml/2.1.2/feature.xsd";

    public static final String WFS_SCHEMA_LOCATION = "/ogc/wfs/1.0.0/WFS-capabilities.xsd";

    public static final QName LINEAR_RING = new QName(GML_NAMESPACE, "LinearRing");

    public static final QName POLYGON = new QName(GML_3_2_NAMESPACE, "Polygon");

    public static final QName ENVELOPE = new QName(GML_3_2_NAMESPACE, "Envelope");

    public static final QName BOX = new QName(GML_3_2_NAMESPACE, "Box");

    public static final QName LINESTRING = new QName(GML_3_2_NAMESPACE, "LineString");

    public static final QName LINESTRING_MEMBER = new QName(GML_NAMESPACE, "lineStringMember");

    public static final QName POINT = new QName(GML_3_2_NAMESPACE, "Point");

    public static final QName POINT_MEMBER = new QName(GML_NAMESPACE, "pointMember");

    public static final QName GEOMETRY_COLLECTION = new QName(GML_3_2_NAMESPACE,
            "GeometryCollection");

    public static final QName MULTI_POINT = new QName(GML_3_2_NAMESPACE, "MultiPoint");

    public static final QName MULTI_LINESTRING = new QName(GML_3_2_NAMESPACE, "MultiLineString");

    public static final QName MULTI_POLYGON = new QName(GML_3_2_NAMESPACE, "MultiPolygon");

    public static enum COMPARISON_OPERATORS {
        Simple_Comparison("Simple_Comparison"),
        LessThan("LessThan"),
        GreaterThan("GreaterThan"),
        LessThanEqualTo("LessThanEqualTo"),
        GreaterThanEqualTo("GreaterThanEqualTo"),
        EqualTo("EqualTo"),
        NotEqualTo("NotEqualTo"),
        Like("Like"),
        Between("Between"),
        NullCheck("NullCheck");

        private String value;

        public String getValue() {
            return value;
        }

        COMPARISON_OPERATORS(String value) {
            this.value = value;
        }
    }

    public static List<QName> wktOperandsAsList() {
        return Arrays.asList(LINEAR_RING, POLYGON, ENVELOPE, LINESTRING, POINT, MULTI_POINT, MULTI_LINESTRING, MULTI_POLYGON, GEOMETRY_COLLECTION);
    }
}
