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
package org.codice.ddf.security.handler.pki;

import java.security.cert.X509Certificate;

public interface OcspService {

  /**
   * Checks the whether the given {@param certs} are revoked or not against the OCSP server.
   *
   * @param certs - an array of certificates to verify
   * @return true if the certificates are not revoked, false if any of them are revoked.
   */
  boolean passesOcspChecker(X509Certificate[] certs);
}
