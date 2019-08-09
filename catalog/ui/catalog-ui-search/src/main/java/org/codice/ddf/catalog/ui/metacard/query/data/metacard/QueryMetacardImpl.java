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
package org.codice.ddf.catalog.ui.metacard.query.data.metacard;

import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_CQL;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_ENTERPRISE;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_SOURCES;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_TAG;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class QueryMetacardImpl extends MetacardImpl {
  private static final QueryMetacardTypeImpl TYPE = new QueryMetacardTypeImpl();

  public QueryMetacardImpl() {
    super(TYPE);
    setTags(Collections.singleton(QUERY_TAG));
  }

  public QueryMetacardImpl(String title) {
    this();
    setTitle(title);
  }

  public QueryMetacardImpl(Metacard wrappedMetacard) {
    super(wrappedMetacard, TYPE);

    for (AttributeDescriptor attrDesc : TYPE.getAttributeDescriptors()) {
      Attribute attr = wrappedMetacard.getAttribute(attrDesc.getName());

      if (attr != null && attrDesc.isMultiValued()) {
        this.setAttribute(new AttributeImpl(attr.getName(), attr.getValues()));
      } else if (attr != null && !attrDesc.isMultiValued()) {
        this.setAttribute(new AttributeImpl(attr.getName(), attr.getValue()));
      }
    }
    // Set tags last in case tags from the wrapped metacard were copied above
    setTags(Collections.singleton(QUERY_TAG));
  }

  public static QueryMetacardImpl from(Metacard metacard) {
    return new QueryMetacardImpl(metacard);
  }

  public String getCql() {
    return (String) getAttribute(QUERY_CQL).getValue();
  }

  public void setCql(String cql) {
    setAttribute(QUERY_CQL, cql);
  }

  public void setEnterprise(Boolean b) {
    setAttribute(QUERY_ENTERPRISE, b);
  }

  /**
   * Get a list of the query sources.
   *
   * @return list of source (always non-null)
   */
  public List<String> getSources() {
    Attribute attribute = getAttribute(QUERY_SOURCES);
    if (attribute == null) {
      return Collections.emptyList();
    }
    return attribute
        .getValues()
        .stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .collect(Collectors.toList());
  }

  public void setSources(List<String> sources) {
    setAttribute(QUERY_SOURCES, new ArrayList<>(sources));
  }
}
