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
package org.codice.ddf.spatial.ogc.csw.catalog.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GmdMetacardType;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswUnmarshallHelper;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.XstreamPathConverter;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.XstreamPathValueTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.path.Path;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.WKTWriter;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;

public class GmdTransformer implements InputTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GmdTransformer.class);

    private static final String COLON = ":";

    private static final GmdMetacardType GMD_METACARD_TYPE = new GmdMetacardType();

    private static final Path FILE_IDENTIFIER_PATH = new Path(
            "/MD_Metadata/fileIdentifier/CharacterString");

    private static final Path DATE_STAMP_PATH = new Path("/MD_Metadata/dateStamp/Date");

    private static final Path CODE_LIST_VALUE_PATH = new Path(
            "/MD_Metadata/hierarchyLevel/MD_ScopeCode/@codeListValue");

    private static final Path CRS_AUTHORITY_PATH = new Path(
            "/MD_Metadata/referenceSystemInfo/MD_ReferenceSystem/referenceSystemIdentifier/RS_Identifier/codeSpace/CharacterString");

    private static final Path CRS_VERSION_PATH = new Path(
            "/MD_Metadata/referenceSystemInfo/MD_ReferenceSystem/referenceSystemIdentifier/RS_Identifier/version");

    private static final Path CRS_CODE_PATH = new Path(
            "/MD_Metadata/referenceSystemInfo/MD_ReferenceSystem/referenceSystemIdentifier/RS_Identifier/code/CharacterString");

    private static final Path TITLE_PATH = new Path(
            "/MD_Metadata/identificationInfo/MD_DataIdentification/citation/CI_Citation/title/CharacterString");

    private static final Path ABSTRACT_PATH = new Path(
            "/MD_Metadata/identificationInfo/MD_DataIdentification/abstract/CharacterString");

    private static final Path FORMAT_PATH = new Path(
            "/MD_Metadata/distributionInfo/MD_Distribution/distributionFormat/MD_Format/name/CharacterString");

    private static final Path LINKAGE_URI_PATH = new Path(
            "/MD_Metadata/distributionInfo/MD_Distribution/distributor/MD_Distributor/distributorTransferOptions/MD_DigitalTransferOptions/onLine/CI_OnlineResource/linkage/URL");

    private static final Path KEYWORD_PATH = new Path(
            "/MD_Metadata/identificationInfo/MD_DataIdentification/descriptiveKeywords/MD_Keywords/keyword/CharacterString");

    private static final Path TOPIC_CATEGORY_PATH = new Path(
            "/MD_Metadata/identificationInfo/MD_DataIdentification/topicCategory/MD_TopicCategoryCode");

    private static final Path BBOX_WEST_LON_PATH = new Path(
            "/MD_Metadata/identificationInfo/MD_DataIdentification/extent/EX_Extent/geographicElement/EX_GeographicBoundingBox/westBoundLongitude/Decimal");

    private static final Path BBOX_EAST_LON_PATH = new Path(
            "/MD_Metadata/identificationInfo/MD_DataIdentification/extent/EX_Extent/geographicElement/EX_GeographicBoundingBox/eastBoundLongitude/Decimal");

    private static final Path BBOX_SOUTH_LAT_PATH = new Path(
            "/MD_Metadata/identificationInfo/MD_DataIdentification/extent/EX_Extent/geographicElement/EX_GeographicBoundingBox/southBoundLatitude/Decimal");

    private static final Path BBOX_NORTH_LAT_PATH = new Path(
            "/MD_Metadata/identificationInfo/MD_DataIdentification/extent/EX_Extent/geographicElement/EX_GeographicBoundingBox/northBoundLatitude/Decimal");

    private static final Path POINT_OF_CONTACT_PATH = new Path(
            "/MD_Metadata/identificationInfo/MD_DataIdentification/pointOfContact/CI_ResponsibleParty/organisationName/CharacterString");

    private static final List<Path> PATHS = Arrays.asList(FILE_IDENTIFIER_PATH,
            DATE_STAMP_PATH,
            CODE_LIST_VALUE_PATH,
            CRS_AUTHORITY_PATH,
            CRS_VERSION_PATH,
            CRS_CODE_PATH,
            TITLE_PATH,
            ABSTRACT_PATH,
            FORMAT_PATH,
            LINKAGE_URI_PATH,
            KEYWORD_PATH,
            TOPIC_CATEGORY_PATH,
            BBOX_WEST_LON_PATH,
            BBOX_EAST_LON_PATH,
            BBOX_SOUTH_LAT_PATH,
            BBOX_NORTH_LAT_PATH,
            POINT_OF_CONTACT_PATH);

    private static final String TRANSFORM_EXCEPTION_MSG =
            "Unable to transform from GMD Metadata to Metacard";

    private final XStream xstream;

    public GmdTransformer() {
        QNameMap qmap = new QNameMap();
        qmap.setDefaultNamespace(GmdMetacardType.GMD_NAMESPACE);
        qmap.setDefaultPrefix("");
        StaxDriver staxDriver = new StaxDriver(qmap);

        xstream = new XStream(staxDriver);
        xstream.setClassLoader(this.getClass()
                .getClassLoader());
        XstreamPathConverter converter = new XstreamPathConverter();
        converter.setPaths(PATHS);
        xstream.registerConverter(converter);
        xstream.alias("MD_Metadata", XstreamPathValueTracker.class);

    }

    @Override
    public Metacard transform(InputStream inputStream)
            throws IOException, CatalogTransformerException {
        return transform(inputStream, null);
    }

    @Override
    public Metacard transform(InputStream inputStream, String id)
            throws IOException, CatalogTransformerException {

        String xml = null;
        XstreamPathValueTracker pathValueTracker = null;
        try {
            xml = IOUtils.toString(inputStream);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        try {
            pathValueTracker = (XstreamPathValueTracker) xstream.fromXML(xml);
        } catch (XStreamException e) {
            throw new CatalogTransformerException(TRANSFORM_EXCEPTION_MSG, e);
        }

        MetacardImpl metacard = toMetacard(pathValueTracker, id);

        metacard.setAttribute(Metacard.METADATA, xml);

        return metacard;
    }

    private MetacardImpl toMetacard(final XstreamPathValueTracker pathValueTracker,
            final String id) {
        MetacardImpl metacard = new MetacardImpl(GMD_METACARD_TYPE);

        if (StringUtils.isNotEmpty(id)) {
            metacard.setAttribute(Metacard.ID, id);
        } else {
            metacard.setId(pathValueTracker.getPathValue(FILE_IDENTIFIER_PATH));
        }
        addMetacardDates(pathValueTracker, metacard);

        addMetacardType(pathValueTracker, metacard);

        addMetacardCrs(pathValueTracker, metacard);

        addMetacardTitle(pathValueTracker, metacard);

        addMetacardAbstract(pathValueTracker, metacard);

        addMetacardFormat(pathValueTracker, metacard);

        addMetacardResourceUri(pathValueTracker, metacard);

        addMetacardLocation(pathValueTracker, metacard);

        addMetacardSubject(pathValueTracker, metacard);

        setPointOfContact(pathValueTracker, metacard);

        return metacard;
    }

    private void addMetacardDates(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {

        String dateStr = pathValueTracker.getPathValue(DATE_STAMP_PATH);
        if (StringUtils.isNotEmpty(dateStr)) {
            Date date = CswUnmarshallHelper.convertToDate(dateStr);
            metacard.setModifiedDate(date);
            metacard.setCreatedDate(date);
        }

    }

    private void setPointOfContact(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {

        String pointOfContact = pathValueTracker.getPathValue(POINT_OF_CONTACT_PATH);
        if (StringUtils.isNotEmpty(pointOfContact)) {
            metacard.setPointOfContact(pointOfContact);
            metacard.setAttribute(GmdMetacardType.GMD_PUBLISHER, pointOfContact);
        }

    }

    private void addMetacardType(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {

        String type = pathValueTracker.getPathValue(CODE_LIST_VALUE_PATH);
        if (StringUtils.isNotEmpty(type)) {
            metacard.setContentTypeName(type);
        }

    }

    private void addMetacardCrs(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {
        StringBuilder crs = new StringBuilder();
        crs.append("urn:ogc:def:crs:");
        crs.append(StringUtils.defaultString(pathValueTracker.getPathValue(CRS_AUTHORITY_PATH)));
        crs.append(COLON);
        crs.append(StringUtils.defaultString(pathValueTracker.getPathValue(CRS_VERSION_PATH)));
        crs.append(COLON);
        crs.append(StringUtils.defaultString(pathValueTracker.getPathValue(CRS_CODE_PATH)));
        metacard.setAttribute(GmdMetacardType.GMD_CRS, crs.toString());

    }

    private void addMetacardTitle(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {
        String title = pathValueTracker.getPathValue(TITLE_PATH);
        if (StringUtils.isNotEmpty(title)) {
            metacard.setTitle(title);
        }
    }

    private void addMetacardAbstract(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {
        String gmdAbstract = pathValueTracker.getPathValue(ABSTRACT_PATH);
        if (StringUtils.isNotEmpty(gmdAbstract)) {
            metacard.setDescription(gmdAbstract);
        }

    }

    private void addMetacardFormat(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {
        String format = pathValueTracker.getPathValue(FORMAT_PATH);
        if (StringUtils.isNotEmpty((format))) {
            metacard.setAttribute(GmdMetacardType.GMD_FORMAT, format);
        }
    }

    private void addMetacardResourceUri(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {
        String linkage = pathValueTracker.getPathValue(LINKAGE_URI_PATH);
        if (StringUtils.isNotEmpty(linkage)) {
            try {
                metacard.setResourceURI(new URI(linkage));
            } catch (URISyntaxException e) {
                LOGGER.info("Unable to read resource URI {}", linkage, e);
            }
        }

    }

    private void addMetacardSubject(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {

        List<String> subjects = pathValueTracker.getAllValues(KEYWORD_PATH);
        subjects.addAll(pathValueTracker.getAllValues(TOPIC_CATEGORY_PATH));
        metacard.setAttribute(GmdMetacardType.GMD_SUBJECT, (Serializable) subjects);

    }

    private void addMetacardLocation(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {
        String westLon = pathValueTracker.getPathValue(BBOX_WEST_LON_PATH);
        String eastLon = pathValueTracker.getPathValue(BBOX_EAST_LON_PATH);
        String southLat = pathValueTracker.getPathValue(BBOX_SOUTH_LAT_PATH);
        String northLat = pathValueTracker.getPathValue(BBOX_NORTH_LAT_PATH);

        if (westLon != null && eastLon != null && southLat != null && northLat != null) {
            WKTWriter wktWriter = new WKTWriter();

            GeometryFactory factory = new GeometryFactory();
            try {
                Envelope envelope = new Envelope(Double.parseDouble(eastLon.trim()),
                        Double.parseDouble(westLon.trim()),
                        Double.parseDouble(southLat.trim()),
                        Double.parseDouble(northLat.trim()));
                String wkt = wktWriter.write(factory.toGeometry(envelope));
                if (wkt != null) {
                    metacard.setLocation(wkt);
                }
            } catch (NumberFormatException nfe) {
                LOGGER.info("Unable to parse double from GMD metadata {}, {}, {}, {}",
                        westLon,
                        eastLon,
                        southLat,
                        northLat);
            }
        }
    }

}
