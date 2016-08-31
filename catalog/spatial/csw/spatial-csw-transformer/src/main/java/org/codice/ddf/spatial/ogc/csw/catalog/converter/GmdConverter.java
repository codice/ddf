/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import java.net.URI;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GmdConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.io.path.Path;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;

public class GmdConverter extends AbstractGmdConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GmdConverter.class);

    @Override
    protected List<String> getXstreamAliases() {
        return Arrays.asList(GmdConstants.GMD_LOCAL_NAME, GmdConstants.GMD_METACARD_TYPE_NAME);
    }

    @Override
    protected XstreamPathValueTracker buildPaths(MetacardImpl metacard) {

        XstreamPathValueTracker pathValueTracker = new XstreamPathValueTracker();

        pathValueTracker.add(new Path("/MD_Metadata/@xmlns"), GmdConstants.GMD_NAMESPACE);

        pathValueTracker.add(new Path("/MD_Metadata/@xmlns:" + GmdConstants.GCO_PREFIX),
                GmdConstants.GCO_NAMESPACE);
        pathValueTracker.add(new Path(GmdConstants.FILE_IDENTIFIER_PATH), metacard.getId());

        pathValueTracker.add(new Path(GmdConstants.CODE_LIST_VALUE_PATH),
                StringUtils.defaultIfEmpty(metacard.getContentTypeName(), "dataset"));
        pathValueTracker.add(new Path(GmdConstants.CODE_LIST_PATH), GmdConstants.METACARD_URI);

        pathValueTracker.add(new Path(GmdConstants.CONTACT_PATH), (String) null);

        GregorianCalendar modifiedCal = new GregorianCalendar();
        if (metacard.getModifiedDate() != null) {

            modifiedCal.setTime(metacard.getModifiedDate());
        }
        modifiedCal.setTimeZone(UTC_TIME_ZONE);

        pathValueTracker.add(new Path(GmdConstants.DATE_TIME_STAMP_PATH),
                XSD_FACTORY.newXMLGregorianCalendar(modifiedCal)
                        .toXMLFormat());

        addIdentificationInfo(metacard, pathValueTracker);

        addDistributionInfo(metacard, pathValueTracker);

        return pathValueTracker;

    }

    @Override
    protected String getRootNodeName() {
        return GmdConstants.GMD_LOCAL_NAME;
    }

    protected void addDistributionInfo(MetacardImpl metacard,
            XstreamPathValueTracker pathValueTracker) {

        String resourceUrl = null;
        Attribute downloadUrlAttr = metacard.getAttribute(Metacard.RESOURCE_DOWNLOAD_URL);
        if (downloadUrlAttr != null) {
            resourceUrl = (String) downloadUrlAttr.getValue();
        }

        if (StringUtils.isNotBlank(resourceUrl)) {
            pathValueTracker.add(new Path(GmdConstants.LINKAGE_URI_PATH), resourceUrl);
        } else {
            URI resourceUri = metacard.getResourceURI();
            if (resourceUri != null) {

                pathValueTracker.add(new Path(GmdConstants.LINKAGE_URI_PATH),
                        resourceUri.toASCIIString());
            }
        }

    }

    protected void addIdentificationInfo(MetacardImpl metacard,
            XstreamPathValueTracker pathValueTracker) {

        pathValueTracker.add(new Path(GmdConstants.TITLE_PATH),
                StringUtils.defaultString(metacard.getTitle()));

        GregorianCalendar createdCal = new GregorianCalendar();

        if (metacard.getCreatedDate() != null) {

            createdCal.setTime(metacard.getCreatedDate());
        }
        createdCal.setTimeZone(UTC_TIME_ZONE);
        pathValueTracker.add(new Path(GmdConstants.CREATED_DATE_PATH),
                XSD_FACTORY.newXMLGregorianCalendar(createdCal)
                        .toXMLFormat());

        pathValueTracker.add(new Path(GmdConstants.CREATED_DATE_TYPE_CODE_PATH),
                GmdConstants.METACARD_URI);
        pathValueTracker.add(new Path(GmdConstants.CREATED_DATE_TYPE_CODE_VALUE_PATH),
                Metacard.CREATED);

        pathValueTracker.add(new Path(GmdConstants.ABSTRACT_PATH),
                StringUtils.defaultString(metacard.getDescription()));
        pathValueTracker.add(new Path(GmdConstants.POINT_OF_CONTACT_PATH),
                StringUtils.defaultString(metacard.getPointOfContact()));

        pathValueTracker.add(new Path(GmdConstants.POINT_OF_CONTACT_ROLE_PATH), (String) null);

        pathValueTracker.add(new Path(GmdConstants.LANGUAGE_PATH),
                StringUtils.defaultIfEmpty(Locale.getDefault()
                        .getLanguage(), Locale.ENGLISH.getLanguage()));

        addExtent(metacard, pathValueTracker);

    }

    protected void addExtent(MetacardImpl metacard, XstreamPathValueTracker pathValueTracker) {

        String wkt = metacard.getLocation();
        if (StringUtils.isNotBlank(wkt)) {
            WKTReader reader = new WKTReader();
            Geometry geometry = null;

            try {
                geometry = reader.read(wkt);
            } catch (ParseException e) {
                LOGGER.debug("Unable to parse geometry {}", wkt, e);
            }

            if (geometry != null) {
                Envelope bounds = geometry.getEnvelopeInternal();
                String westLon = Double.toString(bounds.getMinX());
                String eastLon = Double.toString(bounds.getMaxX());
                String southLat = Double.toString(bounds.getMinY());
                String northLat = Double.toString(bounds.getMaxY());
                pathValueTracker.add(new Path(GmdConstants.BBOX_WEST_LON_PATH), westLon);
                pathValueTracker.add(new Path(GmdConstants.BBOX_EAST_LON_PATH), eastLon);
                pathValueTracker.add(new Path(GmdConstants.BBOX_SOUTH_LAT_PATH), southLat);
                pathValueTracker.add(new Path(GmdConstants.BBOX_NORTH_LAT_PATH), northLat);

            }
        }
    }
}