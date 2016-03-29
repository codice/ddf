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

import org.codice.ddf.libs.geo.GeoFormatException;

public class GeospatialUtil {
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
}
