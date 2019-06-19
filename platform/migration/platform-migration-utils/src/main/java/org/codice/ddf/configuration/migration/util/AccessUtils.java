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
package org.codice.ddf.configuration.migration.util;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import org.apache.commons.lang.Validate;
import org.codice.ddf.util.function.ThrowingRunnable;
import org.codice.ddf.util.function.ThrowingSupplier;

/** Utility classes to help with the access controller. */
public class AccessUtils {

  private static final String INVALID_NULL_ACTION = "invalid null action";

  /** Prevents instantiation. */
  private AccessUtils() {}

  /**
   * Performs the specified action with privileges enabled. The action is performed with <i>all</i>
   * of the permissions possessed by this class' (or by the migration framework's) protection
   * domain.
   *
   * <p>If the action's {@link ThrowingSupplier#get} method throws an <i>unchecked</i> exception, it
   * will propagate through this method.
   *
   * <p><i>Note:</i> Any DomainCombiner associated with the current AccessControlContext will be
   * ignored while the action is performed.
   *
   * @param <T> the type of the value returned by the action
   * @param <E> the type of exceptions thrown by the action
   * @param action the action to be performed
   * @return the value returned by the action's {@code run} method
   * @throws E if the specified action's threw the exception
   * @throws IllegalArgumentException if <code>action</code> is <code>null</code>
   */
  public static <T, E extends Exception> T doPrivileged(ThrowingSupplier<T, E> action) throws E {
    Validate.notNull(action, AccessUtils.INVALID_NULL_ACTION);
    try {
      return AccessController.doPrivileged(
          new PrivilegedExceptionAction<T>() {
            @Override
            public T run() throws Exception {
              return action.get();
            }
          });
    } catch (PrivilegedActionException pe) {
      final Exception e = pe.getException();

      if (e instanceof RuntimeException) { // should never happen but just to be safe!
        throw (RuntimeException) e;
      } else { // by design, the action is declared to only throw E
        throw (E) e;
      }
    }
  }

  /**
   * Performs the specified action with privileges enabled if the specified condition is <code>true
   * </code>. The action is performed with <i>all</i> of the permissions possessed by this class'
   * (or by the migration framework's) protection domain. Otherwise performs the specified action
   * using the caller's permissions.
   *
   * <p>If the action's {@link ThrowingSupplier#get} method throws an <i>unchecked</i> exception, it
   * will propagate through this method.
   *
   * <p><i>Note:</i> Any DomainCombiner associated with the current AccessControlContext will be
   * ignored while the action is performed.
   *
   * @param <T> the type of the value returned by the action
   * @param <E> the type of exceptions thrown by the action
   * @param condition <code>true</code> to perform the action with the permissions possessed by this
   *     class; <code>false</code> to use the caller's permissions
   * @param action the action to be performed
   * @return the value returned by the action's {@code run} method
   * @throws E if the specified action's threw the exception
   * @throws IllegalArgumentException if <code>action</code> is <code>null</code>
   */
  public static <T, E extends Exception> T doConditionallyPrivileged(
      boolean condition, ThrowingSupplier<T, E> action) throws E {
    if (condition) {
      return AccessUtils.doPrivileged(action);
    }
    Validate.notNull(action, AccessUtils.INVALID_NULL_ACTION);
    return action.get();
  }

  /**
   * Performs the specified action with privileges enabled. The action is performed with <i>all</i>
   * of the permissions possessed by this class' (or by the migration framework's) protection
   * domain.
   *
   * <p>If the action's {@link ThrowingSupplier#get} method throws an <i>unchecked</i> exception, it
   * will propagate through this method.
   *
   * <p><i>Note:</i> Any DomainCombiner associated with the current AccessControlContext will be
   * ignored while the action is performed.
   *
   * @param <E> the type of exceptions thrown by the action
   * @param action the action to be performed
   * @throws E if the specified action's threw the exception
   * @throws IllegalArgumentException if <code>action</code> is <code>null</code>
   */
  public static <E extends Exception> void doPrivileged(ThrowingRunnable<E> action) throws E {
    Validate.notNull(action, AccessUtils.INVALID_NULL_ACTION);
    try {
      AccessController.doPrivileged(
          new PrivilegedExceptionAction<Void>() {
            @Override
            public Void run() throws Exception {
              action.run();
              return null;
            }
          });
    } catch (PrivilegedActionException pe) {
      final Exception e = pe.getException();

      if (e instanceof RuntimeException) { // should never happen but just to be safe!
        throw (RuntimeException) e;
      } else { // by design, the action is declared to only throw E
        throw (E) e;
      }
    }
  }

  /**
   * Performs the specified action with privileges enabled if the specified condition is <code>true
   * </code>. The action is performed with <i>all</i> of the permissions possessed by this class'
   * (or by the migration framework's) protection domain. Otherwise performs the specified action
   * using the caller's permissions.
   *
   * <p>If the action's {@link ThrowingSupplier#get} method throws an <i>unchecked</i> exception, it
   * will propagate through this method.
   *
   * <p><i>Note:</i> Any DomainCombiner associated with the current AccessControlContext will be
   * ignored while the action is performed.
   *
   * @param <E> the type of exceptions thrown by the action
   * @param condition <code>true</code> to perform the action with the permissions possessed by this
   *     class; <code>false</code> to use the caller's permissions
   * @param action the action to be performed
   * @throws E if the specified action's threw the exception
   * @throws IllegalArgumentException if <code>action</code> is <code>null</code>
   */
  public static <E extends Exception> void doConditionallyPrivileged(
      boolean condition, ThrowingRunnable<E> action) throws E {
    if (condition) {
      AccessUtils.doPrivileged(action);
    } else {
      Validate.notNull(action, AccessUtils.INVALID_NULL_ACTION);
      action.run();
    }
  }
}
