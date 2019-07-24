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
package org.codice.ddf.security.handler.api;

import static ddf.security.principal.GuestPrincipal.NAME_DELIMITER;

import ddf.security.common.audit.SecurityLogger;
import ddf.security.principal.GuestPrincipal;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Authentication token representing a guest user's credentials */
public class GuestAuthenticationToken extends BaseAuthenticationToken {
  private static final Logger LOGGER = LoggerFactory.getLogger(GuestAuthenticationToken.class);

  public static final String GUEST_CREDENTIALS = "Guest";

  public GuestAuthenticationToken(String name) {
    super(new GuestPrincipal(name), GUEST_CREDENTIALS, parseAddressFromName(name));

    if (!StringUtils.isEmpty(name)) {
      SecurityLogger.audit("Guest token generated for IP address: " + name);
    }
  }

  public static String parseAddressFromName(@Nullable String fullName) {
    if (!StringUtils.isEmpty(fullName)) {
      String[] parts = fullName.split(NAME_DELIMITER);
      if (parts.length == 2) {
        return formatIpAddress(parts[1]);
      }
    }
    return null;
  }

  // IPv6 addresses should be contained within brackets to conform
  // to the spec IETF RFC 2732
  private static String formatIpAddress(String ipAddress) {
    try {
      if (InetAddress.getByName(ipAddress) instanceof Inet6Address && !ipAddress.contains("[")) {
        ipAddress = "[" + ipAddress + "]";
      }
    } catch (UnknownHostException e) {
      LOGGER.debug("Error formatting the ip address, using the unformatted ip address", e);
    }

    return ipAddress;
  }

  public String getIpAddress() {
    String ip = null;
    if (principal instanceof GuestPrincipal) {
      ip = parseAddressFromName(((GuestPrincipal) principal).getName());
    } else if (principal instanceof String) {
      ip = parseAddressFromName((String) principal);
    }
    return ip;
  }

  @Override
  public String getCredentialsAsString() {
    return credentials.toString();
  }

  @Override
  public String toString() {
    return String.format("Guest IP: %s", getIpAddress());
  }
}
