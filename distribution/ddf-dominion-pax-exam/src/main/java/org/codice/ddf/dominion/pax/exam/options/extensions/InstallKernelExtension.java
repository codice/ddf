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
package org.codice.ddf.dominion.pax.exam.options.extensions;

import org.codice.ddf.dominion.options.DDFOptions.InstallKernel;
import org.codice.dominion.options.Options.MavenUrl;
import org.codice.dominion.options.karaf.KarafOptions;
import org.codice.dominion.options.karaf.KarafOptions.DistributionConfiguration.Platform;
import org.codice.dominion.pax.exam.interpolate.PaxExamInterpolator;
import org.codice.dominion.resources.ResourceLoader;
import org.ops4j.pax.exam.Option;

/**
 * Extension point for the {@link InstallKernel} option annotation capable of installing DDF's
 * kernel.
 */
@KarafOptions.DistributionConfiguration(
  framework =
      @MavenUrl(
        groupId = "org.codice.ddf",
        artifactId = "kernel",
        version = MavenUrl.AS_IN_PROJECT,
        type = MavenUrl.AS_IN_PROJECT
      ),
  unpack = "target/exam",
  platform = Platform.WINDOWS,
  start = "bin\\ddf.bat",
  executables = {"solr\\bin\\solr.cmd"}
)
@KarafOptions.DistributionConfiguration(
  framework =
      @MavenUrl(
        groupId = "org.codice.ddf",
        artifactId = "kernel",
        version = MavenUrl.AS_IN_PROJECT,
        type = MavenUrl.AS_IN_PROJECT
      ),
  unpack = "target/exam",
  platform = Platform.UNIX,
  start = "bin/ddf",
  executables = {"solr/bin/solr", "solr/bin/oom_solr.sh"}
)
public class InstallKernelExtension extends AbstractInstallExtension<InstallKernel> {
  @Override
  public Option[] options(
      InstallKernel annotation, PaxExamInterpolator interpolator, ResourceLoader resourceLoader) {
    return new Option[] {solrOption(annotation.solr())};
  }
}
