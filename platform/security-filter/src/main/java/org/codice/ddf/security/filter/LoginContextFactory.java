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
package org.codice.ddf.security.filter;

import static org.codice.ddf.security.filter.SecurityFilter.DDF_REALM;

import java.security.PublicKey;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.apache.karaf.jaas.modules.publickey.PublickeyCallback;

public class LoginContextFactory {
  public LoginContext create(Subject subject, String username, Object identityProof)
      throws LoginException {
    return new LoginContext(
        DDF_REALM,
        subject,
        callbacks -> {
          for (Callback callback : callbacks) {
            if (callback instanceof NameCallback) {
              ((NameCallback) callback).setName(username);
            } else if (callback instanceof PasswordCallback) {
              if (identityProof instanceof String) {
                ((PasswordCallback) callback).setPassword(((String) identityProof).toCharArray());
              } else {
                throw new UnsupportedCallbackException(callback);
              }
            } else if (callback instanceof PublickeyCallback) {
              if (identityProof instanceof PublicKey) {
                ((PublickeyCallback) callback).setPublicKey((PublicKey) identityProof);
              } else {
                throw new UnsupportedCallbackException(callback);
              }
            } else {
              throw new UnsupportedCallbackException(callback);
            }
          }
        });
  }
}
