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
package org.codice.ddf.security.servlet.web.socket;

import java.io.IOException;
import org.eclipse.jetty.websocket.api.Session;

/** WebSocket is a thin wrapper over the org.eclipse.jetty.websocket.api.annotations. */
public interface WebSocket {
  void onOpen(Session session);

  void onClose(Session session, int statusCode, String reason);

  void onError(Session session, Throwable ex);

  void onMessage(Session session, String message) throws IOException;
}
