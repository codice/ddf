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

package ddf.catalog.metacard.validation;

import static ddf.catalog.metacard.validation.MetacardValidityMarkerPlugin.VALIDATION_ERRORS;
import static ddf.catalog.metacard.validation.MetacardValidityMarkerPlugin.VALIDATION_WARNINGS;

import java.util.Date;
import java.util.List;

import ddf.catalog.filter.FilterDelegate;

public class ValidationQueryDelegate extends FilterDelegate<Boolean> {

    /**
     * These three Boolean operations AND, OR, and NOT are not boolean operations in the traditional sense
     * They should return true if any of the operands are true. This is because we want the delegate to return
     * true whenever VALIDATION_WARNINGS or VALIDATION_ERRORS occur in the filter
     * For example: if we get a filter that is like [ validation-warnings is test ] AND [ AnyText is * ] we want {@link ValidationQueryDelegate#and} to return true, so we need an or operation underneath
     * if we get a filter that is like [ validation-warnings is test ] OR [ AnyText is * ] we want {@link ValidationQueryDelegate#or} to return true, so we need an or operation underneath
     * if we get a filter this is like NOT [ validation-warnings is test ] we want {@link ValidationQueryDelegate#not} to return true, so we need to pass the operand as is
     */

    @Override
    public Boolean and(List<Boolean> operands) {
        return operands.stream().reduce(false, (a, b) -> a || b);
    }

    @Override
    public Boolean or(List<Boolean> operands) {
        return operands.stream().reduce(false, (a, b) -> a || b);
    }

    @Override
    public Boolean not(Boolean operand) {
        return operand;
    }

    @Override
    public Boolean nearestNeighbor(String propertyName, String wkt) {
        return false;
    }

    @Override
    public Boolean include() {
        return false;
    }

    @Override
    public Boolean exclude() {
        return false;
    }

    @Override
    public Boolean propertyIsEqualTo(String propertyName, String literal, boolean isCaseSensitive) {
        return propertyName.equals(VALIDATION_ERRORS) || propertyName.equals(VALIDATION_WARNINGS);
    }

    @Override
    public Boolean propertyIsEqualTo(String propertyName, Date literal) {
        return false;
    }

    @Override
    public Boolean propertyIsEqualTo(String propertyName, Date startDate, Date endDate) {
        return false;
    }

    @Override
    public Boolean propertyIsEqualTo(String propertyName, int literal) {
        return false;
    }

    @Override
    public Boolean propertyIsEqualTo(String propertyName, short literal) {
        return false;
    }

    @Override
    public Boolean propertyIsEqualTo(String propertyName, long literal) {
        return false;
    }

    @Override
    public Boolean propertyIsEqualTo(String propertyName, float literal) {
        return false;
    }

    @Override
    public Boolean propertyIsEqualTo(String propertyName, double literal) {
        return false;
    }

    @Override
    public Boolean propertyIsEqualTo(String propertyName, boolean literal) {
        return false;
    }

    @Override
    public Boolean propertyIsEqualTo(String propertyName, byte[] literal) {
        return false;
    }

    @Override
    public Boolean propertyIsEqualTo(String propertyName, Object literal) {
        return false;
    }

    @Override
    public Boolean propertyIsNotEqualTo(String propertyName, String literal,
            boolean isCaseSensitive) {
        return false;
    }

    @Override
    public Boolean propertyIsNotEqualTo(String propertyName, Date literal) {
        return false;
    }

    @Override
    public Boolean propertyIsNotEqualTo(String propertyName, Date startDate, Date endDate) {
        return false;
    }

    @Override
    public Boolean propertyIsNotEqualTo(String propertyName, int literal) {
        return false;
    }

    @Override
    public Boolean propertyIsNotEqualTo(String propertyName, short literal) {
        return false;
    }

    @Override
    public Boolean propertyIsNotEqualTo(String propertyName, long literal) {
        return false;
    }

    @Override
    public Boolean propertyIsNotEqualTo(String propertyName, float literal) {
        return false;
    }

    @Override
    public Boolean propertyIsNotEqualTo(String propertyName, double literal) {
        return false;
    }

    @Override
    public Boolean propertyIsNotEqualTo(String propertyName, boolean literal) {
        return false;
    }

    @Override
    public Boolean propertyIsNotEqualTo(String propertyName, byte[] literal) {
        return false;
    }

    @Override
    public Boolean propertyIsNotEqualTo(String propertyName, Object literal) {
        return false;
    }

    @Override
    public Boolean propertyIsGreaterThan(String propertyName, String literal) {
        return false;
    }

    @Override
    public Boolean propertyIsGreaterThan(String propertyName, Date literal) {
        return false;
    }

    @Override
    public Boolean propertyIsGreaterThan(String propertyName, int literal) {
        return false;
    }

    @Override
    public Boolean propertyIsGreaterThan(String propertyName, short literal) {
        return false;
    }

    @Override
    public Boolean propertyIsGreaterThan(String propertyName, long literal) {
        return false;
    }

    @Override
    public Boolean propertyIsGreaterThan(String propertyName, float literal) {
        return false;
    }

    @Override
    public Boolean propertyIsGreaterThan(String propertyName, double literal) {
        return false;
    }

    @Override
    public Boolean propertyIsGreaterThan(String propertyName, Object literal) {
        return false;
    }

    @Override
    public Boolean propertyIsGreaterThanOrEqualTo(String propertyName, String literal) {
        return false;
    }

    @Override
    public Boolean propertyIsGreaterThanOrEqualTo(String propertyName, Date literal) {
        return false;
    }

    @Override
    public Boolean propertyIsGreaterThanOrEqualTo(String propertyName, int literal) {
        return false;
    }

