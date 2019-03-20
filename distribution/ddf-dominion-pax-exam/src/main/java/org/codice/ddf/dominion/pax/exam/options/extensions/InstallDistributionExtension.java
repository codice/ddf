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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.dominion.commons.pax.exam.options.UsersAttributesFileSystemClaimsOption;
import org.codice.ddf.dominion.options.DDFOptions.InstallDistribution;
import org.codice.dominion.options.karaf.KarafOptions;
import org.codice.dominion.options.karaf.KarafOptions.DistributionConfiguration.Platform;
import org.codice.dominion.pax.exam.interpolate.PaxExamInterpolator;
import org.codice.dominion.pax.exam.options.KarafSshCommandOption;
import org.codice.dominion.resources.ResourceLoader;
import org.codice.maven.MavenUrl;
import org.ops4j.pax.exam.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension point for the {@link InstallDistribution} option annotation capable of installing DDF's
 * distribution.
 */
@KarafOptions.DistributionConfiguration(
  framework =
      @MavenUrl(
        groupId = "org.codice.ddf",
        artifactId = "ddf",
        version = MavenUrl.AS_IN_PROJECT,
        type = MavenUrl.AS_IN_PROJECT
      ),
  name = "ddf",
  platform = Platform.WINDOWS,
  start = "bin\\ddf.bat",
  executables = {"solr\\bin\\solr.cmd"}
)
@KarafOptions.DistributionConfiguration(
  framework =
      @MavenUrl(
        groupId = "org.codice.ddf",
        artifactId = "ddf",
        version = MavenUrl.AS_IN_PROJECT,
        type = MavenUrl.AS_IN_PROJECT
      ),
  name = "ddf",
  platform = Platform.UNIX,
  start = "bin/ddf",
  executables = {"solr/bin/solr", "solr/bin/oom_solr.sh"}
)
public class InstallDistributionExtension extends AbstractInstallExtension<InstallDistribution> {
  private static final Logger LOGGER = LoggerFactory.getLogger(InstallDistributionExtension.class);

  @Override
  public Option[] options(
      InstallDistribution annotation,
      PaxExamInterpolator interpolator,
      ResourceLoader resourceLoader) {
    return Stream.of(
            Stream.of(solrOption(annotation.solr())),
            InstallDistributionExtension.installProfileOptions(
                annotation.profile(), 3L, TimeUnit.MINUTES),
            InstallDistributionExtension.securityProfileOptions(
                interpolator, annotation.security()))
        .flatMap(Function.identity())
        .toArray(Option[]::new);
  }

  /**
   * Gets PaxExam options capable of installing a given DDF profile via the <code>profile:install
   * </code> Karaf shell command.
   *
   * @param profile the name of the profile to install
   * @param duration the amount of time in the specified unit to wait for the profile to be
   *     installed
   * @param unit the time unit for the time to wait for the profile to be installed
   * @return a corresponding stream of options
   */
  public static Stream<Option> installProfileOptions(String profile, long duration, TimeUnit unit) {
    return Stream.of(
        new KarafSshCommandOption("wfr", TimeUnit.SECONDS.toMillis(60)),
        new KarafSshCommandOption("profile:install " + profile, unit.toMillis(duration)) {
          @Override
          public String getCommand() {
            if (StringUtils.isNoneEmpty(profile)) {
              LOGGER.info("Installing '{}' profile", profile);
            }
            return super.getCommand();
          }
        });
  }

  public static Stream<Option> securityProfileOptions(
      PaxExamInterpolator interpolator, String profile) {
    return Stream.of(
        new UsersAttributesFileSystemClaimsOption(interpolator, profile) {
          @Override
          protected void update(Map<String, Map<String, Object>> claims) throws IOException {
            if (StringUtils.isNoneEmpty(profile)) {
              LOGGER.info("Activating '{}' security profile", profile);
            }
            super.update(claims);
          }
        });
  }
}
