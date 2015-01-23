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
package org.codice.ddf.spatial.ogc.wfs.catalog.common;

/**
 * Constants for WFS services and requests.
 * 
 */
public class WfsConstants {

    /* Service Names */
    public static final String WFS = "WFS";

    /* XML Encoded Filter Constsants */
    public static final String ESCAPE = "!";

    public static final String SINGLE_CHAR = "#";

    public static final String WILD_CARD = "*";

    public static final String METERS = "METERS";

    public static final String EPSG_4326 = "EPSG:4326";
    
    public static final String EPSG_4326_URN = "urn:ogc:def:crs:EPSG::4326";

    /* Namespaces */
    
    public static final String XSI_PREFIX = "xsi";

    public static final String WFS_NAMESPACE_PREFIX = "wfs";

    public static final String GML_PREFIX = "gml";

    public static final String ATTRIBUTE_SCHEMA_LOCATION = "xsi:schemaLocation";
    
    public static final String NAMESPACE_URN_ROOT = "urn:ddf.catalog.gml.";
    
    public static final double DEGREES_TO_RADIANS = Math.PI / 180.0;

    public static final double RADIANS_TO_DEGREES = 1 / DEGREES_TO_RADIANS;

    public static final double EARTH_MEAN_RADIUS_METERS = 6371008.7714;

    public static final String LAT_LON_ORDER = "LAT_LON";
    
    public static final String LON_LAT_ORDER = "LON_LAT";
    
    public static final String B = "B";
    
    public static final String KB = "KB";
    
    public static final String MB = "MB";
    
    public static final String GB = "GB";
    
    public static final String TB = "TB";
    
    public static final String PB = "PB";
    
    public static final String BYTES_PER_KB = "1024";
    
    public static final String BYTES_PER_MB = "1048576";
    
    public static final String BYTES_PER_GB = "1073741824";
    
    public static final String BYTES_PER_TB = "1099511627776";
    
    public static final String BYTES_PER_PB = "1125899906842624";

}
