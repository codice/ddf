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
package org.codice.ddf.spatial.process.api.description;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;
import javax.xml.namespace.QName;

/** This class is Experimental and subject to change */
public enum DataType {
  STRING("String"),
  INTEGER("Integer"),
  INT("Int"),
  LONG("Long"),
  SHORT("Short"),
  DECIMAL("Decimal"),
  FLOAT("Float"),
  DOUBLE("Double"),
  BOOLEAN("Boolean"),
  BYTE("Byte"),
  QNAME("QName"),
  DATE_TIME("DateTime"),
  BASE_64_BINARY("Base64Binary"),
  HEX_64_BINARY("Hex64Binary"),
  UNSIGNED_INT("UnsignedInt"),
  UNSIGNED_SHORT("UnsignedShort"),
  UNSIGNED_BYTE("UnsignedByte"),
  TIME("Time"),
  DATE("Date"),
  DURATION("Duration"),
  URI("URI"),
  UUID("UUID");

  // this api doesn't really care about xml but the mapping would be easy to break
  // if we didn't have it here
  private static final String[] REFERENCE = {
    "http://www.w3.org/TR/xmlschema-2/#string",
    "http://www.w3.org/TR/xmlschema-2/#integer",
    "http://www.w3.org/TR/xmlschema-2/#int",
    "http://www.w3.org/TR/xmlschema-2/#long",
    "http://www.w3.org/TR/xmlschema-2/#short",
    "http://www.w3.org/TR/xmlschema-2/#decimal",
    "http://www.w3.org/TR/xmlschema-2/#float",
    "http://www.w3.org/TR/xmlschema-2/#double",
    "http://www.w3.org/TR/xmlschema-2/#boolean",
    "http://www.w3.org/TR/xmlschema-2/#byte",
    "http://www.w3.org/TR/xmlschema-2/#QName",
    "http://www.w3.org/TR/xmlschema-2/#dateTime",
    "http://www.w3.org/TR/xmlschema-2/#base64Binary",
    "http://www.w3.org/TR/xmlschema-2/#binary",
    "http://www.w3.org/TR/xmlschema-2/#unsignedInt",
    "http://www.w3.org/TR/xmlschema-2/#unsignedShort",
    "http://www.w3.org/TR/xmlschema-2/#unsignedByte",
    "http://www.w3.org/TR/xmlschema-2/#time",
    "http://www.w3.org/TR/xmlschema-2/#date",
    "http://www.w3.org/TR/xmlschema-2/#duration",
    "http://www.w3.org/TR/xmlschema-2/#anyURI",
    "http://www.w3.org/TR/xmlschema-2/#string"
  };

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      new DateTimeFormatterBuilder()
          .appendPattern("yyyy-MM-dd'T'HH:mm:ss[XXXXX]")
          .parseDefaulting(ChronoField.OFFSET_SECONDS, 0)
          .toFormatter();

  private static final Function<String, Object> PARSE_DATE_TIME =
      s -> OffsetDateTime.parse(s, DATE_TIME_FORMATTER);

  private static final List<Function<String, Object>> FROM_STRING =
      Arrays.asList(
          String::toString,
          BigInteger::new,
          Integer::parseInt,
          Long::parseLong,
          Short::parseShort,
          BigDecimal::new,
          Float::parseFloat,
          Double::parseDouble,
          Boolean::parseBoolean,
          Byte::parseByte,
          QName::valueOf,
          PARSE_DATE_TIME,
          Base64.getDecoder()::decode,
          String::getBytes,
          Long::parseLong,
          Integer::parseInt,
          Short::parseShort,
          PARSE_DATE_TIME,
          PARSE_DATE_TIME,
          Duration::parse,
          java.net.URI::create,
          String::toString);

  private final String name;

  DataType(String name) {
    this.name = name;
  }

  public String value() {
    return name;
  }

  public String reference() {
    return REFERENCE[ordinal()];
  }

  public Serializable parseFromString(String value) {
    return (Serializable) FROM_STRING.get(ordinal()).apply(value);
  }
}
