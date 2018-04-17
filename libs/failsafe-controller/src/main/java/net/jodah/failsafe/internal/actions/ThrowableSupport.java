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
package net.jodah.failsafe.internal.actions;

import java.lang.reflect.Constructor;
import javax.annotation.Nullable;
import net.jodah.failsafe.FailsafeController;

public class ThrowableSupport {
  private ThrowableSupport() { // prevent instantiation
  }

  /**
   * Sneakily throw the specified exception in such a way that the JVM is tricked into letting it
   * bubble out even though the calling method doesn't declare it.
   *
   * @param <T> the type of exception to be thrown out
   * @param t the exception to be thrown out
   * @throws T always thrown as specified
   */
  static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
    throw (T) t;
  }

  /**
   * Attempts the instantiate the specified throwable class using either a constructor that takes a
   * message string, one that takes a message string and a cause, one that takes a cause, or the
   * default constructor.
   *
   * @param controller the controller for which we are instantiating an exception
   * @param clazz the class of throwable to instantiate
   * @return the newly created throwable
   * @throws IllegalArgumentException if unable to instantiate the throwable
   */
  static Throwable instantiate(FailsafeController<?> controller, Class<? extends Throwable> clazz) {
    final String id = controller.getId();

    try { // check if the class has a ctor with a string for the message
      final Constructor<? extends Throwable> ctor = clazz.getConstructor(String.class);

      return ctor.newInstance();
    } catch (Exception e) { // ignore and try the a ctor that takes a message and a cause
    }
    final Exception cause = new Exception(id);

    try {
      final Constructor<? extends Throwable> ctor =
          clazz.getConstructor(String.class, Exception.class);

      return ctor.newInstance(id, cause);
    } catch (Exception e) { // ignore and try with Throwable as the cause
    }
    try {
      final Constructor<? extends Throwable> ctor =
          clazz.getConstructor(String.class, Throwable.class);

      return ctor.newInstance(id, cause);
    } catch (Exception e) { // ignore and try with a ctor that takes a cause only
    }
    try {
      final Constructor<? extends Throwable> ctor = clazz.getConstructor(Exception.class);

      return ctor.newInstance(cause);
    } catch (Exception e) { // ignore and try with Throwable as the cause
    }
    try {
      final Constructor<? extends Throwable> ctor = clazz.getConstructor(Throwable.class);

      return ctor.newInstance(cause);
    } catch (Exception e) { // ignore and fallback to the default ctor
    }
    try {
      return clazz.newInstance();
    } catch (ExceptionInInitializerError e) {
      final Throwable c = e.getCause();

      throw new IllegalArgumentException(
          String.format("unable to instantiate %s for controller: '%s'", clazz.getName(), id),
          (c != null) ? c : e);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          String.format("unable to instantiate %s for controller: '%s'", clazz.getName(), id), e);
    }
  }

  /**
   * Attempts the instantiate the specified throwable class using either a constructor that takes a
   * message string, one that takes a message string and a cause, one that takes a cause, or the
   * default constructor if it corresponds to a throwable class otherwise simply return it as is.
   *
   * @param controller the controller for which we are instantiating an exception
   * @param o a throwable class to instantiate or any other object to return as is
   * @return the newly created throwable or <code>o</code> if it is not a throwable class
   * @throws IllegalArgumentException if unable to instantiate the throwable
   */
  static Object instantiateIfThrowableClass(FailsafeController<?> controller, @Nullable Object o) {
    if ((o instanceof Class) && Throwable.class.isAssignableFrom((Class<?>) o)) {
      return ThrowableSupport.instantiate(controller, (Class<? extends Throwable>) o);
    }
    return o;
  }
}
