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
package org.codice.ddf.catalog.ui.ws;

import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.io.IOException;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketServlet extends WebSocketServlet {
  private static final String TOKEN_KEY = "ddf.security.token";
  private static final Logger LOGGER = LoggerFactory.getLogger(SocketServlet.class);
  private final Socket socket;
  private final SecurityManager handler;

  public SocketServlet(Socket socket, SecurityManager handler) {
    this.socket = socket;
    this.handler = handler;
  }

  @Override
  public void configure(WebSocketServletFactory factory) {
    factory.setCreator((req, resp) -> new SocketWrapper(socket, handler));
  }

  @WebSocket
  public static class SocketWrapper {
    private final Socket socket;
    private final SecurityManager manager;

    SocketWrapper(Socket socket, SecurityManager manager) {
      this.socket = socket;
      this.manager = manager;
    }

    private void runWithUser(Session session, Runnable runnable) {
      HandlerResult result =
          (HandlerResult)
              ((ServletUpgradeRequest) session.getUpgradeRequest())
                  .getHttpServletRequest()
                  .getAttribute(TOKEN_KEY);

      try {
        manager.getSubject(result.getToken()).execute(runnable::run);
      } catch (SecurityServiceException e) {
        LOGGER.error("Failed to get subject.", e);
      }
    }

    @OnWebSocketConnect
    public void onOpen(Session session) {
      runWithUser(session, () -> socket.onOpen(session));
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
      runWithUser(session, () -> socket.onClose(session, statusCode, reason));
    }

    @OnWebSocketError
    public void onError(Session session, Throwable ex) {
      runWithUser(session, () -> socket.onError(session, ex));
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
      runWithUser(
          session,
          () -> {
            try {
              socket.onMessage(session, message);
            } catch (IOException e) {
              LOGGER.error("Failed to receive socket message.", e);
            }
          });
    }
  }
}
