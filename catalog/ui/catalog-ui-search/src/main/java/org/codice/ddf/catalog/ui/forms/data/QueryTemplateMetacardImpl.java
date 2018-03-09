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

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.types.CoreAttributes;
import java.util.Collections;
import java.util.List;

/**
 * Metacard used for storing query templates in the catalog. Should not be used as a resource.
 * Identifiable by the presence of {@link FormAttributes.Query#TAG} in {@link Metacard#TAGS}.
 *
 * <p>Relevant attributes:
 *
 * <ul>
 *   <li>{@link Metacard#TITLE} - display name for a query template, in general not necessary, but
 *       for templates it should be present and it should be unique
 *   <li>{@link Metacard#DESCRIPTION} - additional information about a template, should be present
 *       but not necessarily unique
 *   <li>{@link FormAttributes.Query#FORMS_FILTER} - contains validated Filter XML 2.0 that
 *       represents the query structure to execute, with filter functions denoting information that
 *       is needed before execution can occur.
 * </ul>
 */
public class QueryTemplateMetacardImpl extends ShareableMetacardImpl {
  public QueryTemplateMetacardImpl(String title, String description) {
    super(new FormTypes.Query());
    setAttribute(CoreAttributes.TITLE, title);
    setAttribute(CoreAttributes.DESCRIPTION, description);
    setTags(Collections.singleton(FormAttributes.Query.TAG));
  }

  public QueryTemplateMetacardImpl(String title, String description, String id) {
    this(title, description);
    setId(id);
  }

  public QueryTemplateMetacardImpl(Metacard metacard) {
    super(metacard);
  }

  /**
   * Check if a given metacard is a query template metacard by checking the tags metacard attribute.
   *
   * @param metacard the metacard to check.
   * @return true if the provided metacard is a query template metacard, false otherwise.
   */
  public static boolean isQueryTemplateMetacard(Metacard metacard) {
    return metacard != null
        && metacard.getTags().stream().anyMatch(FormAttributes.Query.TAG::equals);
  }

  public String getFormsFilter() {
    List<String> values = getValues(FormAttributes.Query.FORMS_FILTER);
    if (!values.isEmpty()) {
      return values.get(0);
    }
    return null;
  }

  public QueryTemplateMetacardImpl setFormsFilter(String filterXml) {
    setAttribute(FormAttributes.Query.FORMS_FILTER, filterXml);
    return this;
  }
}
