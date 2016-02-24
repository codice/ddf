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
    public String propertyIs(String propertyName, Object literal, PropertyOperation operation) {
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
