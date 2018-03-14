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

import com.google.common.collect.Sets;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** A metacard that supports sharing. */
public class ShareableMetacardImpl extends MetacardImpl {
  public ShareableMetacardImpl(MetacardType type) {
    super(type);
  }

  public ShareableMetacardImpl(Metacard metacard) {
    super(metacard);
  }

  public ShareableMetacardImpl(Metacard metacard, MetacardType type) {
    super(metacard, type);
  }

  /**
   * Check if a given metacard is a shareable metacard by checking the tags metacard attribute.
   *
   * @param metacard the metacard to check.
   * @return true if the provided metacard is a shareable metacard, false otherwise.
   */
  public static boolean isShareableMetacard(Metacard metacard) {
    return metacard != null
        && metacard.getTags().stream().anyMatch(FormAttributes.Sharing.NAME::equals);
  }

  /** Wrap any metacard as a ShareableMetacardImpl. */
  public static ShareableMetacardImpl from(Metacard metacard) {
    return new ShareableMetacardImpl(metacard);
  }

  /**
   * Compute the symmetric difference between the sharing permissions of two shareable metacards.
   *
   * @param m - metacard to diff against
   */
  public Set<String> diffSharing(Metacard m) {
    if (isShareableMetacard(m)) {
      return Sets.symmetricDifference(getSharing(), from(m).getSharing());
    }
    return Collections.emptySet();
  }

  protected List<String> getValues(String attribute) {
    Attribute attr = getAttribute(attribute);
    if (attr != null) {
      return attr.getValues().stream().map(String::valueOf).collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  public String getOwner() {
    List<String> values = getValues(Core.METACARD_OWNER);
    if (!values.isEmpty()) {
      return values.get(0);
    }
    return null;
  }

  public void setOwner(String email) {
    setAttribute(Core.METACARD_OWNER, email);
  }

  public Set<String> getSharing() {
    return new HashSet<>(getValues(FormAttributes.Sharing.FORM_SHARING));
  }

  public void setSharing(Set<String> sharing) {
    setAttribute(FormAttributes.Sharing.FORM_SHARING, new ArrayList<>(sharing));
  }
}
