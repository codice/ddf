/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import com.thoughtworks.xstream.io.path.Path;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.DateTime;
import ddf.catalog.data.types.Location;
import ddf.catalog.data.types.Media;
import ddf.catalog.data.types.Topic;
import ddf.catalog.data.types.constants.core.DataType;
import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GmdConstants;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    pathValueTracker.add(
        new Path("/MD_Metadata/@xmlns:" + GmdConstants.GCO_PREFIX), GmdConstants.GCO_NAMESPACE);

    pathValueTracker.add(
        new Path("/MD_Metadata/@xmlns:" + CswConstants.GML_NAMESPACE_PREFIX),
        CswConstants.GML_SCHEMA);

    pathValueTracker.add(new Path(GmdConstants.FILE_IDENTIFIER_PATH), metacard.getId());

    Attribute language = metacard.getAttribute(Core.LANGUAGE);
    if (language != null) {
      addListAttributeToXml(
          metacard, pathValueTracker, GmdConstants.METADATA_LANGUAGE_PATH, Core.LANGUAGE);
    } else {
      pathValueTracker.add(
          new Path(GmdConstants.METADATA_LANGUAGE_PATH), Locale.ENGLISH.getISO3Language());
    }

    pathValueTracker.add(
        new Path(GmdConstants.CODE_LIST_VALUE_PATH),
        StringUtils.defaultIfEmpty(metacard.getContentTypeName(), DataType.DATASET.toString()));
    pathValueTracker.add(new Path(GmdConstants.CODE_LIST_PATH), GmdConstants.METACARD_URI);

    addStringAttributeToXml(
        metacard,
        pathValueTracker,
        GmdConstants.CODE_LIST_VALUE_PATH,
        Core.DATATYPE,
        Metacard.CONTENT_TYPE);

    pathValueTracker.add(new Path(GmdConstants.CODE_LIST_PATH), GmdConstants.METACARD_URI);

    addListAttributeToXml(
        metacard,
        pathValueTracker,
        GmdConstants.CONTACT_ORGANISATION_PATH,
        Contact.POINT_OF_CONTACT_NAME);

    addListAttributeToXml(
        metacard,
        pathValueTracker,
        GmdConstants.CONTACT_EMAIL_PATH,
        Contact.POINT_OF_CONTACT_EMAIL);

    addListAttributeToXml(
        metacard,
        pathValueTracker,
        GmdConstants.CONTACT_PHONE_PATH,
        Contact.POINT_OF_CONTACT_PHONE);

    addListAttributeToXml(
        metacard,
        pathValueTracker,
        GmdConstants.CONTACT_ADDRESS_DELIVERY_POINT_PATH,
        Contact.POINT_OF_CONTACT_ADDRESS);

    addDateAttributeToXml(
        metacard, pathValueTracker, GmdConstants.DATE_TIME_STAMP_PATH, Core.METACARD_MODIFIED);

    addCRSInformation(metacard, pathValueTracker);

    addIdentificationInfo(metacard, pathValueTracker);

    addDistributionInfo(metacard, pathValueTracker);

    return pathValueTracker;
  }

  @Override
  protected String getRootNodeName() {
    return GmdConstants.GMD_LOCAL_NAME;
  }

  protected void addDistributionInfo(
      MetacardImpl metacard, XstreamPathValueTracker pathValueTracker) {
    addStringAttributeToXml(
        metacard, pathValueTracker, GmdConstants.FORMAT_PATH, Media.FORMAT, null);

    addStringAttributeToXml(
        metacard, pathValueTracker, GmdConstants.FORMAT_VERSION_PATH, Media.FORMAT_VERSION, null);

    String resourceUrl = null;
    Attribute downloadUrlAttr = metacard.getAttribute(Core.RESOURCE_DOWNLOAD_URL);
    if (downloadUrlAttr != null) {
      Serializable downloadUrl = downloadUrlAttr.getValue();
      if (downloadUrl instanceof String) {
        resourceUrl = (String) downloadUrl;
      }
    }

    if (StringUtils.isNotEmpty(resourceUrl)) {
      pathValueTracker.add(new Path(GmdConstants.LINKAGE_URI_PATH), resourceUrl);
    } else {
      URI resourceUri = metacard.getResourceURI();
      if (resourceUri != null) {
        pathValueTracker.add(new Path(GmdConstants.LINKAGE_URI_PATH), resourceUri.toASCIIString());
      }
    }
  }

  protected void addCRSInformation(
      MetacardImpl metacard, XstreamPathValueTracker pathValueTracker) {
    Attribute attribute = metacard.getAttribute(Location.COORDINATE_REFERENCE_SYSTEM_CODE);
    if (attribute != null && CollectionUtils.isNotEmpty(attribute.getValues())) {
      attribute
          .getValues()
          .forEach(
              serializable -> {
                if (serializable instanceof String) {
                  String[] crsSplit = ((String) serializable).split(":", 2);
                  if (crsSplit.length == 2) {
                    pathValueTracker.add(new Path(GmdConstants.CRS_CODE_PATH), crsSplit[1]);
                    pathValueTracker.add(new Path(GmdConstants.CRS_AUTHORITY_PATH), crsSplit[0]);
                  }
                }
              });
    }
  }

  protected void addIdentificationInfo(
      MetacardImpl metacard, XstreamPathValueTracker pathValueTracker) {
    pathValueTracker.add(
        new Path(GmdConstants.TITLE_PATH), StringUtils.defaultString(metacard.getTitle()));

    addDateAttributeToXml(metacard, pathValueTracker, GmdConstants.CREATED_DATE_PATH, Core.CREATED);

    pathValueTracker.add(
        new Path(GmdConstants.CREATED_DATE_TYPE_CODE_PATH), GmdConstants.METACARD_URI);
    pathValueTracker.add(new Path(GmdConstants.CREATED_DATE_TYPE_CODE_VALUE_PATH), Core.CREATED);

    addListAttributeToXml(metacard, pathValueTracker, GmdConstants.ABSTRACT_PATH, Core.DESCRIPTION);

    addListAttributeToXml(
        metacard, pathValueTracker, GmdConstants.POINT_OF_CONTACT_PATH, Contact.PUBLISHER_NAME);

    addListAttributeToXml(
        metacard,
        pathValueTracker,
        GmdConstants.POINT_OF_CONTACT_EMAIL_PATH,
        Contact.PUBLISHER_EMAIL);

    addListAttributeToXml(
        metacard,
        pathValueTracker,
        GmdConstants.POINT_OF_CONTACT_PHONE_PATH,
        Contact.PUBLISHER_PHONE);

    addListAttributeToXml(
        metacard,
        pathValueTracker,
        GmdConstants.POINT_OF_CONTACT_ADDRESS_DELIVERY_POINT_PATH,
        Contact.PUBLISHER_ADDRESS);

    addListAttributeToXml(metacard, pathValueTracker, GmdConstants.KEYWORD_PATH, Topic.KEYWORD);

    Attribute language = metacard.getAttribute(Core.LANGUAGE);
    if (language != null) {
      addListAttributeToXml(metacard, pathValueTracker, GmdConstants.LANGUAGE_PATH, Core.LANGUAGE);
    } else {
      pathValueTracker.add(new Path(GmdConstants.LANGUAGE_PATH), Locale.ENGLISH.getISO3Language());
    }

    addListAttributeToXml(
        metacard, pathValueTracker, GmdConstants.TOPIC_CATEGORY_PATH, Topic.CATEGORY);
    addListAttributeToXml(
        metacard, pathValueTracker, GmdConstants.ALTITUDE_PATH, Location.ALTITUDE);
    addListAttributeToXml(
        metacard, pathValueTracker, GmdConstants.COUNTRY_CODE_PATH, Location.COUNTRY_CODE);

    addGeospatialExtent(metacard, pathValueTracker);
    addTemporalExtent(metacard, pathValueTracker);
  }

  private void addTemporalExtent(MetacardImpl metacard, XstreamPathValueTracker pathValueTracker) {
    addDateAttributeToXml(
        metacard, pathValueTracker, GmdConstants.TEMPORAL_START_PATH, DateTime.START);

    addDateAttributeToXml(
        metacard, pathValueTracker, GmdConstants.TEMPORAL_STOP_PATH, DateTime.END);
  }

  protected void addGeospatialExtent(
      MetacardImpl metacard, XstreamPathValueTracker pathValueTracker) {
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

  private void addStringAttributeToXml(
      Metacard metacard,
      XstreamPathValueTracker pathValueTracker,
      String path,
      String metacardAttributeName,
      String fallbackMetacardAttribute) {
    Attribute attribute = metacard.getAttribute(metacardAttributeName);
    if (attribute != null) {
      addStringAttributeToPath(attribute, pathValueTracker, path);
    } else if (StringUtils.isNotEmpty(fallbackMetacardAttribute)) {
      Attribute fallbackAttribute = metacard.getAttribute(fallbackMetacardAttribute);
      if (fallbackAttribute != null) {
        addStringAttributeToPath(fallbackAttribute, pathValueTracker, path);
      }
    }
  }

  private void addStringAttributeToPath(
      Attribute attribute, XstreamPathValueTracker pathValueTracker, String path) {
    Serializable serializable = attribute.getValue();
    if (serializable instanceof String) {
      String value = (String) serializable;
      pathValueTracker.add(new Path(path), value);
    }
  }

  private void addDateAttributeToXml(
      Metacard metacard,
      XstreamPathValueTracker pathValueTracker,
      String path,
      String metcardAttributeName) {
    Attribute attribute = metacard.getAttribute(metcardAttributeName);
    if (attribute != null) {
      Serializable serializable = attribute.getValue();
      if (serializable instanceof Date) {
        GregorianCalendar modifiedCal = new GregorianCalendar();
        modifiedCal.setTime((Date) serializable);
        modifiedCal.setTimeZone(UTC_TIME_ZONE);
        pathValueTracker.add(
            new Path(path), XSD_FACTORY.newXMLGregorianCalendar(modifiedCal).toXMLFormat());
      }
    }
  }

  private void addListAttributeToXml(
      Metacard metacard,
      XstreamPathValueTracker pathValueTracker,
      String path,
      String metacardAttribute) {
    Attribute descriptions = metacard.getAttribute(metacardAttribute);
    if (descriptions != null && CollectionUtils.isNotEmpty(descriptions.getValues())) {
      pathValueTracker.add(
          new Path(path),
          descriptions
              .getValues()
              .stream()
              .map(Serializable::toString)
              .collect(Collectors.toList()));
    }
  }
}
