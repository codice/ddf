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
 * This interface represents the strategy used by ServiceSelector instances for selecting which
 * service to return from a call to ServiceSelector.getService().
 *
 * @param <T> - The type of the service to be returned.
 */
public interface ServiceSelectionStrategy<T> {

  /**
   * @param serviceSet - An unmodifiable, sorted list of all services bound to the ServiceSelector
   *     instance.
   * @return the ServiceReference object that the ServiceSelector should use.
   */
  ServiceReference<T> selectService(SortedSet<ServiceReference<T>> serviceSet);
}
