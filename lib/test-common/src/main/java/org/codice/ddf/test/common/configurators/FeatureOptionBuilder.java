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
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

import org.codice.ddf.test.common.DependencyVersionResolver;
import org.ops4j.pax.exam.Option;

/**
 * Builder class used to create Pax Exam options that will get Karaf features from a Maven
 * repository. To use, simply do {@code FeatureOptionBuilder.add(...).add(...).build()}.
 */
public class FeatureOptionBuilder {

  public static class FeatureOption {

    private Option options;

    private FeatureOption() {
      // Use FeatureOptionBuilder.add() to instantiate
    }

    /**
     * Adds a new {@link Option} for the features listed and contained in the features.xml file
     * identified by the Maven coordinates provided.
     *
     * @param groupId Maven group ID of the features.xml file
     * @param artifactId Maven artifact ID of the features.xml file
     * @param featureNames names of the features to install from the features.xml file identified by
     *     the Maven coordinates provided
     * @return this {@link FeatureOption}
     */
    public FeatureOption addFeatures(String groupId, String artifactId, String... featureNames) {
      options =
          composite(
              options,
              features(
                  maven(groupId, artifactId)
                      .version(DependencyVersionResolver.resolver())
                      .classifier("features")
                      .type("xml"),
                  featureNames));
      return this;
    }

    /**
     * Adds a new {@link Option} for the feature contained in the feature file identified by the
     * Maven coordinates and file name provided.
     *
     * @param groupId Maven group ID of the feature file
     * @param artifactId Maven artifact ID of the feature file
     * @param featureFileName name of the feature file without the extension
     * @param featureName names of the features to install from the feature file identified by the
     *     Maven coordinates and file name provided
     * @return this {@link FeatureOption}
     */
    public FeatureOption addFeatureFrom(
        String groupId, String artifactId, String featureFileName, String featureName) {
      options =
          composite(
              options,
              features(
                  maven(groupId, artifactId)
                      .version(DependencyVersionResolver.resolver())
                      .classifier(featureFileName)
                      .type("xml"),
                  featureName));
      return this;
    }

    /**
     * Builds the Pax Exam {@link Option} from the list of features added using {@link
     * #addFeatures(String, String, String...)}.
     *
     * @return option for the list of features added
     */
    public Option build() {
      return options;
    }
  }

  private FeatureOptionBuilder() {
    // Use static methods to instantiate
  }

  /**
   * Creates a new {@link FeatureOption} that contains the features listed and contained in the
   * features.xml file identified by the Maven coordinates provided.
   *
   * @param groupId Maven group ID of the features.xml file
   * @param artifactId Maven artifact ID of the features.xml file
   * @param featureNames names of features to install from the features.xml file identified by the
   *     Maven coordinates provided
   * @return this {@link FeatureOption}
   */
  public static FeatureOption addFeatures(
      String groupId, String artifactId, String... featureNames) {
    FeatureOption featureOption = new FeatureOption();
    return featureOption.addFeatures(groupId, artifactId, featureNames);
  }

  /**
   * Creates a new {@link FeatureOption} that contains the feature contained in the feature file
   * identified by the Maven coordinates and file name provided.
   *
   * @param groupId Maven group ID of the feature file
   * @param artifactId Maven artifact ID of the feature file
   * @param featureFileName name of the feature file without the extension
   * @param featureName names of the features to install from the feature file identified by the
   *     Maven coordinates and file name provided
   * @return this {@link FeatureOption}
   */
  public static FeatureOption addFeatureFrom(
      String groupId, String artifactId, String featureFileName, String featureName) {
    FeatureOption featureOption = new FeatureOption();
    return featureOption.addFeatureFrom(groupId, artifactId, featureFileName, featureName);
  }

  /**
   * Creates an empty {@link FeatureOption}.
   *
   * @return empty {@link FeatureOption}
   */
  public static FeatureOption empty() {
    return new FeatureOption();
  }
}
