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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.nato.stanag4559.common.GIAS.AttributeInformation;

public class Stanag4559FilterDelegate extends FilterDelegate<String> {

    private Stanag4559FilterFactory filterFactory;

    public static final String EMPTY_STRING = "";

    public static final String SQ = "'";

    private static final String UTC = "UTC";

    private static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";

    public Stanag4559FilterDelegate(HashMap<String, List<AttributeInformation>> queryableAttributes,
            String view) {
        filterFactory = new Stanag4559FilterFactory(queryableAttributes, view);
    }

    @Override
    public String and(List<String> filters) {
        String filter = filterFactory.buildAndFilter(filters);
        return filter;
    }

    @Override
    public String or(List<String> filters) {
        String filter = filterFactory.buildOrFilter(filters);
        return filter;
    }

    @Override
    public String not(String filter) {
        String newFilter = filterFactory.buildNotFilter(filter);
        return newFilter;
    }

    @Override
    public String propertyIsEqualTo(String propertyName, String literal, boolean isCaseSensitive) {
        String filter = filterFactory.buildPropertyIsEqualTo(propertyName, SQ + literal + SQ);
        return filter;
    }

    @Override
    public String propertyIsEqualTo(String propertyName, Date literal) {
        String filter = filterFactory.buildPropertyIsEqualTo(propertyName,
                getStringFromDate(literal));
        return filter;
    }

