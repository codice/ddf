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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.stream.Stream;
import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.lang.StringUtils;
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

  private static Path usersPropertiesFilePath =
      Paths.get(new AbsolutePathResolver("etc/users.properties").getPath());
  private static Path tempTimestampFilePath =
      Paths.get(new AbsolutePathResolver("data/tmp/timestamp.bin").getPath());
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultUsersDeletionScheduler.class);
  private static final String ROUTE_ID = "deletionJob";
  private final CamelContext context;

  public DefaultUsersDeletionScheduler(CamelContext camelContext) {
    this.context = camelContext;
  }

  public boolean scheduleDeletion() {
    String cron = getCronOrDelete();
    if (cron == null) {
      return false;
    }
    if (context != null
        && context.getRouteController().getRouteStatus(ROUTE_ID) != ServiceStatus.Started) {
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
  String getCronOrDelete() {
    if (!getTempTimestampFilePath().toFile().exists()) {

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
      if (getTempTimestampFilePath().toFile().createNewFile()) {
        Files.write(
            getTempTimestampFilePath(), instant.toString().getBytes(StandardCharsets.UTF_8));
        return true;
      }
    } catch (IOException e) {
      LOGGER.debug("Unable to access the temporary file", e);
    }
    return false;
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
                  ImmutableMap.of("users", getUsersPropertiesFilePath().toString()));

          if (!backingEngine.listUsers().isEmpty()) {
            backingEngine.listUsers().forEach(user -> backingEngine.deleteUser(user.getName()));
          }
        }
      }

      LOGGER.debug("Default users have been deleted successfully.");

      Files.deleteIfExists(getTempTimestampFilePath());
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
    if (!getTempTimestampFilePath().toFile().exists()) {
      return null;
    }

    try (Stream<String> lines = Files.lines(getTempTimestampFilePath(), StandardCharsets.UTF_8)) {
      return lines.filter(StringUtils::isNotBlank).map(Instant::parse).findFirst().orElse(null);

    } catch (IOException e) {
      LOGGER.debug("Unable to read installation date.", e);
      return null;
    }
  }

  public void deleteScheduledDeletions() {
    try {
      if (context.getRoute(ROUTE_ID) != null
          && context.getRouteController().getRouteStatus(ROUTE_ID).isStarted()
          && context.getRouteController().getRouteStatus(ROUTE_ID).isStoppable()) {
        context.getRouteController().stopRoute(ROUTE_ID);
        Files.deleteIfExists(getTempTimestampFilePath());
        LOGGER.debug("The deletion of default users has been stopped successfully.");
      }
    } catch (Exception e) {
      LOGGER.debug("Unable to stop deletion.", e);
    }
  }

  public boolean defaultUsersExist() {
    try (Stream<String> defaultUsers =
        Files.lines(getUsersPropertiesFilePath(), StandardCharsets.UTF_8)) {
      return defaultUsers
          .filter(StringUtils::isNotBlank)
          .filter(line -> !line.startsWith("#"))
          .anyMatch(line -> true);
    } catch (IOException e) {
      LOGGER.debug("Unable to access users.properties file.", e);
      return true;
    }
  }

  public static Path getUsersPropertiesFilePath() {
    return usersPropertiesFilePath;
  }

  public static void setUsersPropertiesFilePath(Path usersPropertiesFilePath) {
    DefaultUsersDeletionScheduler.usersPropertiesFilePath = usersPropertiesFilePath;
  }

  public static Path getTempTimestampFilePath() {
    return tempTimestampFilePath;
  }

  public static void setTempTimestampFilePath(Path tempTimestampFilePath) {
    DefaultUsersDeletionScheduler.tempTimestampFilePath = tempTimestampFilePath;
  }
}
