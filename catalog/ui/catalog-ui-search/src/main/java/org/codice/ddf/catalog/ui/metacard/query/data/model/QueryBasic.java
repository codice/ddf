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
package org.codice.ddf.catalog.ui.metacard.query.data.model;

import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.DETAIL_LEVEL;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.FACETS;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_CQL;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_ENTERPRISE;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_FEDERATION;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_FILTER_TREE;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_POLLING;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_SORTS;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_SOURCES;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_TYPE;
import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.SCHEDULES;

import com.google.gson.annotations.SerializedName;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.codice.ddf.catalog.ui.metacard.query.data.metacard.QueryMetacardImpl;

public class QueryBasic {

  @SerializedName("id")
  private String metacardId;

  @SerializedName("title")
  private String title;

  @SerializedName("created")
  private Date created;

  @SerializedName("modified")
  private Date modified;

  @SerializedName("owner")
  private String owner;

  @SerializedName("description")
  private String description;

  @SerializedName("cql")
  private String cql;

  @SerializedName("filterTree")
  private String filterTree;

  @SerializedName("enterprise")
  private Boolean enterprise;

  @SerializedName("sources")
  private List<String> sources;

  @SerializedName("sorts")
  private List<Map<String, String>> sorts;

  @SerializedName("polling")
  private Integer polling;

  @SerializedName("federation")
  private String federation;

  @SerializedName("type")
  private String type;

  @SerializedName("detailLevel")
  private String detailLevel;

  @SerializedName("schedules")
  private List<String> schedules;

  @SerializedName("facets")
  private List<String> facets;

  public QueryBasic(Metacard metacard) {
    this.metacardId = getAttributeValue(metacard, Core.ID, String.class);
    this.title = getAttributeValue(metacard, Core.TITLE, String.class);
    this.owner = getAttributeValue(metacard, Core.METACARD_OWNER, String.class);
    this.description = getAttributeValue(metacard, Core.DESCRIPTION, String.class);
    this.created = getAttributeValue(metacard, Core.CREATED, Date.class);
    this.modified = getAttributeValue(metacard, Core.MODIFIED, Date.class);
    this.cql = getAttributeValue(metacard, QUERY_CQL, String.class);
    this.filterTree = getAttributeValue(metacard, QUERY_FILTER_TREE, String.class);
    this.enterprise = getAttributeValue(metacard, QUERY_ENTERPRISE, Boolean.class);
    this.sources = getAttributeValues(metacard, QUERY_SOURCES, String.class);

    Class<Map<String, String>> clazz = (Class<Map<String, String>>) (Class) Map.class;
    this.sorts = getAttributeValues(metacard, QUERY_SORTS, clazz);

    this.polling = getAttributeValue(metacard, QUERY_POLLING, Integer.class);
    this.federation = getAttributeValue(metacard, QUERY_FEDERATION, String.class);
    this.type = getAttributeValue(metacard, QUERY_TYPE, String.class);
    this.detailLevel = getAttributeValue(metacard, DETAIL_LEVEL, String.class);
    this.schedules = getAttributeValues(metacard, SCHEDULES, String.class);
    this.facets = getAttributeValues(metacard, FACETS, String.class);
  }

  public Metacard getMetacard() {
    Metacard metacard = new MetacardImpl();

    metacard.setAttribute(new AttributeImpl(Core.ID, this.metacardId));
    metacard.setAttribute(new AttributeImpl(Core.TITLE, this.title));
    metacard.setAttribute(new AttributeImpl(Core.METACARD_OWNER, this.owner));
    metacard.setAttribute(new AttributeImpl(Core.DESCRIPTION, this.description));
    metacard.setAttribute(new AttributeImpl(Core.CREATED, this.created));
    metacard.setAttribute(new AttributeImpl(Core.MODIFIED, this.modified));
    metacard.setAttribute(new AttributeImpl(QUERY_CQL, this.cql));
    metacard.setAttribute(new AttributeImpl(QUERY_FILTER_TREE, this.filterTree));
    metacard.setAttribute(new AttributeImpl(QUERY_ENTERPRISE, this.enterprise));
    metacard.setAttribute(new AttributeImpl(QUERY_SOURCES, (Serializable) this.sources));
    metacard.setAttribute(new AttributeImpl(QUERY_SORTS, (Serializable) this.sorts));
    metacard.setAttribute(new AttributeImpl(QUERY_POLLING, this.polling));
    metacard.setAttribute(new AttributeImpl(QUERY_FEDERATION, this.federation));
    metacard.setAttribute(new AttributeImpl(QUERY_TYPE, this.type));
    metacard.setAttribute(new AttributeImpl(DETAIL_LEVEL, this.detailLevel));
    metacard.setAttribute(new AttributeImpl(SCHEDULES, (Serializable) this.schedules));
    metacard.setAttribute(new AttributeImpl(FACETS, (Serializable) this.facets));

    return new QueryMetacardImpl(metacard);
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  private static <T> T getAttributeValue(Metacard metacard, String name, Class<T> type) {
    Attribute attr = metacard.getAttribute(name);

    if (attr == null) {
      return null;
    }

    Serializable value = attr.getValue();

    if (!type.isInstance(value)) {
      return null;
    }

    return type.cast(value);
  }

  private static <T> List<T> getAttributeValues(Metacard metacard, String name, Class<T> type) {
    Attribute attr = metacard.getAttribute(name);

    if (attr == null) {
      return Collections.emptyList();
    }

    return attr.getValues()
        .stream()
        .filter(type::isInstance)
        .map(type::cast)
        .collect(Collectors.toList());
  }
}
