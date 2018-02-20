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
import java.util.stream.Collectors;
import org.boon.json.JsonFactory;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializerFactory;
import org.boon.json.ObjectMapper;
import org.eclipse.jetty.websocket.api.Session;

// An implementation of http://www.jsonrpc.org/specification over websockets
public class JsonRpc implements Socket {

  public static final String VERSION = "2.0";

  public static final int PARSE_ERROR = -32700;
  public static final int INVALID_REQUEST = -32600;
  public static final int METHOD_NOT_FOUND = -32601;
  public static final int INVALID_PARAMS = 32602;
  public static final int INTERNAL_ERROR = -32603;

  private ObjectMapper mapper =
      JsonFactory.create(
          new JsonParserFactory().usePropertyOnly(),
          new JsonSerializerFactory()
              .includeEmpty()
              .includeNulls()
              .includeDefaultValues()
              .setJsonFormatForDates(false));

  private final Map<String, Function> methods;

  public JsonRpc(Map<String, Function> methods) {
    this.methods = methods;
  }

  private static Map<String, Object> parseError(Object message) {
    return error(PARSE_ERROR, "Parse error", message);
  }

  private static Map<String, Object> error(int code, String message) {
    return error(code, message, null);
  }

  private static Map<String, Object> error(int code, String message, Object data) {
    Map<String, Object> response = new HashMap<>();
    response.put("code", code);
    response.put("message", message);
    if (data != null) {
      response.put("data", data);
    }
    return response;
  }

  private static Map<String, Object> success(Object id, Object result) {
    Map<String, Object> response = new HashMap<>();
    response.put("jsonrpc", VERSION);
    response.put("id", id);
    response.put("result", result);
    return response;
  }

  private static Map<String, Object> fail(Object id, Object error) {
    Map<String, Object> response = new HashMap<>();
    response.put("jsonrpc", VERSION);
    response.put("id", id);
    response.put("error", error);
    return response;
  }

  private Map<String, Object> invalid() {
    return invalid(null);
  }

  private Map<String, Object> invalid(Object data) {
    return error(INVALID_REQUEST, "Invalid Request", data);
  }

  private Map<String, Object> invalidParams(Object data) {
    return error(INVALID_PARAMS, "Invalid params", data);
  }

  @Override
  public void onOpen(Session session) {}

  @Override
  public void onClose(Session session, int statusCode, String reason) {}

  @Override
  public void onError(Session session, Throwable ex) {}

  private Object exec(Object message) {

    if (message instanceof List) {
      return ((List) message).parallelStream().map(this::exec).collect(Collectors.toList());
    }

    if (!(message instanceof Map)) {
      return invalid();
    }

    Map msg = (Map) message;

    if (!msg.containsKey("id")) {
      return fail(null, invalid("Required key `id` missing"));
    }

    Object id = msg.get("id");

    if (!(id instanceof String || id instanceof Number || id == null)) {
      return fail(null, invalid("Key `id` not string or number or null"));
    }

    if (!msg.containsKey("jsonrpc")) {
      return fail(id, invalid("Required key `jsonrpc` missing"));
    }

    if (!VERSION.equals(msg.get("jsonrpc"))) {
      return fail(id, invalid("Key `jsonrpc` not equal to `2.0`"));
    }

    if (!msg.containsKey("method")) {
      return fail(id, invalid("Required key `method` missing"));
    }

    if (!(msg.get("method") instanceof String)) {
      return fail(id, invalid("Key `method` not string"));
    }

    if (!methods.containsKey(msg.get("method"))) {
      return fail("id", error(METHOD_NOT_FOUND, "Method not found"));
    }

    Object params = msg.get("params");

    if (params != null && !(params instanceof List || params instanceof Map)) {
      return fail(id, invalidParams("Parameters must be a structured value"));
    }

    try {
      return success(id, methods.get(msg.get("method")).apply(params));
    } catch (IllegalArgumentException ex) {
      return fail(id, invalidParams(ex.getMessage()));
    } catch (Exception ex) {
      return fail(id, error(INTERNAL_ERROR, "Internal error"));
    }
  }

  private Object handleMessage(String message) {
    try {
      return exec(mapper.fromJson(message));
    } catch (Exception ex) {
      return fail(null, parseError(message));
    }
  }

  @Override
  public void onMessage(Session session, String message) throws IOException {
    session.getRemote().sendString(mapper.toJson(handleMessage(message)));
  }
}
