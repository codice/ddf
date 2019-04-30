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
package org.codice.ddf.catalog.ui.security.accesscontrol;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * While visiting a {@link org.opengis.filter.Filter} it is important to validate that the query
 * does not target data that the access control policy does not apply to. A {@link
 * TagAggregationRule} may be used to keep track of the tags it has been given and a flag indicating
 * if some filter predicate was encountered that does not concern {@link
 * ddf.catalog.data.types.Core#METACARD_TAGS}.
 *
 * <p>This is useful when validating the {@link org.opengis.filter.Or} branches of filters to make
 * sure that tagging predicates apply to entire result set.
 */
public class TagAggregationRule {
  private final Set<String> tags;
  private boolean notLimitedToTagCriteria;

  public static TagAggregationRule newEmptyRule() {
    return new TagAggregationRule();
  }

  private TagAggregationRule() {
    tags = new HashSet<>();
    notLimitedToTagCriteria = false;
  }

  public Set<String> getTags() {
    return tags;
  }

  public boolean isNotLimitedToTagCriteria() {
    return notLimitedToTagCriteria;
  }

  public void foundTag(final String foundTag) {
    tags.add(foundTag);
  }

  public void foundTags(final Collection<String> foundTags) {
    tags.addAll(foundTags);
  }

  public void foundOtherCriteria() {
    notLimitedToTagCriteria = true;
  }
}
