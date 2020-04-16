/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.filter.impl;

import com.google.common.collect.Range;
import ddf.catalog.filter.FilterDelegate;
import java.util.Date;
import java.util.List;

/**
 * Implements a simplified version of the FilterDelegate where every method rolls up a to higher
 * abstracted method
 *
 * @param <T> Generic type that the FilterDelegate will return as a final result
 */
public abstract class SimpleFilterDelegate<T> extends FilterDelegate<T> {

  public <S> T defaultOperation(Object property, S literal, Class<S> literalClass, Enum operation) {
    throw new UnsupportedOperationException(
        String.format("%s not supported by %s for property", operation, getClass().getName()));
  }

  public T logicalOperation(Object operand, LogicalPropertyOperation logicalPropertyOperation) {
    return defaultOperation(operand, null, null, logicalPropertyOperation);
  }

  public <S> T comparisonOperation(
      String propertyName,
      S literal,
      Class<S> literalClass,
      ComparisonPropertyOperation comparisonPropertyOperation) {
    return defaultOperation(propertyName, literal, literalClass, comparisonPropertyOperation);
  }

  public <S> T spatialOperation(
      String propertyName,
      S wkt,
      Class<S> wktClass,
      SpatialPropertyOperation spatialPropertyOperation) {
    return defaultOperation(propertyName, wkt, wktClass, spatialPropertyOperation);
  }

  public <S> T temporalOperation(
      String propertyName,
      S literal,
      Class<S> literalClass,
      TemporalPropertyOperation temporalPropertyOperation) {
    return defaultOperation(propertyName, literal, literalClass, temporalPropertyOperation);
  }

  @Deprecated
  public <S> T xpathOperation(
      String xpath,
      S literal,
      Class<S> literalClass,
      XPathPropertyOperation xpathPropertyOperation) {
    return defaultOperation(xpath, literal, literalClass, xpathPropertyOperation);
  }

  // Custom Functions
  @Override
  public T propertyIsEqualTo(String functionName, List<Object> arguments, Object literal) {
    return defaultOperation(functionName, arguments, List.class, FunctionOperation.FUNCTION);
  }

  // Logical operators
  public T and(List<T> operands) {
    return logicalOperation(operands, LogicalPropertyOperation.AND);
  }

  public T or(List<T> operands) {
    return logicalOperation(operands, LogicalPropertyOperation.OR);
  }

  public T not(T operand) {
    return logicalOperation(operand, LogicalPropertyOperation.NOT);
  }

  public T include() {
    return logicalOperation(null, LogicalPropertyOperation.INCLUDE);
  }

  public T exclude() {
    return logicalOperation(null, LogicalPropertyOperation.EXCLUDE);
  }

  // Comparison operators

  // PropertyIsEqualTo

  public T propertyIsEqualTo(String propertyName, String literal, boolean isCaseSensitive) {
    return propertyIsEqualTo(
        propertyName, literal, String.class, ComparisonPropertyOperation.IS_EQUAL_TO);
  }

  public T propertyIsEqualTo(String propertyName, Date literal) {
    return propertyIsEqualTo(
        propertyName, literal, Date.class, ComparisonPropertyOperation.IS_EQUAL_TO);
  }

  public T propertyIsEqualTo(String propertyName, Date startDate, Date endDate) {
    return propertyIsEqualTo(
        propertyName,
        Range.open(startDate, endDate),
        Range.class,
        ComparisonPropertyOperation.IS_EQUAL_TO);
  }

  public T propertyIsEqualTo(String propertyName, int literal) {
    return propertyIsEqualTo(propertyName, (Number) literal);
  }

  public T propertyIsEqualTo(String propertyName, short literal) {
    return propertyIsEqualTo(propertyName, (Number) literal);
  }

  public T propertyIsEqualTo(String propertyName, long literal) {
    return propertyIsEqualTo(propertyName, (Number) literal);
  }

