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
package ddf.catalog.util.impl;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Result;
import java.util.Comparator;
import org.geotools.api.filter.sort.SortOrder;

/** Comparator that ignores case for string attributes or falls back to the passed in comparator */
public class CaseInsensitiveIfStringComparator implements Comparator<Result> {

  private SortOrder sortOrder = SortOrder.DESCENDING;
  private String sortType = "";
  private final Comparator<Result> fallback;

  public CaseInsensitiveIfStringComparator(
      SortOrder sortOrder, String sortType, Comparator<Result> fallback) {
    if (sortOrder != null) {
      this.sortOrder = sortOrder;
    }
    this.sortType = sortType;
    this.fallback = fallback;
  }

  @Override
  public int compare(Result contentA, Result contentB) {
    Attribute a = contentA.getMetacard().getAttribute(sortType);
    Attribute b = contentB.getMetacard().getAttribute(sortType);

    if (!sortType.isEmpty()) {
      if (a == null || b == null) {
        // preserve null ordering
        return fallback.compare(contentA, contentB);
      } else if (a.getValue() instanceof String && b.getValue() instanceof String) {
        return (sortOrder == SortOrder.ASCENDING)
            ? String.CASE_INSENSITIVE_ORDER.compare(
                a.getValue().toString(), b.getValue().toString())
            : String.CASE_INSENSITIVE_ORDER
                .reversed()
                .compare(a.getValue().toString(), b.getValue().toString());
      }
    }

    return fallback.compare(contentA, contentB);
  }
}
