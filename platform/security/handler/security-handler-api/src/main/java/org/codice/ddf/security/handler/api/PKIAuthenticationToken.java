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

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;

public class PKIAuthenticationToken extends BSTAuthenticationToken {
  public static final String BST_X509_LN = "X509";

  public static final String PKI_TOKEN_VALUE_TYPE =
      BSTAuthenticationToken.BST_NS + BSTAuthenticationToken.TOKEN_VALUE_SEPARATOR + BST_X509_LN;

  public PKIAuthenticationToken(Object principal, String encodedCerts) {
    this(principal, encodedCerts.getBytes(StandardCharsets.UTF_8));
    credentials = Base64.getDecoder().decode(encodedCerts);
  }

  public PKIAuthenticationToken(Object principal, byte[] certificates) {
    super(principal, certificates);
    setTokenValueType(BSTAuthenticationToken.BST_NS, BST_X509_LN);
    setTokenId(BST_X509_LN);
  }

  public String getDn() {
    String dn = null;
    if (principal instanceof Principal) {
      dn = ((Principal) principal).getName();
    } else if (principal instanceof String) {
      dn = (String) principal;
    }
    return dn;
  }

  public byte[] getCertificate() {
    byte[] certs = null;
    if (credentials instanceof byte[]) {
      certs = (byte[]) credentials;
    }
    return certs;
  }

  @Override
  public String getCredentials() {
    if (credentials instanceof byte[]) {
      return Base64.getEncoder().encodeToString((byte[]) credentials);
    }
    return "";
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("dn: ");
    sb.append(getDn());
    return sb.toString();
  }
}
