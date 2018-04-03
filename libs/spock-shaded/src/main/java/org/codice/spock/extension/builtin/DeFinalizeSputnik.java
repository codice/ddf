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
package org.codice.spock.extension.builtin;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import org.junit.runner.Describable;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.spockframework.runtime.Sputnik;

/**
 * The <code>DefinalizeSputnik</code> test runner is designed to extend the {@link Sputnik} test
 * runner indirectly in order to add support for de-finalizing (i.e. removing the final constraint)
 * 3rd party Java classes that needs to be mocked or stubbed during testing. It does so by creating
 * a classloader designed with an aggressive strategy where it will load all classes first before
 * delegating to its parent. This classloader will therefore reload all classes while definalizing
 * those that are requested except for all classes in the following packages:
 *
 * <ul>
 *   <li>java
 *   <li>javax
 *   <li>sun
 *   <li>org.xml
 *   <li>org.junit
 * </ul>
 *
 * These packages are not being reloaded as they are required for this test runner to delegate to
 * the real {@link Sputnik} test runner. Even the Spock test specification class will be reloaded in
 * this internal classloader.
 *
 * <p>The indirect extension is done by means of delegation as the real Sputnik test runner is
 * instantiated from within the classloader that is created internally. This is to ensure that
 * everything Groovy and Spock and everything they indirectly reference are loaded from within the
 * classloader.
 *
 * <p>See <a href="https://github.com/spockframework/spock/issues/735"/>for a Spock enhancement
 * request to support mocking final classes/methods at which point, this class will no longer be
 * required.
 */
public class DeFinalizeSputnik extends Sputnik {

  private static final List<Class<?>> SUPPORTED_INTERFACES =
      Arrays.asList(Describable.class, Filterable.class, Sortable.class);

  private final DeFinalizeClassLoader classloader;
  private final Class<?> specClass;
  private final Runner sputnik;
  private final Filterable filterable;
  private final Sortable sortable;

  /**
   * Creates an instance of this test runner for the specified Spock test specification class.
   *
   * @param specClass the Spock test specification class
   * @throws InitializationError if unable to initialize the test runner
   */
  public DeFinalizeSputnik(Class<?> specClass) throws InitializationError {
    super(specClass);
    this.classloader = new DeFinalizeClassLoader(specClass);
    try {
      // reload the specification class using the new classloader
      this.specClass = classloader.loadClass(specClass.getName());
    } catch (ClassNotFoundException e) {
      throw new InitializationError(e);
    }
    this.sputnik = newSputnik();
    if (sputnik instanceof Filterable) {
      this.filterable = (Filterable) sputnik;
    } else {
      this.filterable =
          new Filterable() {
            @Override
            public void filter(Filter filter) throws NoTestsRemainException { // nothing to filter
            }
          };
    }
    if (sputnik instanceof Sortable) {
      this.sortable = (Sortable) sputnik;
    } else {
      this.sortable = sorter -> {};
    }
  }

  @Override
  public int testCount() {
    return sputnik.testCount();
  }

  @Override
  public Description getDescription() {
    return sputnik.getDescription();
  }

  @Override
  public void run(RunNotifier notifier) {
    sputnik.run(notifier);
  }

  @Override
  public void filter(Filter filter) throws NoTestsRemainException {
    filterable.filter(filter);
  }

  @Override
  public void sort(Sorter sorter) {
    sortable.sort(sorter);
  }

  private Runner newSputnik() throws InitializationError {
    // verify Sputnik only implements the same interfaces as us
    // this is so we can catch new interfaces for which we would have to introduce delegate methods
    Class<?> clazz = Sputnik.class;

    while (clazz != null) {
      for (final Class<?> i : clazz.getInterfaces()) {
        if (!DeFinalizeSputnik.SUPPORTED_INTERFACES.contains(i)) {
          throw new InitializationError(
              "Sputnik implements new interface: "
                  + i.getName()
                  + "; DeFinalizeSputnik needs to be updated");
        }
      }
      clazz = clazz.getSuperclass();
    }
    try {
      return Runner.class.cast(
          classloader
              .loadClass(Sputnik.class.getName())
              .getDeclaredConstructor(Class.class)
              .newInstance(specClass));
    } catch (InstantiationException
        | IllegalAccessException
        | NoSuchMethodException
        | ClassNotFoundException
        | ClassFormatError e) {
      throw new InitializationError(e);
    } catch (InvocationTargetException e) {
      final Throwable t = e.getTargetException();

      if (t instanceof Error) {
        throw (Error) t;
      } else if (t instanceof RuntimeException) {
        throw (RuntimeException) t;
      }
      throw new InitializationError(t);
    }
  }
}
