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

import ddf.security.encryption.EncryptionService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.codice.ddf.admin.core.api.ConfigurationAdmin;
import org.codice.ddf.platform.io.internal.PersistenceStrategy;
import org.codice.felix.cm.internal.ConfigurationContext;
import org.codice.felix.cm.internal.ConfigurationContextFactory;
import org.codice.felix.cm.internal.ConfigurationPersistencePlugin;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
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
public class ConfigurationInstaller implements ConfigurationPersistencePlugin {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationInstaller.class);

  private static final Logger SECURITY_LOGGER = LoggerFactory.getLogger("securityLogger");

  private final ConfigurationAdmin ddfConfigAdmin;

  private final List<PersistenceStrategy> strategies;

  private final EncryptionService encryptionService;

  private final Map<String, CachedConfigData> pidDataMap;

  private final ConfigurationContextFactory configurationContextFactory;

  /**
   * {@code SortedServiceList} implements {@link java.util.List} using generic parameter <code>
   * &lt;T&gt;</code> and not a concrete type, so the ctor cannot be matched by the blueprint
   * container if we did <code>List&lt;PersistenceStrategy&gt;</code>
   *
   * <p>See https://issues.apache.org/jira/browse/ARIES-960
   */
  public ConfigurationInstaller(
      ConfigurationAdmin ddfConfigAdmin,
      List strategies,
      EncryptionService encryptionService,
      ConfigurationContextFactory configurationContextFactory) {
    this.ddfConfigAdmin = ddfConfigAdmin;
    this.strategies = strategies;
    this.encryptionService = encryptionService;
    this.pidDataMap = new ConcurrentHashMap<>();

    this.configurationContextFactory = configurationContextFactory;
  }

  /** @return a read-only map of configuration pids to felix file install properties. */
  Map<String, CachedConfigData> getPidDataMap() {
    return Collections.unmodifiableMap(pidDataMap);
  }

  /**
   * @see ConfigurationPersistencePlugin
   * @throws IOException if an error occurs reading configuration data
   * @throws InvalidSyntaxException technically impossible since {@code null} is being passed as the
   *     configuration filter. See {@link ConfigurationAdmin#listConfigurations(String)}.
   */
  public void init() throws IOException, InvalidSyntaxException {
    Configuration[] configs = ddfConfigAdmin.listConfigurations(null);
    if (configs == null) {
      return;
    }
    pidDataMap.putAll(
        Arrays.stream(configs)
            .map(configurationContextFactory::createContext)
            .filter(context -> context.getConfigFile() != null)
            .collect(Collectors.toMap(ConfigurationContext::getServicePid, CachedConfigData::new)));
  }

  /**
   * {@inheritDoc}
   *
   * <p>Sync the config files in etc when an update occurs. There are four cases, which will be
   * denoted using 2-tuple combinations of variables: [felixFileFromCache, felixFileFromConfig]
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
  @Override
  public void handleStore(ConfigurationContext context) throws IOException {
    String pid = context.getServicePid();
    File fileFromConfig = context.getConfigFile();

    CachedConfigData cachedConfigData = pidDataMap.get(pid);
    File fileFromCache = (cachedConfigData != null) ? cachedConfigData.getFelixFile() : null;

    if (fileFromCache == null && fileFromConfig == null) {
      // This config doesn't have an etc file, so we ignore this case (1)
      LOGGER.debug("Was not tracked and will not track pid {}", pid);
      return;
    }

    if (fileFromCache == null) {
      // An etc config file was just dropped and we're seeing it for the first time (2)
      LOGGER.debug("Tracking pid {}", pid);
      CachedConfigData createdConfigData = new CachedConfigData(context);
      pidDataMap.put(pid, createdConfigData);
      writeIfNecessary(context, createdConfigData);
      return;
    }

    if (!Objects.equals(fileFromConfig, fileFromCache)) {
      // The felix file prop changed, which is not allowed (3)
      String msg =
          (fileFromConfig == null)
              ? format("Felix filename has been illegally removed, was [%s]", fileFromCache)
              : format(
                  "Felix filename has been illegally changed from [%s] to [%s]",
                  fileFromCache, fileFromConfig);
      throw new IllegalStateException(msg);
    }

    // Write to disk if necessary (4)
    writeIfNecessary(context, cachedConfigData);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Remove config files from etc when a delete occurs but only if felix is tracking the file.
   *
   * @param pid the pid pointing to the configuration to delete.
   */
  @Override
  public void handleDelete(String pid) throws IOException {
    CachedConfigData cachedConfigData = pidDataMap.remove(pid);
    File fileFromCache = (cachedConfigData != null) ? cachedConfigData.getFelixFile() : null;
    if (fileFromCache != null && fileFromCache.exists()) {
      LOGGER.debug("Deleting file because config was deleted for pid {}", pid);
      try {
        Files.delete(fileFromCache.toPath());
        SECURITY_LOGGER.info("Removing a deleted config: {}", fileFromCache.toPath());
      } catch (IOException e) {
        LOGGER.debug("Problem deleting config file: ", e);
        // Synchronous with config admin, so we can report the failure to the UI this way
        throw e;
      }
    }
  }

  /**
   * In this block, based upon the circumstances of where it gets called, there's a guarantee that
   * {@link ConfigurationContext#getConfigFile()} will not return null.
   */
  private void writeIfNecessary(ConfigurationContext context, CachedConfigData data)
      throws IOException {
    File dest = context.getConfigFile();
    if (dest.exists()) {
      // Improvement: These would become checksums
      Dictionary<String, Object> fileState = data.getProps();
      Dictionary<String, Object> configState = context.getSanitizedProperties();
      processPasswords(context.getServicePid(), configState, encryptionService::encryptValue);
      if (!equalDictionaries(configState, fileState)) {
        try (OutputStream outputStream = new FileOutputStream(dest)) {
          getAppropriateStrategy(dest.toPath()).write(outputStream, configState);
        }
        SECURITY_LOGGER.info("Updating config file: {}", dest.toPath());
        data.setProps(configState);
      }
    }
  }

  private void processPasswords(
      String pid,
      Dictionary<String, Object> dictionary,
      Function<String, String> passwordTransform) {
    ObjectClassDefinition objectClassDefinition = ddfConfigAdmin.getObjectClassDefinition(pid);
    if (objectClassDefinition != null) {
      AttributeDefinition[] attributeDefinitions =
          objectClassDefinition.getAttributeDefinitions(ObjectClassDefinition.ALL);
      for (AttributeDefinition metatypeAttribute : attributeDefinitions) {
        if (metatypeAttribute.getType() == AttributeDefinition.PASSWORD) {
          Object beforePassword = dictionary.get(metatypeAttribute.getID());
          if (beforePassword instanceof String) {
            String afterPassword = passwordTransform.apply((String) beforePassword);
            dictionary.put(metatypeAttribute.getID(), afterPassword);
          }
        }
      }
    }
  }

  private PersistenceStrategy getAppropriateStrategy(Path path) {
    String ext = getFileExtension(path.toString());
    if (ext.isEmpty()) {
      throw new IllegalArgumentException("Path has no file extension");
    }
    return strategies
        .stream()
        .filter(s -> s.getExtension().equals(ext))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("File extension is not a supported config type"));
  }

  private static boolean equalDictionaries(Dictionary x, Dictionary y) {
    if (x.size() != y.size()) {
      return false;
    }
    for (final Enumeration e = x.keys(); e.hasMoreElements(); ) {
      final Object key = e.nextElement();
      if (!Objects.deepEquals(x.get(key), y.get(key))) {
        return false;
      }
    }
    return true;
  }
}
