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

import com.google.common.io.Closeables;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.Validate;
import org.apache.felix.fileinstall.internal.DirectoryWatcher;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.platform.io.internal.PersistenceStrategy;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class extends on the {@link ImportMigrationContext} interface to pre-process exported
 * entries for configuration objects and compare them with the configuration objects currently in
 * memory.
 */
public class ImportMigrationConfigurationAdminContext {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ImportMigrationConfigurationAdminContext.class);

  private static final Path ETC_PATH = Paths.get("etc");

  private static final Path ADMIN_DIR = Paths.get("admin");

  private final ImportMigrationContext context;

  private final ConfigurationAdminMigratable admin;

  private final ConfigurationAdmin configurationAdmin;

  private final boolean restoreWithBundleLocation;

  /**
   * Keeps track of all managed services in memory that were not found in the export file so we know
   * what to delete at the end.
   */
  private final Map<String, Configuration> managedServicesToDelete;

  /**
   * Keeps track of all managed service factories in memory that were not found in the export file
   * so we know what to delete at the end.
   */
  private final Map<String, List<Configuration>> managedServiceFactoriesToDelete;

  private final Map<String, ImportMigrationConfigurationAdminEntry> exportedServices;

  private final Map<String, List<ImportMigrationConfigurationAdminEntry>> exportedFactoryServices;

  private boolean isValid = true; // until proven otherwise

  public ImportMigrationConfigurationAdminContext(
      ImportMigrationContext context,
      ConfigurationAdminMigratable admin,
      ConfigurationAdmin configurationAdmin,
      Configuration[] memoryConfigs,
      boolean restoreWithBundleLocation) {
    Validate.notNull(context, "invalid null context");
    Validate.notNull(admin, "invalid null configuration admin migratable");
    Validate.notNull(configurationAdmin, "invalid null configuration admin");
    Validate.notNull(memoryConfigs, "invalid null configurations");
    this.context = context;
    this.admin = admin;
    this.configurationAdmin = configurationAdmin;
    this.restoreWithBundleLocation = restoreWithBundleLocation;
    // categorize memory configurations
    this.managedServicesToDelete =
        Stream.of(memoryConfigs)
            .filter(ConfigurationAdminMigratable::isManagedService)
            .collect(Collectors.toMap(Configuration::getPid, Function.identity()));
    this.managedServiceFactoriesToDelete =
        Stream.of(memoryConfigs)
            .filter(ConfigurationAdminMigratable::isManagedServiceFactory)
            .collect(Collectors.groupingBy(Configuration::getFactoryPid));
    // categorize exported admin configurations
    final ImportMigrationConfigurationAdminEntry[] entries =
        context
            .entries(ImportMigrationConfigurationAdminContext.ADMIN_DIR)
            .map(this::proxy)
            .filter(Objects::nonNull)
            .toArray(ImportMigrationConfigurationAdminEntry[]::new);

    this.exportedServices =
        Stream.of(entries)
            .filter(ImportMigrationConfigurationAdminEntry::isManagedService)
            .collect(
                Collectors.toMap(
                    ImportMigrationConfigurationAdminEntry::getPid, Function.identity()));
    this.exportedFactoryServices =
        Stream.of(entries)
            .filter(ImportMigrationConfigurationAdminEntry::isManagedServiceFactory)
            .collect(Collectors.groupingBy(ImportMigrationConfigurationAdminEntry::getFactoryPid));
  }

  public Stream<ImportMigrationConfigurationAdminEntry> memoryEntries() {
    if (!isValid) {
      return Stream.empty();
    }
    return Stream.concat(
        exportedServices.values().stream(),
        exportedFactoryServices.values().stream().flatMap(List::stream));
  }

  public String getSystemProperty(String key) {
    return context.getExportedSystemProperty(key);
  }

  @Nullable
  private Configuration getAndRemoveMemoryFactoryService(
      String factoryPid, Path exportedConfigPath) {
    final List<Configuration> memoryConfigs = managedServiceFactoriesToDelete.get(factoryPid);

    if (memoryConfigs == null) {
      return null;
    }
    // @formatter:off - to shut up checkstyle!!!!!!!
    for (final Iterator<Configuration> i = memoryConfigs.iterator(); i.hasNext(); ) {
      // @formatter:on
      final Configuration memoryConfig = i.next();
      final Path memoryConfigPath =
          getPathFromConfiguration(
              memoryConfig.getProperties(),
              () -> String.format("configuration '%s'", memoryConfig.getPid()));

      if (exportedConfigPath.equals(memoryConfigPath)) {
        // remove it from memory list and clean the map if it was the last one
        i.remove();
        if (memoryConfigs.isEmpty()) {
          managedServiceFactoriesToDelete.remove(factoryPid);
        }
        return memoryConfig;
      }
    }
    return null;
  }

  @Nullable
  private Configuration getAndRemoveMemoryService(String pid) {
    return managedServicesToDelete.remove(pid);
  }

  @Nullable
  private Configuration getAndRemoveMemoryConfig(
      @Nullable String factoryPid,
      String pid,
      Dictionary<String, Object> properties,
      Path exportedPath) {
    if (factoryPid != null) {
      // search for it based on the felix file install property
      final Path exportedConfigPath =
          getPathFromConfiguration(properties, () -> String.format("path '%s'", exportedPath));

      if (exportedConfigPath == null) {
        // this means we will not be able to correlate an exported managed service factory
        // with its counterpart here, as such we will be forced to treat it as a new one
        return null;
      }
      return getAndRemoveMemoryFactoryService(factoryPid, exportedConfigPath);
    }
    return getAndRemoveMemoryService(pid);
  }

  @Nullable
  private Path getPathFromConfiguration(
      @Nullable Dictionary<String, Object> properties, Supplier<String> from) {
    if (properties == null) {
      return null;
    }
    final Object o = properties.get(DirectoryWatcher.FILENAME);
    final Path path;

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
          LOGGER.debug(
              "unsupported {} property '{}' from {}", DirectoryWatcher.FILENAME, o, from.get());
          return null;
        }
      } catch (MalformedURLException | URISyntaxException e) {
        LOGGER.debug(
            String.format(
                "failed to parse %s property '%s' from %s; ",
                DirectoryWatcher.FILENAME, o, from.get()),
            e);
        return null;
      }
    } else {
      return null;
    }
    // ignore the whole path if any (there shouldn't be any other than etc) and force it to be under
    // etc
    return ImportMigrationConfigurationAdminContext.ETC_PATH.resolve(path.getFileName());
  }

  private Dictionary<String, Object> readProperties(
      ImportMigrationEntry entry, PersistenceStrategy ps, InputStream is) {
    try {
      return ps.read(is);
    } catch (IOException e) {
      throw new MigrationException(
          "Import error: failed to read configuration [%s] using persistent strategy [%s]; %s.",
          entry.getPath(), ps.getExtension(), e);
    }
  }

  @Nullable
  private ImportMigrationConfigurationAdminEntry proxy(ImportMigrationEntry entry) {
    final Path path = entry.getPath();
    final String extn = FilenameUtils.getExtension(path.toString());
    final PersistenceStrategy ps = admin.getPersister(extn);

    if (ps == null) {
      context
          .getReport()
          .record(
              new MigrationException(
                  "Import error: persistence strategy [%s] for configuration [%s] is not defined.",
                  extn, path));
    } else {
      final Dictionary<String, Object> properties;
      InputStream is = null;

      try {
        is =
            entry
                .getInputStream()
                .orElseThrow(
                    () ->
                        new MigrationException(
                            "Import error: failed to read configuration [%s]; not exported.",
                            path));
        properties = readProperties(entry, ps, is);
      } catch (IOException e) {
        throw new MigrationException(
            "Import error: failed to read configuration [%s]; %s.", path, e);
      } finally {
        Closeables.closeQuietly(is);
      }
      // note: we also remove bunde location, factory pid, and pid from the dictionary as we do not
      // want to restore those later
      final String pid = Objects.toString(properties.remove(Constants.SERVICE_PID), null);

      if (pid == null) {
        // this should never happen unless someone created the zip file manually and messed up the
        // config file
        context
            .getReport()
            .record(
                new MigrationException("Import error: missing pid from configuration [%s].", path));
      } else {
        String bundleLocation = null;
        if (restoreWithBundleLocation) {
          bundleLocation =
              Objects.toString(properties.remove(ConfigurationAdmin.SERVICE_BUNDLELOCATION), null);
        } else {
          properties.remove("felix.fileinstall.filename");
          properties.remove(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
        }
        final String factoryPid =
            Objects.toString(properties.remove(ConfigurationAdmin.SERVICE_FACTORYPID), null);

        try {
          return new ImportMigrationConfigurationAdminEntry(
              configurationAdmin,
              entry,
              bundleLocation,
              factoryPid,
              pid,
              properties,
              getAndRemoveMemoryConfig(factoryPid, pid, properties, entry.getPath()));
        } catch (MigrationException e) {
          // don't throw it back yet as we want to detect as many as possible so just record it
          context.getReport().record(e);
        }
      }
    }
    this.isValid = false;
    return null;
  }

  @SuppressWarnings(
      "PMD.UnusedFormalParameter" /* report parameter is required as this method is used as a functional interface
                                  and is being called in the ConfigurationAdminMigratable class */)
  void deleteUnexportedConfigurationsAfterCompletion(MigrationReport report) {
    if (isValid) {
      Stream.concat(
              managedServicesToDelete.values().stream(),
              managedServiceFactoriesToDelete.values().stream().flatMap(List::stream))
          .forEach(this::delete);
    }
  }

  private void delete(Configuration configuration) {
    try {
      LOGGER.debug(
          "Importing configuration for [{}]; deleting existing configuration...",
          configuration.getPid());
      configuration.delete();
    } catch (IOException e) {
      context
          .getReport()
          .record(
              new MigrationException(
                  "Import error: failed to delete configuration [%s]; %s.",
                  configuration.getPid(), e));
    }
  }
}
