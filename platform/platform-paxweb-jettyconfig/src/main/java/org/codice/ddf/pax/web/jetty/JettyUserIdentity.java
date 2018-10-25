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
package org.codice.ddf.pax.web.jetty;

import java.security.Principal;
import javax.annotation.Nullable;
import javax.security.auth.Subject;
import org.eclipse.jetty.server.UserIdentity;

public class JettyUserIdentity implements UserIdentity {

  private final Subject subject;

  public JettyUserIdentity(Subject subject) {
    this.subject = subject;
  }

  @Override
  public Subject getSubject() {
    return subject;
  }

  @Override
  @Nullable
  public Principal getUserPrincipal() {
    if (subject == null || subject.getPrincipals().isEmpty()) {
      return null;
    }
    return subject.getPrincipals().iterator().next();
  }

  @Override
  public boolean isUserInRole(String role, Scope scope) {
    return false;
  }
}
