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
package ddf.catalog.filter.proxy.adapter;

import static java.lang.Math.abs;

import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.impl.filter.FuzzyFunction;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.measure.Distance;
import ddf.measure.Distance.LinearUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.geometry.jts.spatialschema.geometry.GeometryImpl;
import org.geotools.styling.UomOgcMapping;
import org.geotools.temporal.object.DefaultPeriodDuration;
import org.geotools.util.Converters;
import org.joda.time.DateTime;
import org.opengis.filter.And;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.ExcludeFilter;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.Id;
import org.opengis.filter.IncludeFilter;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNil;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.expression.Add;
import org.opengis.filter.expression.Divide;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.Multiply;
import org.opengis.filter.expression.NilExpression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.expression.Subtract;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.AnyInteracts;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.Begins;
import org.opengis.filter.temporal.BegunBy;
import org.opengis.filter.temporal.BinaryTemporalOperator;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.EndedBy;
import org.opengis.filter.temporal.Ends;
import org.opengis.filter.temporal.Meets;
import org.opengis.filter.temporal.MetBy;
import org.opengis.filter.temporal.OverlappedBy;
import org.opengis.filter.temporal.TContains;
import org.opengis.filter.temporal.TEquals;
import org.opengis.filter.temporal.TOverlaps;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;

public class GeotoolsFilterAdapterImpl implements FilterAdapter, FilterVisitor, ExpressionVisitor {

  public static final String CQL_FEET = "feet";

  public static final String CQL_METERS = "meters";

  public static final String CQL_STATUTE_MILES = "statute miles";

  public static final String CQL_NAUTICAL_MILES = "nautical miles";

  public static final String CQL_KILOMETERS = "kilometers";

  private static final String EXPRESSION_NOT_SUPPORTED =
      " expression not supported by Filter Adapter.";

  private static final String FILTER_NOT_SUPPORTED = " filter not supported by Filter Adapter.";

  private static final double ERROR_THRESHOLD = .000001;

  private static final FilterFactory FF = new FilterFactoryImpl();

  // We shouldn't be using a regex here. This should be replaced by a function like the ones found
  // in ddf.catalog.impl.filter
  private static final String DECIMAL_REGEX = "\\\\d*\\\\.\\\\d+|\\\\d+\\\\.?\\\\d*";

  private static final String SHORTENED_RELATIVE_TEMPORAL_REGEX =
      "RELATIVE\\(P(?!$)(?:(dec)Y)?(?:(dec)M)?(?:(dec)W)?(?:(dec)D)?(?:T(?=dec)(?:(dec)H)?(?:(dec)M)?(?:(dec)S)?)?\\)";

  private static final Pattern RELATIVE_TEMPORAL_REGEX =
      Pattern.compile(SHORTENED_RELATIVE_TEMPORAL_REGEX.replaceAll("dec", DECIMAL_REGEX));

  public <T> T adapt(Filter filter, FilterDelegate<T> filterDelegate)
      throws UnsupportedQueryException {
    if (filter == null) {
      throw new IllegalArgumentException("Cannot adapt a null Filter.");
    }
    try {
      @SuppressWarnings("unchecked")
      T result = (T) filter.accept(this, filterDelegate);
      return result;
    } catch (UnsupportedOperationException e) {
      throw new UnsupportedQueryException(e.getMessage(), e);
    }
  }

  public Object visit(NilExpression expression, Object delegate) {
    throw new UnsupportedOperationException(
        NilExpression.class.getSimpleName() + EXPRESSION_NOT_SUPPORTED);
  }

  public Object visit(Add expression, Object delegate) {
    throw new UnsupportedOperationException(Add.NAME + EXPRESSION_NOT_SUPPORTED);
  }

  public Object visit(Divide expression, Object delegate) {
    throw new UnsupportedOperationException(Divide.NAME + EXPRESSION_NOT_SUPPORTED);
  }

  @Override
  public Object visit(Function expression, Object delegate) {
    if (expression == null) {
      throw new UnsupportedOperationException(
          Function.class.getSimpleName() + " must not be null.");
    }
    List<Expression> parameters = expression.getParameters();

    int argCount = expression.getFunctionName().getArgumentCount();

    if (parameters == null || parameters.size() != argCount) {
      throw new UnsupportedOperationException(
          Function.class.getSimpleName() + " requires " + argCount + " arguments.");
    }
    // unwrap the function arguments and return them
    List<Object> ret = new ArrayList<>();
    for (int i = 0; i < argCount; i++) {
      Class<?> type = expression.getFunctionName().getArguments().get(i).getType();
      Expression arg = parameters.get(i);
      ret.add(arg.accept(this, type)); // the type would only be used by literals
    }

    return ret;
  }

