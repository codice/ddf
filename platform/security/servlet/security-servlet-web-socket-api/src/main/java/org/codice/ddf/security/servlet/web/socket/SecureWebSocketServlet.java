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

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.security.util.ThreadContextProperties;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecureWebSocketServlet implements WebSocketCreator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SecureWebSocketServlet.class);

  private final WebSocket ws;

  private final ExecutorService executor;

  private final List<SessionPlugin> sessionPlugins;

  public SecureWebSocketServlet(
      ExecutorService executor, WebSocket ws, List<SessionPlugin> sessionPlugins) {
    this.ws = ws;
    this.executor = executor;
    this.sessionPlugins = sessionPlugins;
  }

  public void destroy() {
    executor.shutdown();
  }

  @Override
  public Object createWebSocket(
      ServletUpgradeRequest servletUpgradeRequest, ServletUpgradeResponse servletUpgradeResponse) {
    return new SocketWrapper(executor, ws, sessionPlugins, servletUpgradeRequest.getSession());
  }

  public static class SocketWrapper {

    private final WebSocket ws;

    private final ExecutorService executor;

    private final HttpSession httpSession;

    private final List<SessionPlugin> sessionPlugins;

    SocketWrapper(
        ExecutorService executor,
        WebSocket ws,
        List<SessionPlugin> sessionPlugins,
        HttpSession httpSession) {
      this.ws = ws;
      this.executor = executor;
      this.sessionPlugins = sessionPlugins;
      this.httpSession = httpSession;
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
              ThreadContextProperties.addTraceId();
              addClientInfoToContext(session);

              try {
                ws.onMessage(session, message);
              } catch (IOException e) {
                LOGGER.error("Failed to receive ws message.", e);
              } finally {
                ThreadContextProperties.removeTraceId();
                ThreadContextProperties.removeClientInfo();
              }
            });
      } else {
        onError(session, new WebSocketAuthenticationException("User not logged in.", message));
      }
    }

    private void addClientInfoToContext(Session session) {
      String clientIP;
      String clientPort;
      String clientHost;
      URI requestUri = null;
      if (session instanceof org.eclipse.jetty.websocket.common.WebSocketSession) {
        requestUri =
            ((org.eclipse.jetty.websocket.common.WebSocketSession) session).getRequestURI();
      }
      String xForwardedFor =
          session.getUpgradeRequest().getHeader(HttpHeader.X_FORWARDED_FOR.toString());
      if (StringUtils.isNotEmpty(xForwardedFor)) {
        // a proxy has set the x-forwarded-* headers which indicate the actual client info
        clientIP = xForwardedFor;
        clientPort = session.getUpgradeRequest().getHeader(HttpHeader.X_FORWARDED_PORT.toString());
        clientHost = session.getUpgradeRequest().getHeader(HttpHeader.X_FORWARDED_HOST.toString());
      } else {
        // otherwise use the remote address/port info
        clientIP = session.getRemoteAddress().getAddress().toString();
        clientPort = Integer.toString(session.getRemoteAddress().getPort());
        clientHost = session.getRemoteAddress().getHostName();
      }
      ThreadContextProperties.addClientInfo(clientIP, clientHost, clientPort, requestUri);
      callSessionPlugins(session);
    }

    private void callSessionPlugins(Session session) {
      sessionPlugins.forEach(
          sessionPlugin -> {
            try {
              sessionPlugin.handle(session);
            } catch (Exception e) {
              LOGGER.debug("The session plugin {} failed (continuing)", sessionPlugin, e);
            }
          });
    }

    private boolean isUserLoggedIn() {
      if (httpSession == null) {
        return false;
      }

      try {
        httpSession.getCreationTime();
        return true;
      } catch (IllegalStateException ise) {
        // the session is invalid
        return false;
      }
    }
  }
}