  public T propertyIsEqualTo(String propertyName, float literal) {
    return propertyIsEqualTo(propertyName, (Number) literal);
  }

  public T propertyIsEqualTo(String propertyName, double literal) {
    return propertyIsEqualTo(propertyName, (Number) literal);
  }

  public T propertyIsEqualTo(String propertyName, Number literal) {
    return propertyIsEqualTo(
        propertyName, literal, Number.class, ComparisonPropertyOperation.IS_EQUAL_TO);
  }

  public T propertyIsEqualTo(String propertyName, boolean literal) {
    return propertyIsEqualTo(
        propertyName, literal, Boolean.class, ComparisonPropertyOperation.IS_EQUAL_TO);
  }

  public T propertyIsEqualTo(String propertyName, byte[] literal) {
    return propertyIsEqualTo(
        propertyName, literal, byte[].class, ComparisonPropertyOperation.IS_EQUAL_TO);
  }

  public T propertyIsEqualTo(String propertyName, Object literal) {

    return propertyIsEqualTo(
        propertyName, literal, Object.class, ComparisonPropertyOperation.IS_EQUAL_TO);
  }

  public <S> T propertyIsEqualTo(
      String propertyName,
      S literal,
      Class<S> literalClass,
      ComparisonPropertyOperation operation) {
    return comparisonOperation(propertyName, literal, literalClass, operation);
  }

  // PropertyIsNotEqualTo

  public T propertyIsNotEqualTo(String propertyName, String literal, boolean isCaseSensitive) {
    return propertyIsNotEqualTo(
        propertyName, literal, String.class, ComparisonPropertyOperation.IS_NOT_EQUAL_TO);
  }

  public T propertyIsNotEqualTo(String propertyName, Date literal) {
    return propertyIsNotEqualTo(
        propertyName, literal, Date.class, ComparisonPropertyOperation.IS_NOT_EQUAL_TO);
  }

  public T propertyIsNotEqualTo(String propertyName, Date startDate, Date endDate) {
    return propertyIsNotEqualTo(
        propertyName,
        Range.open(startDate, endDate),
        Range.class,
        ComparisonPropertyOperation.IS_NOT_EQUAL_TO);
  }

  public T propertyIsNotEqualTo(String propertyName, int literal) {
    return propertyIsNotEqualTo(propertyName, (Number) literal);
  }

  public T propertyIsNotEqualTo(String propertyName, short literal) {
    return propertyIsNotEqualTo(propertyName, (Number) literal);
  }

  public T propertyIsNotEqualTo(String propertyName, long literal) {
    return propertyIsNotEqualTo(propertyName, (Number) literal);
  }

  public T propertyIsNotEqualTo(String propertyName, float literal) {
    return propertyIsNotEqualTo(propertyName, (Number) literal);
  }

  public T propertyIsNotEqualTo(String propertyName, double literal) {
    return propertyIsNotEqualTo(propertyName, (Number) literal);
  }

  public T propertyIsNotEqualTo(String propertyName, Number literal) {
    return propertyIsNotEqualTo(
        propertyName, literal, Number.class, ComparisonPropertyOperation.IS_NOT_EQUAL_TO);
  }

  public T propertyIsNotEqualTo(String propertyName, boolean literal) {
    return propertyIsNotEqualTo(
        propertyName, literal, Boolean.class, ComparisonPropertyOperation.IS_NOT_EQUAL_TO);
  }

  public T propertyIsNotEqualTo(String propertyName, byte[] literal) {
    return propertyIsNotEqualTo(
        propertyName, literal, byte[].class, ComparisonPropertyOperation.IS_NOT_EQUAL_TO);
  }

  public T propertyIsNotEqualTo(String propertyName, Object literal) {
    return propertyIsNotEqualTo(
        propertyName, literal, Object.class, ComparisonPropertyOperation.IS_NOT_EQUAL_TO);
  }

