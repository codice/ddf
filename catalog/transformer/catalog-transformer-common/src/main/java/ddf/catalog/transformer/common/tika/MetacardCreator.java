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
package ddf.catalog.transformer.common.tika;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;

/**
 * Creates {@link Metacard}s from Tika {@link Metadata} objects.
 */
public class MetacardCreator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardCreator.class);

    /**
     * A convenience method for creating a new {@link Metacard} of type
     * {@link BasicTypes#BASIC_METACARD} from a {@link Metadata} object.
     *
     * @param metadata    the {@code Metadata} object containing the metadata relevant to the
     *                    metacard, must not be null
     * @param id          the value for the {@link Metacard#ID} attribute that should be set in the
     *                    generated {@code Metacard}, may be null
     * @param metadataXml the XML for the {@link Metacard#METADATA} attribute that should be set in
     *                    the generated {@code Metacard}, may be null
     * @return a new {@code Metacard}
     */
    public static Metacard createBasicMetacard(final Metadata metadata, final String id,
            final String metadataXml) {
        final Metacard metacard = new MetacardImpl(BasicTypes.BASIC_METACARD);

        final String contentType = metadata.get(Metadata.CONTENT_TYPE);
        if (StringUtils.isNotBlank(contentType)) {
            metacard.setAttribute(new AttributeImpl(Metacard.CONTENT_TYPE, contentType));
        }

        final String title = metadata.get(TikaCoreProperties.TITLE);
        if (StringUtils.isNotBlank(title)) {
            metacard.setAttribute(new AttributeImpl(Metacard.TITLE, title));
        }

        final String createdDateStr = metadata.get(TikaCoreProperties.CREATED);
        final Date createdDate = convertDate(createdDateStr);
        if (createdDate != null) {
            metacard.setAttribute(new AttributeImpl(Metacard.CREATED, createdDate));
        }

        final String modifiedDateStr = metadata.get(TikaCoreProperties.MODIFIED);
        final Date modifiedDate = convertDate(modifiedDateStr);
        if (modifiedDate != null) {
            metacard.setAttribute(new AttributeImpl(Metacard.MODIFIED, modifiedDate));
        }

        if (StringUtils.isNotBlank(id)) {
            metacard.setAttribute(new AttributeImpl(Metacard.ID, id));
        }

        if (StringUtils.isNotBlank(metadataXml)) {
            metacard.setAttribute(new AttributeImpl(Metacard.METADATA, metadataXml));
        }

        final String lat = metadata.get(Metadata.LATITUDE);
        final String lon = metadata.get(Metadata.LONGITUDE);
        final String wkt = toWkt(lon, lat);
        if (StringUtils.isNotBlank(wkt)) {
            metacard.setAttribute(new AttributeImpl(Metacard.GEOGRAPHY, wkt));
        }

        return metacard;
    }

    private static String toWkt(final String lon, final String lat) {
        if (StringUtils.isBlank(lon) || StringUtils.isBlank(lat)) {
            return null;
        }

        final String wkt = String.format("POINT(%s %s)", lon, lat);
        LOGGER.debug("wkt: {}", wkt);
        return wkt;
    }

    private static Date convertDate(final String dateStr) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }

        return javax.xml.bind.DatatypeConverter.parseDateTime(dateStr)
                .getTime();
    }
}
