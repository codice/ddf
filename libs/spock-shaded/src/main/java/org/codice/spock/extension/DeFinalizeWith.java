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
package org.codice.spock.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.runner.Runner;

/**
 * This annotation is used to indicate the actual test runner that should be used to run the tests
 * whenever a class uses the {@link org.codice.spock.extension.builtin.DeFinalizer} test runner.
 *
 * <p>The standard JUnit's {@link org.junit.runners.JUnit4} test runner or Spock's {@link
 * org.spockframework.runtime.Sputnik} test runner will be used if the class or the Spock's
 * specification is not annotated with this annotation.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DeFinalizeWith {

  /** @return a runner class (must have a constructor that takes a single class to run) */
  public Class<? extends Runner> value();
}
