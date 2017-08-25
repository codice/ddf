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

import com.google.common.collect.ImmutableMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang.Validate;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureState;
import org.apache.karaf.features.FeaturesService;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.util.function.ThrowingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility classes used to process features by comparing the exported version with the memory one
 * and determining if it should be started, resolved, installed, or uninstalled.
 *
 * <p><i>Note:</i> A feature in the resolved state is considered to be stopped.
 */
public class FeatureProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProcessor.class);

  private static final EnumSet<FeaturesService.Option> NO_AUTO_REFRESH =
      EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles);

  private final FeaturesService service;

  /**
   * Constructs a new feature processor.
   *
   * @param service the features service to use
   * @throws IllegalArgumentException if <code>service</code> is <code>null</code>
   */
  public FeatureProcessor(FeaturesService service) {
    Validate.notNull(service, "invalid null features service");
    this.service = service;
  }

  /**
   * Gets a feature from memory if available.
   *
   * @param report the report where to record errors if the feature is not found
   * @param id the id of the feature to retrieve
   * @return the corresponding feature or <code>null</code> if it is not available
   */
  @Nullable
  public Feature getFeature(MigrationReport report, String id) {
    try {
      final Feature feature = service.getFeature(id);

      if (feature == null) {
        LOGGER.debug("Installing feature '{}'; feature not installed", id);
        report.record(
            new MigrationException(
                "Import error: failed to retrieve feature [%s]; feature not found.", id));
      }
      return feature;
    } catch (Exception e) {
      report.record(
          new MigrationException("Import error: failed to retrieve feature [%s]; %s.", id, e));
      return null;
    }
  }

  /**
   * Gets all available features from memory.
   *
   * @param operation the operation for which we are retrieving the features
   * @return an array of all available features
   * @throws MigrationException if an error occurs while retrieving the features from memory
   */
  public Feature[] listFeatures(String operation) {
    try {
      final Feature[] features = service.listFeatures();

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Memory features: {}",
            Stream.of(features)
                .map(f -> f.toString() + " [" + f.getStartLevel() + "]")
                .collect(java.util.stream.Collectors.joining(", ")));
      }
      return features;
    } catch (Exception e) {
      throw new MigrationException("%s error: failed to retrieve features; %s", operation, e);
    }
  }

  /**
   * Installs the specified feature.
   *
   * @param report the report where to record errors if unable to install the feature
   * @param feature the feature to install
   * @return <code>true</code> if the feature was installed successfully; <code>false</code>
   *     otherwise
   */
  public boolean installFeature(ProfileMigrationReport report, Feature feature) {
    return run(
        report,
        feature,
        Operation.INSTALL,
        id -> service.installFeature(id, FeatureProcessor.NO_AUTO_REFRESH));
  }

  /**
   * Uninstalls the specified feature.
   *
   * @param report the report where to record errors if unable to uninstall the feature
   * @param feature the feature to uninstall
   * @return <code>true</code> if the feature was uninstalled successfully; <code>false</code>
   *     otherwise
   */
  public boolean uninstallFeature(ProfileMigrationReport report, Feature feature) {
    return run(
        report,
        feature,
        Operation.UNINSTALL,
        id -> service.uninstallFeature(id, FeatureProcessor.NO_AUTO_REFRESH));
  }

  /**
   * Starts the specified feature.
   *
   * @param report the report where to record errors if unable to start the feature
   * @param feature the feature to start
   * @param region the region where the feature resides
   * @return <code>true</code> if the feature was started successfully; <code>false</code> otherwise
   */
  public boolean startFeature(ProfileMigrationReport report, Feature feature, String region) {
    return run(
        report,
        feature,
        Operation.START,
        id ->
            service.updateFeaturesState(
                ImmutableMap.of(region, ImmutableMap.of(id, FeatureState.Started)),
                FeatureProcessor.NO_AUTO_REFRESH));
  }

  /**
   * Stops the specified feature by moving it back to the resolved state.
   *
   * @param report the report where to record errors if unable to stop the feature
   * @param feature the feature to stop
   * @param region the region where the feature resides
   * @return <code>true</code> if the feature was stopped successfully; <code>false</code> otherwise
   */
  public boolean stopFeature(ProfileMigrationReport report, Feature feature, String region) {
    return run(
        report,
        feature,
        Operation.STOP,
        id ->
            service.updateFeaturesState(
                ImmutableMap.of(region, ImmutableMap.of(id, FeatureState.Resolved)),
                FeatureProcessor.NO_AUTO_REFRESH));
  }

  /**
   * Processes features by recording tasks to start, stop, install, or uninstall features that were
   * originally in the corresponding state.
   *
   * @param report the report where to record errors
   * @param jprofile the profile where to retrieve the set of features from the original system
   * @param tasks the task list where to record tasks to be executed
   * @return <code>false</code> if we failed to retrieve from memory a left over feature; <code>
   *     true</code> if everything was ok
   */
  public boolean processFeatures(
      ProfileMigrationReport report, JsonProfile jprofile, TaskList tasks) {
    LOGGER.debug("Processing features import");
    final Map<String, JsonFeature> jfeaturesMap =
        jprofile
            .features()
            .collect( // linked map to preserve order
                org.codice.ddf.admin.application.service.migratable.Collectors.toLinkedMap(
                    JsonFeature::getId, Function.identity()));

    processMemoryFeatures(jfeaturesMap, tasks);
    return processLeftoverExportedFeatures(report, jfeaturesMap, tasks);
  }

  /**
   * Processes features in memory by recording tasks to start, stop, install, or uninstall features
   * that were originally in the corresponding state.
   *
   * <p><i>Note:</i> Any features found in memory are removed from the provided map since they have
   * been processed.
   *
   * @param jfeatures the set of features on the original system
   * @param tasks the task list where to record tasks to be executed
   */
  public void processMemoryFeatures(Map<String, JsonFeature> jfeatures, TaskList tasks) {
    LOGGER.debug("Processing features defined in memory");
    final Feature[] features = listFeatures("Import");

    for (final Feature feature : features) {
      final JsonFeature jfeature =
          jfeatures.remove(feature.getId()); // remove from jfeatures when we find it

      processFeature(jfeature, feature, tasks);
    }
  }

  /**
   * Processes features that were left over after having dealt with what is in memory. The
   * implementation will try to find the missing features (although unlikely to be found). If found,
   * they will be processed by recording tasks to start, stop, install, or uninstall them if they
   * were originally in the corresponding state.
   *
   * @param report the report where to record errors
   * @param jfeatures the set of features on the original system that were not yet processed from
   *     memory
   * @param tasks the task list where to record tasks to be executed
   * @return <code>false</code> if we failed to retrieve from memory a left over feature; <code>
   *     true</code> if everything was ok
   */
  public boolean processLeftoverExportedFeatures(
      ProfileMigrationReport report, Map<String, JsonFeature> jfeatures, TaskList tasks) {
    LOGGER.debug("Processing leftover exported features");
    boolean allProcessed = true; // until proven otherwise

    // check if there are anything left from the exported info that should be installed, started,
    // stopped or that is not installed
    for (final JsonFeature jfeature : jfeatures.values()) {
      final String id = jfeature.getId();

      if (jfeature.getState() != FeatureState.Uninstalled) {
        final Feature feature = getFeature(report, id);

        if (feature == null) { // feature not available!!!
          allProcessed = false;
        } else {
          processFeature(jfeature, feature, tasks);
        }
      } else {
        LOGGER.debug("Skipping feature [{}]; already uninstalled", id);
      }
    }
    return allProcessed;
  }

  /**
   * Processes the specified feature by comparing its state in memory to the one from the original
   * system and determining if it needs to be uninstalled, installed, started, or stopped.
   *
   * @param jfeature the original feature information or <code>null</code> if it was not installed
   * @param feature the current feature from memory
   * @param tasks the task list where to record tasks to be executed
   */
  public void processFeature(@Nullable JsonFeature jfeature, Feature feature, TaskList tasks) {
    final String id = feature.getId();
    final FeatureState state = service.getState(id);

    if (jfeature != null) {
      switch (jfeature.getState()) {
        case Uninstalled:
          processUninstalledFeature(feature, id, state, tasks);
          break;
        case Installed:
          processInstalledFeature(feature, id, state, jfeature.getRegion(), tasks);
          break;
        case Started:
          processStartedFeature(feature, id, state, jfeature.getRegion(), tasks);
          break;
        case Resolved:
        default: // assume any other states we don't know about is treated as if we should stop
          processResolvedFeature(feature, id, state, jfeature.getRegion(), tasks);
          break;
      }
    } else if (state != FeatureState.Uninstalled) {
      tasks.add(Operation.UNINSTALL, id, r -> uninstallFeature(r, feature));
    } else {
      LOGGER.debug("Skipping feature '{}'; already {}", id, state);
    }
  }

  /**
   * Processes the specified feature for uninstallation if the feature in memory is not uninstalled.
   *
   * @param feature the current feature from memory
   * @param id the feature id
   * @param state the feature state in memory
   * @param tasks the task list where to record tasks to be executed
   * @return <code>true</code> if processed; <code>false</code> otherwise
   */
  public void processUninstalledFeature(
      Feature feature, String id, FeatureState state, TaskList tasks) {
    if (state != FeatureState.Uninstalled) {
      tasks.add(Operation.UNINSTALL, id, r -> uninstallFeature(r, feature));
    }
  }

  /**
   * Processes the specified feature for installation if the feature in memory is not uninstalled.
   *
   * <p><i>Note:</i> A feature that is started in memory will need to be stopped in order to be in
   * the same resolved state it was on the original system.
   *
   * @param feature the current feature from memory
   * @param id the feature id
   * @param state the feature state in memory
   * @param tasks the task list where to record tasks to be executed
   */
  public void processInstalledFeature(
      Feature feature, String id, FeatureState state, String region, TaskList tasks) {
    if (state == FeatureState.Uninstalled) {
      tasks.add(Operation.INSTALL, id, r -> installFeature(r, feature));
    } else if (state == FeatureState.Started) {
      tasks.add(Operation.STOP, id, r -> stopFeature(r, feature, region));
    }
  }

  /**
   * Processes the specified feature for resolution. If the feature was uninstalled, it will be
   * installed and then later stopped to get to the resolved state. Since we only want to deal with
   * one state change at a time, we will purposely increase the number of attempts left for stopping
   * such that another processing round can be done at which point the state of the feature in
   * memory will be seen as installed instead of uninstalled like it is right now such that it can
   * then be finally stopped to be moved into the resolved state as it was on the original system.
   * Otherwise, if it is not in the resolved state, it will simply be stopped.
   *
   * @param feature the current feature from memory
   * @param id the feature id
   * @param state the feature state in memory
   * @param region the region for the feature
   * @param tasks the task list where to record tasks to be executed
   */
  public void processResolvedFeature(
      Feature feature, String id, FeatureState state, String region, TaskList tasks) {
    if (state == FeatureState.Uninstalled) {
      // we need to first install it and on the next round, stop it
      // as such, let's make sure to increase the number of attempts left for the stop operation
      tasks.add(Operation.INSTALL, id, r -> installFeature(r, feature));
      tasks.increaseAttemptsFor(Operation.STOP);
    } else if (state != FeatureState.Resolved) {
      tasks.add(Operation.STOP, id, r -> stopFeature(r, feature, region));
    }
  }

  /**
   * Processes the specified feature for starting. If the feature was uninstalled, it will be
   * installed and then later started to get to the started state. Since we only want to deal with
   * one state change at a time, we will purposely increase the number of attempts left for starting
   * such that another processing round can be done at which point the state of the feature in
   * memory will be seen as installed instead of uninstalled like it is right now such that it can
   * then be finally started as it was on the original system. Otherwise, if it is not in the
   * started state, it will simply be started.
   *
   * @param feature the current feature from memory
   * @param id the feature id
   * @param state the feature state in memory
   * @param region the region for the feature
   * @param tasks the task list where to record tasks to be executed
   */
  public void processStartedFeature(
      Feature feature, String id, FeatureState state, String region, TaskList tasks) {
    if (state == FeatureState.Uninstalled) {
      // we need to first install it and on the next round, start it
      // as such, let's make sure to increase the number of attempts left for the start operation
      tasks.add(Operation.INSTALL, id, r -> installFeature(r, feature));
      tasks.increaseAttemptsFor(Operation.START);
    } else if (state != FeatureState.Started) {
      tasks.add(Operation.START, id, r -> startFeature(r, feature, region));
    }
  }

  private boolean run(
      ProfileMigrationReport report,
      Feature feature,
      Operation operation,
      ThrowingConsumer<String, Exception> task) {
    final String id = feature.getId();
    final String attempt = report.getFeatureAttemptString(operation, id);
    final String operating = operation.operatingName();

    LOGGER.debug("{} feature '{}'{}", operating, id, attempt);
    report.record("%s feature [%s]%s.", operating, id, attempt);
    try {
      task.accept(id);
    } catch (Exception e) {
      report.recordOnFinalAttempt(
          new MigrationException(
              "Import error: failed to %s feature [%s]; %s.",
              operation.name().toLowerCase(), id, e));
      return false;
    }
    return true;
  }
}
