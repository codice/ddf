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
package ddf.catalog.metrics;

import java.util.Date;
import java.util.List;

import ddf.catalog.filter.FilterDelegate;

/**
 * Filter delegate to determine the types of filter features being used.
 *
 * @author Phillip Klinefelter
 * @author ddf.isgs@lmco.com
 *
 */
public class QueryTypeFilterDelegate extends FilterDelegate<Boolean> {

    private boolean isSpatial = false;

    private boolean isTemporal = false;

    private boolean isXpath = false;

    private boolean isLogical = false;

    private boolean isFuzzy = false;

    private boolean isCaseSensitive = false;

    private boolean isComparison = false;

    @Override
    public Boolean nearestNeighbor(String propertyName, String wkt) {
        return isSpatial = true;
    }

    @Override
    public Boolean beyond(String propertyName, String wkt, double distance) {
        return isSpatial = true;
    }

    @Override
    public Boolean contains(String propertyName, String wkt) {
        return isSpatial = true;
    }

    @Override
    public Boolean crosses(String propertyName, String wkt) {
        return isSpatial = true;
    }

    @Override
    public Boolean disjoint(String propertyName, String wkt) {
        return isSpatial = true;
    }

    @Override
    public Boolean dwithin(String propertyName, String wkt, double distance) {
        return isSpatial = true;
    }

    @Override
    public Boolean intersects(String propertyName, String wkt) {
        return isSpatial = true;
    }

    @Override
    public Boolean overlaps(String propertyName, String wkt) {
        return isSpatial = true;
    }

    @Override
    public Boolean touches(String propertyName, String wkt) {
        return isSpatial = true;
    }

    @Override
    public Boolean within(String propertyName, String wkt) {
        return isSpatial = true;
    }

    @Override
    public Boolean xpathExists(String xpath) {
        return isXpath = true;
    }

    @Override
    public Boolean xpathIsLike(String xpath, String pattern, boolean isCaseSensitive) {
        return isXpath = true;
    }

    @Override
    public Boolean xpathIsFuzzy(String xpath, String literal) {
        return isXpath = true;
    }

    @Override
    public Boolean after(String propertyName, Date date) {
        return isTemporal = true;
    }

    @Override
    public Boolean before(String propertyName, Date date) {
        return isTemporal = true;
    }

    @Override
    public Boolean during(String propertyName, Date startDate, Date endDate) {
        return isTemporal = true;
    }

    @Override
    public Boolean relative(String propertyName, long duration) {
        return isTemporal = true;
    }

    @Override
    public Boolean and(List<Boolean> operands) {
        return isLogical = true;
    }

    @Override
    public Boolean or(List<Boolean> operands) {
        return isLogical = true;
    }

    @Override
    public Boolean not(Boolean operand) {
        return isLogical = true;
    }

    @Override
    public Boolean include() {
        return isLogical = true;
    }

    @Override
    public Boolean exclude() {
        return isLogical = true;
    }

    @Override
    public Boolean propertyIsEqualTo(String propertyName, String literal, boolean isCaseSensitive) {
        if (isCaseSensitive) {
            this.isCaseSensitive = true;
        }
        return isComparison = true;
    }

    @Override
    public Boolean propertyIs(String propertyName, Object literal, PropertyOperation operation) {
        return isComparison = true;
    }

    @Override
    public Boolean propertyIsNotEqualTo(String propertyName, String literal,
            boolean isCaseSensitive) {
        if (isCaseSensitive) {
            this.isCaseSensitive = true;
        }
        return isComparison = true;
    }

    @Override
    public Boolean propertyIsLike(String propertyName, String pattern, boolean isCaseSensitive) {
        if (isCaseSensitive) {
            this.isCaseSensitive = true;
        }
        return isComparison = true;
    }

    @Override
    public Boolean propertyIsFuzzy(String propertyName, String literal) {
        isFuzzy = true;
        return isComparison = true;
    }

    public boolean isSpatial() {
        return isSpatial;
    }

    public boolean isTemporal() {
        return isTemporal;
    }

    public boolean isXpath() {
        return isXpath;
    }

    public boolean isLogical() {
        return isLogical;
    }

    public boolean isFuzzy() {
        return isFuzzy;
    }

    public boolean isCaseSensitive() {
        return isCaseSensitive;
    }

    public boolean isComparison() {
        return isComparison;
    }

}
