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
package org.codice.ddf.security.handler.cas;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import org.codice.ddf.security.handler.api.BSTAuthenticationToken;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CASAuthenticationToken extends BSTAuthenticationToken {
  public static final String CAS_ID = "CAS";

  private static final Logger LOGGER = LoggerFactory.getLogger(CASAuthenticationToken.class);

  private static final String SEP_CHAR = "|";

  public CASAuthenticationToken(Principal principal, String proxyTicket, String serviceUrl) {
    this(principal, proxyTicket, serviceUrl, BaseAuthenticationToken.DEFAULT_REALM);
  }

  public CASAuthenticationToken(
      Principal principal, String proxyTicket, String serviceUrl, String realm) {
    super(principal, proxyTicket + SEP_CHAR + serviceUrl, realm);
    setTokenValueType("", CAS_ID);
    setTokenId(CAS_ID);
  }

  public String getTicketWithService() {
    String ticket = (String) getCredentials();
    return ticket;
  }

  public String getUser() {
    String user = null;
    if (principal instanceof Principal) {
      user = ((Principal) principal).getName();
    } else if (principal instanceof String) {
      user = (String) principal;
    }
    if (user == null) {
      LOGGER.debug("Unexpected null user.");
    }
    return user;
  }

  public byte[] getCertificate() {
    byte[] certs = null;
    if (credentials instanceof byte[]) {
      certs = (byte[]) credentials;
    }
    return certs;
  }

  @Override
  public String getEncodedCredentials() {
    String encodedTicket =
        Base64.getEncoder().encodeToString(getTicketWithService().getBytes(StandardCharsets.UTF_8));
    LOGGER.trace("BST: {}", encodedTicket);
    return encodedTicket;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("User: ");
    sb.append(getUser());
    sb.append("; ticket: ");
    String ticket = getTicketWithService();
    if ((ticket != null) && (ticket.length() > 5)) {
      sb.append(getTicketWithService().substring(0, 5));
    } else {
      sb.append(ticket);
    }
    sb.append("...; realm: ");
    sb.append(realm);
    return sb.toString();
  }
}
