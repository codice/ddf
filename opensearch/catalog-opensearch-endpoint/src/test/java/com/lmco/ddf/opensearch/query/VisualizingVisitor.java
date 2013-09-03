/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package com.lmco.ddf.opensearch.query;

import java.util.HashMap;
import java.util.Map;

import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.opengis.filter.And;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

public class VisualizingVisitor extends DefaultFilterVisitor {

    private static final XLogger LOGGER = new XLogger( LoggerFactory.getLogger( VisualizingVisitor.class ) );
    public static final String SEPARATOR = " - ";
    private int indent = 0;

    private Map<String, FilterStatus> map = new HashMap<String, FilterStatus>();

    @Override
    public Object visit(Function expression, Object data) {
        countOccurrence(expression);
        LOGGER.debug(indent(indent+2) + "FUNCTION:" +  " " + expression.getName() + SEPARATOR
                + expression.getClass().getName());
        return super.visit(expression, data);
    }
    
    @Override
    public Object visit(Not filter, Object data) {
        countOccurrence(filter);
        LOGGER.debug(indent(indent) + "NOT" + SEPARATOR
                + filter.getClass().getName());
        return super.visit(filter, data);
    }
    @Override
    public Object visit(Or filter, Object data) {
        countOccurrence(filter);

        LOGGER.debug(indent(indent) + "OR" + SEPARATOR
                + filter.getClass().getName());

        indent++;
        return super.visit(filter, data);
    }

    @Override
    public Object visit(And filter, Object data) {

        countOccurrence(filter);

        LOGGER.debug(indent(indent) + "AND" + SEPARATOR
                + filter.getClass().getName());

        indent++;

        return super.visit(filter, data);
    }

    private void countOccurrence(Object filter) {

        if (getStatus(filter) == null) {
            
            FilterStatus status = new FilterStatus();
            status.increment() ;
            map.put(filter.getClass().getName(), status);
        }
        else {
            map.get(filter.getClass().getName()).increment();
        }
    }

    private FilterStatus getStatus(Object filter) {
        return map.get(filter.getClass().getName());
    }
    
    @Override
    public Object visit(PropertyIsLike filter, Object data) {

        countOccurrence(filter);
        getStatus(filter).setCaseSensitive(filter.isMatchingCase()) ;
        getStatus(filter).setWildcard(filter.getWildCard()); 

        LOGGER.debug(indent(indent) + filter.NAME + SEPARATOR
                + filter.getClass().getName());

        LOGGER.debug(indent(indent + 2) + filter.getLiteral());

        return super.visit(filter, data);
    }
    
    @Override
    public Object visit(PropertyIsBetween filter, Object data) {

        countOccurrence(filter);

        LOGGER.debug(indent(indent) + filter.NAME + SEPARATOR
                + filter.getClass().getName());

        LOGGER.debug(indent(indent + 2) + filter.getLowerBoundary());
        LOGGER.debug(indent(indent + 2) + filter.getUpperBoundary());

        return super.visit(filter, data);
    }

    @Override
    public Object visit(PropertyName expression, Object data) {

        countOccurrence(expression);

        LOGGER.debug(indent(indent + 2) + expression.getPropertyName()
                + SEPARATOR + expression.getClass().getName());

        return data;
    }

    @Override
    public Object visit(Literal expression, Object data) {

        countOccurrence(expression);

        LOGGER.debug(indent(indent) + expression.getValue()
                + VisualizingVisitor.SEPARATOR
                + expression.getClass().getName());
        return data;
    }

    public static String indent(int count) {
        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < count; i++) {
            buffer.append("  ");
        }

        return buffer.toString();
    }

    public Map<String, FilterStatus> getMap() {
        return map;
    }
}
