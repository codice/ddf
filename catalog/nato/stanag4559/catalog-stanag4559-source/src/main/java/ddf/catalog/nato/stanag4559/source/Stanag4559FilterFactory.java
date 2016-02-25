/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.nato.stanag4559.source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ddf.catalog.nato.stanag4559.common.GIAS.AttributeInformation;
import ddf.catalog.nato.stanag4559.common.GIAS.AttributeType;

public class Stanag4559FilterFactory {

    public static final String METADATA_CONTENT_TYPE = "metadata-content-type";

    public static final String TYPE = "type";

    public static final String LP = "(";

    public static final String RP = ")";

    public static final String LIKE = " like ";

    public static final String OR = " or ";

    public static final String AND = " and ";

    public static final String EXISTS = " exists";

    public static final String NOT = "not ";

    public static final String EQ = " =  ";

    public static final String LT = " < ";

    public static final String GT = " > ";

    public static final String LTE = " <= ";

    public static final String GTE = " >= ";

    public static final String BTW = " <> ";

    public static final String COMMA = ",";

    private HashMap<String, List<AttributeInformation>> queryableAttributes;

    private String view;

    public Stanag4559FilterFactory(HashMap<String, List<AttributeInformation>> queryableAttributes,
            String view) {
        this.queryableAttributes = queryableAttributes;
        this.view = view;
    }

    public String buildPropertyIsLike(String value) {
        if (queryableAttributes == null) {
            return Stanag4559FilterDelegate.EMPTY_STRING;
        }

        List<AttributeInformation> attributeInformationList = queryableAttributes.get(view);

        if (attributeInformationList == null) {
            return Stanag4559FilterDelegate.EMPTY_STRING;
        }

        List<String> filters = new ArrayList<>();

        // Replace * with %, since * is not a valid wildcard in BQS
        value = value.replaceAll("\\*", "%");
        value = value.replaceAll("\\?", "%");

        for (AttributeInformation attributeInformation : attributeInformationList) {
            if (isTextAttributeType(attributeInformation)) {
                filters.add(LP + attributeInformation.attribute_name + LIKE
                        + Stanag4559FilterDelegate.SQ + value + Stanag4559FilterDelegate.SQ + RP);
            }
        }
        return buildOrFilter(filters);
    }

    public boolean isTextAttributeType(AttributeInformation attributeInformation) {
        return attributeInformation.attribute_type.equals(AttributeType.TEXT)
                && !attributeInformation.attribute_name.equals(TYPE);
    }

    public String buildPropertyIsEqualTo(String property, String value) {
        property = mapToNsil(property);
        return LP + property + EQ + value + RP;
    }

    public String buildPropertyIsNotEqualTo(String property, String value) {
        property = mapToNsil(property);
        return buildNotFilter(buildPropertyIsEqualTo(property, value));
    }

    public String buildPropertyIsGreaterThan(String property, String value) {
        property = mapToNsil(property);
        return LP + property + GT + value + RP;
    }

    public String buildPropertyIsGreaterThanOrEqual(String property, String value) {
        property = mapToNsil(property);
        return LP + property + GTE + value + RP;
    }

    public String buildPropertyIsLessThan(String property, String value) {
        property = mapToNsil(property);
        return LP + property + LT + value + RP;
    }

    public String buildPropertyIsLessThanOrEqual(String property, String value) {
        property = mapToNsil(property);
        return LP + property + LTE + value + RP;
    }

    public String buildPropertyIsBetween(String property, String lowerBound, String upperBound) {
        property = mapToNsil(property);
        return LP + property + BTW + lowerBound + COMMA + upperBound + RP;
    }

    public String buildOrFilter(List<String> filters) {
        if (filters.size() == 0) {
            return Stanag4559FilterDelegate.EMPTY_STRING;
        }

        if (filters.size() == 1) {
            return filters.get(0);
        }

        StringBuilder stringBuilder = new StringBuilder();

        for (String filter : filters) {
            stringBuilder.append(filter + OR);
        }

        String result = stringBuilder.toString();
        return result.substring(0, result.length() - 4);
    }

    public String buildAndFilter(List<String> filters) {
        if (filters.size() == 0) {
            return Stanag4559FilterDelegate.EMPTY_STRING;
        }

        if (filters.size() == 1) {
            return filters.get(0);
        }

        StringBuilder stringBuilder = new StringBuilder(LP);

        for (String filter : filters) {
            stringBuilder.append(filter + AND);
        }
        String result = stringBuilder.toString();
        return result.substring(0, result.length() - 5) + RP;
    }

    public String buildPropertyIsNull(String property) {
        return buildNotFilter(LP + property + EXISTS + RP);
    }

    public String buildNotFilter(String filter) {
        return NOT + filter;
    }

    public String mapToNsil(String attribute) {

        if (attribute.equals(METADATA_CONTENT_TYPE)) {
            return TYPE;
        }
        return attribute;
    }

}
