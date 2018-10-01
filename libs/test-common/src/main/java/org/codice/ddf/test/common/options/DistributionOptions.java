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
package org.codice.ddf.test.common.options;

import static org.codice.ddf.test.common.options.SystemProperties.SYSTEM_PROPERTIES_FILE_PATH;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.io.File;
import org.apache.commons.lang3.SystemUtils;
import org.codice.ddf.test.common.DependencyVersionResolver;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafDistributionBaseConfigurationOption;
import org.ops4j.pax.exam.karaf.options.KarafDistributionKitConfigurationOption;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;

/** Options for configuring a distribution for testing purposes. */
public class DistributionOptions extends BasicOptions {

  public static final File UNPACK_DIRECTORY = new File("target/exam");

  /**
   * Returns a kernel distribution configuration.
   *
   * @return
   */
  public static Option kernelDistributionOption(boolean startSolr) {
    MavenArtifactUrlReference mavenArtifactUrlReference =
        maven()
            .groupId("org.codice.ddf")
            .artifactId("kernel")
            .version(DependencyVersionResolver.resolver())
            .type("zip");
    KarafDistributionBaseConfigurationOption distroOption;

    if (SystemUtils.IS_OS_WINDOWS) {
      distroOption =
          new KarafDistributionKitConfigurationOption(
                  mavenArtifactUrlReference,
                  KarafDistributionKitConfigurationOption.Platform.WINDOWS)
              .executable("bin\\ddf.bat")
              .filesToMakeExecutable(
                  "bin\\ddfsolr.bat",
                  "bin\\karaf.bat",
                  "bin\\setenv.bat",
                  "bin\\start.bat",
                  "bin\\client.bat",
                  "bin\\shell.bat",
                  "bin\\stop.bat",
                  "solr\\bin\\solr",
                  "bin\\solr.bat")
              .unpackDirectory(UNPACK_DIRECTORY)
              .useDeployFolder(false);
    } else {
      distroOption =
          new KarafDistributionKitConfigurationOption(
                  mavenArtifactUrlReference, KarafDistributionKitConfigurationOption.Platform.NIX)
              .executable("bin/ddf")
              .filesToMakeExecutable(
                  "bin/karaf",
                  "bin/setenv",
                  "bin/start",
                  "bin/client",
                  "bin/shell",
                  "bin/stop",
                  "bin/solr",
                  "solr/bin/solr")
              .unpackDirectory(UNPACK_DIRECTORY)
              .useDeployFolder(false);
    }
    return new DefaultCompositeOption(
        distroOption,
        mavenRepos(),
        editConfigurationFilePut(
            SYSTEM_PROPERTIES_FILE_PATH, "start.solr", Boolean.toString(startSolr)));
  }

  public static Option kernelDistributionOption() {
    return kernelDistributionOption(false);
  }

  // Required so pax-exam can include it's own pax-exam related artifacts during test run time.
  private static Option mavenRepos() {
    return editConfigurationFilePut(
        "etc/org.ops4j.pax.url.mvn.cfg",
        "org.ops4j.pax.url.mvn.repositories",
        "http://repo1.maven.org/maven2@id=central,"
            + "http://oss.sonatype.org/content/repositories/snapshots@snapshots@noreleases@id=sonatype-snapshot,"
            + "http://oss.sonatype.org/content/repositories/ops4j-snapshots@snapshots@noreleases@id=ops4j-snapshot,"
            + "http://repository.apache.org/content/groups/snapshots-group@snapshots@noreleases@id=apache,"
            + "http://svn.apache.org/repos/asf/servicemix/m2-repo@id=servicemix,"
            + "http://repository.springsource.com/maven/bundles/release@id=springsource,"
            + "http://repository.springsource.com/maven/bundles/external@id=springsourceext,"
            + "http://oss.sonatype.org/content/repositories/releases/@id=sonatype");
  }
}
