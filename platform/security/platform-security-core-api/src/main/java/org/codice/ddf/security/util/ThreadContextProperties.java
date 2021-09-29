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
package org.codice.ddf.security.util;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import org.apache.shiro.util.ThreadContext;

public final class ThreadContextProperties {

  public static final String TRACE_CONTEXT_KEY = "trace-context";

  private static final String TRACE_ID = "trace-id";

  public static final String CLIENT_INFO_KEY = "client-info";

  public static final String SERVLET_REMOTE_ADDR = "remoteAddr";

  public static final String SERVLET_REMOTE_HOST = "remoteHost";

  public static final String SERVLET_REMOTE_PORT = "remotePort";

  public static final String SERVLET_SCHEME = "scheme";

  public static final String SERVLET_CONTEXT_PATH = "contextPath";

  private ThreadContextProperties() {
    // as a utility this should never be constructed, hence it's private
  }

  /**
   * Adds the trace context and unique trace id to the ThreadContext.
   *
   * @return traceId in ThreadContext
   */
  public static String addTraceId() {
    String traceId = getTraceId();
    if (traceId == null) {
      Map<String, String> traceContextMap = new HashMap<>();
      traceId = UUID.randomUUID().toString().replaceAll("-", "");
      traceContextMap.put(TRACE_ID, traceId);
      ThreadContext.put(TRACE_CONTEXT_KEY, traceContextMap);
    }
    return traceId;
  }

  /** @return trace-id from ThreadContext if it exists otherwise returns null */
  public static String getTraceId() {
    String traceId = null;
    Map<String, String> traceContextMap =
        (Map<String, String>) ThreadContext.get(TRACE_CONTEXT_KEY);
    if (traceContextMap != null && traceContextMap.size() > 0) {
      traceId = traceContextMap.get(TRACE_ID);
    }
    return traceId;
  }

  /** removes the trace context map from the ThreadContext */
  public static void removeTraceId() {
    ThreadContext.remove(ThreadContextProperties.TRACE_CONTEXT_KEY);
  }

  /**
   * Adds the client info map to ThreadContext. The Client Info can include the client's IP address,
   * client host, client port, and the request context path.
   */
  public static void addClientInfo(ServletRequest request) {
    ServletContext servletContext = request.getServletContext();
    ThreadContext.put(
        CLIENT_INFO_KEY,
        createClientInfoMap(
            request.getRemoteAddr(),
            request.getRemoteHost(),
            request.getRemotePort(),
            request.getScheme(),
            servletContext == null ? null : servletContext.getContextPath()));
  }

  /**
   * Adds the client info map to the ThreadContext
   * @param clientAddress
   * @param requestUri
   */
  public static void addClientInfo(InetSocketAddress clientAddress, @Nullable URI requestUri) {
    ThreadContext.put(
        CLIENT_INFO_KEY,
        createClientInfoMap(
            clientAddress.getAddress().getHostAddress(),
            clientAddress.getHostName(),
            clientAddress.getPort(),
            requestUri != null ? requestUri.getScheme() : null,
            requestUri != null ? requestUri.getPath() : null));
  }

  /** @return the client IP address or null if not present */
  public static String getRemoteAddress() {
    String remoteAddr = null;
    Object clientInfoRaw = ThreadContext.get(CLIENT_INFO_KEY);
    if (clientInfoRaw instanceof Map) {
      remoteAddr = (String) ((Map) clientInfoRaw).get(SERVLET_REMOTE_ADDR);
    }
    return remoteAddr;
  }

  /** @return the client Port address or null if not present */
  public static String getRemotePort() {
    String remotePort = null;
    Object clientInfoRaw = ThreadContext.get(CLIENT_INFO_KEY);
    if (clientInfoRaw instanceof Map) {
      remotePort = (String) ((Map) clientInfoRaw).get(SERVLET_REMOTE_PORT);
    }
    return remotePort;
  }

  /** @return the client host name or null if it not present */
  public static String getRemoteHost() {
    String remoteHost = null;
    Object clientInfoRaw = ThreadContext.get(CLIENT_INFO_KEY);
    if (clientInfoRaw instanceof Map) {
      remoteHost = (String) ((Map) clientInfoRaw).get(SERVLET_REMOTE_HOST);
    }
    return remoteHost;
  }

  /** @return the context path of the request if available or null if not present */
  public static String getContextPath() {
    String remoteHost = null;
    Object clientInfoRaw = ThreadContext.get(CLIENT_INFO_KEY);
    if (clientInfoRaw instanceof Map) {
      remoteHost = (String) ((Map) clientInfoRaw).get(SERVLET_CONTEXT_PATH);
    }
    return remoteHost;
  }

  /** @return the scheme of the request if available or null if not present */
  public static String getScheme() {
    String remoteHost = null;
    Object clientInfoRaw = ThreadContext.get(CLIENT_INFO_KEY);
    if (clientInfoRaw instanceof Map) {
      remoteHost = (String) ((Map) clientInfoRaw).get(SERVLET_SCHEME);
    }
    return remoteHost;
  }

  /** removes the client info map from the Thread Context */
  public static void removeClientInfo() {
    ThreadContext.remove(ThreadContextProperties.CLIENT_INFO_KEY);
  }

  private static Map<String, String> createClientInfoMap(
      String remoteAddr,
      String remoteHost,
      int remotePort,
      @Nullable String requestScheme,
      @Nullable String requestPath) {
    Map<String, String> clientInfoMap = new HashMap<>();
    clientInfoMap.put(SERVLET_REMOTE_ADDR, remoteAddr);
    clientInfoMap.put(SERVLET_REMOTE_HOST, remoteHost);
    clientInfoMap.put(SERVLET_REMOTE_PORT, Integer.toString(remotePort));
    if (requestScheme != null) {
      clientInfoMap.put(SERVLET_SCHEME, requestScheme);
    }
    if (requestPath != null) {
      clientInfoMap.put(SERVLET_CONTEXT_PATH, requestPath);
    }
    return clientInfoMap;
  }
}
