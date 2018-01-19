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
package org.codice.ddf.security.handler.api;

import java.util.Map;
import java.util.Set;

/**
 * Service contract for listing and invalidating user sessions.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface SessionHandler {

  /**
   * Gets the set of active sessions and their attached SPs.
   *
   * @return Map of session name::attached SPs for active sessions
   */
  Map<String, Set<String>> getActiveSessions();

  /**
   * Invalidates a session and sends LogoutRequests to all attached SPs.
   *
   * @param subjectName the name of the subject to invalidate
   */
  void invalidateSession(String subjectName);
}
