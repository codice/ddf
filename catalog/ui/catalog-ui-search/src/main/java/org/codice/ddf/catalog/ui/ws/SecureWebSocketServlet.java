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
import java.util.concurrent.ExecutorService;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecureWebSocketServlet extends WebSocketServlet {

  private static final String TOKEN_KEY = "ddf.security.token";
  private static final Logger LOGGER = LoggerFactory.getLogger(SecureWebSocketServlet.class);
  private final WebSocket ws;
  private final SecurityManager manager;
  private final ExecutorService executor;

  public SecureWebSocketServlet(ExecutorService executor, WebSocket ws, SecurityManager manager) {
    this.ws = ws;
    this.manager = manager;
    this.executor = executor;
  }

  public void destroy() {
    executor.shutdown();
  }

  @Override
  public void configure(WebSocketServletFactory factory) {
    factory.setCreator((req, resp) -> new SocketWrapper(executor, ws, manager));
  }

  @org.eclipse.jetty.websocket.api.annotations.WebSocket
  public static class SocketWrapper {

    private final WebSocket ws;
    private final SecurityManager manager;
    private final ExecutorService executor;

    SocketWrapper(ExecutorService executor, WebSocket ws, SecurityManager manager) {
      this.ws = ws;
      this.manager = manager;
      this.executor = executor;
    }

    private void runWithUser(Session session, Runnable runnable) {
      HandlerResult result =
          (HandlerResult)
              ((ServletUpgradeRequest) session.getUpgradeRequest())
                  .getHttpServletRequest()
                  .getAttribute(TOKEN_KEY);

      executor.submit(
          () -> {
            try {
              manager.getSubject(result.getToken()).execute(runnable::run);
            } catch (SecurityServiceException e) {
              LOGGER.error("Failed to get subject.", e);
            }
          });
    }

    @OnWebSocketConnect
    public void onOpen(Session session) {
      runWithUser(session, () -> ws.onOpen(session));
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
      runWithUser(session, () -> ws.onClose(session, statusCode, reason));
    }

    @OnWebSocketError
    public void onError(Session session, Throwable ex) {
      runWithUser(session, () -> ws.onError(session, ex));
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
      runWithUser(
          session,
          () -> {
            try {
              ws.onMessage(session, message);
            } catch (IOException e) {
              LOGGER.error("Failed to receive ws message.", e);
            }
          });
    }
  }
}
