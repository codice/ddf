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
package org.codice.ddf.configuration.admin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.Validate;
import org.apache.felix.fileinstall.internal.DirectoryWatcher;
import org.codice.ddf.migration.ExportMigrationContext;
import org.codice.ddf.migration.ExportMigrationEntry;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.platform.io.internal.PersistenceStrategy;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class extends on the {@link ExportMigrationContext} interface to pre-create entries for all
 * configuration objects.
 */
public class ExportMigrationConfigurationAdminContext {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExportMigrationConfigurationAdminContext.class);

  private static final Path ADMIN_PATH = Paths.get("admin");

  private static final Path ETC_DIR = Paths.get("etc");

  private final ExportMigrationContext context;

  private final String defaultFileExtension;

  private final ConfigurationAdminMigratable admin;

  private final Set<String> warnedExtensions = new HashSet<>(8);

  private boolean warnedDefaultExtension = false;

  private final Set<ExportMigrationEntry> fileEntries;

  private final Map<Path, ExportMigrationConfigurationAdminEntry> memoryEntries;

  public ExportMigrationConfigurationAdminContext(
      ExportMigrationContext context,
      String defaultFileExtension,
      ConfigurationAdminMigratable admin,
      Configuration[] configs) {
    Validate.notNull(context, "invalid null contexts");
    Validate.notNull(defaultFileExtension, "invalid null default file extension");
    Validate.notNull(admin, "invalid null configuration admin migratable");
    Validate.notNull(configs, "invalid null configurations");
    this.context = context;
    this.defaultFileExtension = defaultFileExtension;
    this.admin = admin;
    this.fileEntries =
        context
            .entries(ExportMigrationConfigurationAdminContext.ETC_DIR, false, admin::isConfigFile)
            .collect(Collectors.toSet());
    this.memoryEntries =
        Stream.of(configs)
            .filter(this::isValid)
            .map(this::getEntry)
            .filter(Objects::nonNull)
            .collect(
                Collectors.toMap(
                    ExportMigrationConfigurationAdminEntry::getPath, Function.identity()));
  }

  /**
   * This method is designed to circumvent a bug in Felix where it has been seen that a config
   * object that is meant to represent a managed service factory is not properly created. Instead of
   * being created using the createFactoryConfiguration(), it is obtained using getConfiguration().
   * After that, the properties are updated with the service factory pid as a property. Although it
   * has that property, it ain't a real managed service factory as the factory or blueprint was
   * never called to instantiate a corresponding service. In such case, we end up with a dummy
   * config object in memory that is not attached to an actual instance of manager service.
   *
   * <p>If we were to export that object, we would end up re-creating it as a managed service
   * factory on the other system and we potentially could end up with multiple instances of that
   * manager service which could create problem. In addition, the system would not be a proper
   * representation of the original system which didn't have that associated managed service.
   *
   * <p>We do not intent to protect against all possible stupidity they could do (e.g. different
   * factory pid in properties than reported by the config object's interface). We only protect
   * against the object not being created as a managed service factory when it should have been.
   *
   * @param cfg the config to check its validity
   * @return <code>true</code> if the config is valid; <code>false</code> otherwise
   */
  private boolean isValid(Configuration cfg) {
    final String fpid =
        Objects.toString(cfg.getProperties().get(ConfigurationAdmin.SERVICE_FACTORYPID), null);

    if (fpid == null) {
      return true;
    } // else - property reports it should be a managed service factory, so it is valid only if the
    // cfg object reports it is too
    return ConfigurationAdminMigratable.isManagedServiceFactory(cfg);
  }

  public Stream<ExportMigrationEntry> fileEntries() {
    return fileEntries.stream();
  }

  public Stream<ExportMigrationConfigurationAdminEntry> memoryEntries() {
    return memoryEntries.values().stream();
  }

  @Nullable
  private ExportMigrationConfigurationAdminEntry getEntry(Configuration configuration) {
    Path path = getPathFromConfiguration(configuration);
    final String pathString = path.toString();
    final String extn = FilenameUtils.getExtension(pathString);
    PersistenceStrategy ps = admin.getPersister(extn);

    if (ps == null) {
      if (warnedExtensions.add(extn)) {
        context
            .getReport()
            .record(
                new MigrationWarning(
                    String.format(
                        "Persistence strategy [%s] is not defined; defaulting to [%s]",
                        extn, defaultFileExtension)));
      }
      ps = admin.getPersister(defaultFileExtension);
      if (ps == null) {
        if (!warnedDefaultExtension) {
          this.warnedDefaultExtension = true;
          context
              .getReport()
              .record(
                  new MigrationWarning(
                      String.format(
                          "Default persistence strategy [%s] is not defined",
                          defaultFileExtension)));
        }
        return null;
      }
      path = Paths.get(pathString + FilenameUtils.EXTENSION_SEPARATOR + defaultFileExtension);
    }
    return new ExportMigrationConfigurationAdminEntry(context.getEntry(path), configuration, ps);
  }

  private Path getPathFromConfiguration(Configuration configuration) {
    final Object o = configuration.getProperties().get(DirectoryWatcher.FILENAME);
    Path path = null;

    if (o != null) {
      try {
        if (o instanceof URL) {
          path = new File(((URL) o).toURI()).toPath();
        } else if (o instanceof URI) {
          path = new File((URI) o).toPath();
        } else if (o instanceof String) {
          path = new File(new URL((String) o).toURI()).toPath();
        } else if (o instanceof File) {
          path = ((File) o).toPath();
        } else if (o instanceof Path) {
          path = (Path) o;
        } else {
          path = constructPathForBasename(configuration);
          LOGGER.debug("unsupported {} property from '{}'", DirectoryWatcher.FILENAME, o);
          context
              .getReport()
              .record(
                  new MigrationWarning(
                      "Path [%s] from %s property for configuration [%s] is of an unsupported format; exporting as [%s].",
                      o, DirectoryWatcher.FILENAME, configuration.getPid(), path));
        }
      } catch (MalformedURLException | URISyntaxException e) {
        path = constructPathForBasename(configuration);
        LOGGER.debug("failed to parse {} property from '{}'; ", DirectoryWatcher.FILENAME, o, e);
        context
            .getReport()
            .record(
                new MigrationWarning(
                    "Path [%s] from %s property for configuration [%s] cannot be parsed; exporting as [%s].",
                    o, DirectoryWatcher.FILENAME, configuration.getPid(), path));
      }
    } else {
      path = constructPathForBasename(configuration);
    }
    // ignore the whole path if any (there shouldn't be any other than etc) and force it to be under
    // admin in the exported file
    return ExportMigrationConfigurationAdminContext.ADMIN_PATH.resolve(path.getFileName());
  }

  private Path constructPathForBasename(Configuration configuration) {
    final String fpid = configuration.getFactoryPid();
    final String basename;

    if (fpid != null) { // it is a managed service factory!!!
      // Felix Fileinstall uses the hyphen as separator between factoryPid and alias. For
      // safety reasons, all hyphens are removed from the generated UUID.
      final String alias = UUID.randomUUID().toString().replaceAll("-", "");

      basename = fpid + '-' + alias;
    } else {
      basename = configuration.getPid();
    }
    return Paths.get(basename + FilenameUtils.EXTENSION_SEPARATOR + defaultFileExtension);
  }
}
