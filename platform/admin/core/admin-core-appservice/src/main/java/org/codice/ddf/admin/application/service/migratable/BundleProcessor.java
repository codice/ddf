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

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.util.function.ThrowingRunnable;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class used to process bundles by comparing the exported version with the memory one and
 * determining if it should be started, stopped, installed, or uninstalled.
 */
public class BundleProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(BundleProcessor.class);

  /**
   * Gets all available bundles from memory.
   *
   * @param context the bundle context from which to retrieve bundles
   * @return an array of all available bundles
   * @throws MigrationException if an error occurs while retrieving the bundles from memory
   */
  public Bundle[] listBundles(BundleContext context) {
    final Bundle[] bundles = context.getBundles();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "Memory bundles: {}",
          Stream.of(bundles)
              .map(b -> String.format("%s (%s)", b, JsonBundle.getStateString(b)))
              .collect(Collectors.joining(", ")));
    }
    return bundles;
  }

  /**
   * Installs the specified bundle.
   *
   * @param context the bundle context to use for installing bundles
   * @param report the report where to record errors if unable to install the bundle
   * @param bundle the bundle to install
   * @return <code>true</code> if the bundle was installed successfully; <code>false</code>
   *     otherwise
   */
  public boolean installBundle(
      BundleContext context, ProfileMigrationReport report, Bundle bundle) {
    return run(
        report, bundle, Operation.INSTALL, () -> context.installBundle(bundle.getLocation()));
  }

  /**
   * Installs the specified bundle.
   *
   * @param context the bundle context to use for installing bundles
   * @param report the report where to record errors if unable to install the bundle
   * @param name the name of the bundle to install
   * @param location the location for the bundle to install
   * @return <code>true</code> if the bundle was installed successfully; <code>false</code>
   *     otherwise
   */
  public boolean installBundle(
      BundleContext context, ProfileMigrationReport report, String name, String location) {
    return run(
        report,
        name,
        JsonBundle.UNINSTALLED_STATE_STRING,
        Operation.INSTALL,
        () -> context.installBundle(location));
  }

  /**
   * Uninstalls the specified bundle.
   *
   * @param report the report where to record errors if unable to uninstall the bundle
   * @param bundle the bundle to uninstall
   * @return <code>true</code> if the bundle was uninstalled successfully; <code>false</code>
   *     otherwise
   */
  public boolean uninstallBundle(ProfileMigrationReport report, Bundle bundle) {
    return run(report, bundle, Operation.UNINSTALL, bundle::uninstall);
  }

  /**
   * Starts the specified bundle.
   *
   * @param report the report where to record errors if unable to start the bundle
   * @param bundle the bundle to start
   * @return <code>true</code> if the bundle was started successfully; <code>false</code> otherwise
   */
  public boolean startBundle(ProfileMigrationReport report, Bundle bundle) {
    return run(report, bundle, Operation.START, bundle::start);
  }

  /**
   * Stops the specified bundle.
   *
   * @param report the report where to record errors if unable to stop the bundle
   * @param bundle the bundle to stop
   * @return <code>true</code> if the bundle was stopped successfully; <code>false</code> otherwise
   */
  public boolean stopBundle(ProfileMigrationReport report, Bundle bundle) {
    return run(report, bundle, Operation.STOP, bundle::stop);
  }

  /**
   * Processes bundles by recording tasks to start, stop, install, or uninstall bundles that were
   * originally in the corresponding state.
   *
   * @param context the bundle context to use for managing bundles
   * @param jprofile the profile where to retrieve the set of bundles from the original system
   * @param tasks the task list where to record tasks to be executed
   */
  public void processBundlesAndPopulateTaskList(
      BundleContext context, JsonProfile jprofile, TaskList tasks) {
    LOGGER.debug("Processing bundles");
    final Map<String, Bundle> bundlesMap =
        Stream.of(listBundles(context))
            .collect(Collectors.toMap(JsonBundle::getFullName, Function.identity()));

    processExportedBundlesAndPopulateTaskList(context, jprofile, bundlesMap, tasks);
    processLeftoverBundlesAndPopulateTaskList(bundlesMap, tasks);
  }

  /**
   * Processes bundles that were exported by recording tasks to start, stop, install, or uninstall
   * bundles that were originally in the corresponding state.
   *
   * <p><i>Note:</i> Any bundles found in memory are removed from the provided map since they have
   * been processed.
   *
   * @param context the bundle context to use for managing memory bundles
   * @param jprofile the profile where to retrieve the set of bundles from the original system
   * @param bundles the set of bundles in memory
   * @param tasks the task list where to record tasks to be executed
   */
  public void processExportedBundlesAndPopulateTaskList(
      BundleContext context, JsonProfile jprofile, Map<String, Bundle> bundles, TaskList tasks) {
    LOGGER.debug("Processing exported bundles");
    jprofile
        .bundles()
        .forEach(
            jbundle ->
                processBundleAndPopulateTaskList(
                    context,
                    jbundle,
                    bundles.remove(jbundle.getFullName()), // remove from bundles when we find it
                    tasks));
  }

  /**
   * Processes bundles that were left over after having dealt with what was exported. The
   * implementation will try to uninstall all of them.
   *
   * @param bundles the set of bundles in memory that were not exported
   * @param tasks the task list where to record tasks to be executed
   */
  public void processLeftoverBundlesAndPopulateTaskList(
      Map<String, Bundle> bundles, TaskList tasks) {
    LOGGER.debug("Processing leftover bundles");
    for (final Bundle bundle : bundles.values()) {
      if (!processUninstalledBundleAndPopulateTaskList(bundle, tasks) && LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Skipping bundle '{}'; already {}",
            JsonBundle.getFullName(bundle),
            JsonBundle.getSimpleState(bundle));
      }
    }
  }

  /**
   * Processes the specified bundle by comparing its state in memory to the one from the original
   * system and determining if it needs to be uninstalled, installed, started, or stopped.
   *
   * <p><i>Note:</i> Stopped or installed are handled the same way. Started is handled as making the
   * bundle active.
   *
   * @param context the bundle context to use for installing bundles
   * @param jbundle the original bundle information
   * @param bundle the current bundle from memory or <code>null</code> if it is not installed
   * @param tasks the task list where to record tasks to be executed
   */
  public void processBundleAndPopulateTaskList(
      BundleContext context, JsonBundle jbundle, @Nullable Bundle bundle, TaskList tasks) {
    if (bundle == null) {
      processMissingBundleAndPopulateTaskList(context, jbundle, tasks);
    } else {
      switch (jbundle.getSimpleState()) {
        case UNINSTALLED:
          processUninstalledBundleAndPopulateTaskList(bundle, tasks);
          break;
        case ACTIVE:
          processActiveBundleAndPopulateTaskList(context, bundle, tasks);
          break;
        case INSTALLED:
        default: // assume any other states we don't know about is treated as if we should stop
          processInstalledBundleAndPopulateTaskList(context, bundle, tasks);
          break;
      }
    }
  }

  /**
   * Processes the specified bundle for installation since it was missing from memory.
   *
   * <p><i>Note:</i> A missing bundle will only be installed in this attempt which will not be
   * setting it to the state it was (unless it was uninstalled). The change from installed to
   * uninstalled or active will require another process attempt as we only want to deal with one
   * state change at a time. The next processing round will see the state of the bundle in memory as
   * installed instead of missing like it is right now such that it can then be finally uninstalled
   * or started as it was on the original system.
   *
   * @param context the bundle context to use for installing bundles
   * @param jbundle the original bundle information
   * @param tasks the task list where to record tasks to be executed
   */
  public void processMissingBundleAndPopulateTaskList(
      BundleContext context, JsonBundle jbundle, TaskList tasks) {
    // we need to force an install and on the next round, whatever it used to be.
    // Even if it was uninstall because we need to reserve its spot in the bundle order
    final String name = jbundle.getFullName();

    tasks.add(Operation.INSTALL, name, r -> installBundle(context, r, name, jbundle.getLocation()));
  }

  /**
   * Processes the specified bundle for uninstallation if the bundle in memory is not uninstalled.
   *
   * @param bundle the current bundle from memory
   * @param tasks the task list where to record tasks to be executed
   * @return <code>true</code> if processed; <code>false</code> otherwise
   */
  public boolean processUninstalledBundleAndPopulateTaskList(Bundle bundle, TaskList tasks) {
    final JsonBundle.SimpleState state = JsonBundle.getSimpleState(bundle);

    if (state != JsonBundle.SimpleState.UNINSTALLED) {
      final String name = JsonBundle.getFullName(bundle);

      tasks.add(Operation.UNINSTALL, name, r -> uninstallBundle(r, bundle));
      return true;
    }
    return false;
  }

  /**
   * Processes the specified bundle for activation if the bundle in memory is not installed or
   * active.
   *
   * <p><i>Note:</i> An uninstalled bundle will only be installed in this attempt which will not be
   * setting it to the active state. The change from installed to active will require another
   * process attempt as we only want to deal with one state change at a time. The next processing
   * round will see the state of the bundle in memory as installed instead of uninstalled like it is
   * right now such that it can then be finally started as it was on the original system.
   *
   * @param context the bundle context to use for installing bundles
   * @param bundle the current bundle from memory
   * @param tasks the task list where to record tasks to be executed
   */
  public void processActiveBundleAndPopulateTaskList(
      BundleContext context, Bundle bundle, TaskList tasks) {
    final JsonBundle.SimpleState state = JsonBundle.getSimpleState(bundle);
    final String name = JsonBundle.getFullName(bundle);

    if (state == JsonBundle.SimpleState.UNINSTALLED) {
      // we need to first install it and on the next round, start it
      tasks.add(Operation.INSTALL, name, r -> installBundle(context, r, bundle));
    } else if (state != JsonBundle.SimpleState.ACTIVE) {
      tasks.add(Operation.START, name, r -> startBundle(r, bundle));
    }
  }

  /**
   * Processes the specified bundle for installation if the bundle in memory is not uninstalled.
   *
   * <p><i>Note:</i> A bundle that is active in memory will need to be stopped in order to be in the
   * same installed state it was on the original system.
   *
   * @param context the bundle context to use for installing bundles
   * @param bundle the current bundle from memory
   * @param tasks the task list where to record tasks to be executed
   */
  public void processInstalledBundleAndPopulateTaskList(
      BundleContext context, Bundle bundle, TaskList tasks) {
    final JsonBundle.SimpleState state = JsonBundle.getSimpleState(bundle);
    final String name = JsonBundle.getFullName(bundle);

    if (state == JsonBundle.SimpleState.UNINSTALLED) {
      tasks.add(Operation.INSTALL, name, r -> installBundle(context, r, bundle));
    } else if (state == JsonBundle.SimpleState.ACTIVE) {
      tasks.add(Operation.STOP, name, r -> stopBundle(r, bundle));
    }
  }

  private boolean run(
      ProfileMigrationReport report,
      Bundle bundle,
      Operation operation,
      ThrowingRunnable<BundleException> task) {
    return run(
        report, JsonBundle.getFullName(bundle), JsonBundle.getStateString(bundle), operation, task);
  }

  private boolean run(
      ProfileMigrationReport report,
      String name,
      String state,
      Operation operation,
      ThrowingRunnable<BundleException> task) {
    final String attempt = report.getBundleAttemptString(operation, name);
    final String operating = operation.getOperatingName();

    LOGGER.debug("{} bundle '{}'{}", operating, name, attempt);
    report.record("%s bundle [%s]%s.", operating, name, attempt);
    try {
      task.run();
    } catch (IllegalStateException | BundleException | SecurityException e) {
      report.recordOnFinalAttempt(
          new MigrationException(
              "Import error: failed to %s bundle [%s] from state [%s]; %s.",
              operation.name().toLowerCase(), name, state, e));
      return false;
    }
    return true;
  }
}
