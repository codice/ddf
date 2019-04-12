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
package ddf.catalog.source.solr;

import ddf.catalog.data.AttributeType.AttributeFormat;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides the mappings between {@link AttributeFormat} objects and the correct Solr suffix and
 * vice versa
 */
public class SchemaFields {

  public static final String OBJECT_SUFFIX = "_obj";

  public static final String LONG_SUFFIX = "_lng";

  public static final String INTEGER_SUFFIX = "_int";

  public static final String SHORT_SUFFIX = "_shr";

  public static final String FLOAT_SUFFIX = "_flt";

  public static final String DOUBLE_SUFFIX = "_dbl";

  public static final String BOOLEAN_SUFFIX = "_bln";

  public static final String GEO_SUFFIX = "_geo";

  public static final String TEXT_SUFFIX = "_txt";

  public static final String XML_SUFFIX = "_xml";

  public static final String DATE_SUFFIX = "_tdt";

  public static final String BINARY_SUFFIX = "_bin";

  public static final String TOKENIZED = "_tokenized";

  public static final String HAS_CASE = "_has_case";

  public static final String TEXT_PATH = "_tpt";

  public static final String INDEXED = "_index";

  public static final String METACARD_TYPE_FIELD_NAME = "metacard_type_name" + TEXT_SUFFIX;

  public static final String METACARD_TYPE_OBJECT_FIELD_NAME = "metacard_type" + OBJECT_SUFFIX;

  public static final String SORT_SUFFIX = "_sort";

  protected static final Map<String, AttributeFormat> SUFFIX_TO_FORMAT_MAP;

  protected static final Map<AttributeFormat, String> FORMAT_TO_SUFFIX_MAP;

  static {
    HashMap<String, AttributeFormat> suffixToFormatMap = new HashMap<>();
    suffixToFormatMap.put(GEO_SUFFIX, AttributeFormat.GEOMETRY);
    suffixToFormatMap.put(DATE_SUFFIX, AttributeFormat.DATE);
    suffixToFormatMap.put(BINARY_SUFFIX, AttributeFormat.BINARY);
    suffixToFormatMap.put(XML_SUFFIX, AttributeFormat.XML);
    suffixToFormatMap.put(TEXT_SUFFIX, AttributeFormat.STRING);
    suffixToFormatMap.put(BOOLEAN_SUFFIX, AttributeFormat.BOOLEAN);
    suffixToFormatMap.put(DOUBLE_SUFFIX, AttributeFormat.DOUBLE);
    suffixToFormatMap.put(FLOAT_SUFFIX, AttributeFormat.FLOAT);
    suffixToFormatMap.put(INTEGER_SUFFIX, AttributeFormat.INTEGER);
    suffixToFormatMap.put(LONG_SUFFIX, AttributeFormat.LONG);
    suffixToFormatMap.put(SHORT_SUFFIX, AttributeFormat.SHORT);
    suffixToFormatMap.put(OBJECT_SUFFIX, AttributeFormat.OBJECT);
    SUFFIX_TO_FORMAT_MAP = Collections.unmodifiableMap(suffixToFormatMap);

    Map<AttributeFormat, String> formatToSuffixMap = new EnumMap<>(AttributeFormat.class);
    formatToSuffixMap.put(AttributeFormat.GEOMETRY, GEO_SUFFIX);
    formatToSuffixMap.put(AttributeFormat.DATE, DATE_SUFFIX);
    formatToSuffixMap.put(AttributeFormat.BINARY, BINARY_SUFFIX);
    formatToSuffixMap.put(AttributeFormat.XML, XML_SUFFIX);
    formatToSuffixMap.put(AttributeFormat.STRING, TEXT_SUFFIX);
    formatToSuffixMap.put(AttributeFormat.BOOLEAN, BOOLEAN_SUFFIX);
    formatToSuffixMap.put(AttributeFormat.DOUBLE, DOUBLE_SUFFIX);
    formatToSuffixMap.put(AttributeFormat.FLOAT, FLOAT_SUFFIX);
    formatToSuffixMap.put(AttributeFormat.INTEGER, INTEGER_SUFFIX);
    formatToSuffixMap.put(AttributeFormat.LONG, LONG_SUFFIX);
    formatToSuffixMap.put(AttributeFormat.SHORT, SHORT_SUFFIX);
    formatToSuffixMap.put(AttributeFormat.OBJECT, OBJECT_SUFFIX);
    FORMAT_TO_SUFFIX_MAP = Collections.unmodifiableMap(formatToSuffixMap);
  }

  public AttributeFormat getFormat(String suffix) {
    return SUFFIX_TO_FORMAT_MAP.get(suffix);
  }

  public String getFieldSuffix(AttributeFormat attributeFormat) {

    return FORMAT_TO_SUFFIX_MAP.get(attributeFormat);
  }
}
