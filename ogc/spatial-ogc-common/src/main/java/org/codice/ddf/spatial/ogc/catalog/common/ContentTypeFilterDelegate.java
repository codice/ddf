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
package org.codice.ddf.spatial.ogc.catalog.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.filter.FilterDelegate;

/**
 * Extracts list of content types from filter
 * 
 * @author Jason Smith
 * @author ddf.isgs@lmco.com
 * 
 */
public class ContentTypeFilterDelegate extends FilterDelegate<List<ContentType>> {

    // Logical operators
    @Override
    public List<ContentType> include() {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> exclude() {
        return Collections.<ContentType> emptyList();

    }

    @Override
    public List<ContentType> not(List<ContentType> operand) {
        return Collections.<ContentType> emptyList();

    }

    @Override
    public List<ContentType> and(List<List<ContentType>> operands) {
        return combineLists(operands);
    }

    @Override
    public List<ContentType> or(List<List<ContentType>> operands) {
        return combineLists(operands);
    }

    // PropertyIsNull
    @Override
    public List<ContentType> propertyIsNull(String propertyName) {
        return Collections.<ContentType> emptyList();
    }

    // PropertyIsLike
    @Override
    public List<ContentType> propertyIsLike(String propertyName, String pattern,
            boolean isCaseSensitive) {
        return propertyIsEqualTo(propertyName, pattern, isCaseSensitive);
    }

    // PropertyIsFuzzy
    @Override
    public List<ContentType> propertyIsFuzzy(String propertyName, String literal) {
        return Collections.<ContentType> emptyList();
    }

    // PropertyIsEqualTo
    @Override
    public List<ContentType> propertyIsEqualTo(String propertyName, String literal,
            boolean isCaseSensitive) {
        List<ContentType> types = null;
        verifyInputData(propertyName, literal);

        if (propertyName.equalsIgnoreCase(Metacard.CONTENT_TYPE)) {

            ContentType type = new ContentTypeImpl(literal, "");
            types = new ArrayList<ContentType>();
            types.add(type);
        } else {
            types = Collections.<ContentType> emptyList();
        }

        return types;
    }

    @Override
    public List<ContentType> propertyIsEqualTo(String propertyName, Date literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsEqualTo(String propertyName, Date startDate, Date endDate) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsEqualTo(String propertyName, int literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsEqualTo(String propertyName, short literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsEqualTo(String propertyName, long literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsEqualTo(String propertyName, double literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsEqualTo(String propertyName, float literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsEqualTo(String propertyName, byte[] literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsEqualTo(String propertyName, boolean literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsEqualTo(String propertyName, Object literal) {
        return Collections.<ContentType> emptyList();
    }

    // PropertyIsNotEqualTo
    @Override
    public List<ContentType> propertyIsNotEqualTo(String propertyName, String literal,
            boolean isCaseSensitive) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsNotEqualTo(String propertyName, Date literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsNotEqualTo(String propertyName, Date startDate, Date endDate) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsNotEqualTo(String propertyName, int literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsNotEqualTo(String propertyName, short literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsNotEqualTo(String propertyName, long literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsNotEqualTo(String propertyName, double literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsNotEqualTo(String propertyName, float literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsNotEqualTo(String propertyName, byte[] literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsNotEqualTo(String propertyName, boolean literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsNotEqualTo(String propertyName, Object literal) {
        return Collections.<ContentType> emptyList();
    }

    // PropertyIsGreaterThan
    @Override
    public List<ContentType> propertyIsGreaterThan(String propertyName, String literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsGreaterThan(String propertyName, Date literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsGreaterThan(String propertyName, int literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsGreaterThan(String propertyName, short literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsGreaterThan(String propertyName, long literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsGreaterThan(String propertyName, double literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsGreaterThan(String propertyName, float literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsGreaterThan(String propertyName, Object literal) {
        return Collections.<ContentType> emptyList();
    }

    // PropertyIsGreaterThanOrEqualTo
    @Override
    public List<ContentType> propertyIsGreaterThanOrEqualTo(String propertyName, String literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsGreaterThanOrEqualTo(String propertyName, Date literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsGreaterThanOrEqualTo(String propertyName, int literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsGreaterThanOrEqualTo(String propertyName, short literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsGreaterThanOrEqualTo(String propertyName, long literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsGreaterThanOrEqualTo(String propertyName, double literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsGreaterThanOrEqualTo(String propertyName, float literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsGreaterThanOrEqualTo(String propertyName, Object literal) {
        return Collections.<ContentType> emptyList();
    }

    // PropertyIsLessThan
    @Override
    public List<ContentType> propertyIsLessThan(String propertyName, String literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsLessThan(String propertyName, Date literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsLessThan(String propertyName, int literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsLessThan(String propertyName, short literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsLessThan(String propertyName, long literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsLessThan(String propertyName, double literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsLessThan(String propertyName, float literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsLessThan(String propertyName, Object literal) {
        return Collections.<ContentType> emptyList();
    }

    // PropertyIsLessThanOrEqualTo
    @Override
    public List<ContentType> propertyIsLessThanOrEqualTo(String propertyName, String literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsLessThanOrEqualTo(String propertyName, Date literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsLessThanOrEqualTo(String propertyName, int literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsLessThanOrEqualTo(String propertyName, short literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsLessThanOrEqualTo(String propertyName, long literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsLessThanOrEqualTo(String propertyName, double literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsLessThanOrEqualTo(String propertyName, float literal) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsLessThanOrEqualTo(String propertyName, Object literal) {
        return Collections.<ContentType> emptyList();
    }

    // PropertyIsBetween
    @Override
    public List<ContentType> propertyIsBetween(String propertyName, String lowerBoundary,
            String upperBoundary) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsBetween(String propertyName, Date lowerBoundary,
            Date upperBoundary) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsBetween(String propertyName, int lowerBoundary,
            int upperBoundary) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsBetween(String propertyName, short lowerBoundary,
            short upperBoundary) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsBetween(String propertyName, long lowerBoundary,
            long upperBoundary) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsBetween(String propertyName, float lowerBoundary,
            float upperBoundary) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsBetween(String propertyName, double lowerBoundary,
            double upperBoundary) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> propertyIsBetween(String propertyName, Object lowerBoundary,
            Object upperBoundary) {
        return Collections.<ContentType> emptyList();
    }

    // XpathExists
    @Override
    public List<ContentType> xpathExists(String xpath) {
        return Collections.<ContentType> emptyList();
    }

    // XpathIsLike
    @Override
    public List<ContentType> xpathIsLike(String xpath, String pattern, boolean isCaseSensitive) {
        return Collections.<ContentType> emptyList();
    }

    // XpathIsFuzzy
    @Override
    public List<ContentType> xpathIsFuzzy(String xpath, String literal) {
        return Collections.<ContentType> emptyList();
    }

    // Spatial filters
    @Override
    public List<ContentType> beyond(String propertyName, String wkt, double distance) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> contains(String propertyName, String wkt) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> dwithin(String propertyName, String wkt, double distance) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> intersects(String propertyName, String wkt) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> nearestNeighbor(String propertyName, String wkt) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> within(String propertyName, String wkt) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> crosses(String propertyName, String wkt) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> disjoint(String propertyName, String wkt) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> overlaps(String propertyName, String wkt) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> touches(String propertyName, String wkt) {
        return Collections.<ContentType> emptyList();
    }

    // Temporal filters
    @Override
    public List<ContentType> after(String propertyName, Date date) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> before(String propertyName, Date date) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> during(String propertyName, Date startDate, Date endDate) {
        return Collections.<ContentType> emptyList();
    }

    @Override
    public List<ContentType> relative(String propertyName, long duration) {
        return Collections.<ContentType> emptyList();
    }

    private void verifyInputData(String propertyName, String pattern) {
        if (StringUtils.isEmpty(propertyName) || StringUtils.isEmpty(pattern)) {
            throw new UnsupportedOperationException(
                    "PropertyName and Literal value is required for search.");
        }
    }

    private List<ContentType> combineLists(List<List<ContentType>> lists) {
        List<ContentType> combinedTypes = new ArrayList<ContentType>();
        if (lists != null) {
            for (List<ContentType> list : lists) {
                combinedTypes.addAll(list);
            }
        }
        return combinedTypes;
    }
}
