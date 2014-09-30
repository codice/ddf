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
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * Parses OWS Bounding Box geometry XML and converts it to WKT.
 * 
 * Bounding Box XML is of the form:
 * 
 * <pre>
 *  {@code
 *  <ows:BoundingBox crs="urn:x-ogc:def:crs:EPSG:6.11:4326">
 *     <ows:LowerCorner>60.042 13.754</ows:LowerCorner>
 *     <ows:UpperCorner>68.410 17.920</ows:UpperCorner>
 *  </ows:BoundingBox>
 * }
 * </pre>
 * 
 * @author rodgersh
 * 
 */
public class BoundingBoxReader {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(BoundingBoxReader.class);

    private static final String SPACE = " ";

    private HierarchicalStreamReader reader;
    
    private boolean isLonLatOrder;

    public BoundingBoxReader(HierarchicalStreamReader reader, boolean isLonLatOrder) {
        this.reader = reader;
        this.isLonLatOrder = isLonLatOrder;
    }

    public String getWkt() {
        String wkt = null;

        // reader should initially be positioned at <ows:BoundingBox> element
        LOGGER.debug("Initial node name = {}", reader.getNodeName());
        if (!reader.getNodeName().contains("BoundingBox"))
            return null;

        String crs = reader.getAttribute("crs");
        // TODO: CRS value determines order of bounding box values, i.e.,
        // WGS-84 specifies lon,lat order
        // EPSG:4326 specifies lat,lon order
        // For now we assume lat,lon order
        LOGGER.debug("crs = {}", crs);

        // Move down to the first child node of <BoundingBox>, which should
        // be the <LowerCorner> tag
        reader.moveDown();

        // Parse LowerCorner position from bounding box
        String[] lowerCornerPosition = null;
        if (reader.getNodeName().contains("LowerCorner")) {
            String value = reader.getValue();
            lowerCornerPosition = getCoordinates(value, isLonLatOrder);
        }

        // Move back up to the <BoundingBox> parent tag
        reader.moveUp();

        // Move down to the next child node of the <BoundingBox> tag, which
        // should be the <UpperCorner> tag
        reader.moveDown();

        // Parse UpperCorner position from bounding box
        String[] upperCornerPosition = null;
        if (reader.getNodeName().contains("UpperCorner")) {
            String value = reader.getValue();
              upperCornerPosition = getCoordinates(value, isLonLatOrder);
        }

        // If both corner positions parsed, then compute other 2 corner
        // positions of the
        // bounding box and create the WKT for the bounding box.
        if (lowerCornerPosition != null && lowerCornerPosition.length == 2
                && upperCornerPosition != null && upperCornerPosition.length == 2) {

            /**
             * The WKT created here need to be in LON/LAT. This is the order that we store WKT in
             * the metacard location field.
             */
            wkt = createWkt(lowerCornerPosition, upperCornerPosition);
        }

        // Move position back up to the parent <BoundingBox> tag, where we
        // started
        reader.moveUp();

        LOGGER.debug("Returning WKT in LON/LAT coord order: {}.", wkt);
        return wkt;
    }

    /**
     * We want to create WKT in LON/LAT order.
     * 
     * Points for bounding box will be computed starting with the lower corner, and proceeding in a
     * clockwise rotation. So lower corner point would be point 1, upper corner point would be point
     * 3, and points 2 and 4 would be computed.
     */
    private String createWkt(String[] lowerCornerPosition, String[] upperCornerPosition) {
        LOGGER.debug("Creating WKT in LON/LAT coordinate order.");
        if (upperCornerPosition[0].equals(lowerCornerPosition[0]) &&
                upperCornerPosition[1].equals(lowerCornerPosition[1])) {
            return "POINT(" + lowerCornerPosition[0] + SPACE + lowerCornerPosition[1] +")";
        }
        return "POLYGON((" + lowerCornerPosition[0] + SPACE + lowerCornerPosition[1]
                + ", " + upperCornerPosition[0] + SPACE + lowerCornerPosition[1] + ", "
                + upperCornerPosition[0] + SPACE + upperCornerPosition[1] + ", "
                + lowerCornerPosition[0] + SPACE + upperCornerPosition[1] + ", "
                + lowerCornerPosition[0] + SPACE + lowerCornerPosition[1] + "))";
    }

    /**
     * @param coords
     *            The latitude and longitude coordinates (in no particular order).
     * @param areCoordsInLonLatOrder
     *            True if the source returns the coordinates in LON/LAT order; false, otherwise.
     * @return The coordinates in LON/LAT order.
     */
    private String[] getCoordinates(String coords, boolean areCoordsInLonLatOrder) {

        if (!areCoordsInLonLatOrder) {
            /**
             * We want to create WKT in LON/LAT order. Since the response has the coords in LAT/LON
             * order, we need to reverse them.
             */
            return StringUtils.reverseDelimited(coords, SPACE.charAt(0)).split(SPACE);
        } else {
            /**
             * We want to create WKT in LON/LAT order. Since this is the order of the coords in the
             * response, we use them as-is.
             */
            return coords.split(SPACE);
        }
    }
}
