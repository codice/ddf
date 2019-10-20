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
package org.codice.ddf.admin.configuration;

import static com.google.common.io.Files.getFileExtension;
import static java.lang.String.format;

import com.google.common.annotations.VisibleForTesting;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.encryption.EncryptionService;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.codice.ddf.admin.core.api.ConfigurationAdmin;
import org.codice.ddf.platform.io.internal.PersistenceStrategy;
import org.codice.felix.cm.internal.ConfigurationContext;
import org.codice.felix.cm.internal.ConfigurationPersistencePlugin;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Addresses shortcomings of the Felix {@code ConfigInstaller} with our own implementation that
 * properly updates the etc directory.
 *
 * <p>This configuration installer handles <b>writeback</b> like the Felix one did and does a few
 * other things:
 *
 * <ul>
 *   <li>Removes files from etc when they are deleted from {@link
 *       org.osgi.service.cm.ConfigurationAdmin}
 *   <li>Protects against tampering with the {@code felix.fileinstall.filename} by third parties,
 *       which could disrupt the dropping of config files
 *   <li>Ensures password fields in config files are encrypted
 * </ul>
 *
 * Handling <b>writeback</b> means running this class handles the updating of dropped-in config
 * files in the etc directory. To keep Felix itself from disrupting this process, {@code
 * enableConfigSave} in {@code custom.properties} should be set to {@code false}.
 */
