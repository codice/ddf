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

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Media;
import ddf.catalog.data.types.Topic;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.BooleanUtils;
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

  private static final Date UNIX_EPOCH_DATE = Date.from(Instant.EPOCH);

  private static final Date EXCEL_EPOCH_DATE = Date.from(Instant.parse("1904-01-01T00:00:00.000Z"));

  public static final String COMPRESSION_TYPE_METADATA_KEY = "Compression Type";

  public static final String DURATION_METDATA_KEY = "xmpDM:duration";

  public static final String ALTERNATE_MAPPING_OVERRIDE_FILE_KEY = "tika.metadata.mapping.file";

  static final Map<String, List<String>> ALTERNATE_METADATA_KEY_MAPPING;

  private static final Pattern DURATION_SECONDS = Pattern.compile("([+-]?\\s*\\d*\\.?\\d+)\\s*s");

  private static final Pattern DURATION_HMS = Pattern.compile("(\\d+):(\\d+):(\\d*\\.?\\d+).*");

  static {
    ALTERNATE_METADATA_KEY_MAPPING = new HashMap<>();

    try (InputStream mappingStream = getAlternateMappings()) {
      Properties properties = new Properties();
      properties.load(mappingStream);
      properties.entrySet().stream()
          .forEach(
              entry ->
                  ALTERNATE_METADATA_KEY_MAPPING.put(
                      entry.getKey().toString(),
                      Arrays.asList(entry.getValue().toString().split("\\s*,\\s*"))));
    } catch (Exception e) {
      LOGGER.warn("Unable to load tika additional metadata mapping file", e);
    }
  }

  private MetacardCreator() {}

  /**
   * @param metadata the {@code Metadata} object containing the metadata relevant to the metacard,
   *     must not be null
   * @param id the value for the {@link Metacard#ID} attribute that should be set in the generated
   *     {@code Metacard}, may be null
   * @param metadataXml the XML for the {@link Metacard#METADATA} attribute that should be set in
   *     the generated {@code Metacard}, may be null
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
   * @param id the value for the {@link Metacard#ID} attribute that should be set in the generated
   *     {@code Metacard}, may be null
   * @param metadataXml the XML for the {@link Metacard#METADATA} attribute that should be set in
   *     the generated {@code Metacard}, may be null
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
      setAttribute(metacard, Metacard.TITLE, metadata.get(TikaCoreProperties.TITLE));
    }

    setAttribute(metacard, Metacard.CONTENT_TYPE, metadata.get(Metadata.CONTENT_TYPE));

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

    setAttribute(metacard, Metacard.ID, id);

    setAttribute(metacard, Metacard.METADATA, metadataXml);
    final String lat = metadata.get(Metadata.LATITUDE);
    final String lon = metadata.get(Metadata.LONGITUDE);
    setAttribute(metacard, Metacard.GEOGRAPHY, toWkt(lon, lat));

    setAttribute(metacard, Media.FORMAT, metadata.get(TikaCoreProperties.FORMAT));

    setAttribute(metacard, Core.DESCRIPTION, metadata.get(TikaCoreProperties.DESCRIPTION));

    setAttribute(metacard, Media.TYPE, metadata.get(Metadata.CONTENT_TYPE));

    setAttribute(metacard, Media.BITS_PER_SAMPLE, metadata.getInt(TIFF.BITS_PER_SAMPLE));

    setAttribute(metacard, Media.HEIGHT, metadata.getInt(TIFF.IMAGE_LENGTH));

    setAttribute(metacard, Media.WIDTH, metadata.getInt(TIFF.IMAGE_WIDTH));

    setAttribute(metacard, Media.COMPRESSION, metadata.get(COMPRESSION_TYPE_METADATA_KEY));

    setDoubleAttributeFromString(metacard, Media.DURATION, metadata.get(DURATION_METDATA_KEY));

    setAttribute(metacard, Contact.CREATOR_NAME, metadata.get(TikaCoreProperties.CREATOR));

    setMultipleAttributes(metacard, Topic.KEYWORD, metadata.getValues(TikaCoreProperties.SUBJECT));

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

    applyAlternateMappings(metadata, metacard);

    return metacard;
  }

  private static void applyAlternateMappings(final Metadata metadata, Metacard metacard) {
    MetacardType metacardType = metacard.getMetacardType();
    for (Map.Entry<String, List<String>> entry : ALTERNATE_METADATA_KEY_MAPPING.entrySet()) {
      String attributeName = entry.getKey();
      if (StringUtils.isBlank(attributeName)) {
        continue;
      }

      Attribute attribute = metacard.getAttribute(attributeName);
      if (attribute == null) {
        for (String mapping : entry.getValue()) {
          String[] values = metadata.getValues(mapping);
          List<Serializable> attrValues = new ArrayList<>();
          for (String value : values) {
            Serializable result = getMetadataValue(attributeName, value, metacardType);
            if (result != null) {
              attrValues.add(result);
            }
          }
          if (!attrValues.isEmpty()) {
            metacard.setAttribute(new AttributeImpl(attributeName, attrValues));
          }
        }
      }
    }
  }

  private static Serializable getMetadataValue(
      String attributeName, String value, MetacardType metacardType) {
    Serializable result = null;
    if (StringUtils.isNotBlank(value)) {
      AttributeDescriptor descriptor = metacardType.getAttributeDescriptor(attributeName);

      if (descriptor != null) {
        AttributeFormat attributeFormat = descriptor.getType().getAttributeFormat();

        if (attributeFormat == AttributeFormat.INTEGER) {
          try {
            result = Integer.valueOf(value.trim());
          } catch (NumberFormatException nfe) {
            LOGGER.debug(
                "Expected integer but was not integer. This is expected behavior when "
                    + "a defined integer attribute does not exist on the ingested product.");
          }
        } else if (attributeFormat == AttributeFormat.LONG) {
          try {
            result = Long.valueOf(value.trim());
          } catch (NumberFormatException nfe) {
            LOGGER.debug(
                "Expected long but was not long. This is expected behavior when "
                    + "a defined long attribute does not exist on the ingested product.");
          }
        } else if (attributeFormat == AttributeFormat.SHORT) {
          try {
            result = Short.valueOf(value.trim());
          } catch (NumberFormatException nfe) {
            LOGGER.debug(
                "Expected short but was not short. This is expected behavior when "
                    + "a defined short attribute does not exist on the ingested product.");
          }
        } else if (attributeFormat == AttributeFormat.DOUBLE) {
          result = getDoubleFromString(value);
        } else if (attributeFormat == AttributeFormat.FLOAT) {
          try {
            result = Float.valueOf(value.trim());
          } catch (NumberFormatException nfe) {
            LOGGER.debug(
                "Expected float but was not float. This is expected behavior when "
                    + "a defined float attribute does not exist on the ingested product.");
          }
        } else if (attributeFormat == AttributeFormat.DATE) {
          result = convertDate(value);
        } else if (attributeFormat == AttributeFormat.BOOLEAN) {
          result = BooleanUtils.toBoolean(value.trim());
        } else if (attributeFormat == AttributeFormat.BINARY) {
          result = value.getBytes(StandardCharsets.UTF_8);
        } else {
          result = value;
        }
      } else {
        result = value;
      }
    }

    return result;
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

  private static Double getDoubleFromString(String value) {
    Double result = null;

    if (StringUtils.isNotBlank(value)) {
      String trimmedValue = value.trim();
      try {
        result = Double.valueOf(trimmedValue);
      } catch (NumberFormatException nfe) {
        Matcher matcher = DURATION_SECONDS.matcher(trimmedValue);
        if (matcher.matches()) {
          try {
            result = Double.valueOf(matcher.group(1));
          } catch (NumberFormatException nfe2) {
            LOGGER.debug("Unable to parse double", nfe2);
          }
        } else {
          matcher = DURATION_HMS.matcher(trimmedValue);
          if (matcher.matches()) {
            try {
              double hours = Double.valueOf(matcher.group(1));
              double minutes = Double.valueOf(matcher.group(2));
              double seconds = Double.valueOf(matcher.group(3));
              result = seconds + 60.0 * minutes + 3600.0 * hours;
            } catch (NumberFormatException nfe3) {
              LOGGER.debug("Unable to construct duration", nfe3);
            }
          }
        }
      }
    }

    return result;
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
    Date date = javax.xml.bind.DatatypeConverter.parseDateTime(dateStr).getTime();

    // Tika will return epoch dates when they are missing/null in the source metadata
    if (date.equals(UNIX_EPOCH_DATE) || date.equals(EXCEL_EPOCH_DATE)) {
      return null;
    }

    return date;
  }

  private static InputStream getAlternateMappings() throws IOException {
    String overrideFile = System.getProperty(ALTERNATE_MAPPING_OVERRIDE_FILE_KEY);
    if (StringUtils.isNotBlank(overrideFile)) {
      return new FileInputStream(overrideFile);
    } else {
      return MetacardCreator.class.getResourceAsStream(
          "/tika-additional-metadata-mapping.properties");
    }
  }
}
