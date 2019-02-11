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
package ddf.catalog.filter.impl;

import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

/**
 * Simple implementation of filter that does not depend on GeoTools. Please use {@link
 * ddf.catalog.filter.FilterBuilder} instead to create filters.
 */
public class SortByImpl implements SortBy {

  private PropertyName propertyName;

  private SortOrder sortOrder;

  /**
   * Create SortBy filter.
   *
   * @param propertyName property name
   * @param sortOrder sort order
   */
  public SortByImpl(String propertyName, String sortOrder) {
    this(new PropertyNameImpl(propertyName), SortOrder.valueOf(sortOrder));
  }

  /**
   * Create SortBy filter.
   *
   * @param propertyName property name
   * @param sortOrder sort order
   */
  public SortByImpl(String propertyName, SortOrder sortOrder) {
    this(new PropertyNameImpl(propertyName), sortOrder);
  }

  /**
   * Create SortBy filter.
   *
   * @param propertyName property name
   * @param sortOrder sort order
   */
  public SortByImpl(PropertyName propertyName, SortOrder sortOrder) {
    this.propertyName = propertyName;
    this.sortOrder = sortOrder;
  }

  @Override
  public PropertyName getPropertyName() {
    return propertyName;
  }

  @Override
  public SortOrder getSortOrder() {
    return sortOrder;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("propertyName=");
    sb.append(propertyName);
    sb.append(",sortOrder=");
    sb.append(sortOrder);
    return sb.toString();
  }
}
