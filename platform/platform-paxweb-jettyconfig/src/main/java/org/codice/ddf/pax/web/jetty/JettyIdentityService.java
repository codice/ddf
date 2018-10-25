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
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.RunAsToken;
import org.eclipse.jetty.server.UserIdentity;

public class JettyIdentityService implements IdentityService {

  @Override
  @Nullable
  public Object associate(@Nullable UserIdentity user) {
    if (user != null && user.getUserPrincipal() != null) {
      Subject subject = (Subject) user.getUserPrincipal();
      ThreadContext.bind(subject);
    }
    return null;
  }

  @Override
  public void disassociate(Object previous) {
    ThreadContext.unbindSubject();
  }

  @Override
  @Nullable
  public Object setRunAs(UserIdentity user, RunAsToken token) {
    return null;
  }

  @Override
  public void unsetRunAs(Object token) {
    // not needed
  }

  @Override
  public UserIdentity newUserIdentity(
      javax.security.auth.Subject subject, Principal userPrincipal, String[] roles) {
    return new JettyUserIdentity(subject);
  }

  @Override
  @Nullable
  public RunAsToken newRunAsToken(String runAsName) {
    return null;
  }

  @Override
  @Nullable
  public UserIdentity getSystemUserIdentity() {
    return null;
  }
}
