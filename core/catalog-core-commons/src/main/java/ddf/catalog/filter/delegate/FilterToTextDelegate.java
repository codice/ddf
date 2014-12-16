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
package ddf.catalog.filter.delegate;

import ddf.catalog.filter.FilterDelegate;

import java.util.Date;
import java.util.List;

public class FilterToTextDelegate extends FilterDelegate<String> {

    // Logical operators
    @Override
    public String include() {
        return "true";
    }

    @Override
    public String exclude() {
        return "false";
    }

    @Override
    public String not(String operand) {
        return "not(" + operand + ")";
    }

    @Override
    public String and(List<String> operands) {
        StringBuilder result = new StringBuilder();
        result.append("and(");
        listToString(operands, result);
        result.append(")");
        return result.toString();
    }

    @Override
    public String or(List<String> operands) {
        StringBuilder result = new StringBuilder();
        result.append("or(");
        listToString(operands, result);
        result.append(")");
        return result.toString();
    }

    private void listToString(List<String> operands, StringBuilder result) {
        boolean firstIteration = true;
        for (String operand : operands) {
            if (!firstIteration) {
                result.append(",");
            } else {
                firstIteration = false;
            }
            result.append(operand);
        }
    }

    // PropertyIsNull
    @Override
    public String propertyIsNull(String propertyName) {
        return "null(" + propertyName + ")";
    }

    // PropertyIsLike
    @Override
    public String propertyIsLike(String propertyName, String pattern, boolean isCaseSensitive) {
        return "like(" + propertyName + "," + pattern + ")";
    }

    // PropertyIsFuzzy
    @Override
    public String propertyIsFuzzy(String propertyName, String literal) {
        return "fuzzy(" + propertyName + "," + literal + ")";
    }

    // PropertyIsEqualTo
    @Override
    public String propertyIsEqualTo(String propertyName, String literal, boolean isCaseSensitive) {
        return propertyName + "=" + literal;
    }

    @Override
    public String propertyIsEqualTo(String propertyName, Date literal) {
        return propertyName + "=" + literal;
    }

    @Override
    public String propertyIsEqualTo(String propertyName, Date startDate, Date endDate) {
        return propertyName + "=" + startDate + " to " + endDate;
    }

    @Override
    public String propertyIsEqualTo(String propertyName, int literal) {
        return propertyName + "=" + literal + "i";
    }

    @Override
    public String propertyIsEqualTo(String propertyName, short literal) {
        return propertyName + "=" + literal + "s";
    }

    @Override
    public String propertyIsEqualTo(String propertyName, long literal) {
        return propertyName + "=" + literal + "l";
    }

    @Override
    public String propertyIsEqualTo(String propertyName, double literal) {
        return propertyName + "=" + literal + "d";
    }

    @Override
    public String propertyIsEqualTo(String propertyName, float literal) {
        return propertyName + "=" + literal + "f";
    }

    @Override
    public String propertyIsEqualTo(String propertyName, byte[] literal) {
        String bytes = bytesToString(literal);
        return propertyName + "=" + bytes + "b";
    }

    @Override
    public String propertyIsEqualTo(String propertyName, boolean literal) {
        return propertyName + "=" + literal;
    }

    @Override
    public String propertyIsEqualTo(String propertyName, Object literal) {
        return propertyName + "=" + literal + "o";
    }

    // PropertyIsNotEqualTo
    @Override
    public String propertyIsNotEqualTo(String propertyName, String literal, boolean isCaseSensitive) {
        return propertyName + "!=" + literal;
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, Date literal) {
        return propertyName + "!=" + literal;
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, Date startDate, Date endDate) {
        return propertyName + "!=" + startDate + " to " + endDate;
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, int literal) {
        return propertyName + "!=" + literal + "i";
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, short literal) {
        return propertyName + "!=" + literal + "s";
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, long literal) {
        return propertyName + "!=" + literal + "l";
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, double literal) {
        return propertyName + "!=" + literal + "d";
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, float literal) {
        return propertyName + "!=" + literal + "f";
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, byte[] literal) {
        String bytes = bytesToString(literal);
        return propertyName + "!=" + bytes + "b";
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, boolean literal) {
        return propertyName + "!=" + literal;
    }

