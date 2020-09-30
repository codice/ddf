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

import java.util.SortedSet;
import org.osgi.framework.ServiceReference;

/**
 * This is a basic implementation of ServiceSelectionStrategy which returns the first element from
 * the supplied java.util.SortedSet.
 *
 * @param <T> - The type of the service to be returned. {@link
 *     ddf.catalog.util.impl.ServiceSelectionStrategy}
 */
public class FirstElementServiceSelectionStrategy<T> implements ServiceSelectionStrategy<T> {

  /**
   * @param serviceSet - An unmodifiable, sorted list of all services bound to the ServiceSelector
   *     instance.
   * @return the first element of serviceSet.
   */
  @Override
  public ServiceReference<T> selectService(SortedSet<ServiceReference<T>> serviceSet) {
    return serviceSet.first();
  }
}
