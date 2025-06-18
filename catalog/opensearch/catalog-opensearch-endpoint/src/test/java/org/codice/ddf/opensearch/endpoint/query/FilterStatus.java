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
package org.codice.ddf.opensearch.endpoint.query;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.geotools.api.filter.Filter;

public class FilterStatus {
  private final List<Filter> filters = new ArrayList<>();

  private int count = 0;

  private boolean isCaseSensitive = false;

  private String wildcard = null;

  public List<Filter> getFilters() {
    return filters;
  }

  public void addFilter(Filter filter) {
    filters.add(filter);
  }

  public String getWildcard() {
    return wildcard;
  }

  public void setWildcard(String wildCard) {
    this.wildcard = wildCard;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public boolean isCaseSensitive() {
    return isCaseSensitive;
  }

  public void setCaseSensitive(boolean isCaseSensitive) {
    this.isCaseSensitive = isCaseSensitive;
  }

  public void increment() {
    count++;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