    @Override
    public String propertyIsNotEqualTo(String propertyName, Object literal) {
        return propertyName + "!=" + literal + "o";
    }

    // PropertyIsGreaterThan
    @Override
    public String propertyIsGreaterThan(String propertyName, String literal) {
        return propertyName + ">" + literal;
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, Date literal) {
        return propertyName + ">" + literal;
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, int literal) {
        return propertyName + ">" + literal + "i";
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, short literal) {
        return propertyName + ">" + literal + "s";
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, long literal) {
        return propertyName + ">" + literal + "l";
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, double literal) {
        return propertyName + ">" + literal + "d";
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, float literal) {
        return propertyName + ">" + literal + "f";
    }

    @Override
    public String propertyIsGreaterThan(String propertyName, Object literal) {
        return propertyName + ">" + literal + "o";
    }

    // PropertyIsGreaterThanOrEqualTo
    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, String literal) {
        return propertyName + ">=" + literal;
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, Date literal) {
        return propertyName + ">=" + literal;
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, int literal) {
        return propertyName + ">=" + literal + "i";
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, short literal) {
        return propertyName + ">=" + literal + "s";
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, long literal) {
        return propertyName + ">=" + literal + "l";
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, double literal) {
        return propertyName + ">=" + literal + "d";
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, float literal) {
        return propertyName + ">=" + literal + "f";
    }

    @Override
    public String propertyIsGreaterThanOrEqualTo(String propertyName, Object literal) {
        return propertyName + ">=" + literal + "o";
    }

    // PropertyIsLessThan
    @Override
    public String propertyIsLessThan(String propertyName, String literal) {
        return propertyName + "<" + literal;
    }

    @Override
    public String propertyIsLessThan(String propertyName, Date literal) {
        return propertyName + "<" + literal;
    }

    @Override
    public String propertyIsLessThan(String propertyName, int literal) {
        return propertyName + "<" + literal + "i";
    }

    @Override
    public String propertyIsLessThan(String propertyName, short literal) {
        return propertyName + "<" + literal + "s";
    }

    @Override
    public String propertyIsLessThan(String propertyName, long literal) {
        return propertyName + "<" + literal + "l";
    }

    @Override
    public String propertyIsLessThan(String propertyName, double literal) {
        return propertyName + "<" + literal + "d";
    }

    @Override
    public String propertyIsLessThan(String propertyName, float literal) {
        return propertyName + "<" + literal + "f";
    }

    @Override
    public String propertyIsLessThan(String propertyName, Object literal) {
        return propertyName + "<" + literal + "o";
    }

    // PropertyIsLessThanOrEqualTo
    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, String literal) {
        return propertyName + "<=" + literal;
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, Date literal) {
        return propertyName + "<=" + literal;
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, int literal) {
        return propertyName + "<=" + literal + "i";
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, short literal) {
        return propertyName + "<=" + literal + "s";
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, long literal) {
        return propertyName + "<=" + literal + "l";
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, double literal) {
        return propertyName + "<=" + literal + "d";
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, float literal) {
        return propertyName + "<=" + literal + "f";
    }

    @Override
    public String propertyIsLessThanOrEqualTo(String propertyName, Object literal) {
        return propertyName + "<=" + literal + "o";
    }

    // PropertyIsBetween
    @Override
    public String propertyIsBetween(String propertyName, String lowerBoundary, String upperBoundary) {
        return lowerBoundary + "<=" + propertyName + "<=" + upperBoundary;
    }

    @Override
    public String propertyIsBetween(String propertyName, Date lowerBoundary, Date upperBoundary) {
        return lowerBoundary + "<=" + propertyName + "<=" + upperBoundary;
    }

    @Override
    public String propertyIsBetween(String propertyName, int lowerBoundary, int upperBoundary) {
        return lowerBoundary + "i<=" + propertyName + "<=" + upperBoundary + "i";
    }

    @Override
    public String propertyIsBetween(String propertyName, short lowerBoundary, short upperBoundary) {
        return lowerBoundary + "s<=" + propertyName + "<=" + upperBoundary + "s";
    }

    @Override
    public String propertyIsBetween(String propertyName, long lowerBoundary, long upperBoundary) {
        return lowerBoundary + "l<=" + propertyName + "<=" + upperBoundary + "l";
    }

    @Override
    public String propertyIsBetween(String propertyName, float lowerBoundary, float upperBoundary) {
        return lowerBoundary + "f<=" + propertyName + "<=" + upperBoundary + "f";
    }

    @Override
    public String propertyIsBetween(String propertyName, double lowerBoundary, double upperBoundary) {
        return lowerBoundary + "d<=" + propertyName + "<=" + upperBoundary + "d";
    }

    @Override
    public String propertyIsBetween(String propertyName, Object lowerBoundary, Object upperBoundary) {
        return lowerBoundary + "o<=" + propertyName + "<=" + upperBoundary + "o";
    }

    private String bytesToString(byte[] literal) {
        StringBuilder bytes = new StringBuilder();
        bytes.append("{");
        for (int i = 0; i < literal.length; i++) {
            if (i > 0) {
                bytes.append(",");
            }
            bytes.append(literal[i]);
        }
        bytes.append("}");
        return bytes.toString();
    }

    // XpathExists
    @Override
    public String xpathExists(String xpath) {
        return "xpath(" + xpath + ")";
    }

    // XpathIsLike
    @Override
    public String xpathIsLike(String xpath, String pattern, boolean isCaseSensitive) {
        return "xpath(" + xpath + "," + pattern + ")";
    }

    // XpathIsFuzzy
    @Override
    public String xpathIsFuzzy(String xpath, String literal) {
        return "xpath(" + xpath + ",fuzzy(" + literal + "))";
    }

    // Spatial filters
    @Override
    public String beyond(String propertyName, String wkt, double distance) {
        return "beyond(" + propertyName + ",wkt(" + wkt + ")," + distance + ")";
    }

    @Override
    public String contains(String propertyName, String wkt) {
        return "contains(" + propertyName + ",wkt(" + wkt + "))";
    }

    @Override
    public String dwithin(String propertyName, String wkt, double distance) {
        return "dwithin(" + propertyName + ",wkt(" + wkt + ")," + distance + ")";
    }

    @Override
    public String intersects(String propertyName, String wkt) {
        return "intersects(" + propertyName + ",wkt(" + wkt + "))";
    }

    @Override
    public String nearestNeighbor(String propertyName, String wkt) {
        return "nn(" + propertyName + ",wkt(" + wkt + "))";
    }

    @Override
    public String within(String propertyName, String wkt) {
        return "within(" + propertyName + ",wkt(" + wkt + "))";
    }

    @Override
    public String crosses(String propertyName, String wkt) {
        return "crosses(" + propertyName + ",wkt(" + wkt + "))";
    }

    @Override
    public String disjoint(String propertyName, String wkt) {
        return "disjoint(" + propertyName + ",wkt(" + wkt + "))";
    }

    @Override
    public String overlaps(String propertyName, String wkt) {
        return "overlaps(" + propertyName + ",wkt(" + wkt + "))";
    }

    @Override
    public String touches(String propertyName, String wkt) {
        return "touches(" + propertyName + ",wkt(" + wkt + "))";
    }

    // Temporal filters
    @Override
    public String after(String propertyName, Date date) {
        return "after(" + propertyName + "," + date + ")";
    }

    @Override
    public String before(String propertyName, Date date) {
        return "before(" + propertyName + "," + date + ")";
    }

    @Override
    public String during(String propertyName, Date startDate, Date endDate) {
        return "during(" + propertyName + "," + startDate + "," + endDate + ")";
    }

    @Override
    public String relative(String propertyName, long duration) {
        return "relative(" + propertyName + "," + duration + ")";
    }

}