  @Override
  public Object visit(Literal expression, Object clazz) {
    if (expression.getValue() == null) {
      throw new UnsupportedOperationException(
          Literal.class.getSimpleName() + " value must not be null.");
    }
    // try and get the object as the requested class otherwise just return the object
    if (clazz instanceof Class) {
      Object ret = Converters.convert(expression.getValue(), (Class) clazz);
      if (ret != null) {
        return ret;
      } else {
        throw new IllegalArgumentException(
            "Could not convert:" + expression.getValue().getClass() + " to " + clazz);
      }
    }
    return expression.getValue();
  }

  public Object visit(Multiply expression, Object delegate) {
    throw new UnsupportedOperationException(Multiply.NAME + " expression not supported.");
  }

  public Object visit(PropertyName expression, Object delegate) {
    if (expression.getPropertyName() == null) {
      throw new UnsupportedOperationException("Property name must not be null.");
    }
    return expression.getPropertyName();
  }

  public Object visit(Subtract expression, Object delegate) {
    throw new UnsupportedOperationException(Subtract.NAME + " expresssion not supported.");
  }

  public Object visitNullFilter(Object delegate) {
    throw new UnsupportedOperationException("Null filter not supported by Filter Adapter.");
  }

  public Object visit(ExcludeFilter filter, Object delegate) {
    return ((FilterDelegate<?>) delegate).exclude();
  }

  public Object visit(IncludeFilter filter, Object delegate) {
    return ((FilterDelegate<?>) delegate).include();
  }

  public Object visit(Id filter, Object delegate) {
    throw new UnsupportedOperationException(Id.class.getSimpleName() + FILTER_NOT_SUPPORTED);
  }

  @SuppressWarnings("unchecked")
  public Object visit(And filter, Object delegate) {
    List<Object> results = new ArrayList<>();
    List<Filter> childList = filter.getChildren();

    if (childList != null) {
      for (Filter child : childList) {
        results.add(child.accept(this, delegate));
      }
      if (results.size() == 1) {
        // removing unused and
        return results.get(0);
      } else if (results.size() > 0) {
        return ((FilterDelegate<Object>) delegate).and(results);
      }
    }
    // No children or the children returned 0 results
    throw new UnsupportedOperationException("No valid operands for And filter.");
  }

  @SuppressWarnings("unchecked")
  public Object visit(Not filter, Object delegate) {
    Filter child = filter.getFilter();

    return ((FilterDelegate<Object>) delegate).not(child.accept(this, delegate));
  }

  @SuppressWarnings("unchecked")
  public Object visit(Or filter, Object delegate) {
    List<Object> results = new ArrayList<>();
    List<Filter> childList = filter.getChildren();

    if (childList != null) {
      for (Filter child : childList) {
        results.add(child.accept(this, delegate));
      }
      if (results.size() > 0) {
        return ((FilterDelegate<Object>) delegate).or(results);
      }
    }
    // No children or the children returned 0 results
    throw new UnsupportedOperationException("No valid operands for And filter.");
  }

