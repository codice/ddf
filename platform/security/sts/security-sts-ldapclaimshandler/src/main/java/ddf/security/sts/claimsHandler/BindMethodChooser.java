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
package ddf.security.sts.claimsHandler;

import org.apache.commons.lang.StringUtils;
import org.forgerock.opendj.ldap.requests.BindRequest;
import org.forgerock.opendj.ldap.requests.DigestMD5SASLBindRequest;
import org.forgerock.opendj.ldap.requests.GSSAPISASLBindRequest;
import org.forgerock.opendj.ldap.requests.Requests;

public class BindMethodChooser {

  public static BindRequest selectBindMethod(
      String bindMethod,
      String bindUserDN,
      String bindUserCredentials,
      String realm,
      String kdcAddress) {
    BindRequest request;
    switch (bindMethod) {
      case "Simple":
        request = Requests.newSimpleBindRequest(bindUserDN, bindUserCredentials.toCharArray());
        break;
      case "SASL":
        request = Requests.newPlainSASLBindRequest(bindUserDN, bindUserCredentials.toCharArray());
        break;
      case "GSSAPI SASL":
        request = Requests.newGSSAPISASLBindRequest(bindUserDN, bindUserCredentials.toCharArray());
        ((GSSAPISASLBindRequest) request).setRealm(realm);
        ((GSSAPISASLBindRequest) request).setKDCAddress(kdcAddress);
        break;
      case "Digest MD5 SASL":
        request =
            Requests.newDigestMD5SASLBindRequest(bindUserDN, bindUserCredentials.toCharArray());
        ((DigestMD5SASLBindRequest) request).setCipher(DigestMD5SASLBindRequest.CIPHER_HIGH);
        ((DigestMD5SASLBindRequest) request).getQOPs().clear();
        ((DigestMD5SASLBindRequest) request).getQOPs().add(DigestMD5SASLBindRequest.QOP_AUTH_CONF);
        ((DigestMD5SASLBindRequest) request).getQOPs().add(DigestMD5SASLBindRequest.QOP_AUTH_INT);
        ((DigestMD5SASLBindRequest) request).getQOPs().add(DigestMD5SASLBindRequest.QOP_AUTH);
        if (StringUtils.isNotEmpty(realm)) {
          ((DigestMD5SASLBindRequest) request).setRealm(realm);
        }
        break;
      default:
        request = Requests.newSimpleBindRequest(bindUserDN, bindUserCredentials.toCharArray());
        break;
    }

    return request;
  }
}
