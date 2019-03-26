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
package ddf.catalog.transformer.common.tika;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Media;
import ddf.catalog.data.types.Topic;
import java.util.Arrays;
import java.util.Date;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Office;
import org.apache.tika.metadata.TIFF;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.metadata.XMPDM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates {@link Metacard}s from Tika {@link Metadata} objects. */
public class MetacardCreator {
  private static final Logger LOGGER = LoggerFactory.getLogger(MetacardCreator.class);

  public static final String COMPRESSION_TYPE_METADATA_KEY = "Compression Type";

  public static final String DURATION_METDATA_KEY = "xmpDM:duration";

  /**
   * @param metadata the {@code Metadata} object containing the metadata relevant to the metacard,
   *     must not be null
   * @param id the value for the {@link Core#ID} attribute that should be set in the generated
   *     {@code Metacard}, may be null
   * @param metadataXml the XML for the {@link Core#METADATA} attribute that should be set in the
   *     generated {@code Metacard}, may be null
   * @param metacardType The {@link MetacardType} for the created metacard
   * @return a new {@code Metacard}
   */
  public static Metacard createMetacard(
      final Metadata metadata,
      final String id,
      final String metadataXml,
      MetacardType metacardType) {
    return createMetacard(metadata, id, metadataXml, metacardType, true);
  }

  /**
   * @param metadata the {@code Metadata} object containing the metadata relevant to the metacard,
   *     must not be null
   * @param id the value for the {@link Core#ID} attribute that should be set in the generated
   *     {@code Metacard}, may be null
   * @param metadataXml the XML for the {@link Core#METADATA} attribute that should be set in the
   *     generated {@code Metacard}, may be null
   * @param metacardType The {@link MetacardType} for the created metacard
   * @param useMetadataTitle If true then use title from the metadata as the metacard title
   * @return a new {@code Metacard}
   */
  public static Metacard createMetacard(
      final Metadata metadata,
      final String id,
      final String metadataXml,
      MetacardType metacardType,
      boolean useMetadataTitle) {
    final Metacard metacard = new MetacardImpl(metacardType);

    if (useMetadataTitle) {
      setAttribute(metacard, Core.TITLE, metadata.get(TikaCoreProperties.TITLE));
    }

    setAttribute(metacard, Metacard.CONTENT_TYPE, metadata.get(Metadata.CONTENT_TYPE));

    final String createdDateStr = metadata.get(TikaCoreProperties.CREATED);
    final Date createdDate = convertDate(createdDateStr);
    if (createdDate != null) {
      metacard.setAttribute(new AttributeImpl(Core.CREATED, createdDate));
    }

    final String modifiedDateStr = metadata.get(TikaCoreProperties.MODIFIED);
    final Date modifiedDate = convertDate(modifiedDateStr);
    if (modifiedDate != null) {
      metacard.setAttribute(new AttributeImpl(Core.MODIFIED, modifiedDate));
    }

    setAttribute(metacard, Core.ID, id);

    setAttribute(metacard, Core.METADATA, metadataXml);
    final String lat = metadata.get(Metadata.LATITUDE);
    final String lon = metadata.get(Metadata.LONGITUDE);
    setAttribute(metacard, Core.LOCATION, toWkt(lon, lat));

    setAttribute(metacard, Media.FORMAT, metadata.get(TikaCoreProperties.FORMAT));

    setAttribute(metacard, Core.DESCRIPTION, metadata.get(TikaCoreProperties.DESCRIPTION));

    setAttribute(metacard, Media.TYPE, metadata.get(Metadata.CONTENT_TYPE));

    setAttribute(metacard, Media.BITS_PER_SAMPLE, metadata.getInt(TIFF.BITS_PER_SAMPLE));

    setAttribute(metacard, Media.HEIGHT, metadata.getInt(TIFF.IMAGE_LENGTH));

    setAttribute(metacard, Media.WIDTH, metadata.getInt(TIFF.IMAGE_WIDTH));

    setAttribute(metacard, Media.COMPRESSION, metadata.get(COMPRESSION_TYPE_METADATA_KEY));

    setDoubleAttributeFromString(metacard, Media.DURATION, metadata.get(DURATION_METDATA_KEY));

    setAttribute(metacard, Contact.CREATOR_NAME, metadata.get(TikaCoreProperties.CREATOR));

    setMultipleAttributes(metacard, Topic.KEYWORD, metadata.getValues(TikaCoreProperties.KEYWORDS));

    setAttribute(
        metacard,
        Contact.POINT_OF_CONTACT_NAME,
        metadata.get(Office.USER_DEFINED_METADATA_NAME_PREFIX + "owner"));

    String lastAuthor = metadata.get(Office.LAST_AUTHOR);
    String creator = metadata.get(TikaCoreProperties.CREATOR);
    if (StringUtils.isNotBlank(lastAuthor)
        && StringUtils.isNotBlank(creator)
        && !lastAuthor.equals(creator)) {
      setAttribute(metacard, Contact.CONTRIBUTOR_NAME, metadata.get(Office.LAST_AUTHOR));
    }

    setAttribute(
        metacard,
        Contact.POINT_OF_CONTACT_PHONE,
        metadata.get(Office.USER_DEFINED_METADATA_NAME_PREFIX + "Telephone number"));

    setAttribute(metacard, Contact.PUBLISHER_NAME, metadata.get(DublinCore.PUBLISHER));

    setAttribute(
        metacard, Mp4MetacardType.AUDIO_SAMPLE_RATE, metadata.get(XMPDM.AUDIO_SAMPLE_RATE));

    return metacard;
  }

  private static void setAttribute(Metacard metacard, String attributeName, String attributeValue) {
    if (metacard != null
        && StringUtils.isNotBlank(attributeValue)
        && StringUtils.isNotBlank(attributeName)) {
      metacard.setAttribute(new AttributeImpl(attributeName, attributeValue));
    }
  }

  private static void setAttribute(
      Metacard metacard, String attributeName, Integer attributeValue) {
    if (metacard != null && attributeValue != null && StringUtils.isNotBlank(attributeName)) {
      metacard.setAttribute(new AttributeImpl(attributeName, attributeValue));
    }
  }

  private static void setMultipleAttributes(
      Metacard metacard, String attributeName, String[] attributeValues) {
    if (metacard != null && attributeValues != null && StringUtils.isNotBlank(attributeName)) {
      metacard.setAttribute(new AttributeImpl(attributeName, Arrays.asList(attributeValues)));
    }
  }

  private static void setDoubleAttributeFromString(
      Metacard metacard, String attributeName, String attributeValue) {
    if (metacard != null && attributeValue != null && StringUtils.isNotBlank(attributeName)) {
      try {
        metacard.setAttribute(
            new AttributeImpl(attributeName, Double.valueOf(attributeValue.trim())));
      } catch (NumberFormatException nfe) {
        LOGGER.debug(
            "Expected double but was not double. This is expected behavior when "
                + "a defined double attribute does not exist on the ingested product.");
      }
    }
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

    return javax.xml.bind.DatatypeConverter.parseDateTime(dateStr).getTime();
  }
}
