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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Metacard used for storing result templates in the catalog. Should not be used as a resource.
 * Identifiable by the presence of {@link FormAttributes.Result#TAG} in {@link Metacard#TAGS}.
 *
 * <p>Relevant attributes:
 *
 * <ul>
 *   <li>{@link Metacard#TITLE} - display name for a result template, in general not necessary, but
 *       for templates it should be present and it should be unique
 *   <li>{@link Metacard#DESCRIPTION} - additional information about a template, should be present
 *       but not necessarily unique
 *   <li>{@link FormAttributes.Result#DESCRIPTORS} - contains a list of attribute descriptor names
 *       that denote the fields that a user is interested in.
 * </ul>
 */
public class ResultTemplateMetacardImpl extends ShareableMetacardImpl {
  public ResultTemplateMetacardImpl(String title, String description) {
    super(new FormTypes.Result());
    setAttribute(CoreAttributes.TITLE, title);
    setAttribute(CoreAttributes.DESCRIPTION, description);
    setTags(Collections.singleton(FormAttributes.Result.TAG));
  }

  public ResultTemplateMetacardImpl(String title, String description, String id) {
    this(title, description);
    setId(id);
  }

  public ResultTemplateMetacardImpl(Metacard metacard) {
    super(metacard);
  }

  /**
   * Check if a given metacard is a result template metacard by checking the tags metacard
   * attribute.
   *
   * @param metacard the metacard to check.
   * @return true if the provided metacard is a result template metacard, false otherwise.
   */
  public static boolean isResultTemplateMetacard(Metacard metacard) {
    return metacard != null
        && metacard.getTags().stream().anyMatch(FormAttributes.Result.TAG::equals);
  }

  public Set<String> getResultDescriptors() {
    return new HashSet<>(getValues(FormAttributes.Result.DETAIL_LEVEL));
  }

  public ResultTemplateMetacardImpl setResultDescriptors(Set<String> resultDescriptors) {
    setAttribute(FormAttributes.Result.DETAIL_LEVEL, new ArrayList<>(resultDescriptors));
    return this;
  }
}