public class ConfigurationUpdater implements ConfigurationPersistencePlugin {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationUpdater.class);

  private static final Pattern ENC_PATTERN = Pattern.compile("^ENC\\((.*)\\)$");

  static final String FELIX_FILENAME = "felix.fileinstall.filename";

  private final ConfigurationAdmin ddfConfigAdmin;

  private final List<PersistenceStrategy> strategies;

  private final EncryptionService encryptionService;

  private final Map<String, CachedConfigData> pidDataMap;

  /*
   * SortedServiceList implements java.util.List using generic parameter <T> and not a concrete type,
   * so the ctor cannot be matched by the blueprint container if we did List<PersistenceStrategy>
   *
   * See https://issues.apache.org/jira/browse/ARIES-960
   */
  public ConfigurationUpdater(
      ConfigurationAdmin ddfConfigAdmin, List strategies, EncryptionService encryptionService) {
    this.ddfConfigAdmin = ddfConfigAdmin;
    this.strategies = strategies;
    this.encryptionService = encryptionService;
    this.pidDataMap = new ConcurrentHashMap<>();
  }

  /** @return a read-only map of configuration pids to felix file install properties. */
  @VisibleForTesting
  Map<String, CachedConfigData> getPidDataMap() {
    return Collections.unmodifiableMap(pidDataMap);
  }

  /**
   * @inheritDoc Initialize the updater with all the currently known configuration.
   * @see ConfigurationPersistencePlugin
   */
  @Override
  public void initialize(Set<ConfigurationContext> state) {
    state.forEach(
        context -> {
          try {
            handleStore(context);
          } catch (IOException e) {
            LOGGER.error(
                "Problem updating config file [{}]. {}", context.getConfigFile(), e.getMessage());
            LOGGER.debug("Exception occurred while trying to update config file. ", e);
          }
        });
  }

  @Override
  public final void handleStore(ConfigurationContext context) throws IOException {
    try {
      AccessController.doPrivileged(
          (PrivilegedExceptionAction<Object>)
              () -> {
                doHandleStore(context);
                return null;
              });
    } catch (PrivilegedActionException e) {
      if (e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      }
      throw new IOException(e.getCause());
    }
  }

  /**
   * Sync the config files in etc when an update occurs. There are four cases, which will be denoted
   * using 2-tuple combinations of variables: [felixFileFromCache, felixFileFromConfig]
   *
   * <p>[null, null] (1) Config was created from config admin directly; not by dropping a file.
   * There is nothing to track.
   *
   * <p>[null, X] (2) Some config was just created by dropping a file into etc; track the file.
   * Update the properties on disk if necessary and perform any special first time processing.
   *
   * <p>[Y, null] [Y, X] (3) Felix prop was externally changed or removed. This is an error.
   *
   * <p>[Y, Y] (4) Felix file prop did not change, do nothing. This is the happy path. Properties
   * will be updated on disk if necessary.
   */
  private void doHandleStore(ConfigurationContext context) throws IOException {
    final String pid = context.getServicePid();
    final File fileFromConfigAdmin = context.getConfigFile();

    final CachedConfigData cachedConfigData = pidDataMap.get(pid);
    final File fileFromCache = (cachedConfigData != null) ? cachedConfigData.getFelixFile() : null;

    if (fileFromCache == null && fileFromConfigAdmin == null) {
      // This config doesn't have an etc file, so we ignore this case (1)
      LOGGER.debug("Was not tracked and will not track pid {}", pid);
      return;
    }

    final String appropriatePid =
        (context.getFactoryPid() == null) ? context.getServicePid() : context.getFactoryPid();

    if (fileFromCache == null) {
      // An etc config file was just dropped and we're seeing it for the first time (2)
      final CachedConfigData createdConfigData = new CachedConfigData(context);
      processUpdate(
          appropriatePid, fileFromConfigAdmin, context.getSanitizedProperties(), createdConfigData);
      if (fileFromConfigAdmin.exists()) {
        LOGGER.debug(
            "Tracking pid [{}] for installed configuration [{}]",
            pid,
            fileFromConfigAdmin.getAbsolutePath());
        pidDataMap.put(pid, createdConfigData);
      } else {
        LOGGER.debug(
            "Associated file [{}] for pid [{}] did not exist, will not track",
            fileFromConfigAdmin.getAbsolutePath(),
            pid);
      }
      return;
    }

    if (!Objects.equals(fileFromConfigAdmin, fileFromCache)) {
      if (fileFromConfigAdmin != null) {
        // The felix file prop changed, which is not allowed (3)
        String msg =
            format(
                "%s has been illegally changed from [%s] to [%s]",
                FELIX_FILENAME, fileFromCache, fileFromConfigAdmin);
        throw new IllegalStateException(msg);
      }
      if (fileFromCache.exists()) {
        // The felix file prop was removed, which we can revert if the file exists (3)
        // Should revert to URI form since Felix's ConfigInstaller uses that as a key
        final String revertedFilename = fileFromCache.getAbsoluteFile().toURI().toString();
        LOGGER.debug(
            "{} has been illegally removed, reverting to [{}]", FELIX_FILENAME, revertedFilename);
        context.setProperty(FELIX_FILENAME, revertedFilename);
        processUpdate(
            appropriatePid, fileFromCache, context.getSanitizedProperties(), cachedConfigData);
        return;
      }
    }

    // Routine property updates for tracked files - write to disk if necessary (4)
    processUpdate(
        appropriatePid, fileFromConfigAdmin, context.getSanitizedProperties(), cachedConfigData);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Remove config files from etc when a delete occurs but only if felix is tracking the file.
   *
   * @param pid the pid pointing to the configuration to delete.
   */
  @Override
  public final void handleDelete(String pid) throws IOException {
    try {
      AccessController.doPrivileged(
          (PrivilegedExceptionAction<Object>)
              () -> {
                doHandleDelete(pid);
                return null;
              });
    } catch (PrivilegedActionException e) {
      if (e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      }
      throw new IOException(e.getCause());
    }
  }

  /** Delete the config file if possible. */
  private void doHandleDelete(String pid) throws IOException {
    CachedConfigData cachedConfigData = pidDataMap.remove(pid);
    File fileFromCache = (cachedConfigData != null) ? cachedConfigData.getFelixFile() : null;
    if (fileFromCache != null && fileFromCache.exists()) {
      LOGGER.debug("Deleting file because config was deleted for pid [{}]", pid);
      try {
        Files.delete(fileFromCache.toPath());
        SecurityLogger.audit("Removed a deleted config [{}]", fileFromCache.getAbsolutePath());
      } catch (IOException e) {
        LOGGER.debug("Problem deleting config file [{}]: ", fileFromCache.getAbsolutePath(), e);
        SecurityLogger.audit("Failure to delete config file [{}]", fileFromCache.getAbsolutePath());
        // Synchronous with config admin, so we can report the failure to the UI this way
        throw e;
      }
    }
  }

  /**
   * Compare the data from config admin and the data in the cache and update where necessary.
   *
   * @param appropriatePid the factory pid if not null, otherwise the service pid.
   * @param dest the config file in etc to write to.
   * @param configAdminState the config properties currently being processed by config admin.
   * @param cachedData the previous state of the config currently being processed by config admin.
   * @throws IOException if an error occurs while writing.
   */
  private void processUpdate(
      String appropriatePid,
      File dest,
      Dictionary<String, Object> configAdminState,
      CachedConfigData cachedData)
      throws IOException {
    if (dest != null && dest.exists()) {
      encryptPasswords(appropriatePid, configAdminState, this::encryptValue);
      if (!cachedData.equalProps(configAdminState)) {
        writeConfigFile(dest, configAdminState);
        SecurityLogger.audit("Updated config file [{}]", dest.getAbsolutePath());
        // Update the cache
        cachedData.setProps(configAdminState);
      }
    }
  }

  /**
   * {@link EncryptionService} is whitelisted, so we cannot add this method onto the interface and
   * impl until a major release.
   */
  private String encryptValue(String plaintextValue) {
    Matcher m = ENC_PATTERN.matcher(plaintextValue);
    if (m.find()) {
      LOGGER.trace("Password already encrypted");
      return plaintextValue;
    }
    return encryptionService.encryptValue(plaintextValue);
  }

  /**
   * Encrypt password fields in the dictionary - object class def's are stored as a mapping of
   * factory pids, hence the need to precomputer an "appropriate pid" to submit to the function.
   */
  private void encryptPasswords(
      String appropriatePid,
      Dictionary<String, Object> dictionary,
      Function<String, String> passwordTransform) {
    ObjectClassDefinition objectClassDefinition =
        ddfConfigAdmin.getObjectClassDefinition(appropriatePid);
    if (objectClassDefinition == null) {
      return;
    }
    Stream.of(objectClassDefinition)
        .map(ocd -> ocd.getAttributeDefinitions(ObjectClassDefinition.ALL))
        .flatMap(Arrays::stream)
        .filter(ad -> ad.getType() == AttributeDefinition.PASSWORD)
        .map(AttributeDefinition::getID)
        .filter(id -> dictionary.get(id) instanceof String)
        .forEach(id -> dictionary.put(id, passwordTransform.apply((String) dictionary.get(id))));
  }

  /** Persist config changes to etc. */
  private void writeConfigFile(File dest, Dictionary<String, Object> configState)
      throws IOException {
    try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(dest))) {
      getAppropriateStrategy(dest.toPath()).write(outputStream, configState);
    } catch (IOException e) {
      LOGGER.debug("Writing to config file failed: ", e);
      SecurityLogger.audit("Failure to update config file [{}]", dest.getAbsolutePath());
      throw e;
    }
  }

  /**
   * Get an appropriate config writing utility for {@code .cfg}'s, {@code .config}'s, or any other
   * supported extension with a registered {@link PersistenceStrategy}.
   */
  private PersistenceStrategy getAppropriateStrategy(Path path) {
    String ext = getFileExtension(path.toString());
    if (ext.isEmpty()) {
      LOGGER.warn(
          "Config file without an extension was allowed to be processed [{}]",
          path.toAbsolutePath());
      throw new IllegalArgumentException(
          format("Path has no file extension [%s]", path.toAbsolutePath()));
    }
    return strategies
        .stream()
        .filter(s -> s.getExtension().equals(ext))
        .findFirst()
        .orElseThrow(
            () -> {
              LOGGER.warn(
                  "Config file with an unsupported extension was allowed to be processed [{}]",
                  path.toAbsolutePath());
              return new IllegalArgumentException(
                  format("File extension [%s] is not a supported config type", ext));
            });
  }
}
