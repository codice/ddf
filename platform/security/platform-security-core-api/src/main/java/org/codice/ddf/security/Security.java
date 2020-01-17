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
package org.codice.ddf.security;

import ddf.security.Subject;
import ddf.security.service.SecurityServiceException;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyStore;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;

public interface Security {
  Subject getSubject(String username, String password, String ip);

  /**
   * Determines if the current Java {@link Subject} has the admin role.
   *
   * @return {@code true} if the Java {@link Subject} exists and has the admin role, {@code false}
   *     otherwise
   * @throws SecurityException if a security manager exists and the {@link
   *     javax.security.auth.AuthPermission AuthPermission("getSubject")} permission is not
   *     authorized
   */
  boolean javaSubjectHasAdminRole();

  /**
   * Runs the {@link Callable} in the current thread as the current security framework's {@link
   * Subject}. If the security framework's {@link Subject} is not currently set and the Java Subject
   * contains the admin role, elevates and runs the {@link Callable} as the system {@link Subject}.
   *
   * @param codeToRun code to run
   * @param <T> type of the returned value
   * @return value returned by the {@link Callable}
   * @throws SecurityServiceException if the current subject didn' have enough permissions to run
   *     the code
   * @throws SecurityException if a security manager exists and the {@link
   *     javax.security.auth.AuthPermission AuthPermission("getSystemSubject")} or {@link
   *     javax.security.auth.AuthPermission AuthPermission("getSubject")} permissions are not
   *     authorized
   * @throws InvocationTargetException wraps any exception thrown by {@link Callable#call()}. {@link
   *     Callable} exception can be retrieved using the {@link
   *     InvocationTargetException#getCause()}.
   */
  <T> T runWithSubjectOrElevate(Callable<T> codeToRun)
      throws SecurityServiceException, InvocationTargetException;

  /**
   * Gets the {@link Subject} associated with this system. Uses a cached subject since the subject
   * will not change between calls.
   *
   * @return system's {@link Subject} or {@code null} if unable to get the system's {@link Subject}
   * @throws SecurityException if a security manager exists and the {@link
   *     javax.security.auth.AuthPermission AuthPermission("getSystemSubject")} or {@link
   *     javax.security.auth.AuthPermission AuthPermission("getSubject")} permissions are not
   *     authorized
   */
  @Nullable
  Subject getSystemSubject();

  /**
   * Gets the guest {@link Subject} associated with the specified IP. Uses a cached subject when
   * possible since the subject will not change between calls.
   *
   * @return system's {@link Subject}
   */
  Subject getGuestSubject(String ipAddress);

  <T> T runAsAdmin(PrivilegedAction<T> action);

  <T> T runAsAdminWithException(PrivilegedExceptionAction<T> action)
      throws PrivilegedActionException;

  KeyStore getSystemKeyStore();
}
