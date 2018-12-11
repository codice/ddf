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
package ddf.migration.commands;

import com.google.common.annotations.VisibleForTesting;
import ddf.migration.api.DataMigratable;
import ddf.migration.api.ServiceNotFoundException;
import java.util.Collection;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.commands.catalog.SubjectCommands;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Command(scope = "migrate", name = "data", description = "Runs a specific data migration task")
public class MigrateCommand extends SubjectCommands {
  private static final Logger LOGGER = LoggerFactory.getLogger(MigrateCommand.class);

  @Argument(name = "serviceId", description = "The service ID of a data migration task")
  private String serviceId = null;

  @Option(name = "-a", aliases = "--all", description = "Runs all data migrations")
  private boolean allMigrationTasks = false;

  @Option(
    name = "-s",
    aliases = "--services",
    description = "Lists all available data migration options"
  )
  private boolean listMigrationTasks = false;

  @Reference private BundleContext bundleContext;

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public void setBundleContext(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }

  @Override
  @VisibleForTesting
  protected Object executeWithSubject() throws Exception {
    Collection<ServiceReference<DataMigratable>> services =
        bundleContext.getServiceReferences(DataMigratable.class, null);

    if (allMigrationTasks) {
      LOGGER.trace("Running all data migration tasks");

      for (ServiceReference<DataMigratable> serviceRef : services) {
        migrate(serviceRef);
      }
    } else if (listMigrationTasks) {
      LOGGER.trace("Listing all available data migrations tasks");

      for (ServiceReference<DataMigratable> serviceRef : services) {
        console.println(serviceRef.getProperty("name"));
        console.println(String.format("\t%s\n", serviceRef.getProperty("description")));
      }
    } else {
      LOGGER.info("Running data migration task [{}]", serviceId);

      ServiceReference<DataMigratable> serviceRef =
          services
              .stream()
              .filter(service -> service.getProperty("id").equals(serviceId))
              .findFirst()
              .orElseThrow(
                  () ->
                      new ServiceNotFoundException(
                          String.format("Data migration service not found for %s", serviceId)));

      migrate(serviceRef);
    }

    return null;
  }

  private void migrate(ServiceReference<DataMigratable> serviceRef) {
    DataMigratable dataMigration = bundleContext.getService(serviceRef);
    dataMigration.migrate();
  }

  @VisibleForTesting
  void setAllMigrationTasks(boolean allMigrationTasks) {
    this.allMigrationTasks = allMigrationTasks;
  }

  @VisibleForTesting
  void setListMigrationTasks(boolean listMigrationTasks) {
    this.listMigrationTasks = listMigrationTasks;
  }
}
