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
package org.codice.ddf.catalog.ui.security.accesscontrol;

import ddf.catalog.data.types.Core;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visits a filter and aggregates the tags. When the query corresponding to the filter is run, it is
 * guaranteed any result in the list will have at least one tag found in {@link #getTags()}. If this
 * cannot be guaranteed, the resultant tag set will be empty.
 *
 * <p>Note that {@link PropertyIsNotEqualTo} is not implemented, despite it potentially referencing
 * metacard tags. That's because aggregation can only effectively operate on what is known to be
 * true, not necessarily what is known to be false. This is because true predicates narrow down the
 * result space in deterministic ways without the need of the entire taxonomy or database schema to
 * compute the truthy alternative for a falsey filter, which would be prohibitively large.
 *
 * <p>For best results, run this visitor against a simplified filter. Running against filters
 * without optimized logic is only partially supported. Tags within the following logical operators
 * will be propagated up the tree correctly:
 *
 * <ul>
 *   <li>AND within an AND
 *   <li>OR within an OR
 * </ul>
 *
 * <p>Tags within the following logical operators <b>will be ignored</b>:
 *
 * <ul>
 *   <li>NOT within a NOT
 * </ul>
 *
 * <p>Given the vast majority of filters will not explicitly contain double negation, and the UI
 * already does some optimization work for us, skipping policy optimization on these kinds of
 * filters is acceptable.
 */
@SuppressWarnings("squid:S1192" /* Constants not actually helpful for log statements */)
public class TagAggregationVisitor extends TagBaseVisitor {
  private static final Logger LOGGER = LoggerFactory.getLogger(TagAggregationVisitor.class);
  private final Deque<TagAggregationRule> rules = new ArrayDeque<>();
  private final Set<String> tags = new HashSet<>();

  @Override
  protected Object preProcessNonTagPredicate(Filter filter, Object data) {
    reportNonTagEncountered();
    return data;
  }

  public Set<String> getTags() {
    return tags;
  }

  @Override
  public Object visit(Not filter, Object data) {
    rules.addFirst(TagAggregationRule.newEmptyRule());
    data = super.visit(filter, data);
    rules.removeFirst();

    if (CollectionUtils.isNotEmpty(rules)) {
      // Any NOT operation counts as "other criteria"
      rules.peekFirst().foundOtherCriteria();
    }

    return data;
  }

  @Override
  public Object visit(And filter, Object data) {
    rules.addFirst(TagAggregationRule.newEmptyRule());
    data = super.visit(filter, data);

    TagAggregationRule rule = rules.removeFirst();
    if (rules.isEmpty()) {
      tags.addAll(rule.getTags());
    } else if (rule.getTags().isEmpty()) {
      // An AND without tags should set the flag in case it's the child of an OR
      rules.peekFirst().foundOtherCriteria();
    } else {
      rules.peekFirst().foundTags(rule.getTags());
    }

    return data;
  }

  @Override
  public Object visit(Or filter, Object data) {
    rules.addFirst(TagAggregationRule.newEmptyRule());
    data = super.visit(filter, data);

    TagAggregationRule rule = rules.removeFirst();
    if (rule.isNotLimitedToTagCriteria()) {
      // An OR with "other criteria" cannot guarantee result set is a proper subset of policy
      return data;
    }

    if (rules.isEmpty()) {
      tags.addAll(rule.getTags());
    } else {
      rules.peekFirst().foundTags(rule.getTags());
    }

    return data;
  }

  @Override
  public Object visit(PropertyIsLike filter, Object data) {
    final Map.Entry<String, String> pair =
        filterAsKeyValuePair(filter.getExpression(), filter.getLiteral());
    if (pair != null) {
      LOGGER.trace("Found key-value predicate {}, looking for metacard tags", pair);
      saveTag(pair.getKey(), pair.getValue());
    } else {
      LOGGER.trace(
          "Filter {} could not be converted to key-value pair thus is not referring to metacard tags",
          filter);
      reportNonTagEncountered();
    }
    return super.visit(filter, data);
  }

  @Override
  public Object visit(PropertyIsEqualTo filter, Object data) {
    final Map.Entry<String, String> pair =
        filterAsKeyValuePair(filter.getExpression1(), filter.getExpression2());
    if (pair != null) {
      LOGGER.trace("Found key-value predicate {}, looking for metacard tags", pair);
      saveTag(pair.getKey(), pair.getValue());
    } else {
      LOGGER.trace(
          "Filter {} could not be converted to key-value pair thus is not referring to metacard tags",
          filter);
      reportNonTagEncountered();
    }
    return super.visit(filter, data);
  }

  private void saveTag(String property, String value) {
    if (!Core.METACARD_TAGS.equals(property)) {
      LOGGER.trace("Property \"{}\" and value '{}' do not concern metacard tags", property, value);
      reportNonTagEncountered();
      return;
    }

    if (StringUtils.isBlank(value)) {
      LOGGER.debug("Found blank string in query on metacard tags field \"{}\", ignoring", property);
      return;
    }

    if (rules.isEmpty()) {
      tags.add(value);
    } else {
      rules.peekFirst().foundTag(value);
    }
  }

  private void reportNonTagEncountered() {
    if (CollectionUtils.isNotEmpty(rules)) {
      LOGGER.trace("Search criteria found that is not a metacard tag, setting flag");
      rules.peekFirst().foundOtherCriteria();
      return;
    }
    LOGGER.trace(
        "Rule Deque was empty so the flag cannot be set, this query must not have any nesting logic");
  }

  @Nullable
  private static Map.Entry<String, String> filterAsKeyValuePair(
      Expression expression1, String literal) {
    final String property = expressionPropertyAsString(expression1);
    if (property == null || literal == null) {
      LOGGER.trace(
          "Given expression [{}] and literal [{}], either the property [{}] or "
              + "original literal was null",
          expression1,
          literal,
          property);
      return null;
    }
    return new HashMap.SimpleEntry<>(property, literal);
  }

  @Nullable
  private static Map.Entry<String, String> filterAsKeyValuePair(
      Expression expression1, Expression expression2) {
    final String property = expressionPropertyAsString(expression1);
    final String value = expressionLiteralAsString(expression2);
    if (property == null || value == null) {
      LOGGER.trace(
          "Given expression1 [{}] and expression2 [{}], either the derived property [{}] or "
              + "derived value [{}] was null",
          expression1,
          expression2,
          property,
          value);
      return null;
    }
    return new HashMap.SimpleEntry<>(property, value);
  }

  @Nullable
  private static String expressionPropertyAsString(Expression expression) {
    if (!(expression instanceof PropertyName)) {
      LOGGER.trace(
          "Expected an {} but got {}",
          PropertyName.class.getTypeName(),
          expression.getClass().getTypeName());
      return null;
    }

    final PropertyName name = (PropertyName) expression;
    return name.getPropertyName();
  }

  @Nullable
  private static String expressionLiteralAsString(Expression expression) {
    if (!(expression instanceof Literal)) {
      LOGGER.trace(
          "Expected an {} but got {}",
          Literal.class.getTypeName(),
          expression.getClass().getTypeName());
      return null;
    }

    final Literal literal = (Literal) expression;
    final Object value = literal.getValue();

    if (!(value instanceof String)) {
      LOGGER.trace(
          "Expected an {} but got {}", String.class.getTypeName(), value.getClass().getTypeName());
      return null;
    }

    return (String) value;
  }
}
