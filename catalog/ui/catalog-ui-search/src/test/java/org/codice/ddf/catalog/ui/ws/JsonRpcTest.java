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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.boon.json.JsonFactory;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializerFactory;
import org.boon.json.ObjectMapper;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class JsonRpcTest {

  private ObjectMapper mapper =
      JsonFactory.create(
          new JsonParserFactory().usePropertyOnly(),
          new JsonSerializerFactory()
              .includeEmpty()
              .includeNulls()
              .includeDefaultValues()
              .setJsonFormatForDates(false));

  private Map<String, Function> methods = ImmutableMap.of("id", (value) -> value);

  private JsonRpc rpc = new JsonRpc(methods);

  private static void assertError(Map<String, Object> response, int code) {
    assertThat(response.containsKey("error"), is(true));
    assertThat(response.get("error"), instanceOf(Map.class));
    Map error = (Map) response.get("error");
    assertThat(error.get("code"), is(code));
  }

  private Map<String, Object> onMessage(JsonRpc rpc, String message) throws IOException {
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    Session session = mock(Session.class);
    RemoteEndpoint endpoint = mock(RemoteEndpoint.class);
    doReturn(endpoint).when(session).getRemote();
    rpc.onMessage(session, message);
    verify(endpoint).sendStringByFuture(captor.capture());
    return (Map<String, Object>) mapper.fromJson(captor.getValue());
  }

  private Map<String, Object> onMessage(JsonRpc rpc, Map<String, Object> message)
      throws IOException {
    return onMessage(rpc, mapper.toJson(message));
  }

  private Map<String, Object> request(int id, String method, Object params) {
    Map<String, Object> req = new HashMap<>();
    req.put("jsonrpc", JsonRpc.VERSION);
    req.put("id", id);
    req.put("method", method);
    req.put("params", params);
    return req;
  }

  @Test
  public void testRpcVersionInResponse() throws Exception {
    Map<String, Object> response = onMessage(rpc, ImmutableMap.of());
    assertThat(response.get("jsonrpc"), is(JsonRpc.VERSION));
  }

  @Test
  public void testInvalidRequest() throws Exception {
    assertError(onMessage(rpc, "[}"), JsonRpc.PARSE_ERROR);
  }

  @Test
  public void testVersionCheck() throws Exception {
    assertError(onMessage(rpc, ImmutableMap.of()), JsonRpc.INVALID_REQUEST);
  }

  @Test
  public void testMethodNotFound() throws Exception {
    Map<String, Object> req = request(0, "not-found", null);
    assertError(onMessage(rpc, req), JsonRpc.METHOD_NOT_FOUND);
  }

  @Test
  public void testSucessfulCall() throws Exception {
    List value = ImmutableList.of(0);
    Map<String, Object> req = request(0, "id", value);
    Map<String, Object> resp = onMessage(rpc, req);
    assertThat(resp.get("id"), is(0));
    assertThat(resp.get("result"), is(value));
  }
}
