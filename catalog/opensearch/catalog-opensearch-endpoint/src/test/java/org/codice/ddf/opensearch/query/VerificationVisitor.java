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

package org.codice.ddf.opensearch.query;

import java.util.HashMap;
import java.util.Map;

import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Within;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.TOverlaps;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

public class VerificationVisitor extends DefaultFilterVisitor {
    public static final String SEPARATOR = " - ";

    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(VerificationVisitor.class));

    private int indent = 0;

    private Map<String, FilterStatus> map = new HashMap<String, FilterStatus>();

    public static String indent(int count) {
        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < count; i++) {
            buffer.append("  ");
        }

        return buffer.toString();
    }

    @Override
    public Object visit(Function expression, Object data) {
        countOccurrence(expression);
        LOGGER.debug(indent(indent + 2) + "FUNCTION:" + " " + expression.getName() + SEPARATOR
                + expression.getClass().getName());
        return super.visit(expression, data);
    }

    @Override
    public Object visit(Not filter, Object data) {
        countOccurrence(filter);
        LOGGER.debug(indent(indent) + "NOT" + SEPARATOR + filter.getClass().getName());
        return super.visit(filter, data);
    }

    @Override
    public Object visit(Or filter, Object data) {
        countOccurrence(filter);

        LOGGER.debug(indent(indent) + "OR" + SEPARATOR + filter.getClass().getName());

        indent++;
        return super.visit(filter, data);
    }

    @Override
    public Object visit(And filter, Object data) {

        countOccurrence(filter);

        LOGGER.debug(indent(indent) + "AND" + SEPARATOR + filter.getClass().getName());

        indent++;

        return super.visit(filter, data);
    }

    @Override
    public Object visit(DWithin filter, Object data) {
        countOccurrence(filter);

        LOGGER.debug(indent(indent) + filter.NAME + SEPARATOR + filter.getClass().getName());

        return super.visit(filter, data);
    }

    @Override
    public Object visit(Within filter, Object data) {
        countOccurrence(filter);

        LOGGER.debug(indent(indent) + filter.NAME + SEPARATOR + filter.getClass().getName());

        return super.visit(filter, data);
    }

    @Override
    public Object visit(Intersects filter, Object data) {
        countOccurrence(filter);

        LOGGER.debug(indent(indent) + filter.NAME + SEPARATOR + filter.getClass().getName());

        return super.visit(filter, data);
    }

    @Override
    public Object visit(Contains filter, Object data) {
        countOccurrence(filter);

        LOGGER.debug(indent(indent) + filter.NAME + SEPARATOR + filter.getClass().getName());

        return super.visit(filter, data);
    }

    @Override
    public Object visit(TOverlaps filter, Object data) {
        countOccurrence(filter);

        LOGGER.debug(indent(indent) + filter.NAME + SEPARATOR + filter.getClass().getName());

        return super.visit(filter, data);
    }

    @Override
    public Object visit(During filter, Object data) {
        countOccurrence(filter);

        LOGGER.debug(indent(indent) + filter.NAME + SEPARATOR + filter.getClass().getName());

        return super.visit(filter, data);
    }

    @Override
    public Object visit(PropertyIsEqualTo filter, Object data) {
        countOccurrence(filter);

        LOGGER.debug(indent(indent) + filter.NAME + SEPARATOR + filter.getClass().getName());

        return super.visit(filter, data);
    }

    @Override
    public Object visit(PropertyIsLike filter, Object data) {
        countOccurrence(filter);
        getStatus(filter).setCaseSensitive(filter.isMatchingCase());
        getStatus(filter).setWildcard(filter.getWildCard());

        LOGGER.debug(indent(indent) + filter.NAME + SEPARATOR + filter.getClass().getName());

        LOGGER.debug(indent(indent + 2) + filter.getLiteral());

        return super.visit(filter, data);
    }

    @Override
    public Object visit(PropertyIsBetween filter, Object data) {

        countOccurrence(filter);

        LOGGER.debug(indent(indent) + filter.NAME + SEPARATOR + filter.getClass().getName());

        LOGGER.debug(indent(indent + 2) + filter.getLowerBoundary());
        LOGGER.debug(indent(indent + 2) + filter.getUpperBoundary());

        return super.visit(filter, data);
    }

    @Override
    public Object visit(PropertyName expression, Object data) {

        countOccurrence(expression);

        LOGGER.debug(indent(indent + 2) + expression.getPropertyName() + SEPARATOR + expression
                .getClass().getName());

        return data;
    }

    @Override
    public Object visit(Literal expression, Object data) {

        countOccurrence(expression);

        LOGGER.debug(
                indent(indent) + expression.getValue() + VerificationVisitor.SEPARATOR + expression
                        .getClass().getName());
        return data;
    }

    private void countOccurrence(Filter filter) {
        if (getStatus(filter) == null) {
            FilterStatus status = new FilterStatus();
            status.increment();
            status.addFilter(filter);
            map.put(filter.getClass().getName(), status);
        } else {
            FilterStatus status = map.get(filter.getClass().getName());
            status.increment();
            status.addFilter(filter);
        }
    }

    private void countOccurrence(Expression expression) {
        if (getStatus(expression) == null) {
            FilterStatus status = new FilterStatus();
            status.increment();
            map.put(expression.getClass().getName(), status);
        } else {
            FilterStatus status = map.get(expression.getClass().getName());
            status.increment();
        }
    }

    private FilterStatus getStatus(Object filter) {
        return map.get(filter.getClass().getName());
    }

    public Map<String, FilterStatus> getMap() {
        return (HashMap<String, FilterStatus>) map;
    }
}
