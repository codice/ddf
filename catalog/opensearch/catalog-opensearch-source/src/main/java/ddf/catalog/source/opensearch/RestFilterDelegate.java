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

package ddf.catalog.source.opensearch;

import java.util.Date;
import java.util.List;

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.FilterDelegate;

/**
 * Used to find Filter objects that can be fulfilled by a DDF REST request.
 *
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 */
public class RestFilterDelegate extends FilterDelegate<RestUrl> {

    private RestUrl restUrl;

    /**
     * Constructs instance
     *
     * @param restUrl
     */
    public RestFilterDelegate(RestUrl restUrl) {
        this.restUrl = restUrl;
    }

    public RestUrl getRestUrl() {
        return restUrl;
    }

    @Override
    public RestUrl propertyIsEqualTo(String propertyName, String literal, boolean isCaseSensitive) {
        if (Metacard.ID.equals(propertyName)) {
            restUrl.setId(literal);
            return restUrl;
        }
        return null;
    }

    @Override
    public RestUrl and(List<RestUrl> operands) {
        return findFirstOperand(operands);
    }

    @Override
    public RestUrl or(List<RestUrl> operands) {
        return findFirstOperand(operands);
    }

    private RestUrl findFirstOperand(List<RestUrl> restUrls) {
        for (RestUrl restUrl : restUrls) {
            if (restUrl != null) {
                return restUrl;
            }
        }
        return null;
    }

    @Override
    public RestUrl nearestNeighbor(String propertyName, String wkt) {
        return null;
    }

    @Override
    public RestUrl beyond(String propertyName, String wkt, double distance) {
        return null;
    }

    @Override
    public RestUrl contains(String propertyName, String wkt) {
        return null;
    }

    @Override
    public RestUrl crosses(String propertyName, String wkt) {
        return null;
    }

    @Override
    public RestUrl disjoint(String propertyName, String wkt) {
        return null;
    }

    @Override
    public RestUrl dwithin(String propertyName, String wkt, double distance) {
        return null;
    }

    @Override
    public RestUrl intersects(String propertyName, String wkt) {
        return null;
    }

    @Override
    public RestUrl overlaps(String propertyName, String wkt) {
        return null;
    }

    @Override
    public RestUrl touches(String propertyName, String wkt) {
        return null;
    }

    @Override
    public RestUrl within(String propertyName, String wkt) {
        return null;
    }

    @Override
    public RestUrl not(RestUrl operand) {
        return null;
    }

    @Override
    public RestUrl include() {
        return null;
    }

    @Override
    public RestUrl exclude() {
        return null;
    }

    @Override
    public RestUrl propertyIsEqualTo(String propertyName, Date literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsEqualTo(String propertyName, Date startDate, Date endDate) {
        return null;
    }

    @Override
    public RestUrl propertyIsEqualTo(String propertyName, int literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsEqualTo(String propertyName, short literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsEqualTo(String propertyName, long literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsEqualTo(String propertyName, float literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsEqualTo(String propertyName, double literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsEqualTo(String propertyName, boolean literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsEqualTo(String propertyName, byte[] literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsEqualTo(String propertyName, Object literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsNotEqualTo(String propertyName, String literal,
            boolean isCaseSensitive) {
        return null;
    }

    @Override
    public RestUrl propertyIsNotEqualTo(String propertyName, Date literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsNotEqualTo(String propertyName, Date startDate, Date endDate) {
        return null;
    }

    @Override
    public RestUrl propertyIsNotEqualTo(String propertyName, int literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsNotEqualTo(String propertyName, short literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsNotEqualTo(String propertyName, long literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsNotEqualTo(String propertyName, float literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsNotEqualTo(String propertyName, double literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsNotEqualTo(String propertyName, boolean literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsNotEqualTo(String propertyName, byte[] literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsNotEqualTo(String propertyName, Object literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsGreaterThan(String propertyName, String literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsGreaterThan(String propertyName, Date literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsGreaterThan(String propertyName, int literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsGreaterThan(String propertyName, short literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsGreaterThan(String propertyName, long literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsGreaterThan(String propertyName, float literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsGreaterThan(String propertyName, double literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsGreaterThan(String propertyName, Object literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsGreaterThanOrEqualTo(String propertyName, String literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsGreaterThanOrEqualTo(String propertyName, Date literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsGreaterThanOrEqualTo(String propertyName, int literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsGreaterThanOrEqualTo(String propertyName, short literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsGreaterThanOrEqualTo(String propertyName, long literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsGreaterThanOrEqualTo(String propertyName, float literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsGreaterThanOrEqualTo(String propertyName, double literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsGreaterThanOrEqualTo(String propertyName, Object literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsLessThan(String propertyName, String literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsLessThan(String propertyName, Date literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsLessThan(String propertyName, int literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsLessThan(String propertyName, short literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsLessThan(String propertyName, long literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsLessThan(String propertyName, float literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsLessThan(String propertyName, double literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsLessThan(String propertyName, Object literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsLessThanOrEqualTo(String propertyName, String literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsLessThanOrEqualTo(String propertyName, Date literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsLessThanOrEqualTo(String propertyName, int literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsLessThanOrEqualTo(String propertyName, short literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsLessThanOrEqualTo(String propertyName, long literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsLessThanOrEqualTo(String propertyName, float literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsLessThanOrEqualTo(String propertyName, double literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsLessThanOrEqualTo(String propertyName, Object literal) {
        return null;
    }

    @Override
    public RestUrl propertyIsBetween(String propertyName, String lowerBoundary,
            String upperBoundary) {
        return null;
    }

    @Override
    public RestUrl propertyIsBetween(String propertyName, Date lowerBoundary, Date upperBoundary) {
        return null;
    }

    @Override
    public RestUrl propertyIsBetween(String propertyName, int lowerBoundary, int upperBoundary) {
        return null;
    }

    @Override
    public RestUrl propertyIsBetween(String propertyName, short lowerBoundary,
            short upperBoundary) {
        return null;
    }

    @Override
    public RestUrl propertyIsBetween(String propertyName, long lowerBoundary, long upperBoundary) {
        return null;
    }

    @Override
    public RestUrl propertyIsBetween(String propertyName, float lowerBoundary,
            float upperBoundary) {
        return null;
    }

    @Override
    public RestUrl propertyIsBetween(String propertyName, double lowerBoundary,
            double upperBoundary) {
        return null;
    }

    @Override
    public RestUrl propertyIsBetween(String propertyName, Object lowerBoundary,
            Object upperBoundary) {
        return null;
    }

    @Override
    public RestUrl propertyIsNull(String propertyName) {
        return null;
    }

    @Override
    public RestUrl propertyIsLike(String propertyName, String pattern, boolean isCaseSensitive) {
        return null;
    }

    @Override
    public RestUrl propertyIsFuzzy(String propertyName, String literal) {
        return null;
    }

    @Override
    public RestUrl xpathExists(String xpath) {
        return null;
    }

    @Override
    public RestUrl xpathIsLike(String xpath, String pattern, boolean isCaseSensitive) {
        return null;
    }

    @Override
    public RestUrl xpathIsFuzzy(String xpath, String literal) {
        return null;
    }

    @Override
    public RestUrl after(String propertyName, Date date) {
        return null;
    }

    @Override
    public RestUrl before(String propertyName, Date date) {
        return null;
    }

    @Override
    public RestUrl during(String propertyName, Date startDate, Date endDate) {
        return null;
    }

    @Override
    public RestUrl relative(String propertyName, long duration) {
        return null;
    }
}
