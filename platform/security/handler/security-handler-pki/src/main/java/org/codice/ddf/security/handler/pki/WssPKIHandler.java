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
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.wss4j.common.token.BinarySecurity;
import org.apache.wss4j.common.token.X509Security;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.PKIAuthenticationToken;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class WssPKIHandler extends AbstractPKIHandler {
  /** WS-Security compliant PKI type to use when configuring context policy. */
  private static final String AUTH_TYPE = "WSSPKI";

  @Override
  public String getAuthenticationType() {
    return AUTH_TYPE;
  }

  @Override
  protected BaseAuthenticationToken extractAuthenticationInfo(X509Certificate[] certs) {
    PKIAuthenticationToken pkiToken = tokenFactory.getTokenFromCerts(certs);
    BinarySecurityTokenType binarySecurityType =
        pkiToken.createBinarySecurityTokenType(pkiToken.getCredentials());

    // Turn the received JAXB object into a DOM element
    Document doc = DOMUtils.createDocument();
    BinarySecurity binarySecurity = new X509Security(doc);
    binarySecurity.setEncodingType(binarySecurityType.getEncodingType());
    binarySecurity.setValueType(X509Security.X509_V3_TYPE);
    String data = binarySecurityType.getValue();
    Node textNode = doc.createTextNode(data);
    binarySecurity.getElement().appendChild(textNode);

    BaseAuthenticationToken baseAuthenticationToken =
        new BaseAuthenticationToken(null, binarySecurity.toString());
    baseAuthenticationToken.setUseWssSts(true);
    return baseAuthenticationToken;
  }
}
