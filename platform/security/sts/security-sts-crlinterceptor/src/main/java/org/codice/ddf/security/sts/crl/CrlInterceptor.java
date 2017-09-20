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
package org.codice.ddf.security.sts.crl;

import java.security.cert.X509Certificate;
import javax.servlet.http.HttpServletRequest;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.codice.ddf.security.handler.pki.CrlChecker;

/**
 * Interceptor that checks an incoming message against a defined certification revocation list
 * (CRL).
 */
public class CrlInterceptor extends AbstractPhaseInterceptor<Message> {

  private CrlChecker crlChecker;

  /**
   * Creates a new crl interceptor. Loads in a CRL from the CRL file pointed to by the
   * encryption.properties file.
   */
  public CrlInterceptor(CrlChecker crlChecker) {
    super(Phase.PRE_PROTOCOL);
    getAfter().add(SAAJInInterceptor.class.getName());
    this.crlChecker = crlChecker;
  }

  @Override
  public void handleMessage(Message message) throws Fault {
    HttpServletRequest request =
        (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
    X509Certificate[] certs =
        (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");

    if (!crlChecker.passesCrlCheck(certs)) {
      throw new AccessDeniedException("Cannot complete request, certificate was revoked by CRL.");
    }
  }
}
