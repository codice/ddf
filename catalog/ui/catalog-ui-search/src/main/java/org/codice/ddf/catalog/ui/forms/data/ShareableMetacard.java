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

import static org.codice.ddf.catalog.ui.forms.data.ShareableAttributes.SHAREABLE_METADATA;
import static org.codice.ddf.catalog.ui.forms.data.ShareableAttributes.SHAREABLE_TAG;

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

/**
 * A metacard that supports sharing.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 *
 * <p>TODO DDF-3671 Revisit sharing functionality for metacards
 */
@SuppressWarnings("squid:S1135" /* Action to-do has a ticket number and will be addressed later */)
public class ShareableMetacard extends MetacardImpl {
  public ShareableMetacard(MetacardType type) {
    super(type);
  }

  public ShareableMetacard(Metacard metacard) {
    super(metacard);
  }

  public ShareableMetacard(Metacard metacard, MetacardType type) {
    super(metacard, type);
  }

  /**
   * Check if a given metacard is a shareable metacard by checking the tags metacard attribute.
   *
   * @param metacard the metacard to check.
   * @return true if the provided metacard is a shareable metacard, false otherwise.
   */
  public static boolean isShareableMetacard(Metacard metacard) {
    return metacard != null && metacard.getTags().stream().anyMatch(SHAREABLE_TAG::equals);
  }

  /** Wrap any metacard as a ShareableMetacardImpl. */
  public static ShareableMetacard from(Metacard metacard) {
    return new ShareableMetacard(metacard);
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
    return new HashSet<>(getValues(SHAREABLE_METADATA));
  }

  public void setSharing(Set<String> sharing) {
    setAttribute(SHAREABLE_METADATA, new ArrayList<>(sharing));
  }
}
