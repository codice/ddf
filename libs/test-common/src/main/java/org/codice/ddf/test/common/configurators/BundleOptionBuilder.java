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
package org.codice.ddf.test.common.configurators;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import org.codice.ddf.test.common.DependencyVersionResolver;
import org.ops4j.pax.exam.Option;

/**
 * Builder class used to create Pax Exam options that will get OSGi bundles from a Maven repository.
 * To use, simply do {@code BundleOptionBuilder.add(...).add(...).build()}.
 */
public class BundleOptionBuilder {

  /** {@link BundleOption} used to add a bundle {@link Option}. */
  public static class BundleOption {

    private Option options;

    private BundleOption() {
      // Use BundleOptionBuilder.add() to instantiate
    }

    /**
     * Adds a new {@link Option} for the bundle specified by the Maven coordinates provided.
     *
     * @param groupId Maven group ID of the bundle to add
     * @param artifactId Maven artifact ID of the bundle to add
     * @return this {@link BundleOption}
     */
    public BundleOption add(String groupId, String artifactId) {
      options =
          composite(
              options,
              mavenBundle(groupId, artifactId)
                  .version(DependencyVersionResolver.resolver())
                  .start());
      return this;
    }

    public BundleOption add(String groupId, String artifactId, String version) {
      options = composite(options, mavenBundle(groupId, artifactId).version(version).start());
      return this;
    }

    /**
     * Builds the Pax Exam {@link Option} from the list of OSGi bundles added using {@link
     * #add(String, String)}.
     *
     * @return option for the list of bundles added
     */
    public Option build() {
      return options;
    }
  }

  private BundleOptionBuilder() {
    // Cannot be instantiate
  }

  /**
   * Creates a new {@link BundleOption} that contains the bundle specified by the Maven coordinates
   * provided.
   *
   * @param groupId Maven group ID of the bundle to add
   * @param artifactId Maven artifact ID of the bundle to add
   * @return a new {@link BundleOption}
   */
  public static BundleOption add(String groupId, String artifactId) {
    BundleOption bundleOption = new BundleOption();
    return bundleOption.add(groupId, artifactId);
  }

  /**
   * Creates an empty {@link BundleOption}.
   *
   * @return empty {@link BundleOption}
   */
  public static BundleOption empty() {
    return new BundleOption();
  }
}
