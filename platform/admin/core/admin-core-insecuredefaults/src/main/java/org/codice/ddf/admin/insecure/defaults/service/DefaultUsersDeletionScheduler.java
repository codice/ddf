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
package org.codice.ddf.admin.insecure.defaults.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.codice.ddf.configuration.AbsolutePathResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultUsersDeletionScheduler {

  public static final Path USERS_PROPERTIES_FILE_PATH =
      Paths.get(new AbsolutePathResolver("etc/users.properties").getPath());
  private static final Path TEMP_TIMESTAMP_FILE_PATH =
      Paths.get(new AbsolutePathResolver("data/tmp/timestamp.bin").getPath());
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultUsersDeletionScheduler.class);
  private static final String ROUTE_ID = "deletionJob";
  private final CamelContext context;

  public DefaultUsersDeletionScheduler(CamelContext camelContext) {
    this.context = camelContext;
  }

  public boolean scheduleDeletion() {
    String cron = getCron();
    if (cron == null) {
      return false;
    }
    if (context != null && context.getRouteStatus(ROUTE_ID) != ServiceStatus.Started) {
      try {
        RouteBuilder builder =
            new RouteBuilder() {
              @Override
              public void configure() throws Exception {
                fromF("quartz2://scheduler/deletionTimer?cron=%s", cron)
                    .routeId(ROUTE_ID)
                    .bean(DefaultUsersDeletionScheduler.class, "removeDefaultUsers");
              }
            };
        context.addRoutes(builder);
        context.start();
      } catch (Exception e) {
        LOGGER.debug("Unable to create camel route.", e);
        return false;
      }
    }
    return true;
  }

  @VisibleForTesting
  String getCron() {
    if (!TEMP_TIMESTAMP_FILE_PATH.toFile().exists()) {

      // Create temp file and add timestamp
      Instant instant = Instant.now();
      if (!createTempFile(instant)) {
        return null;
      }
      return cronCalculator(instant);

    } else {

      // Read timestamp
      Instant firstInstall = installationDate();
      if (firstInstall == null) {
        return null;
      }

      if (Instant.now()
          .isAfter(
              firstInstall
                  .plus(Duration.ofDays(3))
                  .minus(Duration.ofMinutes(30)))) { // Timestamp expired
        removeDefaultUsers();
        return null;
      }

      return cronCalculator(firstInstall);
    }
  }

  private boolean createTempFile(Instant instant) {
    try {
      if (TEMP_TIMESTAMP_FILE_PATH.toFile().createNewFile()) {
        return writeTimestamp(instant);
      }
    } catch (IOException e) {
      LOGGER.debug("Unable to access the temporary file", e);
    }
    return false;
  }

  private boolean writeTimestamp(Instant instant) throws IOException {
    try (ObjectOutputStream objectOutputStream =
        new ObjectOutputStream(new FileOutputStream(TEMP_TIMESTAMP_FILE_PATH.toFile()))) {
      objectOutputStream.writeObject(instant);
      return true;
    }
  }

  public static boolean removeDefaultUsers() {
    try {
      Bundle bundle = FrameworkUtil.getBundle(DefaultUsersDeletionScheduler.class);
      if (bundle != null) {
        BundleContext bundleContext = bundle.getBundleContext();
        Collection<ServiceReference<BackingEngineFactory>> implementers =
            bundleContext.getServiceReferences(BackingEngineFactory.class, null);
        for (ServiceReference<BackingEngineFactory> impl : implementers) {

          BackingEngineFactory backingEngineFactory = bundleContext.getService(impl);
          BackingEngine backingEngine =
              backingEngineFactory.build(
                  ImmutableMap.of("users", USERS_PROPERTIES_FILE_PATH.toString()));

          if (!backingEngine.listUsers().isEmpty()) {
            backingEngine.listUsers().forEach(user -> backingEngine.deleteUser(user.getName()));
          }
        }
      }

      LOGGER.debug("Default users have been deleted successfully.");

      Files.deleteIfExists(TEMP_TIMESTAMP_FILE_PATH);
      return true;
    } catch (Exception e) {
      LOGGER.debug("Unable to remove default users.", e);
      return false;
    }
  }

  private String cronCalculator(Instant firstInstall) {
    Instant threeDayTimestamp = firstInstall.plus(Duration.ofDays(3).minus(Duration.ofMinutes(30)));
    LocalDateTime localDateTime =
        LocalDateTime.ofInstant(threeDayTimestamp, ZoneId.systemDefault());

    return String.format(
        "%d+%d+%d+%d+%d+?+%d",
        localDateTime.getSecond(),
        localDateTime.getMinute(),
        localDateTime.getHour(),
        localDateTime.getDayOfMonth(),
        localDateTime.getMonthValue(),
        localDateTime.getYear());
  }

  public Instant installationDate() {
    if (!TEMP_TIMESTAMP_FILE_PATH.toFile().exists()) {
      return null;
    }
    try (ObjectInputStream objectInputStream =
        new ObjectInputStream(new FileInputStream(TEMP_TIMESTAMP_FILE_PATH.toFile()))) {

      return (Instant) objectInputStream.readObject();
    } catch (IOException | ClassNotFoundException e) {
      LOGGER.debug("Unable to read installation date.", e);
      return null;
    }
  }

  public void deleteScheduledDeletions() {
    try {
      if (context.getRoute(ROUTE_ID) != null
          && context.getRouteStatus(ROUTE_ID).isStarted()
          && context.getRouteStatus(ROUTE_ID).isStoppable()) {
        context.stopRoute(ROUTE_ID);
        Files.deleteIfExists(TEMP_TIMESTAMP_FILE_PATH);
        LOGGER.debug("The deletion of default users has been stopped successfully.");
      }
    } catch (Exception e) {
      LOGGER.debug("Unable to stop deletion.", e);
    }
  }

  public boolean defaultUsersExist() {
    try {
      return Files.lines(USERS_PROPERTIES_FILE_PATH)
              .filter(line -> !line.trim().isEmpty() && !line.startsWith("#"))
              .count()
          != 0;
    } catch (IOException e) {
      LOGGER.debug("Unable to access users.properties file.", e);
      return true;
    }
  }
}
