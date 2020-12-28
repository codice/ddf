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

import com.google.gson.annotations.SerializedName;
import ddf.catalog.data.Metacard;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.codice.ddf.catalog.ui.forms.api.FilterNode;

/**
 * Provides data model pojo that can be annotated for JSON serialization.
 *
 * <p>{@link FormTemplate} specifies a query template, which is a special type of filter whose nodes
 * and expressions may be annotated with additional data. Contains a {@link FilterNode} which is the
 * root of the filter tree.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public class FormTemplate<T> extends CommonTemplate {
  @SerializedName("filterTemplate")
  private T root;

  private String creator;

  private Map<String, Object> querySettings;

  public FormTemplate(
      Metacard metacard,
      T root,
      Map<String, List<Serializable>> securityAttributes,
      String creator,
      Map<String, Object> querySettings) {
    super(metacard, securityAttributes);
    this.root = root;
    this.creator = creator;
    this.querySettings = querySettings;
  }

  public T getRoot() {
    return root;
  }
}
