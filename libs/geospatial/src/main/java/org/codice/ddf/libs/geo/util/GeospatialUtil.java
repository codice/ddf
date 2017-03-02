/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.libs.geo.util;

import org.apache.commons.collections.CollectionUtils;
import org.codice.ddf.libs.geo.GeoFormatException;
import org.geotools.factory.Hints;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Convenience methods for performing geospatial conversions.
 */
public class GeospatialUtil {
    public static final String EPSG_4326 = "EPSG:4326";

    public static final String EPSG_4326_URN = "urn:ogc:def:crs:EPSG::4326";

    public static final String LAT_LON_ORDER = "LAT_LON";

    public static final String LON_LAT_ORDER = "LON_LAT";

    private static final Logger LOGGER = LoggerFactory.getLogger(GeospatialUtil.class);

    /**
     * Parses Latitude in the DMS format of DD:MM:SS.S N/S
     *
     * @param dmsLat Degrees Minutes Seconds formatted latitude.
     * @return Latitude in decimal degrees
     */
    public static Double parseDMSLatitudeWithDecimalSeconds(String dmsLat)
            throws GeoFormatException {
        Double lat = null;

        if (dmsLat != null) {
            dmsLat = dmsLat.trim();
            String hemi = dmsLat.substring(dmsLat.length() - 1);

            if (!(hemi.equalsIgnoreCase("N") || hemi.equalsIgnoreCase("S"))) {
                throw new GeoFormatException(String.format(
                        "Unrecognized hemisphere, %s, should be 'N' or 'S'",
                        hemi));
            }

            int hemisphereMult = 1;
            if (hemi.equalsIgnoreCase("s")) {
                hemisphereMult = -1;
            }

            String numberPortion = dmsLat.substring(0, dmsLat.length() - 1);
            if (dmsLat.contains(":")) {
                String[] dmsArr = numberPortion.split(":");

                int degrees = 0;

                try {
                    degrees = Integer.parseInt(dmsArr[0]);
                } catch (NumberFormatException nfe) {
                    throw new GeoFormatException(String.format(
                            "Unable to parse degrees: %s from: %s",
                            dmsArr[0],
                            dmsLat), nfe);
                }

                int minutes = 0;
                double seconds = 0.0;

                if (dmsArr.length >= 2) {
                    try {
                        minutes = Integer.parseInt(dmsArr[1]);
                    } catch (NumberFormatException nfe) {
                        throw new GeoFormatException(String.format(
                                "Unable to parse minutes: %s from: %s",
                                dmsArr[1],
                                dmsLat), nfe);
                    }
                }

                if (dmsArr.length == 3) {
                    try {
                        seconds = Double.parseDouble(dmsArr[2]);
                    } catch (NumberFormatException nfe) {
                        throw new GeoFormatException(String.format(
                                "Unable to parse seconds: %s from: %s",
                                dmsArr[2],
                                dmsLat), nfe);
                    }
                }

                lat = hemisphereMult * (degrees + ((double) minutes / 60) + (seconds / 3600));

                if (lat < -90 || lat > 90) {
                    throw new GeoFormatException(String.format(
                            "Invalid latitude provided (must be between -90 and 90 degrees), converted latitude: %f",
                            lat));
                }
            }
        }

        return lat;
    }

    /**
     * Parses Longitude in the DMS format of [D]DD:MM:SS.S E/W
     *
     * @param dmsLon Degrees Minutes Seconds formatted longitude.
     * @return Longitude in decimal degrees.
     */
    public static Double parseDMSLongitudeWithDecimalSeconds(String dmsLon)
            throws GeoFormatException {
        Double lon = null;

        if (dmsLon != null) {
            dmsLon = dmsLon.trim();
            String hemi = dmsLon.substring(dmsLon.length() - 1);
            int hemisphereMult = 1;

            if (!(hemi.equalsIgnoreCase("W") || hemi.equalsIgnoreCase("E"))) {
                throw new GeoFormatException(String.format(
                        "Unrecognized hemisphere, %s, should be 'E' or 'W'",
                        hemi));
            }

            if (hemi.equalsIgnoreCase("w")) {
                hemisphereMult = -1;
            }

            String numberPortion = dmsLon.substring(0, dmsLon.length() - 1);
            if (dmsLon.contains(":")) {
                String[] dmsArr = numberPortion.split(":");

                int degrees = 0;

                try {
                    degrees = Integer.parseInt(dmsArr[0]);
                } catch (NumberFormatException nfe) {
                    throw new GeoFormatException(String.format(
                            "Unable to parse degrees: %s from: %s",
                            dmsArr[0],
                            dmsLon), nfe);
                }

                int minutes = 0;
                double seconds = 0.0;

                if (dmsArr.length >= 2) {
                    try {
                        minutes = Integer.parseInt(dmsArr[1]);
                    } catch (NumberFormatException nfe) {
                        throw new GeoFormatException(String.format(
                                "Unable to parse minutes: %s from: %s",
                                dmsArr[1],
                                dmsLon), nfe);
                    }
                }

                if (dmsArr.length == 3) {
                    try {
                        seconds = Double.parseDouble(dmsArr[2]);
                    } catch (NumberFormatException nfe) {
                        throw new GeoFormatException(String.format(
                                "Unable to parse seconds: %s from: %s",
                                dmsArr[2],
                                dmsLon), nfe);
                    }
                }

                lon = hemisphereMult * (degrees + ((double) minutes / 60) + (seconds / 3600));

                if (lon < -180 || lon > 180) {
                    throw new GeoFormatException(String.format(
                            "Invalid longitude provided (must be between -180 and 180 degrees), converted longitude: %f",
                            lon));
                }
            }
        }

        return lon;
    }

