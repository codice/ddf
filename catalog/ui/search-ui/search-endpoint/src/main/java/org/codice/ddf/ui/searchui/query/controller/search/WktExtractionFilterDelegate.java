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
 **/
package org.codice.ddf.ui.searchui.query.controller.search;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import ddf.catalog.filter.FilterDelegate;

public class WktExtractionFilterDelegate extends FilterDelegate<String> {

    // spatial operators

    @Override
    public String nearestNeighbor(String propertyName, String wkt) {
        return wkt;
    }

    @Override
    public String beyond(String propertyName, String wkt, double distance) {
        return wkt;
    }

    @Override
    public String contains(String propertyName, String wkt) {
        return wkt;
    }

    @Override
    public String crosses(String propertyName, String wkt) {
        return wkt;
    }

    @Override
    public String disjoint(String propertyName, String wkt) {
        return wkt;
    }

    @Override
    public String dwithin(String propertyName, String wkt, double distance) {
        return wkt;
    }

    @Override
    public String intersects(String propertyName, String wkt) {
        return wkt;
    }

    @Override
    public String overlaps(String propertyName, String wkt) {
        return wkt;
    }

    @Override
    public String touches(String propertyName, String wkt) {
        return wkt;
    }

    @Override
    public String within(String propertyName, String wkt) {
        return wkt;
    }

    // pass-through

    @Override
    public String and(List<String> operands) {
        return findFirstOperand(operands);
    }

    @Override
    public String or(List<String> operands) {
        return findFirstOperand(operands);
    }

    private String findFirstOperand(List<String> operands) {
        for (String operand : operands) {
            if (StringUtils.isNotBlank(operand)) {
                return operand;
            }
        }
        return "";
    }

    @Override
    public String not(String operand) {
        return "";
    }

    @Override
    public String include() {
        return "";
    }

    @Override
    public String exclude() {
        return "";
    }

    @Override
    public String propertyIsEqualTo(String propertyName, String literal, boolean isCaseSensitive) {
        return "";
    }

    @Override
    public String propertyIsEqualTo(String propertyName, Date literal) {
        return "";
    }

    @Override
    public String propertyIsEqualTo(String propertyName, Date startDate, Date endDate) {
        return "";
    }

    @Override
    public String propertyIsEqualTo(String propertyName, int literal) {
        return "";
    }

    @Override
    public String propertyIsEqualTo(String propertyName, short literal) {
        return "";
    }

    @Override
    public String propertyIsEqualTo(String propertyName, long literal) {
        return "";
    }

    @Override
    public String propertyIsEqualTo(String propertyName, float literal) {
        return "";
    }

    @Override
    public String propertyIsEqualTo(String propertyName, double literal) {
        return "";
    }

    @Override
    public String propertyIsEqualTo(String propertyName, boolean literal) {
        return "";
    }

    @Override
    public String propertyIsEqualTo(String propertyName, byte[] literal) {
        return "";
    }

    @Override
    public String propertyIsEqualTo(String propertyName, Object literal) {
        return "";
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, String literal,
            boolean isCaseSensitive) {
        return "";
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, Date literal) {
        return "";
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, Date startDate, Date endDate) {
        return "";
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, int literal) {
        return "";
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, short literal) {
        return "";
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, long literal) {
        return "";
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, float literal) {
        return "";
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, double literal) {
        return "";
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, boolean literal) {
        return "";
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, byte[] literal) {
        return "";
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, Object literal) {
        return "";
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, String literal) {
        return "";
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, Date literal) {
        return "";
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, int literal) {
        return "";
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, short literal) {
        return "";
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, long literal) {
        return "";
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, float literal) {
        return "";
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, double literal) {
        return "";
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, Object literal) {
        return "";
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, String literal) {
        return "";
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, Date literal) {
        return "";
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, int literal) {
        return "";
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, short literal) {
        return "";
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, long literal) {
        return "";
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, float literal) {
        return "";
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, double literal) {
        return "";
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, Object literal) {
        return "";
    }

    @Override
    public String propertyIsLessThan(String propertyName, String literal) {
        return "";
    }

    @Override
    public String propertyIsLessThan(String propertyName, Date literal) {
        return "";
    }

    @Override
    public String propertyIsLessThan(String propertyName, int literal) {
        return "";
    }

    @Override
    public String propertyIsLessThan(String propertyName, short literal) {
        return "";
    }

    @Override
    public String propertyIsLessThan(String propertyName, long literal) {
        return "";
    }

    @Override
    public String propertyIsLessThan(String propertyName, float literal) {
        return "";
    }

    @Override
    public String propertyIsLessThan(String propertyName, double literal) {
        return "";
    }

    @Override
    public String propertyIsLessThan(String propertyName, Object literal) {
        return "";
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, String literal) {
        return "";
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, Date literal) {
        return "";
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, int literal) {
        return "";
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, short literal) {
        return "";
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, long literal) {
        return "";
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, float literal) {
        return "";
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, double literal) {
        return "";
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, Object literal) {
        return "";
    }

    @Override
    public String propertyIsBetween(String propertyName, String lowerBoundary,
            String upperBoundary) {
        return "";
    }

    @Override
    public String propertyIsBetween(String propertyName, Date lowerBoundary, Date upperBoundary) {
        return "";
    }

    @Override
    public String propertyIsBetween(String propertyName, int lowerBoundary, int upperBoundary) {
        return "";
    }

    @Override
    public String propertyIsBetween(String propertyName, short lowerBoundary, short upperBoundary) {
        return "";
    }

    @Override
    public String propertyIsBetween(String propertyName, long lowerBoundary, long upperBoundary) {
        return "";
    }

    @Override
    public String propertyIsBetween(String propertyName, float lowerBoundary, float upperBoundary) {
        return "";
    }

    @Override
    public String propertyIsBetween(String propertyName, double lowerBoundary,
            double upperBoundary) {
        return "";
    }

    @Override
    public String propertyIsBetween(String propertyName, Object lowerBoundary,
            Object upperBoundary) {
        return "";
    }

    @Override
    public String propertyIsNull(String propertyName) {
        return "";
    }

    @Override
    public String propertyIsLike(String propertyName, String pattern, boolean isCaseSensitive) {
        return "";
    }

    @Override
    public String propertyIsFuzzy(String propertyName, String literal) {
        return "";
    }

    @Override
    public String xpathExists(String xpath) {
        return "";
    }

    @Override
    public String xpathIsLike(String xpath, String pattern, boolean isCaseSensitive) {
        return "";
    }

    @Override
    public String xpathIsFuzzy(String xpath, String literal) {
        return "";
    }

    @Override
    public String after(String propertyName, Date date) {
        return "";
    }

    @Override
    public String before(String propertyName, Date date) {
        return "";
    }

    @Override
    public String during(String propertyName, Date startDate, Date endDate) {
        return "";
    }

    @Override
    public String relative(String propertyName, long duration) {
        return "";
    }
}
