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
package ddf.security.common.util;

import ddf.security.Subject;
import ddf.security.service.SecurityManager;

/** @deprecated As of release 2.9.0, replaced by {@link org.codice.ddf.security.common.Security} */
public class Security {

  /**
   * @deprecated As of release 2.9.0, replaced by {@link
   *     org.codice.ddf.security.common.Security#getSubject(String, String)}
   */
  public static Subject getSubject(String username, String password) {
    return org.codice.ddf.security.common.Security.getInstance().getSubject(username, password);
  }

  /**
   * @deprecated As of release 2.9.0, replaced by {@link
   *     org.codice.ddf.security.common.Security#tokenAboutToExpire(Subject)}
   */
  @Deprecated
  public static boolean tokenAboutToExpire(Subject subject) {
    return org.codice.ddf.security.common.Security.getInstance().tokenAboutToExpire(subject);
  }

  /**
   * @deprecated As of release 2.9.0, replaced by {@link
   *     org.codice.ddf.security.common.Security#javaSubjectHasAdminRole()}
   */
  @Deprecated
  public static boolean javaSubjectHasAdminRole() {
    return org.codice.ddf.security.common.Security.getInstance().javaSubjectHasAdminRole();
  }

  /**
   * @deprecated As of release 2.9.0, replaced by {@link
   *     org.codice.ddf.security.common.Security#getSystemSubject()}
   */
  @Deprecated
  public static synchronized Subject getSystemSubject() {
    return org.codice.ddf.security.common.Security.getInstance().getSystemSubject();
  }

  /**
   * @deprecated As of release 2.9.0, replaced by {@link
   *     org.codice.ddf.security.common.Security#getSecurityManager()}
   */
  @Deprecated
  public static SecurityManager getSecurityManager() {
    return org.codice.ddf.security.common.Security.getInstance().getSecurityManager();
  }
}
