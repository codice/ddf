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

import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.useOwnExamBundlesStartLevel;

import org.codice.ddf.test.common.DependencyVersionResolver;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;

/**
 * Provides the configuration {@link Option}s required to run Pax Exam and Karaf. Application
 * specific configurations such as bundles and features should be configured using an implementation
 * of {@link ApplicationOptions}.
 *
 * <p>The following system properties can be used to customize the Karaf's runtime behavior:
 *
 * <ul>
 *   <li>{@code keepRuntimeFolder}: keeps the runtime folder after the tests complete
 *   <li>{@code isDebugEnabled}: enables the remote debug agent and port (5005)
 *   <li>{@code maven.repo.local}: sets the maven repository where local artifacts will be retrieved
 * </ul>
 *
 * @see ApplicationOptions
 */
public class KarafOptions implements ContainerOptions {

  private static final String SSH_PORT_KEY = "sshPort";

  private static final String RMI_SERVER_PORT_KEY = "rmiRegistryPort";

  private static final String RMI_REG_PORT_KEY = "rmiServerPort";

  private static final String DEBUG_ENABLED_PROPERTY = "isDebugEnabled";

  private final PortFinder portFinder;

  /**
   * Constructor.
   *
   * @param portFinder instance of the {@link PortFinder} to use to assign unique ports to the new
   *     Pax Exam container
   */
  public KarafOptions(PortFinder portFinder) {
    this.portFinder = portFinder;
  }

  @Override
  public Option get() {
    return composite(
        getVmOptions(),
        getPaxExamOptions(),
        getMavenRepositoryOptions(),
        getKarafOptions(),
        getPortConfigurationOptions(),
        getDebugOptions(),
        getKeepRuntimeFolderOptions(),
        getKarafFeatureOptions());
  }

  private Option getVmOptions() {
    return composite(
        vmOption("-Xmx2048M"),
        // Avoid tests stealing focus on OS X
        vmOption("-Djava.awt.headless=true"),
        vmOption("-Dfile.encoding=UTF8"));
  }

  private Option getPaxExamOptions() {
    return composite(
        useOwnExamBundlesStartLevel(100),
        cleanCaches(),
        logLevel().logLevel(LogLevelOption.LogLevel.WARN));
  }

  private Option getMavenRepositoryOptions() {
    return composite(
        editConfigurationFilePut(
            "etc/org.ops4j.pax.url.mvn.cfg",
            "org.ops4j.pax.url.mvn.repositories",
            "http://repo1.maven.org/maven2@id=central,"
                + "http://oss.sonatype.org/content/repositories/snapshots@snapshots@noreleases@id=sonatype-snapshot,"
                + "http://oss.sonatype.org/content/repositories/ops4j-snapshots@snapshots@noreleases@id=ops4j-snapshot,"
                + "http://repository.apache.org/content/groups/snapshots-group@snapshots@noreleases@id=apache,"
                + "http://svn.apache.org/repos/asf/servicemix/m2-repo@id=servicemix,"
                + "http://repository.springsource.com/maven/bundles/release@id=springsource,"
                + "http://repository.springsource.com/maven/bundles/external@id=springsourceext,"
                + "http://oss.sonatype.org/content/repositories/releases/@id=sonatype"),
        when(System.getProperty("maven.repo.local") != null)
            .useOptions(
                editConfigurationFilePut(
                    "etc/org.ops4j.pax.url.mvn.cfg",
                    "org.ops4j.pax.url.mvn.localRepository",
                    System.getProperty("maven.repo.local"))));
  }

  private Option getKarafOptions() {
    return composite(
        configureConsole().ignoreLocalConsole(),
        // Disables the periodic backups of .bundlefile as those backups serve no purpose
        // when running tests and appear to cause intermittent failures when the backup
        // thread attempts to create the backup before the exam bundle is completely
        // exploded.
        editConfigurationFilePut(
            "etc/custom.system.properties", "eclipse.enableStateSaver", Boolean.FALSE.toString()));
  }

  private Option getPortConfigurationOptions() {
    return composite(
        editConfigurationFilePut(
            "etc/org.apache.karaf.shell.cfg",
            SSH_PORT_KEY,
            portFinder.getPortAsString(SSH_PORT_KEY)),
        editConfigurationFilePut(
            "etc/org.apache.karaf.management.cfg",
            RMI_REG_PORT_KEY,
            portFinder.getPortAsString(RMI_REG_PORT_KEY)),
        editConfigurationFilePut(
            "etc/org.apache.karaf.management.cfg",
            RMI_SERVER_PORT_KEY,
            portFinder.getPortAsString(RMI_SERVER_PORT_KEY)));
  }

  private Option getDebugOptions() {
    return when(Boolean.getBoolean(DEBUG_ENABLED_PROPERTY)).useOptions(debugConfiguration());
  }

  private Option getKarafFeatureOptions() {
    return features(
        maven()
            .groupId("org.apache.karaf.features")
            .artifactId("standard")
            .version(DependencyVersionResolver.resolver())
            .classifier("features")
            .type("xml"),
        "standard");
  }

  private Option getKeepRuntimeFolderOptions() {
    return when(Boolean.getBoolean("keepRuntimeFolder")).useOptions(keepRuntimeFolder());
  }
}
