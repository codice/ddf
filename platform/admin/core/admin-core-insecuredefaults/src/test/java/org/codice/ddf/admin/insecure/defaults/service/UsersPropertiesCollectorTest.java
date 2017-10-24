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

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.EventAdmin;

public class UsersPropertiesCollectorTest {

  private static final Path USERS_PROPERTIES_FILE_PATH =
      UsersPropertiesDeletionScheduler.USERS_PROPERTIES_FILE_PATH;
  private UsersPropertiesCollector collector;
  private EventAdmin eventAdmin;
  private UsersPropertiesDeletionScheduler scheduler;

  @Before
  public void setUp() throws Exception {
    eventAdmin = mock(EventAdmin.class);
    scheduler = mock(UsersPropertiesDeletionScheduler.class);
    collector = new UsersPropertiesCollector(eventAdmin, scheduler);

    assertTrue(USERS_PROPERTIES_FILE_PATH.getParent().toFile().mkdirs());
    assertTrue(USERS_PROPERTIES_FILE_PATH.toFile().createNewFile());
  }

  @Test
  public void usersPropertiesNotice() throws IOException {
    collector.setUsersPropertiesDeletion(true);
    when(scheduler.scheduleDeletion()).thenReturn(true);
    collector.run();

    verify(scheduler, times(1)).installationDate();
    verify(eventAdmin, times(1)).postEvent(anyObject());
  }

  @Test
  public void usersPropertiesNoticeWithTimestamp() throws Exception {
    collector.setUsersPropertiesDeletion(true);
    when(scheduler.scheduleDeletion()).thenReturn(true);
    when(scheduler.installationDate()).thenReturn(Instant.now());
    collector.run();

    verify(eventAdmin, times(1)).postEvent(anyObject());
  }

  @Test
  public void deleteScheduledDeletionsTest() throws Exception {
    collector.setUsersPropertiesDeletion(false);
    collector.run();
    verify(scheduler, times(1)).deleteScheduledDeletions();
  }

  @After
  public void tearDown() throws Exception {
    Files.deleteIfExists(USERS_PROPERTIES_FILE_PATH);
    Files.deleteIfExists(USERS_PROPERTIES_FILE_PATH.getParent());
  }
}