    @Override
    public String propertyIsEqualTo(String propertyName, int literal) {
        String filter = filterFactory.buildPropertyIsEqualTo(propertyName, EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsEqualTo(String propertyName, short literal) {
        String filter = filterFactory.buildPropertyIsEqualTo(propertyName, EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsEqualTo(String propertyName, long literal) {
        String filter = filterFactory.buildPropertyIsEqualTo(propertyName, EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsEqualTo(String propertyName, float literal) {
        String filter = filterFactory.buildPropertyIsEqualTo(propertyName, EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsEqualTo(String propertyName, double literal) {
        String filter = filterFactory.buildPropertyIsEqualTo(propertyName, EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsEqualTo(String propertyName, boolean literal) {
        String filter = filterFactory.buildPropertyIsEqualTo(propertyName, SQ + Boolean.valueOf(
                literal)
                .toString()
                .toUpperCase() + SQ);
        return filter;
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, String literal,
            boolean isCaseSensitive) {
        String filter = filterFactory.buildPropertyIsNotEqualTo(propertyName, SQ + literal + SQ);
        return filter;
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, Date literal) {
        String filter = filterFactory.buildPropertyIsNotEqualTo(propertyName, getStringFromDate(
                literal));
        return filter;
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, int literal) {
        String filter = filterFactory.buildPropertyIsNotEqualTo(propertyName,
                EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, short literal) {
        String filter = filterFactory.buildPropertyIsNotEqualTo(propertyName,
                EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, long literal) {
        String filter = filterFactory.buildPropertyIsNotEqualTo(propertyName,
                EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, float literal) {
        String filter = filterFactory.buildPropertyIsNotEqualTo(propertyName,
                EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, double literal) {
        String filter = filterFactory.buildPropertyIsNotEqualTo(propertyName,
                EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, boolean literal) {
        String filter = filterFactory.buildPropertyIsNotEqualTo(propertyName, SQ + Boolean.valueOf(
                literal)
                .toString()
                .toUpperCase() + SQ);
        return filter;
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, String literal) {
        String filter = filterFactory.buildPropertyIsGreaterThan(propertyName, SQ + literal + SQ);
        return filter;
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, Date literal) {
        String filter = filterFactory.buildPropertyIsGreaterThan(propertyName, getStringFromDate(
                literal));
        return filter;
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, int literal) {
        String filter = filterFactory.buildPropertyIsGreaterThan(propertyName,
                EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, short literal) {
        String filter = filterFactory.buildPropertyIsGreaterThan(propertyName,
                EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, long literal) {
        String filter = filterFactory.buildPropertyIsGreaterThan(propertyName,
                EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, float literal) {
        String filter = filterFactory.buildPropertyIsGreaterThan(propertyName,
                EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, double literal) {
        String filter = filterFactory.buildPropertyIsGreaterThan(propertyName,
                EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, String literal) {
        String filter = filterFactory.buildPropertyIsGreaterThanOrEqual(propertyName,
                EMPTY_STRING + SQ + literal + SQ);
        return filter;
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, Date literal) {
        String filter = filterFactory.buildPropertyIsGreaterThanOrEqual(propertyName,
                getStringFromDate(literal));
        return filter;
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, int literal) {
        String filter = filterFactory.buildPropertyIsGreaterThanOrEqual(propertyName,
                EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, short literal) {
        String filter = filterFactory.buildPropertyIsGreaterThanOrEqual(propertyName,
                EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, long literal) {
        String filter = filterFactory.buildPropertyIsGreaterThanOrEqual(propertyName,
                EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, float literal) {
        String filter = filterFactory.buildPropertyIsGreaterThanOrEqual(propertyName,
                EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, double literal) {
        String filter = filterFactory.buildPropertyIsGreaterThanOrEqual(propertyName,
                EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsLessThan(String propertyName, String literal) {
        String filter = filterFactory.buildPropertyIsLessThan(propertyName,
                EMPTY_STRING + SQ + literal + SQ);
        return filter;
    }

    @Override
    public String propertyIsLessThan(String propertyName, Date literal) {
        String filter = filterFactory.buildPropertyIsLessThan(propertyName, getStringFromDate(
                literal));
        return filter;
    }

    @Override
    public String propertyIsLessThan(String propertyName, int literal) {
        String filter = filterFactory.buildPropertyIsLessThan(propertyName, EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsLessThan(String propertyName, short literal) {
        String filter = filterFactory.buildPropertyIsLessThan(propertyName, EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsLessThan(String propertyName, long literal) {
        String filter = filterFactory.buildPropertyIsLessThan(propertyName, EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsLessThan(String propertyName, float literal) {
        String filter = filterFactory.buildPropertyIsLessThan(propertyName, EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsLessThan(String propertyName, double literal) {
        String filter = filterFactory.buildPropertyIsLessThan(propertyName, EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, String literal) {
        String filter = filterFactory.buildPropertyIsLessThanOrEqual(propertyName,
                EMPTY_STRING + SQ + literal + SQ);
        return filter;
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, Date literal) {
        String filter = filterFactory.buildPropertyIsLessThanOrEqual(propertyName,
                getStringFromDate(literal));
        return filter;
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, int literal) {
        String filter = filterFactory.buildPropertyIsLessThanOrEqual(propertyName,
                EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, short literal) {
        String filter = filterFactory.buildPropertyIsLessThanOrEqual(propertyName,
                EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, long literal) {
        String filter = filterFactory.buildPropertyIsLessThanOrEqual(propertyName,
                EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, float literal) {
        String filter = filterFactory.buildPropertyIsLessThanOrEqual(propertyName,
                EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, double literal) {
        String filter = filterFactory.buildPropertyIsLessThanOrEqual(propertyName,
                EMPTY_STRING + literal);
        return filter;
    }

    @Override
    public String propertyIsBetween(String propertyName, String lowerBoundary,
            String upperBoundary) {
        String filter = filterFactory.buildPropertyIsBetween(propertyName,
                SQ + lowerBoundary + SQ,
                SQ + upperBoundary + SQ);
        return filter;
    }

    @Override
    public String propertyIsBetween(String propertyName, Date lowerBoundary, Date upperBoundary) {
        String filter = filterFactory.buildPropertyIsBetween(propertyName, getStringFromDate(
                lowerBoundary), getStringFromDate(upperBoundary));
        return filter;
    }

    @Override
    public String propertyIsBetween(String propertyName, int lowerBoundary, int upperBoundary) {
        String filter = filterFactory.buildPropertyIsBetween(propertyName,
                EMPTY_STRING + lowerBoundary,
                EMPTY_STRING + upperBoundary);
        return filter;
    }

    @Override
    public String propertyIsBetween(String propertyName, short lowerBoundary, short upperBoundary) {
        String filter = filterFactory.buildPropertyIsBetween(propertyName,
                EMPTY_STRING + lowerBoundary,
                EMPTY_STRING + upperBoundary);
        return filter;
    }

    @Override
    public String propertyIsBetween(String propertyName, long lowerBoundary, long upperBoundary) {
        String filter = filterFactory.buildPropertyIsBetween(propertyName,
                EMPTY_STRING + lowerBoundary,
                EMPTY_STRING + upperBoundary);
        return filter;
    }

    @Override
    public String propertyIsBetween(String propertyName, float lowerBoundary, float upperBoundary) {
        String filter = filterFactory.buildPropertyIsBetween(propertyName,
                EMPTY_STRING + lowerBoundary,
                EMPTY_STRING + upperBoundary);
        return filter;
    }

    @Override
    public String propertyIsBetween(String propertyName, double lowerBoundary,
            double upperBoundary) {
        String filter = filterFactory.buildPropertyIsBetween(propertyName,
                EMPTY_STRING + lowerBoundary,
                EMPTY_STRING + upperBoundary);
        return filter;
    }

    @Override
    public String propertyIsNull(String propertyName) {
        String filter = filterFactory.buildPropertyIsNull(propertyName);
        return filter;
    }

    @Override
    public String propertyIsLike(String propertyName, String pattern, boolean isCaseSensitive) {
        String filter = filterFactory.buildPropertyIsLike(pattern);
        return filter;
    }

    // TODO :  Spatial
    @Override
    public String disjoint(String propertyName, String wkt) {
        return EMPTY_STRING;
    }

    @Override
    public String within(String propertyName, String wkt) {
        return EMPTY_STRING;
    }

    @Override
    public String intersects(String propertyName, String wkt) {
        return EMPTY_STRING;
    }

    @Override
    public String dwithin(String propertyName, String wkt, double distance) {
        return EMPTY_STRING;
    }

    @Override
    public String beyond(String propertyName, String wkt, double distance) {
        return EMPTY_STRING;
    }

    private String getStringFromDate(Date date) {
        TimeZone timeZone = TimeZone.getTimeZone(UTC);
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTime(date);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
        return SQ + simpleDateFormat.format(date) + SQ;
    }
}