  public Object visit(PropertyIsBetween filter, Object delegate) {
    String propertyName;
    Object lower;
    Object upper;

    if (filter.getExpression() instanceof PropertyName
        && filter.getLowerBoundary() instanceof Literal
        && filter.getUpperBoundary() instanceof Literal) {
      propertyName = (String) filter.getExpression().accept(this, delegate);
      lower = filter.getLowerBoundary().accept(this, delegate);
      upper = filter.getUpperBoundary().accept(this, delegate);
    } else {
      throw new UnsupportedOperationException(
          "Only support PropertyName for expression and Literal for upper and lower boundaries with PropertyIsBetween filter.");
    }

    if (lower instanceof String && upper instanceof String) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsBetween(propertyName, (String) lower, (String) upper);
    } else if (lower instanceof Date && upper instanceof Date) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsBetween(propertyName, (Date) lower, (Date) upper);
    } else if (lower instanceof Instant && upper instanceof Instant) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsBetween(
              propertyName,
              ((Instant) lower).getPosition().getDate(),
              ((Instant) upper).getPosition().getDate());
    } else if (lower instanceof Integer && upper instanceof Integer) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsBetween(
              propertyName, ((Integer) lower).intValue(), ((Integer) upper).intValue());
    } else if (lower instanceof Short && upper instanceof Short) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsBetween(
              propertyName, ((Short) lower).shortValue(), ((Short) upper).shortValue());
    } else if (lower instanceof Long && upper instanceof Long) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsBetween(propertyName, ((Long) lower).longValue(), ((Long) upper).longValue());
    } else if (lower instanceof Float && upper instanceof Float) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsBetween(
              propertyName, ((Float) lower).floatValue(), ((Float) upper).floatValue());
    } else if (lower instanceof Double && upper instanceof Double) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsBetween(
              propertyName, ((Double) lower).doubleValue(), ((Double) upper).doubleValue());
    } else {
      return ((FilterDelegate<?>) delegate).propertyIsBetween(propertyName, lower, upper);
    }
  }

  public Object visit(PropertyIsEqualTo filter, Object delegate) {
    ExpressionValues filterValues = getExpressions(filter, delegate);
    String propertyName = filterValues.propertyName;
    Object literal = filterValues.literal;
    String functionName = filterValues.functionName;
    List<Object> functionArgs = filterValues.functionArgs;

    // Special case to handle relative temporal queries
    if (literal instanceof String && RELATIVE_TEMPORAL_REGEX.matcher((String) literal).matches()) {
      DateTime currentDateTime = new DateTime();

      org.joda.time.Period period = PeriodParser.parse((String) literal, RELATIVE_TEMPORAL_REGEX);
      DateTime pastDateTime = currentDateTime.minus(period);

      return ((FilterDelegate<?>) delegate)
          .propertyIsBetween(propertyName, pastDateTime.toDate(), currentDateTime.toDate());
    }

    if (functionName != null) {
      return ((FilterDelegate<?>) delegate).propertyIsEqualTo(functionName, functionArgs, literal);
    } else if (literal instanceof String) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsEqualTo(propertyName, (String) literal, filter.isMatchingCase());
    } else if (literal instanceof Date) {
      return ((FilterDelegate<?>) delegate).propertyIsEqualTo(propertyName, (Date) literal);
    } else if (literal instanceof Instant) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsEqualTo(propertyName, ((Instant) literal).getPosition().getDate());
    } else if (literal instanceof Period) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsEqualTo(
              propertyName,
              ((Period) literal).getBeginning().getPosition().getDate(),
              ((Period) literal).getEnding().getPosition().getDate());
    } else if (literal instanceof Integer) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsEqualTo(propertyName, ((Integer) literal).intValue());
    } else if (literal instanceof Short) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsEqualTo(propertyName, ((Short) literal).shortValue());
    } else if (literal instanceof Long) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsEqualTo(propertyName, ((Long) literal).longValue());
    } else if (literal instanceof Float) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsEqualTo(propertyName, ((Float) literal).floatValue());
    } else if (literal instanceof Double) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsEqualTo(propertyName, ((Double) literal).doubleValue());
    } else if (literal instanceof Boolean) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsEqualTo(propertyName, ((Boolean) literal).booleanValue());
    } else if (literal instanceof byte[]) {
      return ((FilterDelegate<?>) delegate).propertyIsEqualTo(propertyName, (byte[]) literal);
    } else {
      return ((FilterDelegate<?>) delegate).propertyIsEqualTo(propertyName, literal);
    }
  }

  public Object visit(PropertyIsNotEqualTo filter, Object delegate) {
    ExpressionValues filterValues = getExpressions(filter, delegate);
    String propertyName = filterValues.propertyName;
    Object literal = filterValues.literal;

    if (literal instanceof String) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsNotEqualTo(propertyName, (String) literal, filter.isMatchingCase());
    } else if (literal instanceof Date) {
      return ((FilterDelegate<?>) delegate).propertyIsNotEqualTo(propertyName, (Date) literal);
    } else if (literal instanceof Instant) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsNotEqualTo(propertyName, ((Instant) literal).getPosition().getDate());
    } else if (literal instanceof Period) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsNotEqualTo(
              propertyName,
              ((Period) literal).getBeginning().getPosition().getDate(),
              ((Period) literal).getEnding().getPosition().getDate());
    } else if (literal instanceof Integer) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsNotEqualTo(propertyName, ((Integer) literal).intValue());
    } else if (literal instanceof Short) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsNotEqualTo(propertyName, ((Short) literal).shortValue());
    } else if (literal instanceof Long) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsNotEqualTo(propertyName, ((Long) literal).longValue());
    } else if (literal instanceof Float) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsNotEqualTo(propertyName, ((Float) literal).floatValue());
    } else if (literal instanceof Double) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsNotEqualTo(propertyName, ((Double) literal).doubleValue());
    } else if (literal instanceof Boolean) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsNotEqualTo(propertyName, ((Boolean) literal).booleanValue());
    } else if (literal instanceof byte[]) {
      return ((FilterDelegate<?>) delegate).propertyIsNotEqualTo(propertyName, (byte[]) literal);
    } else {
      return ((FilterDelegate<?>) delegate).propertyIsNotEqualTo(propertyName, literal);
    }
  }

  public Object visit(PropertyIsGreaterThan filter, Object delegate) {
    ExpressionValues filterValues = getExpressions(filter, delegate);
    String propertyName = filterValues.propertyName;
    Object literal = filterValues.literal;

    // Are property name and literal reversed?
    if (filter.getExpression1() instanceof Literal) {
      // convert literal > property to property < literal
      Filter lessThan = FF.less(FF.property(propertyName), FF.literal(literal));
      return lessThan.accept(this, delegate);
    }

    if (literal instanceof String) {
      return ((FilterDelegate<?>) delegate).propertyIsGreaterThan(propertyName, (String) literal);
    } else if (literal instanceof Date) {
      return ((FilterDelegate<?>) delegate).propertyIsGreaterThan(propertyName, (Date) literal);
    } else if (literal instanceof Integer) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsGreaterThan(propertyName, ((Integer) literal).intValue());
    } else if (literal instanceof Short) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsGreaterThan(propertyName, ((Short) literal).shortValue());
    } else if (literal instanceof Long) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsGreaterThan(propertyName, ((Long) literal).longValue());
    } else if (literal instanceof Float) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsGreaterThan(propertyName, ((Float) literal).floatValue());
    } else if (literal instanceof Double) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsGreaterThan(propertyName, ((Double) literal).doubleValue());
    } else if (literal instanceof Boolean) {
      return ((FilterDelegate<?>) delegate).propertyIsGreaterThan(propertyName, literal);
    } else if (literal instanceof byte[]) {
      return ((FilterDelegate<?>) delegate).propertyIsGreaterThan(propertyName, literal);
    } else {
      return ((FilterDelegate<?>) delegate).propertyIsGreaterThan(propertyName, literal);
    }
  }

  public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object delegate) {
    ExpressionValues filterValues = getExpressions(filter, delegate);
    String propertyName = filterValues.propertyName;
    Object literal = filterValues.literal;

    // Are property name and literal reversed?
    if (filter.getExpression1() instanceof Literal) {
      // convert literal >= property to property <= literal
      Filter lessThanOrEqual = FF.lessOrEqual(FF.property(propertyName), FF.literal(literal));
      return lessThanOrEqual.accept(this, delegate);
    }

    if (literal instanceof String) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsGreaterThanOrEqualTo(propertyName, (String) literal);
    } else if (literal instanceof Date) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsGreaterThanOrEqualTo(propertyName, (Date) literal);
    } else if (literal instanceof Integer) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsGreaterThanOrEqualTo(propertyName, ((Integer) literal).intValue());
    } else if (literal instanceof Short) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsGreaterThanOrEqualTo(propertyName, ((Short) literal).shortValue());
    } else if (literal instanceof Long) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsGreaterThanOrEqualTo(propertyName, ((Long) literal).longValue());
    } else if (literal instanceof Float) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsGreaterThanOrEqualTo(propertyName, ((Float) literal).floatValue());
    } else if (literal instanceof Double) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsGreaterThanOrEqualTo(propertyName, ((Double) literal).doubleValue());
    } else if (literal instanceof Boolean) {
      return ((FilterDelegate<?>) delegate).propertyIsGreaterThanOrEqualTo(propertyName, literal);
    } else if (literal instanceof byte[]) {
      return ((FilterDelegate<?>) delegate).propertyIsGreaterThanOrEqualTo(propertyName, literal);
    } else {
      return ((FilterDelegate<?>) delegate).propertyIsGreaterThanOrEqualTo(propertyName, literal);
    }
  }

  public Object visit(PropertyIsLessThan filter, Object delegate) {
    ExpressionValues filterValues = getExpressions(filter, delegate);
    String propertyName = filterValues.propertyName;
    Object literal = filterValues.literal;

    // Are property name and literal reversed?
    if (filter.getExpression1() instanceof Literal) {
      // convert literal < property to property > literal
      Filter greaterThan = FF.greater(FF.property(propertyName), FF.literal(literal));
      return greaterThan.accept(this, delegate);
    }

    if (literal instanceof String) {
      return ((FilterDelegate<?>) delegate).propertyIsLessThan(propertyName, (String) literal);
    } else if (literal instanceof Date) {
      return ((FilterDelegate<?>) delegate).propertyIsLessThan(propertyName, (Date) literal);
    } else if (literal instanceof Integer) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsLessThan(propertyName, ((Integer) literal).intValue());
    } else if (literal instanceof Short) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsLessThan(propertyName, ((Short) literal).shortValue());
    } else if (literal instanceof Long) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsLessThan(propertyName, ((Long) literal).longValue());
    } else if (literal instanceof Float) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsLessThan(propertyName, ((Float) literal).floatValue());
    } else if (literal instanceof Double) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsLessThan(propertyName, ((Double) literal).doubleValue());
    } else if (literal instanceof Boolean) {
      return ((FilterDelegate<?>) delegate).propertyIsLessThan(propertyName, literal);
    } else if (literal instanceof byte[]) {
      return ((FilterDelegate<?>) delegate).propertyIsLessThan(propertyName, literal);
    } else {
      return ((FilterDelegate<?>) delegate).propertyIsLessThan(propertyName, literal);
    }
  }

  public Object visit(PropertyIsLessThanOrEqualTo filter, Object delegate) {
    ExpressionValues filterValues = getExpressions(filter, delegate);
    String propertyName = filterValues.propertyName;
    Object literal = filterValues.literal;

    // Are property name and literal reversed?
    if (filter.getExpression1() instanceof Literal) {
      // convert literal <= property to property >= literal
      Filter greaterThanOrEqual = FF.greaterOrEqual(FF.property(propertyName), FF.literal(literal));
      return greaterThanOrEqual.accept(this, delegate);
    }

    if (literal instanceof String) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsLessThanOrEqualTo(propertyName, (String) literal);
    } else if (literal instanceof Date) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsLessThanOrEqualTo(propertyName, (Date) literal);
    } else if (literal instanceof Integer) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsLessThanOrEqualTo(propertyName, ((Integer) literal).intValue());
    } else if (literal instanceof Short) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsLessThanOrEqualTo(propertyName, ((Short) literal).shortValue());
    } else if (literal instanceof Long) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsLessThanOrEqualTo(propertyName, ((Long) literal).longValue());
    } else if (literal instanceof Float) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsLessThanOrEqualTo(propertyName, ((Float) literal).floatValue());
    } else if (literal instanceof Double) {
      return ((FilterDelegate<?>) delegate)
          .propertyIsLessThanOrEqualTo(propertyName, ((Double) literal).doubleValue());
    } else if (literal instanceof Boolean) {
      return ((FilterDelegate<?>) delegate).propertyIsLessThanOrEqualTo(propertyName, literal);
    } else if (literal instanceof byte[]) {
      return ((FilterDelegate<?>) delegate).propertyIsLessThanOrEqualTo(propertyName, literal);
    } else {
      return ((FilterDelegate<?>) delegate).propertyIsLessThanOrEqualTo(propertyName, literal);
    }
  }

  public Object visit(PropertyIsLike filter, Object delegate) {
    String propertyName;
    String wildcard = filter.getWildCard();
    String singleChar = filter.getSingleChar();
    String escapeChar = filter.getEscape();

    if (filter.getExpression() == null || filter.getLiteral() == null) {
      throw new UnsupportedOperationException(
          "Expression and Literal must not be null for PropertyIsLike.");
    }

    if (wildcard.length() > 1 || singleChar.length() > 1 || escapeChar.length() > 1) {
      throw new UnsupportedOperationException(
          "Wildcard, single, and escape characters must be a single character for PropertyIsLike.");
    }
    if (wildcard.equals(singleChar)
        || wildcard.equals(escapeChar)
        || singleChar.equals(escapeChar)) {
      throw new UnsupportedOperationException(
          "Wildcard, single, and escape characters must be different for PropertyIsLike.");
    }

    String pattern = normalizePattern(filter.getLiteral(), wildcard, singleChar, escapeChar);
    boolean matchCase = filter.isMatchingCase();
    boolean isFuzzy = false;

    if (filter.getExpression() instanceof FuzzyFunction) {
      FuzzyFunction fuzzy = (FuzzyFunction) filter.getExpression();
      propertyName = ((PropertyName) (fuzzy.getParameters().get(0))).getPropertyName();
      isFuzzy = true;
    } else if (filter.getExpression() instanceof PropertyName) {
      PropertyName expression = (PropertyName) filter.getExpression();
      propertyName = expression.getPropertyName();
    } else {
      throw new UnsupportedOperationException(
          "Only support PropertyName expression for PropertyIsLike filter.");
    }

    boolean isXpathSearch = (propertyName.indexOf('/') != -1 || propertyName.indexOf('@') != -1);

    if (!isFuzzy && !isXpathSearch) {
      return ((FilterDelegate<?>) delegate).propertyIsLike(propertyName, pattern, matchCase);
    } else if (isFuzzy && !isXpathSearch) {
      // TODO check if wildcards are escaped
      return ((FilterDelegate<?>) delegate).propertyIsFuzzy(propertyName, pattern);
    } else if (!isFuzzy && isXpathSearch) {
      if (pattern.trim().isEmpty() || pattern.trim().equals(FilterDelegate.WILDCARD_CHAR)) {
        return ((FilterDelegate<?>) delegate).xpathExists(propertyName);
      } else {
        return ((FilterDelegate<?>) delegate).xpathIsLike(propertyName, pattern, matchCase);
      }
    } else if (isFuzzy && isXpathSearch) {
      // TODO check if wildcards are escaped
      return ((FilterDelegate<?>) delegate).xpathIsFuzzy(propertyName, pattern);
    } else {
      throw new UnsupportedOperationException("Unsupported operands for PropertyIsLike.");
    }
  }

  private String normalizePattern(
      String pattern, String wildcard, String singleChar, String escapeChar) {
    StringBuilder sb = new StringBuilder(pattern.length());
    for (int i = 0; i < pattern.length(); i++) {
      char c = pattern.charAt(i);
      if (c == escapeChar.charAt(0)) {
        if (i + 1 < pattern.length()) {
          i++;
          String next = Character.toString(pattern.charAt(i));
          if (next.equals(FilterDelegate.WILDCARD_CHAR)
              || next.equals(FilterDelegate.SINGLE_CHAR)
              || next.equals(FilterDelegate.ESCAPE_CHAR)) {
            // target normalized character needs to be escaped
            sb.append(FilterDelegate.ESCAPE_CHAR);
            sb.append(next);
          } else {
            // escaped character is not a normalized character
            // and does not need to be escaped anymore
            sb.append(next);
          }
        }
      } else if (c == singleChar.charAt(0)) {
        sb.append(FilterDelegate.SINGLE_CHAR);
      } else if (c == wildcard.charAt(0)) {
        sb.append(FilterDelegate.WILDCARD_CHAR);
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  public Object visit(PropertyIsNull filter, Object delegate) {
    if (filter.getExpression() instanceof PropertyName) {
      String propertyName = (String) filter.getExpression().accept(this, delegate);
      return ((FilterDelegate<?>) delegate).propertyIsNull(propertyName);
    } else {
      throw new UnsupportedOperationException(
          "Only support PropertyName expressions for PropertyIsNull.");
    }
  }

  @Override
  public Object visit(PropertyIsNil arg0, Object arg1) {
    throw new UnsupportedOperationException(
        PropertyIsNil.NAME + " filter is not supported by Filter Adapter.");
  }

  public Object visit(BBOX filter, Object delegate) {
    throw new UnsupportedOperationException(
        BBOX.NAME + " filter is not supported by Filter Adapter.");
  }

  public Object visit(Beyond filter, Object delegate) {
    double distance = normalizeDistance(filter.getDistance(), filter.getDistanceUnits());

    ExpressionValues filterValues = getExpressions(filter, delegate);
    String wkt = geometryToWkt(filterValues.literal);

    if (abs(distance) > ERROR_THRESHOLD) {
      return ((FilterDelegate<?>) delegate).beyond(filterValues.propertyName, wkt, distance);
    } else {
      return ((FilterDelegate<?>) delegate).nearestNeighbor(filterValues.propertyName, wkt);
    }
  }

  public Object visit(Contains filter, Object delegate) {
    ExpressionValues filterValues = getExpressions(filter, delegate);
    String wkt = geometryToWkt(filterValues.literal);

    return ((FilterDelegate<?>) delegate).contains(filterValues.propertyName, wkt);
  }

  public Object visit(DWithin filter, Object delegate) {
    double distance = normalizeDistance(filter.getDistance(), filter.getDistanceUnits());

    ExpressionValues filterValues = getExpressions(filter, delegate);
    String wkt = geometryToWkt(filterValues.literal);

    return ((FilterDelegate<?>) delegate).dwithin(filterValues.propertyName, wkt, distance);
  }

  public Object visit(Intersects filter, Object delegate) {
    ExpressionValues filterValues = getExpressions(filter, delegate);
    String wkt = geometryToWkt(filterValues.literal);

    return ((FilterDelegate<?>) delegate).intersects(filterValues.propertyName, wkt);
  }

  public Object visit(Within filter, Object delegate) {
    ExpressionValues filterValues = getExpressions(filter, delegate);
    String wkt = geometryToWkt(filterValues.literal);

    return ((FilterDelegate<?>) delegate).within(filterValues.propertyName, wkt);
  }

  public Object visit(Crosses filter, Object delegate) {
    ExpressionValues filterValues = getExpressions(filter, delegate);
    String wkt = geometryToWkt(filterValues.literal);

    return ((FilterDelegate<?>) delegate).crosses(filterValues.propertyName, wkt);
  }

  public Object visit(Disjoint filter, Object delegate) {
    ExpressionValues filterValues = getExpressions(filter, delegate);
    String wkt = geometryToWkt(filterValues.literal);

    return ((FilterDelegate<?>) delegate).disjoint(filterValues.propertyName, wkt);
  }

  public Object visit(Overlaps filter, Object delegate) {
    ExpressionValues filterValues = getExpressions(filter, delegate);
    String wkt = geometryToWkt(filterValues.literal);

    return ((FilterDelegate<?>) delegate).overlaps(filterValues.propertyName, wkt);
  }

  public Object visit(Touches filter, Object delegate) {
    ExpressionValues filterValues = getExpressions(filter, delegate);
    String wkt = geometryToWkt(filterValues.literal);

    return ((FilterDelegate<?>) delegate).touches(filterValues.propertyName, wkt);
  }

  private double normalizeDistance(double distance, String distanceUnits) {
    if (UomOgcMapping.FOOT.name().equals(distanceUnits) || CQL_FEET.equals(distanceUnits)) {
      return new Distance(distance, LinearUnit.FOOT_U_S).getAs(LinearUnit.METER);
    } else if (UomOgcMapping.METRE.name().equals(distanceUnits)
        || CQL_METERS.equals(distanceUnits)) {
      return distance;
    } else if (CQL_STATUTE_MILES.equals(distanceUnits)) {
      return new Distance(distance, LinearUnit.MILE).getAs(LinearUnit.METER);
    } else if (CQL_NAUTICAL_MILES.equals(distanceUnits)) {
      return new Distance(distance, LinearUnit.NAUTICAL_MILE).getAs(LinearUnit.METER);
    } else if (CQL_KILOMETERS.equals(distanceUnits)) {
      return new Distance(distance, LinearUnit.KILOMETER).getAs(LinearUnit.METER);
    } else {
      throw new UnsupportedOperationException("Unknown units used in spatial filter");
    }
  }

  private String geometryToWkt(Object literal) {
    String wkt;
    // TODO should support OpenGIS Geometry interface and reconstruct the
    // WKT from the getBoundary method
    if (literal instanceof GeometryImpl) {
      GeometryImpl surface = (GeometryImpl) literal;
      org.locationtech.jts.geom.Geometry jtsGeometry = surface.getJTSGeometry();
      wkt = jtsGeometry.toText();
    } else if (literal instanceof org.locationtech.jts.geom.Geometry) {
      org.locationtech.jts.geom.Geometry jtsGeometry = (org.locationtech.jts.geom.Geometry) literal;
      wkt = jtsGeometry.toText();
    } else {
      throw new UnsupportedOperationException(
          "Unsupported implementation of Geometry for spatial filters.");
    }
    return wkt;
  }

  public Object visit(Equals filter, Object delegate) {
    throw new UnsupportedOperationException(
        "Spatial Equals filter not supported by Filter Adapter.");
  }

  public Object visit(After after, Object delegate) {
    ExpressionValues filterValues = getExpressions(after, delegate);
    String propertyName = filterValues.propertyName;
    Object literal = filterValues.literal;

    if (literal instanceof Date) {
      return ((FilterDelegate<?>) delegate).after(propertyName, (Date) literal);
    } else if (literal instanceof Instant) {
      return ((FilterDelegate<?>) delegate)
          .after(propertyName, ((Instant) literal).getPosition().getDate());
    } else if (literal instanceof Period) {
      return ((FilterDelegate<?>) delegate)
          .after(propertyName, ((Period) literal).getEnding().getPosition().getDate());
    } else {
      throw new UnsupportedOperationException(
          "Unsupported implementation of date/time for After filter.");
    }
  }

  public Object visit(Before before, Object delegate) {
    ExpressionValues filterValues = getExpressions(before, delegate);
    String propertyName = filterValues.propertyName;
    Object literal = filterValues.literal;

    if (literal instanceof Date) {
      return ((FilterDelegate<?>) delegate).before(propertyName, (Date) literal);
    } else if (literal instanceof Instant) {
      return ((FilterDelegate<?>) delegate)
          .before(propertyName, ((Instant) literal).getPosition().getDate());
    } else if (literal instanceof Period) {
      return ((FilterDelegate<?>) delegate)
          .before(propertyName, ((Period) literal).getBeginning().getPosition().getDate());
    } else {
      throw new UnsupportedOperationException(
          "Unsupported implementation of date/time for Before filter.");
    }
  }

  public Object visit(During during, Object delegate) {
    ExpressionValues filterValues = getExpressions(during, delegate);

    // Absolute
    if (filterValues.literal instanceof Period) {
      Period period = (Period) filterValues.literal;
      Date start = period.getBeginning().getPosition().getDate();
      Date end = period.getEnding().getPosition().getDate();

      return ((FilterDelegate<?>) delegate).during(filterValues.propertyName, start, end);
      // Relative
    } else if (filterValues.literal instanceof DefaultPeriodDuration) {
      // TODO should support PeriodDuration and reconstruct the duration
      // instead of using an implementation to get the milliseconds
      DefaultPeriodDuration duration = (DefaultPeriodDuration) filterValues.literal;

      return ((FilterDelegate<?>) delegate)
          .relative(filterValues.propertyName, duration.getTimeInMillis());
    } else {
      throw new UnsupportedOperationException(
          "Unsupported implementation of Period or PeriodDuration for During filter.");
    }
  }

  public Object visit(AnyInteracts anyInteracts, Object delegate) {
    throw new UnsupportedOperationException(AnyInteracts.NAME + FILTER_NOT_SUPPORTED);
  }

  public Object visit(Begins begins, Object delegate) {

    ExpressionValues filterValues = getExpressions(begins, delegate);
    if (filterValues.literal instanceof Period) {
      Period period = (Period) filterValues.literal;
      Date start = period.getBeginning().getPosition().getDate();
      Date end = period.getEnding().getPosition().getDate();

      return ((FilterDelegate<?>) delegate).begins(filterValues.propertyName, start, end);
    } else {
      throw new UnsupportedOperationException(
          Begins.NAME + "filter not supported by Filter Adapter.");
    }
  }

  public Object visit(BegunBy begunBy, Object delegate) {
    throw new UnsupportedOperationException(BegunBy.NAME + FILTER_NOT_SUPPORTED);
  }

  public Object visit(EndedBy endedBy, Object delegate) {
    throw new UnsupportedOperationException(EndedBy.NAME + FILTER_NOT_SUPPORTED);
  }

  public Object visit(Ends ends, Object delegate) {
    throw new UnsupportedOperationException(Ends.NAME + FILTER_NOT_SUPPORTED);
  }

  public Object visit(Meets meets, Object delegate) {
    throw new UnsupportedOperationException(Meets.NAME + FILTER_NOT_SUPPORTED);
  }

  public Object visit(MetBy metBy, Object delegate) {
    throw new UnsupportedOperationException(MetBy.NAME + FILTER_NOT_SUPPORTED);
  }

  public Object visit(OverlappedBy overlappedBy, Object delegate) {
    throw new UnsupportedOperationException(OverlappedBy.NAME + FILTER_NOT_SUPPORTED);
  }

  public Object visit(TContains contains, Object delegate) {
    throw new UnsupportedOperationException(TContains.NAME + FILTER_NOT_SUPPORTED);
  }

  public Object visit(TEquals equals, Object delegate) {
    throw new UnsupportedOperationException(TEquals.NAME + FILTER_NOT_SUPPORTED);
  }

  public Object visit(TOverlaps contains, Object delegate) {
    throw new UnsupportedOperationException(TOverlaps.NAME + FILTER_NOT_SUPPORTED);
  }

  private ExpressionValues getExpressions(BinarySpatialOperator filter, Object delegate) {
    return getExpressions(filter.getExpression1(), filter.getExpression2(), delegate);
  }

  private ExpressionValues getExpressions(BinaryComparisonOperator filter, Object delegate) {
    return getExpressions(filter.getExpression1(), filter.getExpression2(), delegate);
  }

  private ExpressionValues getExpressions(BinaryTemporalOperator filter, Object delegate) {
    return getExpressions(filter.getExpression1(), filter.getExpression2(), delegate);
  }

  private ExpressionValues getExpressions(
      Expression expression1, Expression expression2, Object delegate) {
    String propertyName;
    Object literal;
    String functionName;
    List functionArgs;

    if (expression1 instanceof Function && expression2 instanceof Literal) {
      Function function = (Function) expression1;
      Class<?> clazz = function.getFunctionName().getReturn().getType();
      functionName = function.getName();
      functionArgs = (List) function.accept(this, delegate);
      literal = expression2.accept(this, clazz);
      return new ExpressionValues(functionName, functionArgs, literal);
    } else if (expression2 instanceof Function && expression1 instanceof Literal) {
      Function function = (Function) expression2;
      Class<?> clazz = function.getFunctionName().getReturn().getType();
      functionName = function.getName();
      functionArgs = (List) function.accept(this, delegate);
      literal = expression1.accept(this, clazz);
      return new ExpressionValues(functionName, functionArgs, literal);
    } else if (expression1 instanceof PropertyName && expression2 instanceof Literal) {
      propertyName = (String) expression1.accept(this, delegate);
      literal = expression2.accept(this, delegate);
      return new ExpressionValues(propertyName, literal);
    } else if (expression1 instanceof Literal && expression2 instanceof PropertyName) {
      literal = expression1.accept(this, delegate);
      propertyName = (String) expression2.accept(this, delegate);
      return new ExpressionValues(propertyName, literal);
    } else {
      throw new UnsupportedOperationException(
          "Only support PropertyName and Literal expressions for binary filters.");
    }
  }

  private static class ExpressionValues {
    private String propertyName;

    private Object literal;

    public String functionName;

    public List<Object> functionArgs;

    public ExpressionValues(String propertyName, Object literal) {
      this.propertyName = propertyName;
      this.literal = literal;
    }

    public ExpressionValues(String functionName, List<Object> functionArgs, Object literal) {
      this.functionName = functionName;
      this.functionArgs = functionArgs;
      this.literal = literal;
    }
  }
}
