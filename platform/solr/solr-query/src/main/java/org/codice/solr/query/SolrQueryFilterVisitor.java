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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.codice.solr.client.solrj.SolrClient;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.opengis.filter.And;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrQueryFilterVisitor extends DefaultFilterVisitor {

  public static final String TOKENIZED_METADATA_FIELD =
      "metadata_txt" + SchemaFieldResolver.TOKENIZED;

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrQueryFilterVisitor.class);

  private static final String QUOTE = "\"";

  private static final String END_PAREN = " ) ";

  private static final String START_PAREN = " ( ";

  private static final String OR = " OR ";

  private static final String AND = " AND ";

  // *, ?, and / are escaped by the filter adapter
  private static final String[] LUCENE_SPECIAL_CHARACTERS =
      new String[] {"+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "~", ":"};

  private static final String[] ESCAPED_LUCENE_SPECIAL_CHARACTERS =
      new String[] {
        "\\+", "\\-", "\\&&", "\\||", "\\!", "\\(", "\\)", "\\{", "\\}", "\\[", "\\]", "\\^",
        "\\\"", "\\~", "\\:"
      };

  private static final Map<String, String> FIELD_MAP;

  private static final String SPATIAL_INDEX = "_geo_index";

  // key=solrCoreName.propertyName without suffix, e.g., "notification.user"
  // Since this FilterVisitor is used across multiple Solr cores and this cache map
  // is static, the key must be able to distinguish values that may have the same property name
  // in multiple cores.
  private static Map<String, SchemaField> schemaFieldsCache = new HashMap<>();

  static {
    Map<String, String> tempMap = new HashMap<>();
    tempMap.put("anyText", TOKENIZED_METADATA_FIELD);
    tempMap.put("anyGeo", "location" + SPATIAL_INDEX);
    FIELD_MAP = Collections.unmodifiableMap(tempMap);
  }

  private SchemaFieldResolver schemaFieldResolver;

  private String solrCoreName;

  public SolrQueryFilterVisitor(SolrClient client, String solrCoreName) {
    this(solrCoreName, new SchemaFieldResolver(client));
  }

  public SolrQueryFilterVisitor(String solrCoreName, SchemaFieldResolver schemaFieldResolver) {
    this.schemaFieldResolver = schemaFieldResolver;
    this.solrCoreName = solrCoreName;
  }

  @Override
  public Object visit(And filter, Object data) {
    List<Filter> childList = filter.getChildren();
    return logicalOperator(childList, AND, data);
  }

  @Override
  public Object visit(Or filter, Object data) {
    List<Filter> childList = filter.getChildren();
    return logicalOperator(childList, OR, data);
  }

  private Object logicalOperator(List<Filter> filters, String operator, Object data) {
    if (CollectionUtils.isEmpty(filters)) {
      throw new UnsupportedOperationException(
          "[" + operator + "] operation must contain 1 or more filters.");
    }

    StringBuilder builder = new StringBuilder();
    builder.append(START_PAREN);
    builder.append(
        filters.stream()
            .filter(Objects::nonNull)
            .map(e -> getQuery(e, data))
            .collect(Collectors.joining(operator)));
    builder.append(END_PAREN);

    return new SolrQuery(builder.toString());
  }

  private String getQuery(Filter filter, Object data) {
    Object query = filter.accept(this, data);
    if (query instanceof SolrQuery) {
      return ((SolrQuery) query).getQuery();
    }
    throw new UnsupportedOperationException("Query operation " + filter + " is not supported.");
  }

  @Override
  public Object visit(PropertyIsEqualTo filter, Object data) {
    LOGGER.debug("ENTERING: PropertyIsEqualTo filter");

    ExpressionValueVisitor expressionVisitor = new ExpressionValueVisitor();

    String propertyName = (String) filter.getExpression1().accept(expressionVisitor, data);
    Object literalValue = filter.getExpression2().accept(expressionVisitor, data);

    String mappedPropertyName = getMappedPropertyName(propertyName);

    return new SolrQuery(
        mappedPropertyName
            + ":"
            + QUOTE
            + escapeSpecialCharacters(literalValue.toString())
            + QUOTE);
  }

  @Override
  public Object visit(PropertyIsGreaterThan filter, Object data) {
    return processComparisonOperator(filter, " %s:{ %s TO * ] ");
  }

  @Override
  public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object data) {
    return processComparisonOperator(filter, " %s:[ %s TO * ] ");
  }

  @Override
  public Object visit(PropertyIsLessThan filter, Object data) {
    return processComparisonOperator(filter, " %s:[ * TO %s } ");
  }

  @Override
  public Object visit(PropertyIsLessThanOrEqualTo filter, Object data) {
    return processComparisonOperator(filter, " %s:[ * TO %s ] ");
  }

  SolrQuery processComparisonOperator(BinaryComparisonOperator filter, String solrQuery) {
    ExpressionValueVisitor expressionVisitor = new ExpressionValueVisitor();

    String propertyName = (String) filter.getExpression1().accept(expressionVisitor, null);
    Object literalValue = filter.getExpression2().accept(expressionVisitor, null);

    String mappedPropertyName = getMappedPropertyName(propertyName);
    return new SolrQuery(String.format(solrQuery, mappedPropertyName, literalValue.toString()));
  }

  String getMappedPropertyName(String propertyName) {
    String mappedPropertyName;

    // propertyName will not have the suffix. Field names (the keys) in the fieldsInfo map
    // will have the suffix and the variations on the property name, e.g., for propertyName="user"
    // fieldsInfo will have keys for "user_txt", "user_txt_tokenized", and
    // "user_txt_tokenized_has_case"
    SchemaField schemaField;
    String cacheKey = solrCoreName + "." + propertyName;
    if (schemaFieldsCache.containsKey(cacheKey)) {
      LOGGER.debug("Getting SchemaField for propertyName {} from cache", propertyName);
      schemaField = schemaFieldsCache.get(cacheKey);
    } else {
      LOGGER.debug("Using SchemaFieldResolver for propertyName {}", propertyName);
      schemaField = schemaFieldResolver.getSchemaField(propertyName, true);
      if (schemaField != null) {
        schemaFieldsCache.put(cacheKey, schemaField);
      }
    }

    if (schemaField != null) {
      mappedPropertyName = schemaField.getName();
      LOGGER.debug(
          "propertyName = {},    mappedPropertyName = {},   schemaField = {}",
          propertyName,
          mappedPropertyName,
          schemaField);
    } else {
      // Fallback - treat all fields as String
      mappedPropertyName = getMappedPropertyName(propertyName, AttributeFormat.STRING, true);
      LOGGER.debug("Used fallback to get mappedPropertyName of {}", mappedPropertyName);
    }

    return mappedPropertyName;
  }

  private String getMappedPropertyName(
      String propertyName, AttributeFormat format, boolean isSearchedAsExactString) {
    String specialField = FIELD_MAP.get(propertyName);
    if (specialField != null) {
      return specialField;
    }

    return getField(propertyName, format, isSearchedAsExactString);
  }

  private String escapeSpecialCharacters(String searchPhrase) {
    return StringUtils.replaceEach(
        searchPhrase, LUCENE_SPECIAL_CHARACTERS, ESCAPED_LUCENE_SPECIAL_CHARACTERS);
  }

  public String getField(
      String propertyName, AttributeFormat format, boolean isSearchedAsExactValue) {

    return propertyName
        + schemaFieldResolver.getFieldSuffix(format)
        + (isSearchedAsExactValue ? "" : getSpecialIndexSuffix(format));
  }

  protected String getSpecialIndexSuffix(AttributeFormat format) {

    switch (format) {
      case STRING:
        return SchemaFieldResolver.TOKENIZED;
      case GEOMETRY:
        return SchemaFieldResolver.INDEXED;
      case XML:
        return SchemaFieldResolver.TEXT_PATH;
      default:
        break;
    }

    return "";
  }
}
