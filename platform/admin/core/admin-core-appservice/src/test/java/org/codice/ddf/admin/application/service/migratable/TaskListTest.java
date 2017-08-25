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

import com.github.npathai.hamcrestopt.OptionalMatchers;
import java.util.function.Predicate;
import org.codice.ddf.migration.MigrationException;
import org.hamcrest.Matchers;
import org.hamcrest.junit.internal.ThrowableMessageMatcher;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class TaskListTest {
  private static final String TYPE = "something";

  private final ProfileMigrationReport report = Mockito.mock(ProfileMigrationReport.class);

  private final TaskList tasks = new TaskList(TYPE, report);

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testConstructor() throws Exception {
    Assert.assertThat(tasks.getType(), Matchers.equalTo(TYPE));
    Assert.assertThat(tasks.isEmpty(), Matchers.equalTo(true));
    for (final Operation op : Operation.values()) {
      Assert.assertThat(
          tasks.getAttemptsLeft(op).get(), Matchers.equalTo(ProfileMigratable.RETRY_COUNT));
    }
  }

  @Test
  public void testConstructorWithNullType() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null task type"));

    new TaskList(null, report);
  }

  @Test
  public void testConstructorWithNullReport() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null report"));

    new TaskList(TYPE, null);
  }

  @Test
  public void testAddFirstTask() throws Exception {
    final Predicate<ProfileMigrationReport> task = Mockito.mock(Predicate.class);

    Mockito.doReturn(report).when(report).recordTask();

    tasks.add(Operation.INSTALL, "id", task);

    Assert.assertThat(
        tasks.getTasks(),
        Matchers.allOf(
            Matchers.aMapWithSize(1),
            Matchers.hasEntry(
                Matchers.equalTo(Operation.INSTALL),
                Matchers.allOf(
                    Matchers.aMapWithSize(1),
                    Matchers.hasEntry(Matchers.equalTo("id"), Matchers.sameInstance(task))))));

    Mockito.verify(report).recordTask();
  }

  @Test
  public void testAddSecondTaskForSameOperation() throws Exception {
    final Predicate<ProfileMigrationReport> task = Mockito.mock(Predicate.class);
    final Predicate<ProfileMigrationReport> task2 = Mockito.mock(Predicate.class);

    Mockito.doReturn(report).when(report).recordTask();

    tasks.add(Operation.INSTALL, "id", task);
    tasks.add(Operation.INSTALL, "id2", task2);

    Assert.assertThat(
        tasks.getTasks(),
        Matchers.allOf(
            Matchers.aMapWithSize(1),
            Matchers.hasEntry(
                Matchers.equalTo(Operation.INSTALL),
                Matchers.allOf(
                    Matchers.aMapWithSize(2),
                    Matchers.hasEntry(Matchers.equalTo("id"), Matchers.sameInstance(task)),
                    Matchers.hasEntry(Matchers.equalTo("id2"), Matchers.sameInstance(task2))))));

    Mockito.verify(report, Mockito.times(2)).recordTask();
  }

  @Test
  public void testAddSecondTaskForSameId() throws Exception {
    final Predicate<ProfileMigrationReport> task = Mockito.mock(Predicate.class);
    final Predicate<ProfileMigrationReport> task2 = Mockito.mock(Predicate.class);

    Mockito.doReturn(report).when(report).recordTask();

    tasks.add(Operation.INSTALL, "id", task);
    tasks.add(Operation.UNINSTALL, "id", task2);

    Assert.assertThat(
        tasks.getTasks(),
        Matchers.allOf(
            Matchers.aMapWithSize(2),
            Matchers.hasEntry(
                Matchers.equalTo(Operation.INSTALL),
                Matchers.allOf(
                    Matchers.aMapWithSize(1),
                    Matchers.hasEntry(Matchers.equalTo("id"), Matchers.sameInstance(task)))),
            Matchers.hasEntry(
                Matchers.equalTo(Operation.UNINSTALL),
                Matchers.allOf(
                    Matchers.aMapWithSize(1),
                    Matchers.hasEntry(Matchers.equalTo("id"), Matchers.sameInstance(task2))))));

    Mockito.verify(report, Mockito.times(2)).recordTask();
  }

  @Test
  public void testAddWithNullOperation() throws Exception {
    final Predicate<ProfileMigrationReport> task = Mockito.mock(Predicate.class);

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null operation"));

    tasks.add(null, "id", task);
  }

  @Test
  public void testAddWithNullId() throws Exception {
    final Predicate<ProfileMigrationReport> task = Mockito.mock(Predicate.class);

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null task id"));

    tasks.add(Operation.INSTALL, null, task);
  }

  @Test
  public void testAddWithNullTask() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null task"));

    tasks.add(Operation.INSTALL, "id", null);
  }

  @Test
  public void testIncreaseAttemptsFor() throws Exception {
    tasks.increaseAttemptsFor(Operation.INSTALL);

    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.INSTALL).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT + 1));

    // not affected
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.UNINSTALL).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT));
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.START).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT));
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.STOP).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT));
  }

  @Test
  public void testIncreaseAttemptsForWithNullOperation() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null operation"));

    tasks.increaseAttemptsFor(null);
  }

  @Test
  public void testIsEmptyWhenEmpty() throws Exception {
    Assert.assertThat(tasks.isEmpty(), Matchers.equalTo(true));
  }

  @Test
  public void testIsEmptyWhenTasksWereAdded() throws Exception {
    final Predicate<ProfileMigrationReport> task = Mockito.mock(Predicate.class);

    tasks.add(Operation.INSTALL, "id", task);

    Assert.assertThat(tasks.isEmpty(), Matchers.equalTo(false));
  }

  @Test
  public void testGetOperationWhenEmpty() throws Exception {
    Assert.assertThat(tasks.getOperation(), OptionalMatchers.isEmpty());
  }

  @Test
  public void testGetOperationWhenInstallTaskAdded() throws Exception {
    final Predicate<ProfileMigrationReport> task = Mockito.mock(Predicate.class);

    tasks.add(Operation.INSTALL, "id", task);
    tasks.add(Operation.START, "id", task);
    tasks.add(Operation.STOP, "id", task);
    tasks.add(Operation.UNINSTALL, "id", task);

    Assert.assertThat(tasks.getOperation(), OptionalMatchers.hasValue(Operation.INSTALL));
  }

  @Test
  public void testGetOperationWhenStartTaskAddedAndNoInstallTasksAdded() throws Exception {
    final Predicate<ProfileMigrationReport> task = Mockito.mock(Predicate.class);

    tasks.add(Operation.START, "id", task);
    tasks.add(Operation.STOP, "id", task);
    tasks.add(Operation.UNINSTALL, "id", task);

    Assert.assertThat(tasks.getOperation(), OptionalMatchers.hasValue(Operation.START));
  }

  @Test
  public void testGetOperationWhenStopTaskAddedAndNoInstallOrStartTasksAdded() throws Exception {
    final Predicate<ProfileMigrationReport> task = Mockito.mock(Predicate.class);

    tasks.add(Operation.STOP, "id", task);
    tasks.add(Operation.UNINSTALL, "id", task);

    Assert.assertThat(tasks.getOperation(), OptionalMatchers.hasValue(Operation.STOP));
  }

  @Test
  public void testGetOperationWhenUninstallTaskAddedAndNoInstallOrStartOrStopTasksAdded()
      throws Exception {
    final Predicate<ProfileMigrationReport> task = Mockito.mock(Predicate.class);

    tasks.add(Operation.UNINSTALL, "id", task);

    Assert.assertThat(tasks.getOperation(), OptionalMatchers.hasValue(Operation.UNINSTALL));
  }

  @Test
  public void testExecuteWhenAllTasksSucceeds() throws Exception {
    final Predicate<ProfileMigrationReport> start = Mockito.mock(Predicate.class);
    final Predicate<ProfileMigrationReport> start2 = Mockito.mock(Predicate.class);
    final Predicate<ProfileMigrationReport> stop = Mockito.mock(Predicate.class);

    tasks.add(Operation.START, "1", start);
    tasks.add(Operation.START, "2", start2);
    tasks.add(Operation.STOP, "1", stop);

    Mockito.doReturn(true).when(start).test(report);
    Mockito.doReturn(true).when(start2).test(report);
    Mockito.doReturn(false).when(stop).test(report);

    Assert.assertThat(tasks.execute(), Matchers.equalTo(true));
    Assert.assertThat(tasks.isEmpty(), Matchers.equalTo(true));
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.START).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT - 1));
    // not affected
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.INSTALL).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT));
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.UNINSTALL).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT));
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.STOP).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT));

    Mockito.verify(start).test(report);
    Mockito.verify(start2).test(report);
    Mockito.verify(stop, Mockito.never()).test(report);
  }

  @Test
  public void testExecuteWhenAttemptsAreLeftAndSomeTasksFails() throws Exception {
    final Predicate<ProfileMigrationReport> start = Mockito.mock(Predicate.class);
    final Predicate<ProfileMigrationReport> start2 = Mockito.mock(Predicate.class);
    final Predicate<ProfileMigrationReport> start3 = Mockito.mock(Predicate.class);
    final Predicate<ProfileMigrationReport> stop = Mockito.mock(Predicate.class);

    tasks.add(Operation.START, "1", start);
    tasks.add(Operation.START, "2", start2);
    tasks.add(Operation.START, "3", start3);
    tasks.add(Operation.STOP, "1", stop);

    Mockito.doReturn(false).when(start).test(report);
    Mockito.doReturn(true).when(start2).test(report);
    Mockito.doReturn(false).when(start3).test(report);
    Mockito.doReturn(true).when(stop).test(report);

    Assert.assertThat(tasks.execute(), Matchers.equalTo(false));
    Assert.assertThat(tasks.isEmpty(), Matchers.equalTo(true));
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.START).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT - 1));
    // not affected
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.INSTALL).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT));
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.UNINSTALL).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT));
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.STOP).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT));

    Mockito.verify(start).test(report);
    Mockito.verify(start2).test(report);
    Mockito.verify(start3).test(report);
    Mockito.verify(stop, Mockito.never()).test(report);
  }

  @Test
  public void testExecuteWhenAllTasksSucceedsWhenOneAttemptLeft() throws Exception {
    final Predicate<ProfileMigrationReport> start = Mockito.mock(Predicate.class);
    final Predicate<ProfileMigrationReport> start2 = Mockito.mock(Predicate.class);
    final Predicate<ProfileMigrationReport> stop = Mockito.mock(Predicate.class);

    tasks.add(Operation.START, "1", start);
    tasks.add(Operation.START, "2", start2);
    tasks.add(Operation.STOP, "1", stop);

    tasks.getAttemptsLeft(Operation.START).set(1);

    Mockito.doReturn(true).when(start).test(report);
    Mockito.doReturn(true).when(start2).test(report);
    Mockito.doReturn(false).when(stop).test(report);

    Assert.assertThat(tasks.execute(), Matchers.equalTo(true));
    Assert.assertThat(tasks.isEmpty(), Matchers.equalTo(true));
    Assert.assertThat(tasks.getAttemptsLeft(Operation.START).get(), Matchers.equalTo(0));
    // not affected
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.INSTALL).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT));
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.UNINSTALL).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT));
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.STOP).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT));

    Mockito.verify(start).test(report);
    Mockito.verify(start2).test(report);
    Mockito.verify(stop, Mockito.never()).test(report);
  }

  @Test
  public void testExecuteWhenNoMoreAtemptsLeft() throws Exception {
    final Predicate<ProfileMigrationReport> start = Mockito.mock(Predicate.class);
    final Predicate<ProfileMigrationReport> start2 = Mockito.mock(Predicate.class);
    final Predicate<ProfileMigrationReport> stop = Mockito.mock(Predicate.class);

    tasks.add(Operation.START, "1", start);
    tasks.add(Operation.START, "2", start2);
    tasks.add(Operation.STOP, "1", stop);

    tasks.getAttemptsLeft(Operation.START).set(0);

    Mockito.doReturn(true).when(start).test(report);
    Mockito.doReturn(true).when(start2).test(report);
    Mockito.doReturn(true).when(stop).test(report);
    Mockito.doReturn(report).when(report).recordOnFinalAttempt(Mockito.notNull());

    Assert.assertThat(tasks.execute(), Matchers.equalTo(false));
    Assert.assertThat(tasks.isEmpty(), Matchers.equalTo(false));
    Assert.assertThat(tasks.getAttemptsLeft(Operation.START).get(), Matchers.lessThanOrEqualTo(0));
    // not affected
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.INSTALL).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT));
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.UNINSTALL).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT));
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.STOP).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT));

    final ArgumentCaptor<MigrationException> error =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(start, Mockito.never()).test(report);
    Mockito.verify(start2, Mockito.never()).test(report);
    Mockito.verify(stop, Mockito.never()).test(report);
    Mockito.verify(report).recordOnFinalAttempt(error.capture());

    Assert.assertThat(
        error.getValue(),
        ThrowableMessageMatcher.hasMessage(
            Matchers.containsString("too many " + TYPE + "s start attempts")));
  }

  @Test
  public void testExecuteWhenEmpty() throws Exception {
    Assert.assertThat(tasks.execute(), Matchers.equalTo(true));
    Assert.assertThat(tasks.isEmpty(), Matchers.equalTo(true));
    // not affected
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.INSTALL).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT));
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.UNINSTALL).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT));
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.START).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT));
    Assert.assertThat(
        tasks.getAttemptsLeft(Operation.STOP).get(),
        Matchers.equalTo(ProfileMigratable.RETRY_COUNT));
  }
}
