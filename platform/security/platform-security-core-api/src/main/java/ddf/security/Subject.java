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
package ddf.security;

import java.io.Serializable;
import java.security.Principal;

/**
 * This class extends {@link org.apache.shiro.subject.Subject}, {@link java.io.Serializable}, and
 * {@link java.security.Principal} in order for the Subject to be accessible through the {@link
 * ddf.catalog.operation.Operation} property map.
 */
public interface Subject extends org.apache.shiro.subject.Subject, Serializable, Principal {

  /**
   * Returns true if the Subject is guest
   *
   * @return true if Subject is guest
   */
  public boolean isGuest();
}
