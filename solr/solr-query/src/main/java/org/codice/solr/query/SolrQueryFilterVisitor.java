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

    public static final String TOKENIZED_METADATA_FIELD = "metadata_txt" + SchemaFields.TOKENIZED;
    
    static {
        Map<String, String> tempMap = new HashMap<String, String>();
        tempMap.put("anyText", TOKENIZED_METADATA_FIELD);
        //tempMap.put(Metacard.ANY_GEO, Metacard.GEOGRAPHY + SPATIAL_INDEX);
        FIELD_MAP = Collections.unmodifiableMap(tempMap);
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

        // Change hard-coded STRING to use a DynamicSchemaResolver that caches all columns based on suffix to format
        // need to figure out how to get column metadata from Solr schema
        String mappedPropertyName = getMappedPropertyName(propertyName, AttributeFormat.STRING,
                true);
        
        return new SolrQuery(mappedPropertyName + ":" + QUOTE + escapeSpecialCharacters(literalValue)
                + QUOTE);
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

        // Uses DynamicSchemaResolver to add field suffix (e.g., _xml) 
//        String mappedPropertyName = resolver.getField(propertyName, format, isSearchedAsExactString);
        String mappedPropertyName = getField(propertyName, format, isSearchedAsExactString);
        
        return mappedPropertyName;
    }
    
    private String escapeSpecialCharacters(String searchPhrase) {
        return StringUtils.replaceEach(searchPhrase, LUCENE_SPECIAL_CHARACTERS,
                ESCAPED_LUCENE_SPECIAL_CHARACTERS);
    }
    
    public String getField(String propertyName, AttributeFormat format,
            boolean isSearchedAsExactValue) {

        String fieldName = propertyName + SchemaFields.FORMAT_TO_SUFFIX_MAP.get(format)
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
            return SchemaFields.TOKENIZED;
        case GEOMETRY:
            return SchemaFields.INDEXED;
        case XML:
            return SchemaFields.TEXT_PATH;
        default:
            break;
        }

        return "";
    }
}
