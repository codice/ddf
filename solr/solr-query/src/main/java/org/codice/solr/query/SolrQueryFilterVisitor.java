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
package org.codice.solr.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.opengis.filter.PropertyIsEqualTo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SolrQueryFilterVisitor extends DefaultFilterVisitor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SolrQueryFilterVisitor.class);
    
    private static final String QUOTE = "\"";
    
    // *, ?, and / are escaped by the filter adapter
    private static final String[] LUCENE_SPECIAL_CHARACTERS = new String[] {"+", "-", "&&", "||",
        "!", "(", ")", "{", "}", "[", "]", "^", "\"", "~", ":"};

    private static final String[] ESCAPED_LUCENE_SPECIAL_CHARACTERS = new String[] {"\\+", "\\-",
        "\\&&", "\\||", "\\!", "\\(", "\\)", "\\{", "\\}", "\\[", "\\]", "\\^", "\\\"", "\\~",
        "\\:"};
      
    private static final Map<String, String> FIELD_MAP;

    public static final String TOKENIZED_METADATA_FIELD = "metadata_txt" + SchemaFieldResolver.TOKENIZED;
    
    private static final String SPATIAL_INDEX = "_geo_index";
    
    static {
        Map<String, String> tempMap = new HashMap<String, String>();
        tempMap.put("anyText", TOKENIZED_METADATA_FIELD);
        tempMap.put("anyGeo", "location" + SPATIAL_INDEX);
        FIELD_MAP = Collections.unmodifiableMap(tempMap);
    }
    
    private SchemaFieldResolver schemaFieldResolver;
    
    private String solrCoreName;
    
    // key=solrCoreName.propertyName without suffix, e.g., "notification.user"
    // Since this FilterVisitor is used across multiple Solr cores and this cache map
    // is static, the key must be able to distinguish values that may have the same property name
    // in multiple cores.
    private static Map<String, SchemaField> SCHEMA_FIELDS_CACHE = new HashMap<String, SchemaField>();
    
    
    public SolrQueryFilterVisitor(SolrServer solrServer, String solrCoreName) {
        schemaFieldResolver = new SchemaFieldResolver(solrServer);
        this.solrCoreName = solrCoreName;
    }

    @Override
    public Object visit(PropertyIsEqualTo filter, Object data) {
        LOGGER.debug("ENTERING: PropertyIsEqualTo filter");
        
        ExpressionValueVisitor expressionVisitor = new ExpressionValueVisitor();

        filter.getExpression1().accept(expressionVisitor, data);
        filter.getExpression2().accept(expressionVisitor, data);

        String propertyName = expressionVisitor.getPropertyName();

        String literalValue = (String) expressionVisitor.getLiteralValue();
        
//        if (!isCaseSensitive) {
//            throw new UnsupportedOperationException(
//                    "Case insensitive exact searches are not supported.");
//        }

        if (StringUtils.isBlank(propertyName)) {
            throw new UnsupportedOperationException("PropertyName is required for search.");
        }
        
        if (StringUtils.isBlank(literalValue)) {
            throw new UnsupportedOperationException("Literal value is required for search.");
        }        

        String mappedPropertyName = getMappedPropertyName(propertyName);
       
        return new SolrQuery(mappedPropertyName + ":" + QUOTE + escapeSpecialCharacters(literalValue)
                + QUOTE);
    }
    
    String getMappedPropertyName(String propertyName) {
        String mappedPropertyName = null;
        
        // propertyName will not have the suffix. Field names (the keys) in the fieldsInfo map
        // will have the suffix and the variations on the property name, e.g., for propertyName="user"
        // fieldsInfo will have keys for "user_txt", "user_txt_tokenized", and "user_txt_tokenized_has_case"
        SchemaField schemaField = null;
        String cacheKey = solrCoreName + "." + propertyName;
        if (SCHEMA_FIELDS_CACHE.containsKey(cacheKey)) {
            LOGGER.info("Getting SchemaField for propertyName {} from cache", propertyName);
            schemaField = SCHEMA_FIELDS_CACHE.get(cacheKey);
        } else {
            LOGGER.info("Using SchemaFieldResolver for propertyName {}", propertyName);
            schemaField = schemaFieldResolver.getSchemaField(propertyName, true);
            SCHEMA_FIELDS_CACHE.put(cacheKey, schemaField);
        }        
        
        if (schemaField != null) {
            mappedPropertyName = schemaField.getName();
            LOGGER.info("propertyName = {},    mappedPropertyName = {},   schemaField = {}", 
                    propertyName, mappedPropertyName, schemaField);
        } else {
            // Fallback - treat all fields as String
            mappedPropertyName = getMappedPropertyName(propertyName, AttributeFormat.STRING,
                    true);
            LOGGER.info("Used fallback to get mappedPropertyName of {}", mappedPropertyName);
        }
        
        return mappedPropertyName;
    }
    
    private String getMappedPropertyName(String propertyName, AttributeFormat format,
            boolean isSearchedAsExactString) {
//        if (propertyName == null) {
//            throw new UnsupportedOperationException("Property name should not be null.");
//        }
        
        String specialField = FIELD_MAP.get(propertyName);
        if (specialField != null) {
            return specialField;
        }

        String mappedPropertyName = getField(propertyName, format, isSearchedAsExactString);
        
        return mappedPropertyName;
    }
    
    private String escapeSpecialCharacters(String searchPhrase) {
        return StringUtils.replaceEach(searchPhrase, LUCENE_SPECIAL_CHARACTERS,
                ESCAPED_LUCENE_SPECIAL_CHARACTERS);
    }
    
    public String getField(String propertyName, AttributeFormat format,
            boolean isSearchedAsExactValue) {

        String fieldName = propertyName + schemaFieldResolver.getFieldSuffix(format)
                + (isSearchedAsExactValue ? "" : getSpecialIndexSuffix(format));

//        if (fieldsCache.contains(fieldName)) {
//            return fieldName;
//        }
//
//        switch (format) {
//        case DOUBLE:
//        case LONG:
//        case INTEGER:
//        case SHORT:
//        case FLOAT:
//            return findAnyMatchingNumericalField(propertyName);
//        default:
//            break;
//        }
//
//        LOGGER.info("Could not find exact schema field name for [{}], attempting to search with [{}]", propertyName, fieldName);

        return fieldName;
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
