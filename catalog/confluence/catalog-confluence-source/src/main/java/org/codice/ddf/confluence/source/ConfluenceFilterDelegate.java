/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.confluence.source;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.codice.ddf.confluence.common.Confluence;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Topic;
import ddf.catalog.filter.impl.SimpleFilterDelegate;

public class ConfluenceFilterDelegate extends SimpleFilterDelegate<String> {

    public static final Map<String, ConfluenceQueryParameter> QUERY_PARAMETERS;

    private static final String PROPERTY_FORMAT = "%s %s \"%s\"";

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";

    private static final String WILD_CARD = "*";

    private boolean wildcardQuery = false;

    private int parameterCount = 0;

    private Set<String> queryTags = new HashSet<>();

    static {
        Map<String, ConfluenceQueryParameter> params = new HashMap<>();
        params.put(Metacard.ID,
                new ConfluenceQueryParameter("id", false, true, false, false, true));
        params.put(Metacard.CONTENT_TYPE,
                new ConfluenceQueryParameter("type", false, true, false, false, true));
        params.put(Metacard.TITLE,
                new ConfluenceQueryParameter("title", true, true, false, true, false));
        params.put(Metacard.ANY_TEXT,
                new ConfluenceQueryParameter("text", true, false, false, true, false));
        params.put(Metacard.CREATED,
                new ConfluenceQueryParameter("created", false, true, true, false, false));
        params.put(Metacard.MODIFIED,
                new ConfluenceQueryParameter("lastmodified", false, true, true, false, false));
        params.put(Confluence.TYPE,
                new ConfluenceQueryParameter("type", false, true, false, false, true));
        params.put(Contact.CREATOR_NAME,
                new ConfluenceQueryParameter("creator", false, true, false, true, true));
        params.put(Contact.CONTRIBUTOR_NAME,
                new ConfluenceQueryParameter("contributor", false, true, false, true, true));
        params.put(Topic.KEYWORD,
                new ConfluenceQueryParameter("label", false, true, false, false, true));
        params.put(Core.METACARD_CREATED,
                new ConfluenceQueryParameter("created", false, true, true, false, false));
        params.put(Core.METACARD_MODIFIED,
                new ConfluenceQueryParameter("lastmodified", false, true, true, false, false));
        QUERY_PARAMETERS = Collections.unmodifiableMap(params);

    }

    @Override
    public String and(List<String> operands) {
        return operands.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" AND ", "( ", " )"))
                .replaceAll("\\(\\s*\\)", "");
    }

    @Override
    public String or(List<String> operands) {
        return operands.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" OR ", "( ", " )"))
                .replaceAll("\\(\\s*\\)", "");
    }

    @Override
    public String not(String operand) {
        return "NOT " + operand;
    }

    @Override
    public String propertyIsEqualTo(String propertyName, String literal, boolean isCaseSensitive) {
        ConfluenceQueryParameter param = QUERY_PARAMETERS.get(propertyName);
        propertyCheck(propertyName, literal);
        if (param == null) {
            return null;
        }
        return param.getEqualExpression(literal);
    }

    @Override
    public String propertyIsLike(String propertyName, String literal, boolean isCaseSensitive) {
        ConfluenceQueryParameter param = QUERY_PARAMETERS.get(propertyName);
        propertyCheck(propertyName, literal);
        if (param == null) {
            return null;
        }
        return Arrays.stream(literal.split(" "))
                .map(e -> param.getLikeExpression(e))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" OR ", "( ", " )"))
                .replaceAll("\\(\\s*\\)", "");
    }

    public String after(String propertyName, Date date) {
        ConfluenceQueryParameter param = QUERY_PARAMETERS.get(propertyName);
        propertyCheck(propertyName, null);
        if (param == null) {
            return null;
        }
        return param.getGreaterThanExpression(new SimpleDateFormat(DATE_FORMAT).format(date));
    }

    public String before(String propertyName, Date date) {
        ConfluenceQueryParameter param = QUERY_PARAMETERS.get(propertyName);
        propertyCheck(propertyName, null);
        if (param == null) {
            return null;
        }
        return param.getLessThanExpression(new SimpleDateFormat(DATE_FORMAT).format(date));
    }

    public String during(String propertyName, Date startDate, Date endDate) {
        ConfluenceQueryParameter param = QUERY_PARAMETERS.get(propertyName);
        propertyCheck(propertyName, null);
        if (param == null) {
            return null;
        }
        return param.getGreaterThanExpression(new SimpleDateFormat(DATE_FORMAT).format(startDate))
                + " AND " + param.getLessThanExpression(new SimpleDateFormat(DATE_FORMAT).format(
                endDate));
    }

    private void propertyCheck(String name, String literal) {
        parameterCount++;
        if (Metacard.TAGS.equals(name)) {
            queryTags.add(literal);
        }
        if (Metacard.ANY_TEXT.equals(name) && WILD_CARD.equals(literal)) {
            wildcardQuery = true;
        }
    }

    public boolean isConfluenceQuery() {
        return queryTags.isEmpty() || queryTags.contains(Metacard.DEFAULT_TAG)
                || queryTags.contains(Confluence.CONFLUENCE_TAG);
    }

    public boolean isWildCardQuery() {
        return wildcardQuery && parameterCount <= 1;
    }

    static class ConfluenceQueryParameter {

        private boolean likeValid = false;

        private boolean equalValid = false;

        private boolean greaterLessThanValid = false;

        private boolean wildCardValid = false;

        private boolean translateLike = false;

        private String paramterName;

        public ConfluenceQueryParameter(String paramterName, boolean likeValid, boolean equalValid,
                boolean greaterLessThanValid, boolean wildCardValid, boolean translateLike) {
            this.paramterName = paramterName;
            this.likeValid = likeValid;
            this.equalValid = equalValid;
            this.greaterLessThanValid = greaterLessThanValid;
            this.wildCardValid = wildCardValid;
            this.translateLike = translateLike;
        }

        public String getLikeExpression(String literal) {
            String operator = "~";
            if (!likeValid) {
                if (translateLike) {
                    operator = "=";
                } else {
                    return null;
                }
            }
            if (invalidWildCard(literal)) {
                return null;
            }
            return String.format(PROPERTY_FORMAT, paramterName, operator, literal);
        }

        public String getEqualExpression(String literal) {
            if (!equalValid || invalidWildCard(literal)) {
                return null;
            }
            return String.format(PROPERTY_FORMAT, paramterName, "=", literal);
        }

        public String getGreaterThanExpression(String literal) {
            if (!greaterLessThanValid || invalidWildCard(literal)) {
                return null;
            }
            return String.format(PROPERTY_FORMAT, paramterName, ">", literal);
        }

        public String getLessThanExpression(String literal) {
            if (!greaterLessThanValid || invalidWildCard(literal)) {
                return null;
            }
            return String.format(PROPERTY_FORMAT, paramterName, "<", literal);
        }

        public String getParamterName() {
            return paramterName;
        }

        private boolean invalidWildCard(String literal) {
            return (!wildCardValid && literal.indexOf(WILD_CARD) >= 0) || WILD_CARD.equals(literal);
        }
    }
}
