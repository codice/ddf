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
package ddf.catalog.operation.impl;

import ddf.catalog.operation.FacetValueCount;

/** A simple tuple object pairing text faceting value results with their respective counts. */
public class FacetValueCountImpl implements FacetValueCount {
  private String value;
  private long count;

  /**
   * Creates a pairing from the provided value and count
   *
   * @param value The faceted attribute value
   * @param count The number of occurrences of value
   */
  public FacetValueCountImpl(String value, long count) {
    this.value = value;
    this.count = count;
  }

  @Override
  public long getCount() {
    return count;
  }

  @Override
  public String getValue() {
    return value;
  }
}