  public <S> T propertyIsNotEqualTo(
      String propertyName,
      S literal,
      Class<S> literalClass,
      ComparisonPropertyOperation operation) {
    return comparisonOperation(propertyName, literal, literalClass, operation);
  }

  // PropertyIsGreaterThan

  public T propertyIsGreaterThan(String propertyName, String literal) {
    return propertyIsGreaterThan(
        propertyName, literal, String.class, ComparisonPropertyOperation.IS_GREATER);
  }

  public T propertyIsGreaterThan(String propertyName, Date literal) {
    return propertyIsGreaterThan(
        propertyName, literal, Date.class, ComparisonPropertyOperation.IS_GREATER);
  }

  public T propertyIsGreaterThan(String propertyName, int literal) {
    return propertyIsGreaterThan(propertyName, (Number) literal);
  }

  public T propertyIsGreaterThan(String propertyName, short literal) {
    return propertyIsGreaterThan(propertyName, (Number) literal);
  }

  public T propertyIsGreaterThan(String propertyName, long literal) {
    return propertyIsGreaterThan(propertyName, (Number) literal);
  }

  public T propertyIsGreaterThan(String propertyName, float literal) {
    return propertyIsGreaterThan(propertyName, (Number) literal);
  }

  public T propertyIsGreaterThan(String propertyName, double literal) {
    return propertyIsGreaterThan(propertyName, (Number) literal);
  }

  public T propertyIsGreaterThan(String propertyName, Number literal) {
    return propertyIsGreaterThan(
        propertyName, literal, Number.class, ComparisonPropertyOperation.IS_GREATER);
  }

  public T propertyIsGreaterThan(String propertyName, Object literal) {
    return propertyIsGreaterThan(
        propertyName, literal, Object.class, ComparisonPropertyOperation.IS_GREATER);
  }

  public <S> T propertyIsGreaterThan(
      String propertyName,
      S literal,
      Class<S> literalClass,
      ComparisonPropertyOperation operation) {
    return comparisonOperation(propertyName, literal, literalClass, operation);
  }

  // PropertyIsGreaterThanOrEqualTo

  public T propertyIsGreaterThanOrEqualTo(String propertyName, String literal) {
    return propertyIsGreaterThanOrEqualTo(
        propertyName, literal, String.class, ComparisonPropertyOperation.IS_GREATER_OR_EQUAL_TO);
  }

  public T propertyIsGreaterThanOrEqualTo(String propertyName, Date literal) {
    return propertyIsGreaterThanOrEqualTo(
        propertyName, literal, Date.class, ComparisonPropertyOperation.IS_GREATER_OR_EQUAL_TO);
  }

  public T propertyIsGreaterThanOrEqualTo(String propertyName, int literal) {
    return propertyIsGreaterThanOrEqualTo(propertyName, (Number) literal);
  }

  public T propertyIsGreaterThanOrEqualTo(String propertyName, short literal) {
    return propertyIsGreaterThanOrEqualTo(propertyName, (Number) literal);
  }

  public T propertyIsGreaterThanOrEqualTo(String propertyName, long literal) {
    return propertyIsGreaterThanOrEqualTo(propertyName, (Number) literal);
  }

  public T propertyIsGreaterThanOrEqualTo(String propertyName, float literal) {
    return propertyIsGreaterThanOrEqualTo(propertyName, (Number) literal);
  }

  public T propertyIsGreaterThanOrEqualTo(String propertyName, double literal) {
    return propertyIsGreaterThanOrEqualTo(propertyName, (Number) literal);
  }

  public T propertyIsGreaterThanOrEqualTo(String propertyName, Number literal) {
    return propertyIsGreaterThanOrEqualTo(
        propertyName, literal, Number.class, ComparisonPropertyOperation.IS_GREATER_OR_EQUAL_TO);
  }

