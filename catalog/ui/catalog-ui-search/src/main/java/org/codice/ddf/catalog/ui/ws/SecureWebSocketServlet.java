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

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.common.SecurityTokenHolder;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(SecureWebSocketServlet.class);
  private final WebSocket ws;
  private final ExecutorService executor;

  public SecureWebSocketServlet(ExecutorService executor, WebSocket ws) {
    this.ws = ws;
    this.executor = executor;
  }

  @Override
  public void destroy() {
    executor.shutdown();
  }

  /*
   Pass the TokenHolder into the WebSocket, in order to know when the user has logged out. Can't
   pass the Session because Jetty won't let anything check session attributes unless there's a
   request (excluding WebSocket requests) being actively fulfilled for that session.
  */
  @Override
  public void configure(WebSocketServletFactory factory) {
    factory.setCreator(
        (req, resp) ->
            new SocketWrapper(
                executor,
                ws,
                (SecurityTokenHolder)
                    req.getSession().getAttribute(SecurityConstants.SAML_ASSERTION)));
  }

  @org.eclipse.jetty.websocket.api.annotations.WebSocket
  public static class SocketWrapper {

    private final WebSocket ws;
    private final ExecutorService executor;
    private final SecurityTokenHolder securityTokenHolder;

    SocketWrapper(ExecutorService executor, WebSocket ws, SecurityTokenHolder securityTokenHolder) {
      this.ws = ws;
      this.executor = executor;
      this.securityTokenHolder = securityTokenHolder;
    }

    private void runWithUser(Session session, Runnable runnable) {
      Subject subject =
          (Subject)
              ((ServletUpgradeRequest) session.getUpgradeRequest())
                  .getHttpServletRequest()
                  .getAttribute(SecurityConstants.SECURITY_SUBJECT);

      executor.submit(
          () -> {
            subject.execute(runnable);
          });
    }

    @OnWebSocketConnect
    public void onOpen(Session session) {
      if (isUserLoggedIn()) {
        runWithUser(session, () -> ws.onOpen(session));
      } else {
        onError(session, new WebSocketAuthenticationException("User not logged in."));
      }
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
      if (isUserLoggedIn()) {
        runWithUser(
            session,
            () -> {
              try {
                ws.onMessage(session, message);
              } catch (IOException e) {
                LOGGER.error("Failed to receive ws message.", e);
              }
            });
      } else {
        onError(session, new WebSocketAuthenticationException("User not logged in.", message));
      }
    }

    private boolean isUserLoggedIn() {
      return securityTokenHolder.getRealmTokenMap().size() > 0;
    }
  }
}
