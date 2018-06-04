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
package org.codice.ddf.catalog.ui.forms.model.pojo;

import static org.apache.commons.lang.Validate.notEmpty;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Security;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.boon.json.annotations.JsonProperty;

/**
 * Provides data model pojo that can be annotated and sent to Boon for JSON serialization.
 *
 * <p>{@link FieldFilter}, also known as "detail level", specifies a result template, which is a
 * list of attribute descriptor names that represent the fields that should be visible while
 * browsing metacards after a query is executed.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public class FieldFilter extends CommonTemplate {
  @SuppressWarnings("squid:S1068" /* Needed for serialization */)
  private final Set<String> descriptors;

  @JsonProperty("creator")
  private String creator;

  @JsonProperty("accessIndividuals")
  private List<Serializable> accessIndividuals;

  @JsonProperty("accessGroups")
  private List<Serializable> accessGroups;

  public FieldFilter(
      Metacard metacard,
      Set<String> descriptors,
      String creator,
      Map<String, List<Serializable>> securityAttributes) {
    super(metacard);
    notEmpty(descriptors);
    this.descriptors = descriptors;
    this.creator = creator;
    this.accessIndividuals = securityAttributes.get(Security.ACCESS_INDIVIDUALS);
    this.accessGroups = securityAttributes.get(Security.ACCESS_GROUPS);
  }

  @SuppressWarnings("unchecked")
  public FieldFilter(Map<String, Object> input) {
    super(input);
    List<String> des = (List<String>) input.get("descriptors");
    notEmpty(des);
    this.descriptors = new HashSet<>(des);
  }

  public Set<String> getDescriptors() {
    return descriptors;
  }
}
