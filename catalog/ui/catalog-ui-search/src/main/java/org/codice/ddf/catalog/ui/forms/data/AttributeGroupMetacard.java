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

import static org.codice.ddf.catalog.ui.forms.data.AttributeGroupType.ATTRIBUTE_GROUP_LIST;
import static org.codice.ddf.catalog.ui.forms.data.AttributeGroupType.ATTRIBUTE_GROUP_TAG;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.codice.ddf.catalog.ui.security.accesscontrol.AccessControlUtil;

/**
 * Metacard used for storing sharing attribute groups in the catalog. Should not be used as a
 * resource. Identifiable by the presence of {@link AttributeGroupType#ATTRIBUTE_GROUP_TAG} in
 * {@link Metacard#TAGS}.
 *
 * <p>Relevant attributes:
 *
 * <ul>
 *   <li>{@link Core#TITLE} - display name for an attribute group, in general not necessary, but for
 *       groups it should be present and it should be unique.
 *   <li>{@link Core#DESCRIPTION} - additional information about a group, should be present but not
 *       necessarily unique.
 *   <li>{@link AttributeGroupType#ATTRIBUTE_GROUP_DESCRIPTORS} - contains a list of attribute
 *       descriptor names that denote the fields that are part of the group.
 * </ul>
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public class AttributeGroupMetacard extends MetacardImpl {
  public AttributeGroupMetacard(String title, String description) {
    super(new AttributeGroupType());
    setAttribute(Core.TITLE, title);
    setAttribute(Core.DESCRIPTION, description);
    setTags(Collections.singleton(ATTRIBUTE_GROUP_TAG));
  }

  public AttributeGroupMetacard(String title, String description, String id) {
    this(title, description);
    setId(id);
  }

  public AttributeGroupMetacard(Metacard metacard) {
    super(metacard);
  }

  /**
   * Check if a given metacard is an attribute group metacard by checking the tags metacard
   * attribute.
   *
   * @param metacard the metacard to check.
   * @return true if the provided metacard is a result template metacard, false otherwise.
   */
  public static boolean isAttributeGroupMetacard(Metacard metacard) {
    return metacard != null && metacard.getTags().contains(ATTRIBUTE_GROUP_TAG);
  }

  public Set<String> getGroupDescriptors() {
    return new HashSet<>(AccessControlUtil.getValuesOrEmpty(this, ATTRIBUTE_GROUP_LIST));
  }

  public void setGroupDescriptors(Set<String> resultDescriptors) {
    setAttribute(ATTRIBUTE_GROUP_LIST, new ArrayList<>(resultDescriptors));
  }
}