    @Override
    public Boolean propertyIsGreaterThanOrEqualTo(String propertyName, short literal) {
        return false;
    }

    @Override
    public Boolean propertyIsGreaterThanOrEqualTo(String propertyName, long literal) {
        return false;
    }

    @Override
    public Boolean propertyIsGreaterThanOrEqualTo(String propertyName, float literal) {
        return false;
    }

    @Override
    public Boolean propertyIsGreaterThanOrEqualTo(String propertyName, double literal) {
        return false;
    }

    @Override
    public Boolean propertyIsGreaterThanOrEqualTo(String propertyName, Object literal) {
        return false;
    }

    @Override
    public Boolean propertyIsLessThan(String propertyName, String literal) {
        return false;
    }

    @Override
    public Boolean propertyIsLessThan(String propertyName, Date literal) {
        return false;
    }

    @Override
    public Boolean propertyIsLessThan(String propertyName, int literal) {
        return false;
    }

    @Override
    public Boolean propertyIsLessThan(String propertyName, short literal) {
        return false;
    }

    @Override
    public Boolean propertyIsLessThan(String propertyName, long literal) {
        return false;
    }

    @Override
    public Boolean propertyIsLessThan(String propertyName, float literal) {
        return false;
    }

    @Override
    public Boolean propertyIsLessThan(String propertyName, double literal) {
        return false;
    }

    @Override
    public Boolean propertyIsLessThan(String propertyName, Object literal) {
        return false;
    }

    @Override
    public Boolean propertyIsLessThanOrEqualTo(String propertyName, String literal) {
        return false;
    }

    @Override
    public Boolean propertyIsLessThanOrEqualTo(String propertyName, Date literal) {
        return false;
    }

    @Override
    public Boolean propertyIsLessThanOrEqualTo(String propertyName, int literal) {
        return false;
    }

    @Override
    public Boolean propertyIsLessThanOrEqualTo(String propertyName, short literal) {
        return false;
    }

    @Override
    public Boolean propertyIsLessThanOrEqualTo(String propertyName, long literal) {
        return false;
    }

    @Override
    public Boolean propertyIsLessThanOrEqualTo(String propertyName, float literal) {
        return false;
    }

    @Override
    public Boolean propertyIsLessThanOrEqualTo(String propertyName, double literal) {
        return false;
    }

    @Override
    public Boolean propertyIsLessThanOrEqualTo(String propertyName, Object literal) {
        return false;
    }

    @Override
    public Boolean propertyIsBetween(String propertyName, String lowerBoundary,
            String upperBoundary) {
        return false;
    }

    @Override
    public Boolean propertyIsBetween(String propertyName, Date lowerBoundary, Date upperBoundary) {
        return false;
    }

    @Override
    public Boolean propertyIsBetween(String propertyName, int lowerBoundary, int upperBoundary) {
        return false;
    }

    @Override
    public Boolean propertyIsBetween(String propertyName, short lowerBoundary,
            short upperBoundary) {
        return false;
    }

    @Override
    public Boolean propertyIsBetween(String propertyName, long lowerBoundary, long upperBoundary) {
        return false;
    }

    @Override
    public Boolean propertyIsBetween(String propertyName, float lowerBoundary,
            float upperBoundary) {
        return false;
    }

    @Override
    public Boolean propertyIsBetween(String propertyName, double lowerBoundary,
            double upperBoundary) {
        return false;
    }

    @Override
    public Boolean propertyIsBetween(String propertyName, Object lowerBoundary,
            Object upperBoundary) {
        return false;
    }

    @Override
    public Boolean propertyIsNull(String propertyName) {
        return propertyName.equals(VALIDATION_ERRORS) || propertyName.equals(VALIDATION_WARNINGS);
    }

    @Override
    public Boolean propertyIsLike(String propertyName, String pattern, boolean isCaseSensitive) {
        return propertyName.equals(VALIDATION_ERRORS) || propertyName.equals(VALIDATION_WARNINGS);
    }

    @Override
    public Boolean propertyIsFuzzy(String propertyName, String literal) {
        return false;
    }

    @Override
    public Boolean xpathExists(String xpath) {
        return false;
    }

    @Override
    public Boolean xpathIsLike(String xpath, String pattern, boolean isCaseSensitive) {
        return false;
    }

    @Override
    public Boolean xpathIsFuzzy(String xpath, String literal) {
        return false;
    }

    @Override
    public Boolean beyond(String propertyName, String wkt, double distance) {
        return false;
    }

    @Override
    public Boolean contains(String propertyName, String wkt) {
        return false;
    }

    @Override
    public Boolean crosses(String propertyName, String wkt) {
        return false;
    }

    @Override
    public Boolean disjoint(String propertyName, String wkt) {
        return false;
    }

    @Override
    public Boolean dwithin(String propertyName, String wkt, double distance) {
        return false;
    }

    @Override
    public Boolean intersects(String propertyName, String wkt) {
        return false;
    }

    @Override
    public Boolean overlaps(String propertyName, String wkt) {
        return false;
    }

    @Override
    public Boolean touches(String propertyName, String wkt) {
        return false;
    }

    @Override
    public Boolean within(String propertyName, String wkt) {
        return false;
    }

    @Override
    public Boolean after(String propertyName, Date date) {
        return false;
    }

    @Override
    public Boolean before(String propertyName, Date date) {
        return false;
    }

    @Override
    public Boolean during(String propertyName, Date startDate, Date endDate) {
        return false;
    }

    @Override
    public Boolean relative(String propertyName, long duration) {
        return false;
    }

    //    public Pair<Boolean, Boolean> getValidityParams() {
    //        return new ImmutablePair<>(searchValid, searchInvalid);
    //    }
}