  public T propertyIsGreaterThanOrEqualTo(String propertyName, Object literal) {
    return propertyIsGreaterThanOrEqualTo(
        propertyName, literal, Object.class, ComparisonPropertyOperation.IS_GREATER_OR_EQUAL_TO);
  }

  public <S> T propertyIsGreaterThanOrEqualTo(
      String propertyName,
      S literal,
      Class<S> literalClass,
      ComparisonPropertyOperation operation) {
    return comparisonOperation(propertyName, literal, literalClass, operation);
  }

  // PropertyIsLessThan

  public T propertyIsLessThan(String propertyName, String literal) {
    return propertyIsLessThan(
        propertyName, literal, String.class, ComparisonPropertyOperation.IS_LESS_THAN);
  }

  public T propertyIsLessThan(String propertyName, Date literal) {
    return propertyIsLessThan(
        propertyName, literal, Date.class, ComparisonPropertyOperation.IS_LESS_THAN);
  }

  public T propertyIsLessThan(String propertyName, int literal) {
    return propertyIsLessThan(propertyName, (Number) literal);
  }

  public T propertyIsLessThan(String propertyName, short literal) {
    return propertyIsLessThan(propertyName, (Number) literal);
  }

  public T propertyIsLessThan(String propertyName, long literal) {
    return propertyIsLessThan(propertyName, (Number) literal);
  }

  public T propertyIsLessThan(String propertyName, float literal) {
    return propertyIsLessThan(propertyName, (Number) literal);
  }

  public T propertyIsLessThan(String propertyName, double literal) {
    return propertyIsLessThan(propertyName, (Number) literal);
  }

  public T propertyIsLessThan(String propertyName, Number literal) {
    return propertyIsLessThan(
        propertyName, literal, Number.class, ComparisonPropertyOperation.IS_LESS_THAN);
  }

  public T propertyIsLessThan(String propertyName, Object literal) {
    return propertyIsLessThan(
        propertyName, literal, Object.class, ComparisonPropertyOperation.IS_LESS_THAN);
  }

  public <S> T propertyIsLessThan(
      String propertyName,
      S literal,
      Class<S> literalClass,
      ComparisonPropertyOperation operation) {
    return comparisonOperation(propertyName, literal, literalClass, operation);
  }

  // PropertyIsLessThanOrEqualTo

  public T propertyIsLessThanOrEqualTo(String propertyName, String literal) {
    return propertyIsLessThanOrEqualTo(
        propertyName, literal, String.class, ComparisonPropertyOperation.IS_LESS_OR_EQUAL_TO);
  }

  public T propertyIsLessThanOrEqualTo(String propertyName, Date literal) {
    return propertyIsLessThanOrEqualTo(
        propertyName, literal, Date.class, ComparisonPropertyOperation.IS_LESS_OR_EQUAL_TO);
  }

  public T propertyIsLessThanOrEqualTo(String propertyName, int literal) {
    return propertyIsLessThanOrEqualTo(propertyName, (Number) literal);
  }

  public T propertyIsLessThanOrEqualTo(String propertyName, short literal) {
    return propertyIsLessThanOrEqualTo(propertyName, (Number) literal);
  }

  public T propertyIsLessThanOrEqualTo(String propertyName, long literal) {
    return propertyIsLessThanOrEqualTo(propertyName, (Number) literal);
  }

  public T propertyIsLessThanOrEqualTo(String propertyName, float literal) {
    return propertyIsLessThanOrEqualTo(propertyName, (Number) literal);
  }

  public T propertyIsLessThanOrEqualTo(String propertyName, double literal) {
    return propertyIsLessThanOrEqualTo(propertyName, (Number) literal);
  }

  public T propertyIsLessThanOrEqualTo(String propertyName, Number literal) {
    return propertyIsLessThanOrEqualTo(
        propertyName, literal, Number.class, ComparisonPropertyOperation.IS_LESS_OR_EQUAL_TO);
  }

