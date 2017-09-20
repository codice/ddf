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
package org.codice.ddf.platform.util;

import java.io.Serializable;
import java.util.Comparator;
import org.osgi.framework.ServiceReference;

/** Comparator for OSGi {@link org.osgi.framework.ServiceReference} objects. */
public class ServiceComparator implements Comparator<ServiceReference>, Serializable {

  /**
   * Compares this ServiceReference with the specified ServiceReference for order using the OSGi
   * {@link org.osgi.framework.ServiceReference} compare method.
   *
   * <p>If this ServiceReference and the specified ServiceReference have the same service id they
   * are equal. This ServiceReference is less than the specified ServiceReference if it has a lower
   * service ranking and greater if it has a higher service ranking. Otherwise, if this
   * ServiceReference and the specified ServiceReference have the same service ranking, this
   * ServiceReference is less than the specified ServiceReference if it has a higher service id and
   * greater if it has a lower service id.
   */
  @Override
  public int compare(ServiceReference ref1, ServiceReference ref2) {
    return ref2.compareTo(ref1);
  }
}
