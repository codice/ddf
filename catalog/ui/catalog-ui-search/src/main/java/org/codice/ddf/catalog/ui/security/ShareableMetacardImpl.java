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
package org.codice.ddf.catalog.ui.security;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.data.impl.types.SecurityAttributes;
import ddf.catalog.data.types.Core;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.codice.ddf.catalog.ui.forms.data.AttributeGroupMetacard;
import org.codice.ddf.catalog.ui.forms.data.QueryTemplateMetacard;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;

/**
 * Provides a set of operations that can be performed on {@link Metacard}s that support sharing. Use
 * {@link #canShare(Metacard)} to determine if a {@link Metacard} supports sharing, and {@link
 * #createOrThrow(Metacard)} to obtain and validate an instance of this class.
 */
public class ShareableMetacardImpl extends MetacardImpl {

  protected ShareableMetacardImpl(MetacardType type) {
    super(type);
  }

  protected ShareableMetacardImpl(Metacard metacard) {
    super(metacard);
  }

  @VisibleForTesting
  ShareableMetacardImpl() {
    super(
        new MetacardTypeImpl(
            "metacard.sharing", Arrays.asList(new CoreAttributes(), new SecurityAttributes())));
  }

  @VisibleForTesting
  ShareableMetacardImpl(String id) {
    this();
    setId(id);
  }

  /**
   * Check if a given metacard is a shareable metacard by checking the presence of security
   * attributes.
   *
   * @param metacard the metacard to check.
   * @return true if the provided metacard is a shareable metacard, false otherwise.
   */
  public static boolean canShare(Metacard metacard) {
    return metacard != null
        //        && metacard.getTags().stream().noneMatch("resource"::equals)
        && (WorkspaceMetacardImpl.isWorkspaceMetacard(metacard)
            || QueryTemplateMetacard.isQueryTemplateMetacard(metacard)
            || AttributeGroupMetacard.isAttributeGroupMetacard(metacard))
        && metacard
            .getMetacardType()
            .getAttributeDescriptors()
            .stream()
            .map(AttributeDescriptor::getName)
            .anyMatch(attr -> Objects.equals(SecurityAttributes.ACCESS_GROUPS, attr))
        && metacard
            .getMetacardType()
            .getAttributeDescriptors()
            .stream()
            .map(AttributeDescriptor::getName)
            .anyMatch(attr -> Objects.equals(SecurityAttributes.ACCESS_INDIVIDUALS, attr));
  }

  /**
   * Determine if a {@link Metacard} can support sharing. If so, decorate it as a {@link
   * ShareableMetacardImpl}.
   *
   * @param metacard the metacard to decorate.
   * @return a {@link ShareableMetacardImpl} that exposes sharing operations on the decorated
   *     metacard.
   * @throws IllegalArgumentException if the provided {@link Metacard} does not support sharing.
   */
  public static ShareableMetacardImpl createOrThrow(Metacard metacard) {
    if (!canShare(metacard)) {
      throw new IllegalArgumentException("Metacard does not support sharing");
    }
    return new ShareableMetacardImpl(metacard);
  }

  public static Optional<ShareableMetacardImpl> create(Metacard metacard) {
    if (!canShare(metacard)) {
      return Optional.empty();
    }
    return Optional.of(new ShareableMetacardImpl(metacard));
  }

  @VisibleForTesting
  static ShareableMetacardImpl create(Map<String, Serializable> attributes) {
    ShareableMetacardImpl shareableMetaCard = new ShareableMetacardImpl();
    attributes
        .entrySet()
        .stream()
        .forEach(entry -> shareableMetaCard.setAttribute(entry.getKey(), entry.getValue()));

    return shareableMetaCard;
  }

  /**
   * Get a copy of a shareable metacard.
   *
   * @param metacard the metacard to copy.
   * @return a {@link ShareableMetacardImpl} with all the attributes of the original.
   */
  @VisibleForTesting
  static ShareableMetacardImpl clone(Metacard metacard) {
    ShareableMetacardImpl shareableMetacard = new ShareableMetacardImpl();
    metacard
        .getMetacardType()
        .getAttributeDescriptors()
        .stream()
        .forEach(
            descriptor ->
                shareableMetacard.setAttribute(metacard.getAttribute(descriptor.getName())));

    return shareableMetacard;
  }

  /**
   * Compute the symmetric difference between the access individuals
   *
   * @param metacard - metacard to diff against
   */
  public Set<String> diffSharingAccessIndividuals(Metacard metacard) {
    return Sets.symmetricDifference(
        getAccessIndividuals(), createOrThrow(metacard).getAccessIndividuals());
  }

  /**
   * Compute the symmetric difference between the access groups
   *
   * @param metacard - metacard to diff against
   */
  public Set<String> diffSharingAccessGroups(Metacard metacard) {
    return Sets.symmetricDifference(getAccessGroups(), createOrThrow(metacard).getAccessGroups());
  }

  protected List<String> getValuesOrEmpty(String attributeName) {
    Attribute attribute = getAttribute(attributeName);
    if (attribute != null) {
      return attribute.getValues().stream().map(String::valueOf).collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  public String getOwner() {
    List<String> values = getValuesOrEmpty(Core.METACARD_OWNER);
    if (!values.isEmpty()) {
      return values.get(0);
    }
    return null;
  }

  public ShareableMetacardImpl setOwner(String email) {
    setAttribute(Core.METACARD_OWNER, email);
    return this;
  }

  public Set<String> getAccessIndividuals() {
    return new HashSet<>(getValuesOrEmpty(SecurityAttributes.ACCESS_INDIVIDUALS));
  }

  public Set<String> getAccessGroups() {
    return new HashSet<>(getValuesOrEmpty(SecurityAttributes.ACCESS_GROUPS));
  }

  public ShareableMetacardImpl setAccessIndividuals(Set<String> accessIndividuals) {
    setAttribute(SecurityAttributes.ACCESS_INDIVIDUALS, new ArrayList<>(accessIndividuals));
    return this;
  }

  public ShareableMetacardImpl setAccessGroups(Set<String> accessGroups) {
    setAttribute(SecurityAttributes.ACCESS_GROUPS, new ArrayList<>(accessGroups));
    return this;
  }
}
