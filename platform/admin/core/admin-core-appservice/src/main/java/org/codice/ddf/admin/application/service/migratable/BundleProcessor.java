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

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
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
      LOGGER.debug("Memory bundles: {}", Arrays.toString(bundles));
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
   * @param report the report where to record errors
   * @param jprofile the profile where to retrieve the set of bundles from the original system
   * @param tasks the task list where to record tasks to be executed
   * @return <code>false</code> if we failed to retrieve from memory a left over bundle; <code>
   *     true</code> if everything was ok
   */
  public boolean processBundles(
      BundleContext context, ProfileMigrationReport report, JsonProfile jprofile, TaskList tasks) {
    LOGGER.debug("Processing bundles import");
    final Map<String, JsonBundle> jbundlesMap =
        jprofile
            .bundles()
            .collect( // linked map to preserve order
                org.codice.ddf.admin.application.service.migratable.Collectors.toLinkedMap(
                    JsonBundle::getFullName, Function.identity()));

    processMemoryBundles(context, jbundlesMap, tasks);
    return processLeftoverExportedBundles(context, report, jbundlesMap, tasks);
  }

  /**
   * Processes bundles in memory by recording tasks to start, stop, install, or uninstall bundles
   * that were originally in the corresponding state.
   *
   * <p><i>Note:</i> Any bundles found in memory are removed from the provided map since they have
   * been processed.
   *
   * @param context the bundle context to use for managing memory bundles
   * @param jbundles the set of bundles on the original system
   * @param tasks the task list where to record tasks to be executed
   */
  public void processMemoryBundles(
      BundleContext context, Map<String, JsonBundle> jbundles, TaskList tasks) {
    LOGGER.debug("Processing bundles defined in memory");
    for (final Bundle bundle : listBundles(context)) {
      final String name = JsonBundle.getFullName(bundle);
      final JsonBundle jbundle = jbundles.remove(name); // remove from jbundles when we find it

      processBundle(context, jbundle, bundle, tasks);
    }
  }

  /**
   * Processes bundles that were left over after having dealt with what is in memory. The
   * implementation will try to find the missing bundles (although unlikely to be found). If found,
   * they will be processed by recording tasks to start, stop, install, or uninstall them if they
   * were originally in the corresponding state.
   *
   * @param context the bundle context to use for managing memory bundles
   * @param report the report where to record errors
   * @param jbundles the set of bundles on the original system that were not yet processed from
   *     memory
   * @param tasks the task list where to record tasks to be executed
   * @return <code>false</code> if we failed to retrieve from memory a left over bundle; <code>
   *     true</code> if everything was ok
   */
  public boolean processLeftoverExportedBundles(
      BundleContext context,
      ProfileMigrationReport report,
      Map<String, JsonBundle> jbundles,
      TaskList tasks) {
    LOGGER.debug("Processing leftover exported bundles");
    boolean allProcessed = true; // until proven otherwise

    // check if there are anything left from the exported info that should be installed, started,
    // stopped, or that is not installed
    for (final JsonBundle jbundle : jbundles.values()) {
      final String name = jbundle.getFullName();

      if (jbundle.getSimpleState() != JsonBundle.SimpleState.UNINSTALLED) {
        final Bundle bundle = context.getBundle(jbundle.getLocation());

        if (bundle == null) {
          LOGGER.debug("Installing bundle '{}'; bundle not available", name);
          report.record(
              new MigrationException(
                  "Import error: failed to retrieve bundle [%s]; bundle not found.", name));
          allProcessed = false;
        } else {
          processBundle(context, jbundle, bundle, tasks);
        }
      } else {
        LOGGER.debug("Skipping bundle '{}'; already uninstalled", name);
      }
    }
    return allProcessed;
  }

  /**
   * Processes the specified bundle by comparing its state in memory to the one from the original
   * system and determining if it needs to be uninstalled, installed, started, or stopped.
   *
   * <p><i>Note:</i> Stopped or installed are handled the same way. Started is handled as making the
   * bundle active.
   *
   * @param context the bundle context to use for installing bundles
   * @param jbundle the original bundle information or <code>null</code> if it was not installed
   * @param bundle the current bundle from memory
   * @param tasks the task list where to record tasks to be executed
   */
  public void processBundle(
      BundleContext context, @Nullable JsonBundle jbundle, Bundle bundle, TaskList tasks) {
    final String name = JsonBundle.getFullName(bundle);
    final JsonBundle.SimpleState state = JsonBundle.getSimpleState(bundle);

    if (jbundle != null) {
      switch (jbundle.getSimpleState()) {
        case UNINSTALLED:
          processUninstalledBundle(bundle, name, state, tasks);
          break;
        case ACTIVE:
          processActiveBundle(context, bundle, name, state, tasks);
          break;
        case INSTALLED:
        default: // assume any other states we don't know about is treated as if we should stop
          processInstalledBundle(context, bundle, name, state, tasks);
          break;
      }
    } else if (!processUninstalledBundle(bundle, name, state, tasks)) {
      LOGGER.debug("Skipping bundle '{}'; already {}", name, state);
    }
  }

  /**
   * Processes the specified bundle for uninstallation if the bundle in memory is not uninstalled.
   *
   * @param bundle the current bundle from memory
   * @param name the bundle name
   * @param state the bundle state in memory
   * @param tasks the task list where to record tasks to be executed
   * @return <code>true</code> if processed; <code>false</code> otherwise
   */
  public boolean processUninstalledBundle(
      Bundle bundle, String name, JsonBundle.SimpleState state, TaskList tasks) {
    if (state != JsonBundle.SimpleState.UNINSTALLED) {
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
   * process attempt. Since we only want to deal with one state change at a time, we will purposely
   * increase the number of attempts left for starting such that another processing round can be
   * done at which point the state of the bundle in memory will be seen as installed instead of
   * uninstalled like it is right now such that it can then be finally started as it was on the
   * original system.
   *
   * @param context the bundle context to use for installing bundles
   * @param bundle the current bundle from memory
   * @param name the bundle name
   * @param state the bundle state in memory
   * @param tasks the task list where to record tasks to be executed
   */
  public void processActiveBundle(
      BundleContext context,
      Bundle bundle,
      String name,
      JsonBundle.SimpleState state,
      TaskList tasks) {
    if (state == JsonBundle.SimpleState.UNINSTALLED) { // install it first
      // we need to first install it and on the next round, start it
      // as such, let's make sure to increase the number of attempts left for the start operation
      tasks.add(Operation.INSTALL, name, r -> installBundle(context, r, bundle));
      tasks.increaseAttemptsFor(Operation.START);
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
   * @param name the bundle name
   * @param state the bundle state in memory
   * @param tasks the task list where to record tasks to be executed
   */
  public void processInstalledBundle(
      BundleContext context,
      Bundle bundle,
      String name,
      JsonBundle.SimpleState state,
      TaskList tasks) {
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
    final String name = JsonBundle.getFullName(bundle);
    final String attempt = report.getBundleAttemptString(operation, name);
    final String operating = operation.operatingName();

    LOGGER.debug("{} bundle '{}'{}", operating, name, attempt);
    report.record("%s bundle [%s]%s.", operating, name, attempt);
    try {
      task.run();
    } catch (IllegalStateException | BundleException | SecurityException e) {
      report.recordOnFinalAttempt(
          new MigrationException(
              "Import error: failed to %s bundle [%s]; %s.",
              operation.name().toLowerCase(), name, e));
      return false;
    }
    return true;
  }
}