  public T propertyIsLessThanOrEqualTo(String propertyName, Object literal) {
    return propertyIsLessThanOrEqualTo(
        propertyName, literal, Object.class, ComparisonPropertyOperation.IS_LESS_OR_EQUAL_TO);
  }

  public <S> T propertyIsLessThanOrEqualTo(
      String propertyName,
      S literal,
      Class<S> literalClass,
      ComparisonPropertyOperation operation) {
    return comparisonOperation(propertyName, literal, literalClass, operation);
  }

  // PropertyIsBetween

  public T propertyIsBetween(String propertyName, String lowerBoundary, String upperBoundary) {
    return propertyIsBetween(
        propertyName,
        lowerBoundary,
        upperBoundary,
        String.class,
        ComparisonPropertyOperation.IS_BETWEEN);
  }

  public T propertyIsBetween(String propertyName, Date lowerBoundary, Date upperBoundary) {
    return comparisonOperation(
        propertyName,
        Range.closed(lowerBoundary, upperBoundary),
        Range.class,
        ComparisonPropertyOperation.IS_BETWEEN);
  }

  public T propertyIsBetween(String propertyName, int lowerBoundary, int upperBoundary) {
    return propertyIsBetween(propertyName, (Number) lowerBoundary, (Number) upperBoundary);
  }

  public T propertyIsBetween(String propertyName, short lowerBoundary, short upperBoundary) {
    return propertyIsBetween(propertyName, (Number) lowerBoundary, (Number) upperBoundary);
  }

  public T propertyIsBetween(String propertyName, long lowerBoundary, long upperBoundary) {
    return propertyIsBetween(propertyName, (Number) lowerBoundary, (Number) upperBoundary);
  }

  public T propertyIsBetween(String propertyName, float lowerBoundary, float upperBoundary) {
    return propertyIsBetween(propertyName, (Number) lowerBoundary, (Number) upperBoundary);
  }

  public T propertyIsBetween(String propertyName, double lowerBoundary, double upperBoundary) {
    return propertyIsBetween(propertyName, (Number) lowerBoundary, (Number) upperBoundary);
  }

  public T propertyIsBetween(String propertyName, Number lowerBoundary, Number upperBoundary) {
    return propertyIsBetween(
        propertyName,
        lowerBoundary,
        upperBoundary,
        Number.class,
        ComparisonPropertyOperation.IS_BETWEEN);
  }

  public T propertyIsBetween(String propertyName, Object lowerBoundary, Object upperBoundary) {
    return propertyIsBetween(
        propertyName,
        lowerBoundary,
        upperBoundary,
        Object.class,
        ComparisonPropertyOperation.IS_BETWEEN);
  }

  public <S> T propertyIsBetween(
      String propertyName,
      S lowerBoundary,
      S upperBoundary,
      Class<S> literalClass,
      ComparisonPropertyOperation operation) {
    return comparisonOperation(propertyName, lowerBoundary, literalClass, operation);
  }

  public T propertyIsNull(String propertyName) {
    return comparisonOperation(propertyName, null, null, ComparisonPropertyOperation.IS_NULL);
  }

  public T propertyIsLike(String propertyName, String pattern, boolean isCaseSensitive) {
    return comparisonOperation(
        propertyName, pattern, String.class, ComparisonPropertyOperation.IS_LIKE);
  }

  public T propertyIsFuzzy(String propertyName, String literal) {
    return comparisonOperation(
        propertyName, literal, String.class, ComparisonPropertyOperation.IS_FUZZY);
  }

  // XPath operators
  public T xpathExists(String xpath) {
    return xpathOperation(xpath, null, null, XPathPropertyOperation.XPATH_EXISTS);
  }

  public T xpathIsLike(String xpath, String pattern, boolean isCaseSensitive) {
    return xpathOperation(xpath, pattern, String.class, XPathPropertyOperation.XPATH_IS_LIKE);
  }

