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
import org.codice.spock.extension.DeFinalizeWith;
import org.junit.runner.Describable;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.junit.runners.model.InitializationError;
import org.spockframework.runtime.Sputnik;
import spock.lang.Specification;

/**
 * The <code>Definalizer</code> test runner is designed as a generic proxy test runner for another
 * JUnit test runner by indirectly instantiating that runner in order to add support for
 * de-finalizing (i.e. removing the final constraint) 3rd party Java classes that needs to be mocked
 * or stubbed during testing. It does so by creating a classloader designed with an aggressive
 * strategy where it will load all classes first before delegating to its parent. This classloader
 * will therefore reload all classes while definalizing those that are requested except for all
 * classes in the following packages:
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
 * the real test runner. Even the actual test class will be reloaded in this internal classloader.
 *
 * <p>The indirect extension is done by means of delegation as the real test runner is instantiated
 * from within the classloader that is created internally. This is to ensure that everything the
 * test class, the test runner, and everything they indirectly reference are loaded from within the
 * classloader.
 *
 * <p>This runner is especially useful with Spock where it is not possible to mock final methods as
 * can be done with Mockito.
 *
 * <p>See <a href="https://github.com/spockframework/spock/issues/735"/>for a Spock enhancement
 * request to support mocking final classes/methods at which point, this class will no longer be
 * required.
 */
public class DeFinalizer extends Runner implements Describable, Filterable, Sortable {

  private static final List<Class<?>> SUPPORTED_INTERFACES =
      Arrays.asList(Describable.class, Filterable.class, Sortable.class);

  private final DeFinalizeClassLoader classloader;
  private final Class<?> testClass;
  private final Class<?> reloadedTestClass;
  private final Runner runner;
  private final Filterable filterable;
  private final Sortable sortable;

  /**
   * Creates an instance of this test runner for the specified Spock test specification class.
   *
   * @param testClass the test class
   * @throws InitializationError if unable to initialize the test runner
   */
  public DeFinalizer(Class<?> testClass) throws InitializationError {
    this.testClass = testClass;
    this.classloader = new DeFinalizeClassLoader(testClass);
    try {
      // reload the test class using the new classloader
      this.reloadedTestClass = classloader.loadClass(testClass.getName());
    } catch (ClassNotFoundException e) {
      throw new InitializationError(e);
    }
    this.runner = newTestRunner();
    if (runner instanceof Filterable) {
      this.filterable = (Filterable) runner;
    } else {
      this.filterable =
          new Filterable() {
            @Override
            public void filter(Filter filter) throws NoTestsRemainException { // nothing to filter
            }
          };
    }
    if (runner instanceof Sortable) {
      this.sortable = (Sortable) runner;
    } else {
      this.sortable = sorter -> {};
    }
  }

  @Override
  public int testCount() {
    return runner.testCount();
  }

  @Override
  public Description getDescription() {
    return runner.getDescription();
  }

  @Override
  public void run(RunNotifier notifier) {
    runner.run(notifier);
  }

  @Override
  public void filter(Filter filter) throws NoTestsRemainException {
    filterable.filter(filter);
  }

  @Override
  public void sort(Sorter sorter) {
    sortable.sort(sorter);
  }

  @SuppressWarnings("squid:S2259" /* runnerClass cannot be null */)
  private Runner newTestRunner() throws InitializationError {
    final DeFinalizeWith a = testClass.getAnnotation(DeFinalizeWith.class);
    Class<? extends Runner> runnerClass;

    if (a != null) {
      runnerClass = a.value();
    } else if (Specification.class.isAssignableFrom(testClass)) {
      runnerClass = Sputnik.class;
    } else {
      runnerClass = JUnit4.class;
    }
    // verify the test runner only implements the same interfaces as us
    // this is so we can catch new interfaces for which we would have to introduce delegate methods
    Class<?> clazz = runnerClass;

    while (clazz != null) {
      for (final Class<?> i : clazz.getInterfaces()) {
        if (!DeFinalizer.SUPPORTED_INTERFACES.contains(i)) {
          throw new InitializationError(
              runnerClass.getSimpleName()
                  + " implements new interface: "
                  + i.getName()
                  + "; DeFinalizer needs to be updated");
        }
      }
      clazz = clazz.getSuperclass();
    }
    try {
      return Runner.class.cast(
          classloader
              .loadClass(runnerClass.getName())
              .getDeclaredConstructor(Class.class)
              .newInstance(reloadedTestClass));
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
      } else if (t instanceof InitializationError) {
        throw (InitializationError) t;
      }
      throw new InitializationError(t);
    }
  }
}
