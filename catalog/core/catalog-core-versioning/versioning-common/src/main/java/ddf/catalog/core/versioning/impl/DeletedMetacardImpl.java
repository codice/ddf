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
package ddf.catalog.core.versioning.impl;

import com.google.common.collect.ImmutableSet;
import ddf.catalog.core.versioning.DeletedMetacard;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Experimental. Subject to change. <br>
 * Represents a currently soft deleted metacard.
 */
public class DeletedMetacardImpl extends MetacardImpl implements DeletedMetacard {
  private static final MetacardType METACARD_TYPE;

  private static final Set<AttributeDescriptor> DESCRIPTORS =
      new HashSet<>(MetacardImpl.BASIC_METACARD.getAttributeDescriptors());

  static {
    DESCRIPTORS.add(
        new AttributeDescriptorImpl(DELETED_BY, true, true, false, false, BasicTypes.STRING_TYPE));
    DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            DELETION_OF_ID, true, true, false, false, BasicTypes.STRING_TYPE));
    DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            LAST_VERSION_ID, true, true, false, false, BasicTypes.STRING_TYPE));
    DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            DELETED_METACARD_TAGS, true, true, false, true, BasicTypes.STRING_TYPE));
    METACARD_TYPE = new MetacardTypeImpl(PREFIX, DESCRIPTORS);
  }

  public DeletedMetacardImpl(
      String id,
      String deletionOfId,
      String deletedBy,
      String lastVersionId,
      Metacard metacardBeingDeleted) {
    super(
        metacardBeingDeleted,
        new MetacardTypeImpl(
            PREFIX,
            getDeletedMetacardType(),
            metacardBeingDeleted.getMetacardType().getAttributeDescriptors()));
    this.setDeletionOfId(deletionOfId);
    this.setDeletedBy(deletedBy);
    this.setDeletedMetacardTags(metacardBeingDeleted.getTags());
    this.setLastVersionId(lastVersionId);
    this.setTags(ImmutableSet.of(DELETED_TAG));
    this.setId(id);
  }

  public static boolean isNotDeleted(@Nullable Metacard metacard) {
    return !isDeleted(metacard);
  }

  public static boolean isDeleted(@Nullable Metacard metacard) {
    return metacard instanceof DeletedMetacard
        || getDeletedMetacardType()
            .getName()
            .equals(
                Optional.ofNullable(metacard)
                    .map(Metacard::getMetacardType)
                    .map(MetacardType::getName)
                    .orElse(null));
  }

  public static MetacardType getDeletedMetacardType() {
    return METACARD_TYPE;
  }

  public void setDeletionOfId(String deletionOfId) {
    setAttribute(DELETION_OF_ID, deletionOfId);
  }

  public String getDeletionOfId() {
    return requestString(DELETION_OF_ID);
  }

  public void setDeletedBy(String deletedBy) {
    setAttribute(DELETED_BY, deletedBy);
  }

  public String getDeletedBy() {
    return requestString(DELETED_BY);
  }

  public void setLastVersionId(String lastVersionId) {
    setAttribute(LAST_VERSION_ID, lastVersionId);
  }

  public String getLastVersionId() {
    return requestString(LAST_VERSION_ID);
  }

  public void setDeletedMetacardTags(Set<String> tags) {
    if (tags == null || tags.isEmpty()) {
      setAttribute(DELETED_METACARD_TAGS, null);
      return;
    }
    setAttribute(
        new AttributeImpl(
            DELETED_METACARD_TAGS, (List<Serializable>) new ArrayList<Serializable>(tags)));
  }

  public Set<String> getDeletedMetacardTags() {
    Attribute deletedTags = getAttribute(DELETED_METACARD_TAGS);
    if (deletedTags == null
        || deletedTags.getValues() == null
        || deletedTags.getValues().isEmpty()) {
      return Collections.emptySet();
    }
    return deletedTags.getValues().stream().map(String::valueOf).collect(Collectors.toSet());
  }
}