  public T xpathIsFuzzy(String xpath, String literal) {
    return xpathOperation(xpath, literal, String.class, XPathPropertyOperation.XPATH_IS_FUZZY);
  }

  // Spatial operators

  public T nearestNeighbor(String propertyName, String wkt) {
    return spatialOperation(
        propertyName, wkt, String.class, SpatialPropertyOperation.NEAREST_NEIGHBOR);
  }

  public T beyond(String propertyName, String wkt, double distance) {
    return spatialOperation(propertyName, wkt, String.class, SpatialPropertyOperation.BEYOND);
  }

  public T contains(String propertyName, String wkt) {
    return spatialOperation(propertyName, wkt, String.class, SpatialPropertyOperation.CONTAINS);
  }

  public T crosses(String propertyName, String wkt) {
    return spatialOperation(propertyName, wkt, String.class, SpatialPropertyOperation.CROSSES);
  }

  public T disjoint(String propertyName, String wkt) {
    return spatialOperation(propertyName, wkt, String.class, SpatialPropertyOperation.DISJOINT);
  }

  public T dwithin(String propertyName, String wkt, double distance) {
    return spatialOperation(propertyName, wkt, String.class, SpatialPropertyOperation.DWITHIN);
  }

  public T intersects(String propertyName, String wkt) {
    return spatialOperation(propertyName, wkt, String.class, SpatialPropertyOperation.INTERSECTS);
  }

  public T overlaps(String propertyName, String wkt) {
    return spatialOperation(propertyName, wkt, String.class, SpatialPropertyOperation.OVERLAPS);
  }

  public T touches(String propertyName, String wkt) {
    return spatialOperation(propertyName, wkt, String.class, SpatialPropertyOperation.TOUCHES);
  }

  public T within(String propertyName, String wkt) {
    return spatialOperation(propertyName, wkt, String.class, SpatialPropertyOperation.WITHIN);
  }

  // Temporal operators

  public T after(String propertyName, Date date) {
    return temporalOperation(propertyName, date, Date.class, TemporalPropertyOperation.AFTER);
  }

  public T before(String propertyName, Date date) {
    return temporalOperation(propertyName, date, Date.class, TemporalPropertyOperation.BEFORE);
  }

  public T during(String propertyName, Date startDate, Date endDate) {

    return temporalOperation(
        propertyName,
        Range.closed(startDate, endDate),
        Range.class,
        TemporalPropertyOperation.DURING);
  }

  public T begins(String propertyName, Date startDate, Date endDate) {
    return temporalOperation(propertyName, startDate, Date.class, TemporalPropertyOperation.BEGINS);
  }

  public T relative(String propertyName, long duration) {
    return temporalOperation(
        propertyName, duration, long.class, TemporalPropertyOperation.RELATIVE);
  }

  public enum XPathPropertyOperation {
    XPATH_EXISTS,
    XPATH_IS_LIKE,
    XPATH_IS_FUZZY
  }

  public enum TemporalPropertyOperation {
    AFTER,
    BEFORE,
    DURING,
    BEGINS,
    RELATIVE
  }

  public enum LogicalPropertyOperation {
    AND,
    OR,
    NOT,
    INCLUDE,
    EXCLUDE
  }

  public enum SpatialPropertyOperation {
    NEAREST_NEIGHBOR,
    BEYOND,
    CONTAINS,
    CROSSES,
    DISJOINT,
    DWITHIN,
    INTERSECTS,
    OVERLAPS,
    TOUCHES,
    WITHIN
  }

  public enum ComparisonPropertyOperation {
    IS_EQUAL_TO,
    IS_NOT_EQUAL_TO,
    IS_LIKE,
    IS_FUZZY,
    IS_BETWEEN,
    IS_NULL,
    IS_LESS_THAN,
    IS_LESS_OR_EQUAL_TO,
    IS_GREATER,
    IS_GREATER_OR_EQUAL_TO
  }

  public enum FunctionOperation {
    FUNCTION
  }
}
