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

/**
 * Constants for WFS services and requests.
 * 
 */
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
    

    public static final String WFS_NAMESPACE = "http://www.opengis.net/wfs";

    public static final String GML_NAMESPACE = "http://www.opengis.net/gml";

    public static final String GML_SCHEMA_LOCATION = "/ogc/gml/2.1.2/feature.xsd";

    public static final String WFS_SCHEMA_LOCATION = "/ogc/wfs/1.0.0/WFS-capabilities.xsd";

}