    /**
     * Transform a geometry to EPSG:4326 format with lon/lat coordinate ordering.
     * NOTE: This method will perform the transform swapping coordinates even if the sourceCrsName is
     * EPSG:4326
     *
     * @param geometry      - Geometry to transform
     * @param sourceCrsName - Source geometry's coordinate reference system
     * @return Geometry - Transformed geometry into EPSG:4326 lon/lat coordinate system
     */
    public static Geometry transformToEPSG4326LonLatFormat(Geometry geometry, String sourceCrsName)
            throws GeoFormatException {
        if (geometry == null) {
            throw new GeoFormatException("Unable to convert null geometry");
        }

        //If we don't have source CRS just return geometry as we can't transform without that information
        if (sourceCrsName == null) {
            return geometry;
        }

        try {
            CoordinateReferenceSystem sourceCrs = CRS.decode(sourceCrsName);
            Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
            CRSAuthorityFactory factory = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG",
                    hints);
            CoordinateReferenceSystem targetCRS =
                    factory.createCoordinateReferenceSystem(EPSG_4326);
            MathTransform transform = CRS.findMathTransform(sourceCrs, targetCRS);
            return JTS.transform(geometry, transform);
        } catch (FactoryException | TransformException e) {
            throw new GeoFormatException("Unable to convert coordinate to " + EPSG_4326, e);
        }
    }

    /**
     * Transform a geometry to EPSG:4326 format with lon/lat coordinate ordering.
     * NOTE: This method will NOT perform the transform swapping coordinates even if the sourceCrsName is
     * EPSG:4326.
     *
     * @param geometry  - Geometry to transform
     * @param sourceCrs - Source geometry's coordinate reference system
     * @return Geometry - Transformed geometry into EPSG:4326 lon/lat coordinate system
     */
    public static Geometry transformToEPSG4326LonLatFormat(Geometry geometry,
            CoordinateReferenceSystem sourceCrs) throws GeoFormatException {

        if (geometry == null) {
            throw new GeoFormatException("Unable to convert null geometry");
        }

        //If we don't have source CRS just return geometry as we can't transform without that information
        if (sourceCrs == null || CollectionUtils.isEmpty(sourceCrs.getIdentifiers())) {
            return geometry;
        }

        Geometry transformedGeometry = geometry;
        try {
            boolean sourceCrsMatchesTarget = false;

            for (ReferenceIdentifier referenceIdentifier : sourceCrs.getIdentifiers()) {
                if (referenceIdentifier.toString()
                        .equalsIgnoreCase(EPSG_4326)) {
                    sourceCrsMatchesTarget = true;
                    break;
                }
            }

            if (!sourceCrsMatchesTarget) {
                Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
                CRSAuthorityFactory factory =
                        ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG", hints);
                CoordinateReferenceSystem targetCRS = factory.createCoordinateReferenceSystem(
                        EPSG_4326);

                MathTransform transform = CRS.findMathTransform(sourceCrs, targetCRS);
                transformedGeometry = JTS.transform(geometry, transform);
                LOGGER.debug("Converted CRS {} into {} : {}", sourceCrs, EPSG_4326, geometry);
            }
        } catch (FactoryException | TransformException e) {
            throw new GeoFormatException("Unable to convert coordinate to " + EPSG_4326, e);
        }
        return transformedGeometry;
    }
}
