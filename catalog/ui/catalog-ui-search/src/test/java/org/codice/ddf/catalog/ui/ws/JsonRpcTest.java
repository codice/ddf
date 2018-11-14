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

import static org.codice.gsonsupport.GsonTypeAdapters.MAP_STRING_TO_OBJECT_TYPE;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class JsonRpcTest {

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .serializeNulls()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .create();

  private Map<String, Function> methods = ImmutableMap.of("id", (value) -> value);

  private JsonRpc rpc = new JsonRpc(methods);

  private static void assertError(Map<String, Object> response, long code) {
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
    return GSON.fromJson(captor.getValue(), MAP_STRING_TO_OBJECT_TYPE);
  }

  @Test
  public void testRpcVersionInResponse() throws Exception {
    Map<String, Object> response = onMessage(rpc, "{}");
    assertThat(response.get("jsonrpc"), is(JsonRpc.VERSION));
  }

  @Test
  public void testInvalidRequest() throws Exception {
    assertError(onMessage(rpc, "[}"), JsonRpc.PARSE_ERROR);
  }

  @Test
  public void testVersionCheck() throws Exception {
    assertError(onMessage(rpc, "{}"), JsonRpc.INVALID_REQUEST);
  }

  @Test
  public void testMethodNotFound() throws Exception {
    String message = "{\"method\":\"not-found\",\"id\":0,\"jsonrpc\":\"2.0\",\"params\":null}";
    assertError(onMessage(rpc, message), JsonRpc.METHOD_NOT_FOUND);
  }

  @Test
  public void testSucessfulCall() throws Exception {
    List value = ImmutableList.of(0L);
    String message = "{\"method\":\"id\",\"id\":6,\"jsonrpc\":\"2.0\",\"params\":[0]}";
    Map<String, Object> resp = onMessage(rpc, message);
    assertThat(resp.get("id"), is(6L));
    assertThat(resp.get("result"), is(value));
  }
}
