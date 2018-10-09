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
package org.codice.ddf.catalog.admin.poller;

import java.util.List;
import org.codice.ddf.admin.core.api.Service;

/**
 * {@link AdminPollerServiceBeanMBean} defines an interface for accessing information about
 * configured sources
 */
public interface AdminPollerServiceBeanMBean {

  static final int AVAILABLE = 1;

  static final int UNAVAILABLE = 0;

  static final int STATUS_PENDING = -1;

  /**
   * Get the current status of the source.
   *
   * @param servicePID the PID of the source to get the status of.
   * @return Int indicating the status of the source. 1 if the source is available, 0 if
   *     unavailable, and -1 if the source has not yet been polled for availability.
   */
  int sourceStatus(String servicePID);

  /**
   * Get information on all configured sources.
   *
   * @return A list of sources represented by Service objects.
   */
  List<Service> allSourceInfo();
}
