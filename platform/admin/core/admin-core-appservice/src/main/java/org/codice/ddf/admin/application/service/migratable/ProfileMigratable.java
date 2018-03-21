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
package org.codice.ddf.admin.application.service.migratable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ExportMigrationContext;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.OptionalMigratable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class used to migrate the state of features and bundles. */
public class ProfileMigratable implements Migratable, OptionalMigratable {
  public static final String PROFILE_REQUIRED = "required";

  /**
   * Holds a retry count.
   *
   * <p>Because it was seen with Karaf that sometimes installing or starting a feature or a bundle
   * might have side effects and install or start others which were not supposed to in the original
   * system. As such, we are going to attempt multiple times for each operations such that we can
   * account for side effects of other operations. In addition, because we are unable to determine a
   * proper order to process all features or bundles, it is possible that we end up attempting to
   * install a feature for which the dependencies have not yet been installed. We are therefore
   * forced to attempt multiple times hoping that dependencies are properly being handled by the
   * previous pass. Failures will be reported only on the last attempt. Retries will automatically
   * stop when all features and bundles are determined to be as they were at export time.
   */
  @SuppressWarnings("PMD.DefaultPackage" /* designed to be used by TaskList within this package */)
  static final int ATTEMPT_COUNT = 5;

  private static final Logger LOGGER = LoggerFactory.getLogger(ProfileMigratable.class);

  /**
   * Holds the current export version.
   *
   * <p>1.0 - initial version
   */
  private static final String VERSION = "1.0";

  private static final Path PROFILE_PATH = Paths.get("profile.json");

  private final FeatureMigrator featureMigrator;

  private final BundleMigrator bundleMigrator;

  /**
   * Instantiates a profile migratable with the specified migrators.
   *
   * @param featureMigrator the feature migrator to use
   * @param bundleMigrator the bundle migrator to use
   * @throws IllegalArgumentException if <code>featureMigrator</code> or <code>bundleMigrator</code>
   *     is <code>null</code>
   */
  public ProfileMigratable(FeatureMigrator featureMigrator, BundleMigrator bundleMigrator) {
    Validate.notNull(featureMigrator, "invalid null feature migrator");
    Validate.notNull(bundleMigrator, "invalid null bundle migrator");
    this.featureMigrator = featureMigrator;
    this.bundleMigrator = bundleMigrator;
  }

  @Override
  public String getVersion() {
    return ProfileMigratable.VERSION;
  }

  @Override
  public String getId() {
    return "ddf.profile";
  }

  @Override
  public String getTitle() {
    return "Profile Migratable";
  }

  @Override
  public String getDescription() {
    return "Exports the state of features and bundles";
  }

  @Override
  public String getOrganization() {
    return "Codice";
  }

  @Override
  public void doExport(ExportMigrationContext context) {
    context
        .getEntry(ProfileMigratable.PROFILE_PATH)
        .store(
            (report, os) ->
                JsonUtils.writeValue(
                    os,
                    new JsonProfile(
                        featureMigrator.exportFeatures(), bundleMigrator.exportBundles())));
  }

  @Override
  public void doImport(ImportMigrationContext context) {
    try {
      context.getEntry(ProfileMigratable.PROFILE_PATH).restore(this::restore);
    } catch (MigrationException e) { // don't want to abort the import; just record the error
      context.getReport().record(e);
    }
  }

  private boolean restore(MigrationReport report, Optional<InputStream> ois) throws IOException {
    final InputStream is = ois.orElse(null);

    if (is == null) {
      throw new MigrationException("Import error: missing exported profile information.");
    }
    final JsonProfile jprofile =
        JsonUtils.fromJson(IOUtils.toString(is, StandardCharsets.UTF_8), JsonProfile.class);

    for (int i = 1; i < ProfileMigratable.ATTEMPT_COUNT; i++) {
      LOGGER.debug(
          "importing system profile (attempt {} out of {})", i, ProfileMigratable.ATTEMPT_COUNT);
      final Boolean result = restore(report, jprofile, false);

      if (result != null) {
        return result;
      }
    }
    LOGGER.debug(
        "verifying system profile",
        ProfileMigratable.ATTEMPT_COUNT,
        ProfileMigratable.ATTEMPT_COUNT);
    final Boolean result = restore(report, jprofile, true);

    if (result != null) {
      return result;
    }
    // if we get here then we had more tasks to execute after having tried so many times!!!
    throw new IllegalStateException("too many attempts to import profile");
  }

  /**
   * Performs a restore attempt.
   *
   * @param report the report where to record errors
   * @param jprofile the exported profile
   * @param finalAttempt whether this is a final attempt in which case we no longer suppress errors
   * @return <code>true</code> if the restore was successfull and didn't need to perform any
   *     operations; <code>false</code> if non-suppressed errors were recorded and we need to stop
   *     attempting; or <code>null</code> if we didn't fail but we had to perform some operations
   *     which typically would mean we need to retry
   */
  @SuppressWarnings(
      "squid:S2447" /* returning null was chosen to specially indicate a different result from this method than true or false. It is documented and only used internally.*/)
  private Boolean restore(MigrationReport report, JsonProfile jprofile, boolean finalAttempt) {
    final ProfileMigrationReport profileReport = new ProfileMigrationReport(report, finalAttempt);

    if (profileReport.wasSuccessful(
        () -> {
          if (bundleMigrator.importBundles(profileReport, jprofile)) {
            featureMigrator.importFeatures(profileReport, jprofile);
          }
        })) {

      if (profileReport.hasSuppressedErrors() || profileReport.hasRecordedTasks()) {
        // either we got some suppressed errors recorded or more tasks had to be executed
        // in any case, we got to either loop back or fail the final attempt
        return null;
      }
      // no tasks were recorded and no suppressed errors on this pass which means the verification
      // was perfect!
      return Boolean.TRUE;
    } else {
      // errors were recorded so bail out!!!!
      return Boolean.FALSE;
    }
  }
}
