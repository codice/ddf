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
package ddf.catalog.source.solr;

import ddf.catalog.data.AttributeType.AttributeFormat;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides the mappings between {@link AttributeFormat} objects and the correct Solr suffix and
 * vice versa
 * 
 */
public class SchemaFields {

    private static final Map<String, AttributeFormat> SUFFIX_TO_FORMAT_MAP = new HashMap<>();

    private static final Map<AttributeFormat, String> FORMAT_TO_SUFFIX_MAP = new HashMap<>();

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

    static {
        SUFFIX_TO_FORMAT_MAP.put(GEO_SUFFIX, AttributeFormat.GEOMETRY);
        SUFFIX_TO_FORMAT_MAP.put(DATE_SUFFIX, AttributeFormat.DATE);
        SUFFIX_TO_FORMAT_MAP.put(BINARY_SUFFIX, AttributeFormat.BINARY);
        SUFFIX_TO_FORMAT_MAP.put(XML_SUFFIX, AttributeFormat.XML);
        SUFFIX_TO_FORMAT_MAP.put(TEXT_SUFFIX, AttributeFormat.STRING);
        SUFFIX_TO_FORMAT_MAP.put(BOOLEAN_SUFFIX, AttributeFormat.BOOLEAN);
        SUFFIX_TO_FORMAT_MAP.put(DOUBLE_SUFFIX, AttributeFormat.DOUBLE);
        SUFFIX_TO_FORMAT_MAP.put(FLOAT_SUFFIX, AttributeFormat.FLOAT);
        SUFFIX_TO_FORMAT_MAP.put(INTEGER_SUFFIX, AttributeFormat.INTEGER);
        SUFFIX_TO_FORMAT_MAP.put(LONG_SUFFIX, AttributeFormat.LONG);
        SUFFIX_TO_FORMAT_MAP.put(SHORT_SUFFIX, AttributeFormat.SHORT);
        SUFFIX_TO_FORMAT_MAP.put(OBJECT_SUFFIX, AttributeFormat.OBJECT);

        FORMAT_TO_SUFFIX_MAP.put(AttributeFormat.GEOMETRY, GEO_SUFFIX);
        FORMAT_TO_SUFFIX_MAP.put(AttributeFormat.DATE, DATE_SUFFIX);
        FORMAT_TO_SUFFIX_MAP.put(AttributeFormat.BINARY, BINARY_SUFFIX);
        FORMAT_TO_SUFFIX_MAP.put(AttributeFormat.XML, XML_SUFFIX);
        FORMAT_TO_SUFFIX_MAP.put(AttributeFormat.STRING, TEXT_SUFFIX);
        FORMAT_TO_SUFFIX_MAP.put(AttributeFormat.BOOLEAN, BOOLEAN_SUFFIX);
        FORMAT_TO_SUFFIX_MAP.put(AttributeFormat.DOUBLE, DOUBLE_SUFFIX);
        FORMAT_TO_SUFFIX_MAP.put(AttributeFormat.FLOAT, FLOAT_SUFFIX);
        FORMAT_TO_SUFFIX_MAP.put(AttributeFormat.INTEGER, INTEGER_SUFFIX);
        FORMAT_TO_SUFFIX_MAP.put(AttributeFormat.LONG, LONG_SUFFIX);
        FORMAT_TO_SUFFIX_MAP.put(AttributeFormat.SHORT, SHORT_SUFFIX);
        FORMAT_TO_SUFFIX_MAP.put(AttributeFormat.OBJECT, OBJECT_SUFFIX);

    }

    public AttributeFormat getFormat(String suffix) {
        return SUFFIX_TO_FORMAT_MAP.get(suffix);
    }

    public String getFieldSuffix(AttributeFormat attributeFormat) {

        return FORMAT_TO_SUFFIX_MAP.get(attributeFormat);
    }

}
