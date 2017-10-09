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
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.codice.ddf.configuration.AbsolutePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsersPropertiesDeletionScheduler {

  public static final Path USERS_PROPERTIES_FILE_PATH =
      Paths.get(new AbsolutePathResolver("etc/users.properties").getPath());
  private static final Path TEMP_TIMESTAMP_FILE_PATH =
      Paths.get(new AbsolutePathResolver("data/tmp/timestamp.bin").getPath());
  private static final Logger LOGGER =
      LoggerFactory.getLogger(UsersPropertiesDeletionScheduler.class);
  private static final String ROUTE_ID = "deletionJob";
  private final CamelContext context;

  public UsersPropertiesDeletionScheduler(CamelContext camelContext) {
    this.context = camelContext;
  }

  public boolean scheduleDeletion() {
    String cron = getCron();
    if (cron == null) {
      return false;
    }
    try {
      RouteBuilder builder =
          new RouteBuilder() {
            @Override
            public void configure() throws Exception {
              fromF("quartz2://scheduler/deletionTimer?cron=%s", cron)
                  .routeId(ROUTE_ID)
                  .bean(UsersPropertiesDeletionScheduler.class, "deleteFiles");
            }
          };
      context.addRoutes(builder);
      context.start();
      return true;
    } catch (Exception e) {
      LOGGER.debug("Unable to create camel route.", e);
    }
    return false;
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

      if (Instant.now().isAfter(firstInstall.plus(Duration.ofDays(3)))) { // Timestamp expired
        deleteFiles();
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

  public static boolean deleteFiles() {
    try {
      Files.deleteIfExists(USERS_PROPERTIES_FILE_PATH);
      LOGGER.debug("Users.properties file deleted successfully.");
    } catch (Exception e) {
      LOGGER.debug("Unable to delete the users.properties file.", e);
      return false;
    }
    try {
      Files.deleteIfExists(TEMP_TIMESTAMP_FILE_PATH);
      LOGGER.debug("Temporary file deleted successfully.");
      return true;
    } catch (IOException e) {
      LOGGER.debug("Unable to delete the temporary file.", e);
      return false;
    }
  }

  private String cronCalculator(Instant firstInstall) {
    Instant threeDayTimestamp = firstInstall.plus(Duration.ofDays(3));
    LocalDateTime localDateTime =
        LocalDateTime.ofInstant(threeDayTimestamp, ZoneId.systemDefault());

    return String.format(
        "0+%d+%d+%d+%d+?+%d",
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
      }
    } catch (Exception e) {
      LOGGER.debug("Unable to stop deletion.", e);
    }
  }
}
