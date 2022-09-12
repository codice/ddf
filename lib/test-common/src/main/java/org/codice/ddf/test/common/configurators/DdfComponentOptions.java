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

import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;

import java.io.File;
import java.nio.file.Paths;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the basic DDF configuration {@link Option}s required to run component tests in Pax Exam.
 * The class uses the base Karaf distribution and will automatically start the bundle under test
 * (see note below). Any other bundles and features required to run the tests in the container can
 * be provided by extending this class and implementing {@link #getBundleOptions()} and {@link
 * #getFeatureOptions()}. Other configuration options can be provided by overwriting {@link
 * #getExtraOptions()}.
 *
 * <p><b>Important:</b> This configuration class uses the {@code karaf.version} system property to
 * determine the Karaf distribution version to deploy in the Pax Exam test container. It also uses
 * the {@code component.artifactId} and {@code component.version} system properties to determine the
 * name of the bundle under test and automatically start it. Those properties must therefore be set
 * before running the test class. Here's an example of how Maven's {@code failsafe} can be
 * configured to run the tests:
 *
 * <p>
 *
 * <pre>{@code
 * <plugin>
 *     <artifactId>maven-failsafe-plugin</artifactId>
 *     <version>${failsafe.version}</version>
 *     <configuration>
 *         <systemPropertyVariables>
 *             <pax.exam.karaf.version>${karaf.version}</pax.exam.karaf.version>
 *             <component.artifactId>${artifactId}</component.artifactId>
 *             <component.version>${project.version}</component.version>
 *         </systemPropertyVariables>
 *     </configuration>
 * </plugin>
 * }</pre>
 *
 * @see DdfBaseOptions
 * @see KarafOptions
 */
public class DdfComponentOptions extends DdfBaseOptions {

  private static final Logger LOGGER = LoggerFactory.getLogger(DdfComponentOptions.class);

  /**
   * Constructor.
   *
   * @param portFinder instance of the {@link PortFinder} to use to assign unique ports to the new
   *     Pax Exam container
   */
  public DdfComponentOptions(PortFinder portFinder) {
    super(portFinder);
  }

  @Override
  public Option get() {
    return composite(super.get(), getComponentUnderTestOptions());
  }

  @Override
  protected Option getDistributionOptions() {
    return karafDistributionConfiguration()
        .frameworkUrl(
            maven()
                .groupId("org.apache.karaf")
                .artifactId("apache-karaf")
                .version(getVersion())
                .type("zip"))
        .unpackDirectory(new File("target", "exam"))
        .useDeployFolder(false);
  }

  private String getVersion() {
    ConfigurationManager cm = new ConfigurationManager();
    return cm.getProperty("pax.exam.karaf.version");
  }

  private Option getComponentUnderTestOptions() {
    String componentArtifactId = System.getProperty("component.artifactId");
    String componentVersion = System.getProperty("component.version");
    String bundleJarName = String.format("%s-%s.jar", componentArtifactId, componentVersion);
    String bundleJarUrl =
        Paths.get(PathUtils.getBaseDir(), "target", bundleJarName).toUri().toString();

    LOGGER.info("Component under test artifact name: {}", bundleJarUrl);
    return bundle(bundleJarUrl).start();
  }
}
