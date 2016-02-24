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
    public RestUrl propertyIs(String propertyName, Object literal, PropertyOperation operation) {
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
