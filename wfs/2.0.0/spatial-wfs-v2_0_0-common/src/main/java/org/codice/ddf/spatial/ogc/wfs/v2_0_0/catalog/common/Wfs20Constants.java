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
package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common;

import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants;

/**
 * Constants for WFS services and requests.
 * 
 */
public class Wfs20Constants extends WfsConstants{

    /* Request Names */
    public static final String GET_CAPABILITIES = "GetCapabilities";
    
    public static final String GET_PROPERTY_VALUE = "GetPropertyValue";

    public static final String DESCRIBE_FEATURE_TYPE = "DescribeFeatureType";

    public static final String GET_FEATURE = "GetFeature";

    /* Version Numbers */
    public static final String VERSION_2_0_0 = "2.0.0";

    /* Service Names */

    public static enum SPATIAL_OPERATORS {
        BBOX("BBOX"), Beyond("Beyond"), Contains("Contains"), Crosses("Crosses"), Disjoint(
                "Disjoint"), DWithin("DWithin"), Intersects("Intersects"), Equals("Equals"), Overlaps(
                "Overlaps"), Touches("Touches"), Within("Within");

        private String value;

        public String getValue() {
            return value;
        }

        SPATIAL_OPERATORS(String value) {
            this.value = value;
        }
    }

    public static enum CONFORMANCE_CONSTRAINTS {
        ImplementsQuery("ImplementsQuery"), ImplementsAdHocQuery("ImplementsAdHocQuery"), ImplementsFunctions(
                "ImplementsFunctions"), ImplementsResourceld("ImplementsResourceld"), ImplementsMinStandardFilter(
                "ImplementsMinStandardFilter"), ImplementsStandardFilter("ImplementsStandardFilter"), ImplementsMinSpatialFilter(
                "ImplementsMinSpatialFilter"), ImplementsSpatialFilter("ImplementsSpatialFilter"), ImplementsMinTemporalFilter(
                "ImplementsMinTemporalFilter"), ImplementsTemporalFilter("ImplementsTemporalFilter"), ImplementsVersionNav(
                "ImplementsVersionNav"), ImplementsSorting("ImplementsSorting"), ImplementsExtendedOperators(
                "ImplementsExtendedOperators"), ImplementsMinimumXPath("ImplementsMinimumXPath"), ImplementsSchemaElementFunc(
                "ImplementsSchemaElementFunc"), ImplementsResourceId("ImplementsResourceId");

        private String value;

        public String getValue() {
            return value;
        }

        CONFORMANCE_CONSTRAINTS(String value) {
            this.value = value;
        }
    }

    public static enum COMPARISON_OPERATORS {
        PropertyIsEqualTo("PropertyIsEqualTo"), PropertyIsNotEqualTo("PropertyIsNotEqualTo"), PropertyIsLessThan(
                "PropertyIsLessThan"), PropertyIsGreaterThan("PropertyIsGreaterThan"), PropertyIsLessThanOrEqualTo(
                "PropertyIsLessThanOrEqualTo"), PropertyIsGreaterThanOrEqualTo(
                "PropertyIsGreaterThanOrEqualTo"), PropertyIsLike("PropertyIsLike"), PropertyIsNull(
                "PropertyIsNull"), PropertyIsNil("PropertyIsNil"), PropertyIsBetween(
                "PropertyIsBetween");

        private String value;

        public String getValue() {
            return value;
        }

        COMPARISON_OPERATORS(String value) {
            this.value = value;
        }
    }

    public static enum TEMPORAL_OPERATORS {
        After("After"), Before("Before"), Begins("Begins"), BegunBy("BegunBy"), TContains(
                "TContains"), During("During"), TEquals("TEquals"), TOverlaps("TOverlaps"), Meets(
                "Meets"), OverlappedBy("OverlappedBy"), MetBy("MetBy"), Ends("Ends"), EndedBy(
                "EndedBy");

        private String value;

        public String getValue() {
            return value;
        }

        TEMPORAL_OPERATORS(String value) {
            this.value = value;
        }
    }

    /* Namespaces */


    public static final String WFS_2_0_NAMESPACE = "http://www.opengis.net/wfs/2.0";

    public static final String GML_2_1_2_NAMESPACE = "http://www.opengis.net/gml";

    public static final String GML_3_2_NAMESPACE = "http://www.opengis.net/gml/3.2";

    public static final String WFS_SCHEMA_LOCATION = "/ogc/wfs/2.0.0/wfs.xsd";
    
    public static final String OGC_WFS_PACKAGE = "net.opengis.wfs.v_2_0_0";

    public static final String OGC_FILTER_PACKAGE = "net.opengis.filter.v_2_0_0";

    public static final String OGC_GML_PACKAGE = "net.opengis.gml.v_3_2_0";

    public static final String OGC_OWS_PACKAGE = "net.opengis.ows.v_1_1_0";
    
    public static final String TIME_PERIOD = "TimePeriod";
    
    public static final String TIME_INSTANT = "TimeInstant";

}
