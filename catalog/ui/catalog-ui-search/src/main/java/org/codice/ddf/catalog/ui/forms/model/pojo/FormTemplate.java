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

import ddf.catalog.data.Metacard;
import java.io.Serializable;
import java.util.List;
import org.boon.json.annotations.JsonProperty;
import org.codice.ddf.catalog.ui.forms.model.FilterNode;

/**
 * Provides data model pojo that can be annotated and sent to Boon for JSON serialization.
 *
 * <p>{@link FormTemplate} specifies a query template, which is a special type of filter whose nodes
 * and expressions may be annotated with additional data. Contains a {@link FilterNode} which is the
 * root of the filter tree.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public class FormTemplate extends CommonTemplate {
  @JsonProperty("filterTemplate")
  private FilterNode root;

  @JsonProperty("accessIndividuals")
  private List<Serializable> accessIndividuals;

  @JsonProperty("accessGroups")
  private List<Serializable> accessGroups;

  @JsonProperty("creator")
  private String creator;

  public FormTemplate(
      Metacard metacard,
      FilterNode root,
      List<Serializable> accessIndividuals,
      List<Serializable> accessGroups,
      String creator) {
    super(metacard);
    this.root = root;
    this.accessIndividuals = accessIndividuals;
    this.accessGroups = accessGroups;
    this.creator = creator;
  }

  public FilterNode getRoot() {
    return root;
  }
}
