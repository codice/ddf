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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang.Validate;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureState;
import org.apache.karaf.features.FeaturesService;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.util.function.ThrowingConsumer;
import org.codice.ddf.util.function.ThrowingRunnable;
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
                .map(
                    f ->
                        String.format(
                            "%s (%s/%s)",
                            f,
                            service.getState(f.getId()),
                            (service.isRequired(f) ? "required" : "not required")))
                .collect(java.util.stream.Collectors.joining(", ")));
      }
      return features;
    } catch (Exception e) {
      throw new MigrationException("%s error: failed to retrieve features; %s", operation, e);
    }
  }

  /**
   * Installs the specified set of features.
   *
   * @param report the report where to record errors if unable to install the features
   * @param jfeatures the features to install keyed by the region they should be installed in
   * @return <code>true</code> if the features were installed successfully; <code>false</code>
   *     otherwise
   */
  public boolean installFeatures(
      ProfileMigrationReport report, Map<String, Set<JsonFeature>> jfeatures) {
    return jfeatures.entrySet().stream()
        .allMatch(e -> installFeatures(report, e.getKey(), e.getValue()));
  }

  /**
   * Installs the specified set of features in the specified region.
   *
   * @param report the report where to record errors if unable to install the features
   * @param region the region where to install the features
   * @param jfeatures the features to install
   * @return <code>true</code> if the features were installed successfully; <code>false</code>
   *     otherwise
   */
  public boolean installFeatures(
      ProfileMigrationReport report, String region, Set<JsonFeature> jfeatures) {
    final Set<String> ids = jfeatures.stream().map(JsonFeature::getId).collect(Collectors.toSet());

    return run(
        report,
        region,
        ids.stream(),
        Operation.INSTALL,
        () -> service.installFeatures(ids, region, FeatureProcessor.NO_AUTO_REFRESH));
  }

  /**
   * Uninstalls the specified set of features.
   *
   * @param report the report where to record errors if unable to uninstall the features
   * @param features the features to install keyed by the region they should be uninstalled in
   * @return <code>true</code> if the features were uninstalled successfully; <code>false</code>
   *     otherwise
   */
  public boolean uninstallFeatures(
      ProfileMigrationReport report, Map<String, Set<Feature>> features) {
    return features.entrySet().stream()
        .allMatch(e -> uninstallFeatures(report, e.getKey(), e.getValue()));
  }

  /**
   * Uninstalls the specified set of features in the specified region.
   *
   * @param report the report where to record errors if unable to uninstall the features
   * @param region the region where to uninstall the features
   * @param features the features to uninstall
   * @return <code>true</code> if the features were uninstalled successfully; <code>false</code>
   *     otherwise
   */
  public boolean uninstallFeatures(
      ProfileMigrationReport report, String region, Set<Feature> features) {
    // ----------------------------------------------------------------------------------------
    // to work around a bug in Karaf where for whatever reasons, it checks if a feature is
    // required to determine if it is installed and therefore can be uninstalled, we will first
    // go through and mark them all required
    //
    // see Karaf class: org.apache.karaf.features.internal.service.FeaturesServiceImpl.java
    // in the uninstallFeature() method where it checks the requirements table and if not found
    // in there, it later reports it is not installed even though it is actually installed
    //
    // Update: turns out that what Karaf means by install is "require this feature" and by uninstall
    // is "no longer require this feature". That being said, the uninstallFeature() does a bit more
    // than simply marking the feature no longer required since it actually also removes all traces
    // of the feature internally. So to properly remove a feature, it has to first be marked
    // required. which can technically be done either by updating the requirements for the feature
    // (as done below) or actually calling installFeature() again. We opted for the former since we
    // already deal with updating requirements to match the 'required' state of the feature from
    // the original system
    // ----------------------------------------------------------------------------------------
    final Set<String> ids = features.stream().map(Feature::getId).collect(Collectors.toSet());
    final Set<String> requirements =
        features.stream().map(JsonFeature::toRequirement).collect(Collectors.toSet());
    return run(
        report,
        region,
        ids.stream(),
        Operation.UNINSTALL,
        () -> // first make sure to mark all of them required as uninstallFeatures() only works on
            // required features and goes beyond simply marking them not required
            service.addRequirements(
                ImmutableMap.of(region, requirements), FeatureProcessor.NO_AUTO_REFRESH),
        () -> service.uninstallFeatures(ids, region, FeatureProcessor.NO_AUTO_REFRESH));
  }

  /**
   * Updates the specified features requirements to mark them required or not.
   *
   * @param report the report where to record errors if unable to update the features
   * @param jfeatures the features to update keyed by the region they should be updated in
   * @return <code>true</code> if the features were updated successfully; <code>false</code>
   *     otherwise
   */
  public boolean updateFeaturesRequirements(
      ProfileMigrationReport report, Map<String, Set<JsonFeature>> jfeatures) {
    return jfeatures.entrySet().stream()
        .allMatch(e -> updateFeaturesRequirements(report, e.getKey(), e.getValue()));
  }

  /**
   * Updates the specified features requirements to mark them required or not.
   *
   * @param report the report where to record errors if unable to update the features
   * @param region the region where to update the features
   * @param jfeatures the features to update
   * @return <code>true</code> if the features were updated successfully; <code>false</code>
   *     otherwise
   */
  public boolean updateFeaturesRequirements(
      ProfileMigrationReport report, String region, Set<JsonFeature> jfeatures) {
    return run(
        report,
        region,
        jfeatures.stream().map(JsonFeature::getId),
        Operation.UPDATE,
        jfeatures.stream()
            .collect(
                Collectors.groupingBy(
                    JsonFeature::isRequired,
                    Collectors.mapping(JsonFeature::toRequirement, Collectors.toSet())))
            .entrySet()
            .stream()
            .map(requirementsToUpdate -> updateFeaturesRequirements(region, requirementsToUpdate))
            .toArray(ThrowingRunnable[]::new));
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
   * @param jprofile the profile where to retrieve the set of features from the original system
   * @param tasks the task list where to record tasks to be executed
   */
  public void processFeaturesAndPopulateTaskList(JsonProfile jprofile, TaskList tasks) {
    LOGGER.debug("Processing features");
    final Map<String, Feature> featuresMap =
        Stream.of(listFeatures("Import"))
            .collect(Collectors.toMap(Feature::getId, Function.identity()));

    processExportedFeaturesAndPopulateTaskList(jprofile, featuresMap, tasks);
    processLeftoverFeaturesAndPopulateTaskList(featuresMap, tasks);
  }

  /**
   * Processes exported features by recording tasks to start, stop, install, or uninstall features
   * that were originally in the corresponding state.
   *
   * <p><i>Note:</i> Any features found in memory are removed from the provided map since they have
   * been processed.
   *
   * @param jprofile the profile where to retrieve the set of features from the original system
   * @param features the set of features in memory
   * @param tasks the task list where to record tasks to be executed
   */
  public void processExportedFeaturesAndPopulateTaskList(
      JsonProfile jprofile, Map<String, Feature> features, TaskList tasks) {
    LOGGER.debug("Processing exported features");
    jprofile
        .features()
        .forEach(
            jfeature ->
                processFeatureAndPopulateTaskList(
                    jfeature,
                    features.remove(jfeature.getId()), // remove from features when we find it
                    tasks));
  }

  /**
   * Processes features that were left over from memory after having dealt with what was exported.
   * The implementation will try to uninstall all of them.
   *
   * @param features the set of features in memory that were not exported
   * @param tasks the task list where to record tasks to be executed
   */
  public void processLeftoverFeaturesAndPopulateTaskList(
      Map<String, Feature> features, TaskList tasks) {
    LOGGER.debug("Processing leftover features");
    for (final Feature feature : features.values()) {
      if (!processUninstalledFeatureAndPopulateTaskList(feature, tasks)
          && LOGGER.isDebugEnabled()) {
        LOGGER.debug("Skipping feature '{}'; already uninstalled", feature.getId());
      }
    }
  }

  /**
   * Processes the specified feature by comparing its state in memory to the one from the original
   * system and determining if it needs to be uninstalled, installed, started, or stopped.
   *
   * @param jfeature the original feature information
   * @param feature the current feature from memory or <code>null</code> if it is not installed
   * @param tasks the task list where to record tasks to be executed
   */
  public void processFeatureAndPopulateTaskList(
      JsonFeature jfeature, @Nullable Feature feature, TaskList tasks) {
    if (feature == null) {
      processMissingFeatureAndPopulateTaskList(jfeature, tasks);
    } else {
      switch (jfeature.getState()) {
        case Uninstalled:
          processUninstalledFeatureAndPopulateTaskList(feature, tasks);
          break;
        case Installed:
          processInstalledFeatureAndPopulateTaskList(jfeature, feature, tasks);
          break;
        case Started:
          processStartedFeatureAndPopulateTaskList(jfeature, feature, tasks);
          break;
        case Resolved:
        default: // assume any other states we don't know about is treated as if we should stop
          processResolvedFeatureAndPopulateTaskList(jfeature, feature, tasks);
          break;
      }
    }
  }

  /**
   * Processes the specified feature for installation since it was missing from memory.
   *
   * @param jfeature the original feature information
   * @param tasks the task list where to record tasks to be executed
   */
  public void processMissingFeatureAndPopulateTaskList(JsonFeature jfeature, TaskList tasks) {
    if (jfeature.getState() != FeatureState.Uninstalled) {
      addCompoundInstallTaskFor(jfeature, tasks);
    }
  }

  /**
   * Processes the specified feature for uninstallation if the feature in memory is not uninstalled.
   *
   * @param feature the current feature from memory
   * @param tasks the task list where to record tasks to be executed
   * @return <code>true</code> if processed; <code>false</code> otherwise
   */
  public boolean processUninstalledFeatureAndPopulateTaskList(Feature feature, TaskList tasks) {
    final String id = feature.getId();
    final FeatureState state = service.getState(id);

    if (state != FeatureState.Uninstalled) {
      addCompoundUninstallTaskFor(feature, tasks);
      return true;
    }
    return false;
  }

  /**
   * Processes the specified feature for installation if the feature in memory is not uninstalled.
   *
   * <p><i>Note:</i> A feature that is started in memory will need to be stopped in order to be in
   * the same resolved state it was on the original system.
   *
   * @param jfeature the original feature information
   * @param feature the current feature from memory
   * @param tasks the task list where to record tasks to be executed
   */
  public void processInstalledFeatureAndPopulateTaskList(
      JsonFeature jfeature, Feature feature, TaskList tasks) {
    final String id = feature.getId();
    final FeatureState state = service.getState(id);

    if (state == FeatureState.Uninstalled) {
      addCompoundInstallTaskFor(jfeature, tasks);
      return;
    } else if (state == FeatureState.Started) {
      // should use feature's region but we cannot figure out how to get it
      tasks.add(Operation.STOP, id, r -> stopFeature(r, feature, jfeature.getRegion()));
    }
    if (jfeature.isRequired() != service.isRequired(feature)) {
      addCompoundUpdateTaskFor(jfeature, feature, tasks);
    }
  }

  /**
   * Processes the specified feature for resolution.
   *
   * <p><i>Note:</i> If the feature was uninstalled, it will be installed and then later stopped to
   * get to the resolved state. The change from installed to stopped will require another process
   * attempt as we only want to deal with one state change at a time. The next processing round will
   * see the state of the feature in memory as installed instead of uninstalled like it is right now
   * such that it can then be finally stopped to be moved into the resolved state as it was on the
   * original system. Otherwise, if it is not in the resolved state, it will simply be stopped.
   *
   * @param jfeature the original feature information
   * @param feature the current feature from memory
   * @param tasks the task list where to record tasks to be executed
   */
  public void processResolvedFeatureAndPopulateTaskList(
      JsonFeature jfeature, Feature feature, TaskList tasks) {
    final String id = feature.getId();
    final FeatureState state = service.getState(id);

    if (state == FeatureState.Uninstalled) {
      // we need to first install it and on the next round, stop it
      addCompoundInstallTaskFor(jfeature, tasks);
      return;
    } else if (state != FeatureState.Resolved) {
      // should use feature's region but we cannot figure out how to get it
      tasks.add(Operation.STOP, id, r -> stopFeature(r, feature, jfeature.getRegion()));
    }
    if (jfeature.isRequired() != service.isRequired(feature)) {
      addCompoundUpdateTaskFor(jfeature, feature, tasks);
    }
  }

  /**
   * Processes the specified feature for starting.
   *
   * <p><i>Note:</i> If the feature was uninstalled, it will be installed and then later started to
   * get to the started state. The change from installed to started will require another process
   * attempt as we only want to deal with one state change at a time. The next processing round will
   * see the state of the feature in memory as installed instead of uninstalled like it is right
   * such that it can be finally started as it was on the original system. Otherwise, if it is not
   * in the started state, it will simply be started.
   *
   * @param jfeature the original feature information
   * @param feature the current feature from memory
   * @param tasks the task list where to record tasks to be executed
   */
  public void processStartedFeatureAndPopulateTaskList(
      JsonFeature jfeature, Feature feature, TaskList tasks) {
    final String id = feature.getId();
    final FeatureState state = service.getState(id);

    if (state == FeatureState.Uninstalled) {
      // we need to first install it and on the next round, start it
      addCompoundInstallTaskFor(jfeature, tasks);
      return;
    } else if (state != FeatureState.Started) {
      // should use feature's region but we cannot figure out how to get it
      tasks.add(Operation.START, id, r -> startFeature(r, feature, jfeature.getRegion()));
    }
    if (jfeature.isRequired() != service.isRequired(feature)) {
      addCompoundUpdateTaskFor(jfeature, feature, tasks);
    }
  }

  private void addCompoundInstallTaskFor(JsonFeature jfeature, TaskList tasks) {
    // we shall verify if the installed feature is in the proper required state on a subsequent pass
    // and if not, update its requirements appropriately
    tasks
        .addIfAbsent(
            Operation.INSTALL,
            HashMap<String, Set<JsonFeature>>::new,
            (jfeatures, r) -> installFeatures(r, jfeatures))
        .add(
            jfeature.getId(),
            jfeatures ->
                jfeatures
                    .computeIfAbsent(jfeature.getRegion(), r -> new HashSet<>())
                    .add(jfeature));
  }

  private void addCompoundUninstallTaskFor(Feature feature, TaskList tasks) {
    // since we do not know how to get the region for a feature yet, use the default one: ROOT
    tasks
        .addIfAbsent(
            Operation.UNINSTALL,
            HashMap<String, Set<Feature>>::new,
            (features, r) -> uninstallFeatures(r, features))
        .add(
            feature.getId(),
            features ->
                features
                    .computeIfAbsent(FeaturesService.ROOT_REGION, r -> new HashSet<>())
                    .add(feature));
  }

  @SuppressWarnings({
    "squid:S1172", /* currently unused until we can figure out how to retrieve the region from a feature */
    "PMD.UnusedFormalParameter" /* currently unused until we can figure out how to retrieve the region from a feature */
  })
  private void addCompoundUpdateTaskFor(JsonFeature jfeature, Feature feature, TaskList tasks) {
    // should use feature's region but we cannot figure out how to get it
    tasks
        .addIfAbsent(
            Operation.UPDATE,
            HashMap<String, Set<JsonFeature>>::new,
            (jfeatures, r) -> updateFeaturesRequirements(r, jfeatures))
        .add(
            jfeature.getId(),
            jfeatures ->
                jfeatures
                    .computeIfAbsent(jfeature.getRegion(), r -> new HashSet<>())
                    .add(jfeature));
  }

  private ThrowingRunnable<Exception> updateFeaturesRequirements(
      String region, Map.Entry<Boolean, Set<String>> requirements) {
    if (requirements.getKey()) {
      return () ->
          service.addRequirements(
              ImmutableMap.of(region, requirements.getValue()), FeatureProcessor.NO_AUTO_REFRESH);
    } else {
      return () ->
          service.removeRequirements(
              ImmutableMap.of(region, requirements.getValue()), FeatureProcessor.NO_AUTO_REFRESH);
    }
  }

  private boolean run(
      ProfileMigrationReport report,
      Feature feature,
      Operation operation,
      ThrowingConsumer<String, Exception> task) {
    final String id = feature.getId();
    final String attempt = report.getFeatureAttemptString(operation, id);
    final String operating = operation.getOperatingName();

    LOGGER.debug("{} feature '{}'{}", operating, id, attempt);
    report.record("%s feature [%s]%s.", operating, id, attempt);
    try {
      task.accept(id);
    } catch (Exception e) {
      final String required = service.isRequired(feature) ? "required" : "not required";

      report.recordOnFinalAttempt(
          new MigrationException(
              "Import error: failed to %s feature [%s] from state [%s/%s]; %s.",
              operation.name().toLowerCase(), id, service.getState(id), required, e));
      return false;
    }
    return true;
  }

  private boolean run(
      ProfileMigrationReport report,
      String region,
      Stream<String> ids,
      Operation operation,
      ThrowingRunnable<Exception>... tasks) {
    final String attempt = report.getFeatureAttemptString(operation, region);
    final String operating = operation.getOperatingName();

    ids.forEach(
        id -> {
          LOGGER.debug("{} feature '{}'{}", operating, id, attempt);
          report.record("%s feature [%s]%s.", operating, id, attempt);
        });
    try {
      for (final ThrowingRunnable<Exception> task : tasks) {
        task.run();
      }
    } catch (Exception e) {
      report.recordOnFinalAttempt(
          new MigrationException(
              "Import error: failed to %s features for region [%s]; %s.",
              operation.name().toLowerCase(), region, e));
      return false;
    }
    return true;
  }
}
