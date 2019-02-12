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

import ddf.security.common.audit.SecurityLogger;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.boon.json.JsonFactory;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializerFactory;
import org.boon.json.ObjectMapper;
import org.eclipse.jetty.websocket.api.Session;

// An implementation of http://www.jsonrpc.org/specification over websockets
public class JsonRpc implements WebSocket {

  public static final String VERSION = "2.0";

  public static final int PARSE_ERROR = -32700;
  public static final int INVALID_REQUEST = -32600;
  public static final int METHOD_NOT_FOUND = -32601;
  public static final int INVALID_PARAMS = 32602;
  public static final int INTERNAL_ERROR = -32603;
  public static final int NOT_LOGGED_IN_ERROR = -32000;

  private static final String JSONRPC = "jsonrpc";
  private static final String METHOD = "method";
  private static final String ID = "id";

  private final Map<String, Function> methods;

  private ObjectMapper mapper =
      JsonFactory.create(
          new JsonParserFactory().usePropertyOnly(),
          new JsonSerializerFactory()
              .includeEmpty()
              .includeNulls()
              .includeDefaultValues()
              .setJsonFormatForDates(false));

  public JsonRpc(Map<String, Function> methods) {
    this.methods = methods;
  }

  public static Error error(int code, String message) {
    return new Error(code, message);
  }

  public static Error invalidParams(String message, Object params) {
    return error(INVALID_PARAMS, String.format("Invalid parameters: %s", message), params);
  }

  private static Error error(int code, String message, Object data) {
    return new Error(code, message, data);
  }

  private static Map<String, Object> response(Object id, Object value) {
    Map<String, Object> response = new HashMap<>();
    response.put(JSONRPC, VERSION);
    response.put(ID, id);
    if (value instanceof Error) {
      response.put("error", value);
    } else {
      response.put("result", value);
    }
    return response;
  }

  private static Error invalid(String message) {
    return invalid(message, null);
  }

  private static Error invalid(String message, Object data) {
    return error(INVALID_REQUEST, String.format("Invalid Request: %s", message), data);
  }

  @Override
  public void onOpen(Session session) {
    // no action required on open
  }

  @Override
  public void onClose(Session session, int statusCode, String reason) {
    // no action required on close
  }

  @Override
  public void onError(Session session, Throwable ex) {
    if (ex instanceof WebSocketAuthenticationException) {
      SecurityLogger.audit("Received WebSockets request for user that is not logged in.");
      handleMessage(
          session,
          ((WebSocketAuthenticationException) ex).getWsMessage(),
          (message, id) -> error(NOT_LOGGED_IN_ERROR, ex.getMessage()));
    } else {
      // no action required
    }
  }

  @Override
  public void onMessage(Session session, String message) throws IOException {
    handleMessage(session, message, this::callMethod);
  }

  private void handleMessage(
      Session session, String message, BiFunction<Map, Object, Object> handleFunc) {
    Object id;
    Object result;

    try {
      Map messageMap = parseMessage(message);
      id = parseId(messageMap);
      validateJsonRpcVersion(messageMap, id);
      result = handleFunc.apply(messageMap, id);
    } catch (JsonRpcException exception) {
      id = exception.messageId;
      result = exception.error;
    }

    session.getRemote().sendStringByFuture(mapper.toJson(response(id, result)));
  }

  private Map parseMessage(String message) throws JsonRpcException {
    Object parsed;

    try {
      parsed = mapper.fromJson(message);
    } catch (RuntimeException ex) {
      throw new JsonRpcException(null, error(PARSE_ERROR, "Parse error", message));
    }

    if (!(parsed instanceof Map)) {
      throw new JsonRpcException(null, invalid("message must be a map", parsed));
    }

    return (Map) parsed;
  }

  private Object parseId(Map message) throws JsonRpcException {
    if (!message.containsKey(ID)) {
      throw new JsonRpcException(null, invalid(String.format("required key `%s` missing", ID)));
    }

    Object id = message.get(ID);

    if (!(id instanceof String || id instanceof Number || id == null)) {
      throw new JsonRpcException(
          null, invalid(String.format("key `%s` not string or number or null", ID), id));
    } else {
      return id;
    }
  }

  private void validateJsonRpcVersion(Map message, Object id) throws JsonRpcException {
    if (!message.containsKey(JSONRPC)) {
      throw new JsonRpcException(id, invalid(String.format("required key `%s` missing", JSONRPC)));
    }

    if (!VERSION.equals(message.get(JSONRPC))) {
      throw new JsonRpcException(
          id,
          invalid(
              String.format("key `%s` not equal to `%s`", JSONRPC, VERSION), message.get(JSONRPC)));
    }
  }

  private Object callMethod(Map message, Object id) throws JsonRpcException {
    if (!message.containsKey(METHOD)) {
      throw new JsonRpcException(id, invalid(String.format("required key `%s` missing", METHOD)));
    }

    if (!(message.get(METHOD) instanceof String)) {
      throw new JsonRpcException(
          id, invalid(String.format("key `%s` not string", METHOD), message.get(METHOD)));
    }

    String method = (String) message.get(METHOD);

    if (!methods.containsKey(method)) {
      throw new JsonRpcException(
          id, error(METHOD_NOT_FOUND, String.format("method `%s` not found", method)));
    }

    Object params = message.get("params");

    if (params != null && !(params instanceof List || params instanceof Map)) {
      throw new JsonRpcException(
          id, invalidParams("parameters must be a structured value", params));
    }

    try {
      return methods.get(method).apply(params);
    } catch (RuntimeException e) {
      throw new JsonRpcException(id, error(INTERNAL_ERROR, "Internal Error"));
    }
  }

  private static class JsonRpcException extends RuntimeException {
    private final Object messageId;
    private final Error error;

    private JsonRpcException(Object messageId, Error error) {
      this.messageId = messageId;
      this.error = error;
    }
  }

  private static class Error {
    private final int code;
    private final String message;
    private final Object data;
    private Object id;

    private Error(int code, String message) {
      this(code, message, null);
    }

    private Error(int code, String message, Object data) {
      this.code = code;
      this.message = message;
      this.data = data;
    }
  }
}
