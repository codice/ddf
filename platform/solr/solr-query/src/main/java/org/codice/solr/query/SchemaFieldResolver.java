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
package org.codice.solr.query;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.client.solrj.response.LukeResponse.FieldInfo;
import org.apache.solr.common.SolrException;
import org.codice.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaFieldResolver {

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

  private static final String[] FORMAT_SUFFIXES =
      new String[] {
        OBJECT_SUFFIX,
        LONG_SUFFIX,
        INTEGER_SUFFIX,
        SHORT_SUFFIX,
        FLOAT_SUFFIX,
        DOUBLE_SUFFIX,
        BOOLEAN_SUFFIX,
        GEO_SUFFIX,
        TEXT_SUFFIX,
        XML_SUFFIX,
        DATE_SUFFIX,
        BINARY_SUFFIX
      };

  public static final String TOKENIZED = "_tokenized";

  public static final String HAS_CASE = "_has_case";

  public static final String TEXT_PATH = "_tpt";

  public static final String INDEXED = "_index";

  public static final String METACARD_TYPE_FIELD_NAME = "metacard_type_name" + TEXT_SUFFIX;

  public static final String METACARD_TYPE_OBJECT_FIELD_NAME = "metacard_type" + OBJECT_SUFFIX;

  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaFieldResolver.class);

  private static final Map<String, AttributeFormat> SUFFIX_TO_FORMAT_MAP =
      new HashMap<String, AttributeFormat>();

  private static final Map<AttributeFormat, String> FORMAT_TO_SUFFIX_MAP =
      new EnumMap<>(AttributeFormat.class);

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

  private SolrClient solr;

  public SchemaFieldResolver(SolrClient client) {
    this.solr = client;
  }

  public AttributeFormat getFormat(String suffix) {
    return SUFFIX_TO_FORMAT_MAP.get(suffix);
  }

  public String getFieldSuffix(AttributeFormat attributeFormat) {

    return FORMAT_TO_SUFFIX_MAP.get(attributeFormat);
  }

  public SchemaField getSchemaField(String propertyName, boolean isSearchedAsExactValue) {
    SchemaField schemaField = null;
    LukeRequest luke = new LukeRequest();
    LukeResponse rsp;
    try {
      rsp = luke.process(solr.getClient());
      Map<String, FieldInfo> fieldsInfo = rsp.getFieldInfo();
      if (fieldsInfo != null && !fieldsInfo.isEmpty()) {
        LOGGER.debug("got fieldsInfo for {} fields", fieldsInfo.size());

        for (Map.Entry<String, FieldInfo> entry : fieldsInfo.entrySet()) {

          // See if any fieldName startsWith(propertyName)
          // if it does, then see if remainder of fieldName matches any expected suffix
          // if suffix matches, then get type of field and cache it
          if (entry.getKey().startsWith(propertyName)
              && StringUtils.endsWithAny(entry.getKey(), FORMAT_SUFFIXES)) {
            String fieldType = entry.getValue().getType();
            int index = StringUtils.lastIndexOfAny(entry.getKey(), FORMAT_SUFFIXES);
            String suffix = entry.getKey().substring(index);
            if (!isSearchedAsExactValue) {
              suffix = getSpecialIndexSuffix(suffix);
              fieldType += suffix;
            }
            LOGGER.debug("field {} has type {}", entry.getKey(), fieldType);
            schemaField = new SchemaField(entry.getKey(), fieldType);
            schemaField.setSuffix(suffix);
            return schemaField;
          }
        }
      } else {
        LOGGER.debug("fieldsInfo from LukeRequest are either null or empty");
      }

    } catch (SolrServerException | SolrException | IOException e) {
      LOGGER.info("Exception while processing LukeRequest", e);
    }

    LOGGER.debug("Did not find SchemaField for property {}", propertyName);

    return schemaField;
  }

  private String getSpecialIndexSuffix(String suffix) {

    if (suffix.equalsIgnoreCase(TEXT_SUFFIX)) {
      return SchemaFieldResolver.TOKENIZED;
    } else if (suffix.equalsIgnoreCase(GEO_SUFFIX)) {
      return SchemaFieldResolver.INDEXED;
    } else if (suffix.equalsIgnoreCase(XML_SUFFIX)) {
      return SchemaFieldResolver.TEXT_PATH;
    }

    return "";
  }
}
