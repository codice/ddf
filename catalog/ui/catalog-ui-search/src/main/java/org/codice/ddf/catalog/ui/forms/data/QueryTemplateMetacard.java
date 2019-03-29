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
package org.codice.ddf.catalog.ui.forms.data;

import static org.codice.ddf.catalog.ui.forms.data.QueryTemplateType.QUERY_TEMPLATE_FILTER;
import static org.codice.ddf.catalog.ui.forms.data.QueryTemplateType.QUERY_TEMPLATE_TAG;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.DETAIL_LEVEL;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_FEDERATION;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_SORTS;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_SOURCES;
import static org.codice.ddf.catalog.ui.util.AccessUtil.safeGet;
import static org.codice.ddf.catalog.ui.util.AccessUtil.safeGetList;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codice.ddf.catalog.ui.security.AccessControlUtil;

/**
 * Metacard used for storing query templates in the catalog. Should not be used as a resource.
 * Identifiable by the presence of {@link QueryTemplateType#QUERY_TEMPLATE_TAG} in {@link
 * Metacard#TAGS}.
 *
 * <p>Relevant attributes:
 *
 * <ul>
 *   <li>{@link Core#TITLE} - display name for a query template, in general not necessary, but for
 *       templates it should be present and it should be unique
 *   <li>{@link Core#DESCRIPTION} - additional information about a template, should be present but
 *       not necessarily unique
 *   <li>{@link QueryTemplateType#QUERY_TEMPLATE_FILTER} - contains validated Filter XML 2.0 that
 *       represents the query structure to execute, with filter functions denoting information that
 *       is needed before execution can occur.
 * </ul>
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public class QueryTemplateMetacard extends MetacardImpl {
  public QueryTemplateMetacard(String title, String description) {
    super(new QueryTemplateType());
    setAttribute(Core.TITLE, title);
    setAttribute(Core.DESCRIPTION, description);
    setTags(Collections.singleton(QUERY_TEMPLATE_TAG));
  }

  public QueryTemplateMetacard(String title, String description, String id) {
    this(title, description);
    setId(id);
  }

  public QueryTemplateMetacard(Metacard metacard) {
    super(metacard);
  }

  /**
   * Check if a given metacard is a query template metacard by checking the tags metacard attribute.
   *
   * @param metacard the metacard to check.
   * @return true if the provided metacard is a query template metacard, false otherwise.
   */
  public static boolean isQueryTemplateMetacard(Metacard metacard) {
    return metacard != null && metacard.getTags().contains(QUERY_TEMPLATE_TAG);
  }

  public String getFormsFilter() {
    List<String> values = AccessControlUtil.getValuesOrEmpty(this, QUERY_TEMPLATE_FILTER);
    if (!values.isEmpty()) {
      return values.get(0);
    }
    return null;
  }

  public void setFormsFilter(String filterXml) {
    setAttribute(QUERY_TEMPLATE_FILTER, filterXml);
  }

  public Map<String, Object> getQuerySettings() {
    Map<String, Object> querySettings = new HashMap<>();
    querySettings.put(QUERY_SORTS, safeGetList(this, QUERY_SORTS, String.class));
    querySettings.put("src", safeGetList(this, QUERY_SOURCES, String.class));
    querySettings.put(QUERY_FEDERATION, safeGet(this, QUERY_FEDERATION, String.class));
    querySettings.put(DETAIL_LEVEL, safeGet(this, DETAIL_LEVEL, String.class));
    return querySettings;
  }

  public void setQuerySettings(Map<String, Object> json) {
    List<Serializable> sorts = safeGetList(json, QUERY_SORTS, Serializable.class);
    if (sorts != null) {
      setAttribute(new AttributeImpl(QUERY_SORTS, sorts));
    }
    List<Serializable> sources = safeGetList(json, "src", Serializable.class);
    if (sources != null) {
      setAttribute(new AttributeImpl(QUERY_SOURCES, sources));
    }
    String federation = safeGet(json, QUERY_FEDERATION, String.class);
    if (federation != null) {
      setAttribute(QUERY_FEDERATION, federation);
    }
    String detailLevel = safeGet(json, DETAIL_LEVEL, String.class);
    if (detailLevel != null) {
      setAttribute(DETAIL_LEVEL, detailLevel);
    }
  }
}
