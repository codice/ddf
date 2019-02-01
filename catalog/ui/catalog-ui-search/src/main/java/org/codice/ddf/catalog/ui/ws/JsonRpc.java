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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  private static final String JSONRPC = "jsonrpc";
  private static final String METHOD = "method";

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
    return error(INVALID_PARAMS, String.format("Invalid params: %s", message), params);
  }

  private static Error error(int code, String message, Object data) {
    return new Error(code, message, data);
  }

  private static Map<String, Object> response(Object id, Object value) {
    Map<String, Object> response = new HashMap<>();
    response.put(JSONRPC, VERSION);
    response.put("id", id);
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
    // no action required on error
  }

  @Override
  public void onMessage(Session session, String message) throws IOException {
    session.getRemote().sendStringByFuture(mapper.toJson(handleMessage(message)));
  }

  private Object handleMessage(String message) {
    Object parsed;

    try {
      parsed = mapper.fromJson(message);
    } catch (RuntimeException ex) {
      return response(null, error(PARSE_ERROR, "Parse error", message));
    }

    return exec(parsed);
  }

  private Object exec(Object message) {

    if (!(message instanceof Map)) {
      return response(null, invalid("message must be a map", message));
    }

    Map msg = (Map) message;

    if (!msg.containsKey("id")) {
      return response(null, invalid("required key `id` missing"));
    }

    Object id = msg.get("id");

    if (!(id instanceof String || id instanceof Number || id == null)) {
      return response(null, invalid("key `id` not string or number or null", id));
    }

    if (!msg.containsKey(JSONRPC)) {
      return response(id, invalid("required key `jsonrpc` missing"));
    }

    if (!VERSION.equals(msg.get(JSONRPC))) {
      return response(id, invalid("key `jsonrpc` not equal to `2.0`", msg.get(JSONRPC)));
    }

    if (!msg.containsKey(METHOD)) {
      return response(id, invalid("required key `method` missing"));
    }

    if (!(msg.get(METHOD) instanceof String)) {
      return response(id, invalid("key `method` not string", msg.get(METHOD)));
    }

    String method = (String) msg.get(METHOD);

    if (!methods.containsKey(method)) {
      return response(id, error(METHOD_NOT_FOUND, String.format("method `%s` not found", method)));
    }

    Object params = msg.get("params");

    if (params != null && !(params instanceof List || params instanceof Map)) {
      return response(id, invalidParams("parameters must be a structured value", params));
    }

    try {
      return response(id, methods.get(method).apply(params));
    } catch (RuntimeException e) {
      return response(id, JsonRpc.error(INTERNAL_ERROR, "Internal Error"));
    }
  }

  private static class Error {
    public final int code;
    public final String message;
    public final Object data;

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
