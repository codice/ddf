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
package org.codice.ddf.cxf.paos;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.junit.Test;

public class PaosOutInterceptorTest {
  @Test
  public void testHandleMessageNoAccept() {
    Message message = new MessageImpl();
    message.put(Message.PROTOCOL_HEADERS, new HashMap<String, List<String>>());
    PaosOutInterceptor paosOutInterceptor = new PaosOutInterceptor(Phase.POST_LOGICAL);
    paosOutInterceptor.handleMessage(message);
    assertThat(
        ((Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS)).get(HttpHeaders.ACCEPT),
        contains("application/vnd.paos+xml", "*/*"));
    assertTrue(
        ((Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS))
            .get("PAOS")
            .contains("ver=\"urn:liberty:paos:2003-08\""));
    assertTrue(
        ((Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS))
            .get("PAOS")
            .contains(
                "\"urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp\",\"urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp:2.0:WantAuthnRequestsSigned\""));
  }

  @Test
  public void testHandleMessageAccept() {
    Message message = new MessageImpl();
    HashMap<String, List<String>> headers = new HashMap<>();
    headers.put(HttpHeaders.ACCEPT, new ArrayList<>());
    message.put(Message.PROTOCOL_HEADERS, headers);
    PaosOutInterceptor paosOutInterceptor = new PaosOutInterceptor(Phase.POST_LOGICAL);
    paosOutInterceptor.handleMessage(message);
    assertThat(
        ((Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS)).get(HttpHeaders.ACCEPT),
        contains("application/vnd.paos+xml", "*/*"));
    assertTrue(
        ((Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS))
            .get("PAOS")
            .contains("ver=\"urn:liberty:paos:2003-08\""));
    assertTrue(
        ((Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS))
            .get("PAOS")
            .contains(
                "\"urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp\",\"urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp:2.0:WantAuthnRequestsSigned\""));
  }
}
