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
package org.codice.ddf.admin.application.service.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "profile", name = "list", description = "Lists available install profiles")
@Service
public class ProfileListCommand extends AbstractProfileCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProfileInstallCommand.class);

  @Override
  protected void doExecute(
      ApplicationService applicationService,
      FeaturesService featuresService,
      BundleService bundleService) {
    listProfiles(applicationService);

    if (profilePath.toFile().exists()) {
      listProfileFiles();
    }
  }

  private void listProfiles(ApplicationService applicationService) {
    printSectionHeading("Available Profiles:");
    applicationService.getInstallationProfiles().stream()
        .filter(feature -> !Objects.equals(feature.getInstall(), Feature.DEFAULT_INSTALL_MODE))
        .map(Feature::getName)
        .forEach(profile -> console.println(profile.replaceAll(".*-", "")));
  }

  private void listProfileFiles() {
    try (Stream<Path> files = Files.list(profilePath)) {
      files
          .filter(Files::isRegularFile)
          .filter(file -> file.toAbsolutePath().toString().endsWith(PROFILE_EXTENSION))
          .forEach(
              profile ->
                  console.println(FilenameUtils.removeExtension(profile.toFile().getName())));
    } catch (IOException e) {
      printError("Error occurred when locating profiles");
      LOGGER.error("An error occurred when locating profiles", e);
    }
  }
}
