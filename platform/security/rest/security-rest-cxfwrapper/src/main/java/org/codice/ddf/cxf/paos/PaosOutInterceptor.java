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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;

public class PaosOutInterceptor extends AbstractPhaseInterceptor<Message> {

  private boolean ecpEnabled = true;

  public PaosOutInterceptor(String phase) {
    super(phase);
    ecpEnabled = Boolean.valueOf(System.getProperty("org.codice.ddf.security.ecp.enabled", "true"));
  }

  @Override
  public void handleMessage(Message message) throws Fault {
    if (ecpEnabled) {
      Map<String, List<String>> headers =
          (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);
      List<String> acceptHeaders = headers.get(HttpHeaders.ACCEPT);
      if (acceptHeaders == null) {
        acceptHeaders = new ArrayList<>();
      }

      if (acceptHeaders.isEmpty()) {
        acceptHeaders.add("application/vnd.paos+xml");
        acceptHeaders.add("*/*");
      } else {
        acceptHeaders.add("application/vnd.paos+xml");
      }

      headers.put(HttpHeaders.ACCEPT, acceptHeaders);

      List<String> paosHeaders = new ArrayList<>();
      paosHeaders.add("ver=\"urn:liberty:paos:2003-08\"");
      paosHeaders.add(
          "\"urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp\",\"urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp:2.0:WantAuthnRequestsSigned\"");
      headers.put("PAOS", paosHeaders);
    }
  }
}
