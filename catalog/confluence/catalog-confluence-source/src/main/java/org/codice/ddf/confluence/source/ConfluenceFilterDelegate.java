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
package org.codice.ddf.confluence.source;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Topic;
import ddf.catalog.filter.impl.SimpleFilterDelegate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.confluence.common.Confluence;

public class ConfluenceFilterDelegate extends SimpleFilterDelegate<String> {

  public static final Map<String, ConfluenceQueryParameter> QUERY_PARAMETERS;

  private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";

  private static final String EMPTY_GROUP_PATTERN = "\\(\\s*\\)";

  private boolean wildcardQuery = false;

  private boolean unsupportedQuery = false;

  private int parameterCount = 0;

  private Set<String> queryTags = new HashSet<>();

  static {
    Map<String, ConfluenceQueryParameter> params = new HashMap<>();
    params.put(Metacard.ID, new ConfluenceQueryParameter("id", false, true, false, false, true));
    params.put(
        Metacard.CONTENT_TYPE,
        new ConfluenceQueryParameter("type", false, true, false, false, true));
    params.put(
        Metacard.TITLE, new ConfluenceQueryParameter("title", true, true, false, true, false));
    params.put(
        Metacard.ANY_TEXT, new ConfluenceQueryParameter("text", true, false, false, true, false));
    params.put(
        Metacard.CREATED, new ConfluenceQueryParameter("created", false, true, true, false, false));
    params.put(
        Metacard.MODIFIED,
        new ConfluenceQueryParameter("lastmodified", false, true, true, false, false));
    params.put(
        Topic.CATEGORY, new ConfluenceQueryParameter("type", false, true, false, false, true));
    params.put(
        Contact.CREATOR_NAME,
        new ConfluenceQueryParameter("creator", false, true, false, true, true));
    params.put(
        Contact.CONTRIBUTOR_NAME,
        new ConfluenceQueryParameter("contributor", false, true, false, true, true));
    params.put(
        Topic.KEYWORD, new ConfluenceQueryParameter("label", false, true, false, false, true));
    params.put(
        Core.METACARD_CREATED,
        new ConfluenceQueryParameter("created", false, true, true, false, false));
    params.put(
        Core.METACARD_MODIFIED,
        new ConfluenceQueryParameter("lastmodified", false, true, true, false, false));
    QUERY_PARAMETERS = Collections.unmodifiableMap(params);
  }

  @Override
  public <S> String defaultOperation(
      Object property, S literal, Class<S> literalClass, Enum operation) {
    unsupportedQuery = true;
    return null;
  }

  @Override
  public String and(List<String> operands) {
    return operands.stream()
        .filter(StringUtils::isNotEmpty)
        .collect(Collectors.joining(" AND ", "( ", " )"))
        .replaceAll(EMPTY_GROUP_PATTERN, "");
  }

  @Override
  public String or(List<String> operands) {
    return operands.stream()
        .filter(StringUtils::isNotEmpty)
        .collect(Collectors.joining(" OR ", "( ", " )"))
        .replaceAll(EMPTY_GROUP_PATTERN, "");
  }

  @Override
  public String not(String operand) {
    return operand != null ? "NOT " + operand : null;
  }

  @Override
  public String propertyIsEqualTo(String propertyName, String literal, boolean isCaseSensitive) {
    return getConfluenceParameter(
        propertyName, literal, param -> param.getEqualExpression(literal));
  }

  @Override
  public String propertyIsLike(String propertyName, String literal, boolean isCaseSensitive) {
    return getConfluenceParameter(
        propertyName,
        literal,
        param ->
            Arrays.stream(literal.split(" "))
                .map(param::getLikeExpression)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" OR ", "( ", " )"))
                .replaceAll(EMPTY_GROUP_PATTERN, ""));
  }

  @Override
  public String propertyIsGreaterThan(String propertyName, Date date) {
    return getConfluenceParameter(
        propertyName,
        null,
        param -> param.getGreaterThanExpression(new SimpleDateFormat(DATE_FORMAT).format(date)));
  }

  @Override
  public String after(String propertyName, Date date) {
    return getConfluenceParameter(
        propertyName,
        null,
        param -> param.getGreaterThanExpression(new SimpleDateFormat(DATE_FORMAT).format(date)));
  }

  @Override
  public String before(String propertyName, Date date) {
    return getConfluenceParameter(
        propertyName,
        null,
        param -> param.getLessThanExpression(new SimpleDateFormat(DATE_FORMAT).format(date)));
  }

  @Override
  public String during(String propertyName, Date startDate, Date endDate) {
    return getConfluenceParameter(
        propertyName,
        null,
        param ->
            param.getGreaterThanExpression(new SimpleDateFormat(DATE_FORMAT).format(startDate))
                + " AND "
                + param.getLessThanExpression(new SimpleDateFormat(DATE_FORMAT).format(endDate)));
  }

  private String getConfluenceParameter(
      String propertyName, String literal, Function<ConfluenceQueryParameter, String> function) {
    ConfluenceQueryParameter param = QUERY_PARAMETERS.get(propertyName);
    propertyCheck(propertyName, literal);
    if (param == null) {
      return null;
    }
    return function.apply(param);
  }

  private void propertyCheck(String name, String literal) {
    parameterCount++;
    if (Metacard.TAGS.equals(name)) {
      queryTags.add(literal);
    }
    if (Metacard.ANY_TEXT.equals(name) && ConfluenceQueryParameter.WILD_CARD.equals(literal)) {
      wildcardQuery = true;
    }
  }

  public boolean isConfluenceQuery() {
    return !unsupportedQuery
        && (queryTags.isEmpty()
            || queryTags.contains(Metacard.DEFAULT_TAG)
            || queryTags.contains(Confluence.CONFLUENCE_TAG));
  }

  public boolean isWildCardQuery() {
    return wildcardQuery && parameterCount <= 1;
  }
}
