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
package org.codice.ddf.test.common;

import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import org.codice.ddf.test.common.configurators.ApplicationOptions;
import org.codice.ddf.test.common.configurators.BundleOptionBuilder;
import org.codice.ddf.test.common.configurators.BundleOptionBuilder.BundleOption;
import org.codice.ddf.test.common.configurators.ContainerOptions;
import org.codice.ddf.test.common.configurators.KarafOptions;
import org.codice.ddf.test.common.configurators.PortFinder;
import org.codice.ddf.test.common.rules.TestFailureLogger;
import org.junit.Rule;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for component tests. Extend to provide and implementation for the {@link
 * #getApplicationOptions(PortFinder)} methods and the test methods.
 *
 * <p>It is important to remember that the {@link #config()} method and all the methods it calls,
 * i.e., {@link #getContainerOptions()} and {@link #getApplicationOptions(PortFinder)}, are called
 * inside the test runner process and are only used to configure the test container. All the other
 * methods in this class and its sub-classes will be run inside the test container, which is a
 * separate process.
 *
 * @see ApplicationOptions
 * @see ContainerOptions
 */
public abstract class AbstractComponentTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractComponentTest.class);

  // This port finder will only be available during Pax Exam's configuration phase, not inside the
  // test container and during the tests. If ports need to be assigned during testing, a separate
  // instance should be used.
  private static PortFinder portFinder;

  @Rule public TestFailureLogger testFailureLogger = new TestFailureLogger();

  /**
   * Configuration method called during Pax Exam configuration. The {@link Option}s returned will be
   * used by Pax Exam to configure the container that will be spawn and used to execute the tests.
   *
   * @return Pax Exam {@link Option} that will be used to configure the Pax Exam test container
   */
  @Configuration
  @SuppressWarnings("squid:S2696") // See comment below.
  public Option[] config() {
    LOGGER.trace("config");

    // Can't call close on PortFinder because the port range needs to remain locked until
    // all the tests have executed. The port range will be automatically released when the
    // test runner process exists. Variable also needs to be static to make sure the PortFinder
    // object doesn't get garbage collected between the time config is called and the tests are
    // run.
    portFinder = new PortFinder();

    return options(
        getContainerOptions().get(),
        getApplicationOptions(portFinder).get(),
        getTestBundleOptions().build(),
        editConfigurationFilePut(
            "etc/org.apache.karaf.features.cfg", "serviceRequirements", "disable"));
  }

  /**
   * Gets the object to use to configure the container.
   *
   * @return object that returns the container's configuration {@link Option}s. Default
   *     implementation returns {@link KarafOptions}.
   */
  protected ContainerOptions getContainerOptions() {
    return new KarafOptions(portFinder);
  }

  /**
   * Gets the object to use to configure the component or application inside the container.
   *
   * @return object that returns the application's configuration {@link Option}s
   * @param portFinder reference to the {@link PortFinder} to use during the test container
   *     configuration
   */
  protected abstract ApplicationOptions getApplicationOptions(PortFinder portFinder);

  private BundleOption getTestBundleOptions() {
    return BundleOptionBuilder.add("org.mockito", "mockito-core")
        .add("org.objenesis", "objenesis")
        .add("org.awaitility", "awaitility")
        .add("commons-io", "commons-io")
        .add("org.apache.commons", "commons-collections4")
        .add("org.apache.commons", "commons-lang3")
        .add("ddf.lib", "test-common")
        .add("ddf.lib", "common-system");
  }
}
