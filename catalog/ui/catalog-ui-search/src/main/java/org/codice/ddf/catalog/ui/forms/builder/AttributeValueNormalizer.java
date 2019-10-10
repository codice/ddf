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
package org.codice.ddf.catalog.ui.forms.builder;

import static ddf.catalog.data.AttributeType.AttributeFormat.DATE;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.codice.ddf.catalog.ui.forms.filter.FilterProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Normalization for values on Filter JSON.
 *
 * <p>Input {@link #normalizeForXml(String, String)} should allow exceptions to propagate. Output
 * {@link #normalizeForJson(String, String)} should be more forgiving so the feature is hardened
 * against failure, since the output function reads directly from the database.
 *
 * <p>Property validation is done <b>only</b> on data entering the system, not exiting.
 */
class AttributeValueNormalizer {
  private static final Logger LOGGER = LoggerFactory.getLogger(AttributeValueNormalizer.class);

  // Relaxed validation based upon the taxonomy guidelines; not a perfect representation
  // Primary purpose is to protect persistence layer from bad symbols leaking into valid prop names
  // https://codice.atlassian.net/wiki/spaces/CODICE/pages/77627393/Taxonomy+Guidelines
  private static final Pattern EXPECTED_ATTRIBUTE_NAME_PATTERN =
      Pattern.compile("(ext\\.)?(\\p{Alpha}+\\.)?\\p{Alpha}+(-\\p{Alpha}+)*");

  private static final Pattern EXPECTED_RELATIVE_FUNCTION_PATTERN =
      Pattern.compile("RELATIVE\\(\\p{Alnum}+\\)");

  private final AttributeRegistry registry;

  AttributeValueNormalizer(AttributeRegistry registry) {
    this.registry = registry;
  }

  /**
   * Normalize filter data values when <b>leaving</b> the catalog. <b>Do not</b> throw exceptions
   * here so that the feature continues functioning regardless of bad data that might have found its
   * way into the catalog.
   *
   * @param property the name of the attribute for this value.
   * @param value the value of the attribute to normalize.
   * @return the normalized attribute value, or the original if normalization was not possible.
   */
  public String normalizeForJson(String property, String value) {
    if (eitherStringIsNull(property, value)) {
      return value;
    }
    if (isNotNormalizableDateValue(property, value)) {
      return value;
    }
    Instant instant = instantFromEpoch(value);
    if (instant != null) {
      return instant.toString();
    }

    String isoDateRange = getIsoDateRangeFromEpoch(value);
    if (isoDateRange != null) {
      return isoDateRange;
    }

    return value;
  }

  private String getIsoDateRangeFromEpoch(String value) {
    if (value.indexOf('/') >= 0) {
      String dates[] = value.split("/", 2);
      Instant instantFrom = instantFromEpoch(dates[0]);
      Instant instantTo = instantFromEpoch(dates[1]);

      String fromDate = "";
      String toDate = "";

      if (instantFrom != null) {
        fromDate = instantFrom.toString();
      }
      if (instantTo != null) {
        toDate = instantTo.toString();
      }
      return fromDate + '/' + toDate;
    }

    return null;
  }

  /**
   * Normalize filter data values when <b>entering</b> the catalog. <b>Exceptions should be
   * thrown</b> if the provided data is not valid since at worst a create or update request will
   * fail. This also draws attention to UI changes that would otherwise pollute the persistence
   * layer.
   *
   * @param property the name of the attribute for this value.
   * @param value the value of the attribute to normalize.
   * @return the normalized attribute value, or the original if normalization was not possible.
   * @throws FilterProcessingException if the provided data is not valid.
   */
  public String normalizeForXml(String property, String value) {
    if (eitherStringIsNull(property, value)) {
      return value;
    }
    if (!EXPECTED_ATTRIBUTE_NAME_PATTERN.matcher(property).matches()) {
      throw new FilterProcessingException("Malformed attribute name on search form: " + property);
    }
    if (isNotNormalizableDateValue(property, value)) {
      return value;
    }
    Instant epoch = instantFromEpoch(value);
    if (epoch != null) {
      return value;
    }
    Instant iso = instantFromIso(value);
    if (iso != null) {
      return Objects.toString(iso.toEpochMilli());
    }

    if (value.equals("")) {
      return "";
    }

    // Edge case for relative date function
    if (EXPECTED_RELATIVE_FUNCTION_PATTERN.matcher(value).matches()) {
      return value;
    }

    // Edge case for a during date range (ISO/ISO)
    String normalDateRange = normalizableDateRangeForXML(value);
    if (normalDateRange != null) {
      return normalDateRange;
    }

    throw new FilterProcessingException("Unexpected date format on search form: " + value);
  }

  @Nullable
  private String normalizableDateRangeForXML(String dateRange) {
    if (dateRange.indexOf('/') >= 0) {
      String[] dates = dateRange.split("/", 2);
      Instant dateFromInstant = instantFromIso(dates[0]);
      Instant dateToInstant = instantFromIso(dates[1]);

      String dateFrom = "";
      String dateTo = "";

      if (dateFromInstant != null) {
        dateFrom = Objects.toString(dateFromInstant.toEpochMilli());
      }
      if (dateToInstant != null) {
        dateTo = Objects.toString(dateToInstant.toEpochMilli());
      }

      return dateFrom + '/' + dateTo;
    }
    return null;
  }

  private boolean eitherStringIsNull(String property, String value) {
    if (property == null || value == null) {
      LOGGER.trace("Property [{}] or value [{}] was null, ignoring normalization", property, value);
      return true;
    }
    return false;
  }

  private boolean isNotNormalizableDateValue(String property, String value) {
    Optional<AttributeDescriptor> optional = registry.lookup(property);
    if (!optional.isPresent()) {
      LOGGER.trace("No descriptor available for property [{}]", property);
      return true;
    }
    AttributeDescriptor descriptor = optional.get();
    if (descriptor.getType().getAttributeFormat() != DATE) {
      LOGGER.trace(
          "Descriptor for property [{}] and value [{}] did not have an AttributeFormat of DATE",
          property,
          value);
      return true;
    }
    LOGGER.trace("Found date value [{}] for property [{}]", value, property);
    return false;
  }

  @Nullable
  private static Instant instantFromEpoch(String epochString) {
    try {
      long epoch = Long.parseLong(epochString);
      return Instant.ofEpochMilli(epoch);
    } catch (DateTimeException | NumberFormatException e) {
      LOGGER.debug("Error parsing epoch string [{}]", epochString, e);
      return null;
    }
  }

  @Nullable
  private static Instant instantFromIso(String isoString) {
    try {
      return Instant.parse(isoString);
    } catch (DateTimeParseException e) {
      LOGGER.debug("Error parsing ISO string [{}]", isoString, e);
    }
    try {
      return OffsetDateTime.parse(isoString).toInstant();
    } catch (DateTimeParseException e) {
      LOGGER.debug("Error parsing ISO string [{}]", isoString, e);
    }
    return null;
  }
}
