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
package org.codice.ddf.cxf.oauth;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.codice.ddf.cxf.oauth.OAuthSecurity.OAUTH;

import java.util.List;
import java.util.Map;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuthOutInterceptor extends AbstractPhaseInterceptor<Message> {

  private static final Logger LOGGER = LoggerFactory.getLogger(OAuthOutInterceptor.class);

  public OAuthOutInterceptor(String phase) {
    super(phase);
  }

  /** Gets the access token from the OAUTH header and sets it to the Authorization header. */
  @Override
  public void handleMessage(Message message) {
    Map<String, List<String>> headers =
        (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);

    List<String> authorizationHeader = headers.get(OAUTH);

    if (authorizationHeader == null || authorizationHeader.isEmpty()) {
      return;
    }

    LOGGER.debug("Setting access token to the authorization header.");
    headers.put(AUTHORIZATION, authorizationHeader);
    headers.remove(OAUTH);
  }
}
