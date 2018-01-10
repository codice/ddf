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
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.codice.ddf.migration.MigrationException;
import org.hamcrest.Matchers;
import org.hamcrest.junit.internal.ThrowableMessageMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class TaskListTest {
  private static final String TYPE = "something";

  // this will help make these test cases independent of the Operation enum
  // such that if we decide to re-order the operations, these test cases will continue
  // to work as they only care about what happens if a higher priority task is registered
  private static final Operation OPERATION_A = Operation.values()[0];
  private static final Operation OPERATION_B = Operation.values()[1];
  private static final Operation OPERATION_C = Operation.values()[2];
  private static final Operation OPERATION_D = Operation.values()[3];

  private static final String ID = "id";
  private static final String ID2 = "id2";

  private static final Object CONTAINER = new Object();

  private final ProfileMigrationReport report = Mockito.mock(ProfileMigrationReport.class);

  private final TaskList tasks = new TaskList(TYPE, report);

  private final Predicate<ProfileMigrationReport> task = Mockito.mock(Predicate.class);
  private final Predicate<ProfileMigrationReport> task2 = Mockito.mock(Predicate.class);

  private final Supplier<Object> containerFactory = Mockito.mock(Supplier.class);
  private final BiPredicate<Object, ProfileMigrationReport> compoundTask =
      Mockito.mock(BiPredicate.class);
  private final Consumer<Object> accumulator = Mockito.mock(Consumer.class);

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() throws Exception {
    Mockito.doReturn(CONTAINER).when(containerFactory).get();
    Mockito.doNothing().when(accumulator).accept(CONTAINER);
    Mockito.doReturn(report).when(report).recordTask();
  }

  @Test
  public void testConstructor() throws Exception {
    Assert.assertThat(tasks.getType(), Matchers.equalTo(TYPE));
    Assert.assertThat(tasks.isEmpty(), Matchers.equalTo(true));
    for (final Operation op : Operation.values()) {
      Assert.assertThat(
          tasks.getAttemptsLeft(op).get(), Matchers.equalTo(ProfileMigratable.ATTEMPT_COUNT));
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
    tasks.add(OPERATION_A, ID, task);

    Assert.assertThat(
        tasks.getTasks(),
        Matchers.allOf(
            Matchers.aMapWithSize(1),
            Matchers.hasEntry(
                Matchers.equalTo(OPERATION_A),
                Matchers.allOf(
                    Matchers.aMapWithSize(1),
                    Matchers.hasEntry(Matchers.equalTo(ID), Matchers.sameInstance(task))))));

    Mockito.verify(report).recordTask();
  }

  @Test
  public void testAddSecondTaskForSameOperation() throws Exception {
    tasks.add(OPERATION_A, ID, task);
    tasks.add(OPERATION_A, ID2, task2);

    Assert.assertThat(
        tasks.getTasks(),
        Matchers.allOf(
            Matchers.aMapWithSize(1),
            Matchers.hasEntry(
                Matchers.equalTo(OPERATION_A),
                Matchers.allOf(
                    Matchers.aMapWithSize(2),
                    Matchers.hasEntry(Matchers.equalTo(ID), Matchers.sameInstance(task)),
                    Matchers.hasEntry(Matchers.equalTo("id2"), Matchers.sameInstance(task2))))));

    Mockito.verify(report, Mockito.times(2)).recordTask();
  }

  @Test
  public void testAddSecondTaskForSameId() throws Exception {
    tasks.add(OPERATION_A, ID, task);
    tasks.add(OPERATION_D, ID, task2);

    Assert.assertThat(
        tasks.getTasks(),
        Matchers.allOf(
            Matchers.aMapWithSize(2),
            Matchers.hasEntry(
                Matchers.equalTo(OPERATION_A),
                Matchers.allOf(
                    Matchers.aMapWithSize(1),
                    Matchers.hasEntry(Matchers.equalTo(ID), Matchers.sameInstance(task)))),
            Matchers.hasEntry(
                Matchers.equalTo(OPERATION_D),
                Matchers.allOf(
                    Matchers.aMapWithSize(1),
                    Matchers.hasEntry(Matchers.equalTo(ID), Matchers.sameInstance(task2))))));

    Mockito.verify(report, Mockito.times(2)).recordTask();
  }

  @Test
  public void testAddWithNullOperation() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null operation"));

    tasks.add(null, ID, task);
  }

  @Test
  public void testAddWithNullId() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null task id"));

    tasks.add(OPERATION_A, null, task);
  }

  @Test
  public void testAddWithNullTask() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null task"));

    tasks.add(OPERATION_A, ID, null);
  }

  @Test
  public void testAddIfAbsentFirstTime() throws Exception {
    tasks.addIfAbsent(OPERATION_A, containerFactory, compoundTask).add(ID, accumulator);

    Assert.assertThat(
        tasks.getCompoundTasks(),
        Matchers.allOf(
            Matchers.aMapWithSize(1),
            Matchers.hasEntry(
                Matchers.equalTo(OPERATION_A),
                Matchers.equalTo(
                    tasks.new CompoundTask<>(OPERATION_A, compoundTask, CONTAINER, 1)))));

    Mockito.verify(containerFactory).get();
    Mockito.verify(accumulator).accept(CONTAINER);
    Mockito.verify(report).recordTask();
  }

  @Test
  public void testAddIfAbsentTwiceUsingChaining() throws Exception {
    tasks
        .addIfAbsent(OPERATION_A, containerFactory, compoundTask)
        .add(ID, accumulator)
        .add(ID2, accumulator);

    Assert.assertThat(
        tasks.getCompoundTasks(),
        Matchers.allOf(
            Matchers.aMapWithSize(1),
            Matchers.hasEntry(
                Matchers.equalTo(OPERATION_A),
                Matchers.equalTo(
                    tasks.new CompoundTask<>(OPERATION_A, compoundTask, CONTAINER, 2)))));

    Mockito.verify(containerFactory).get();
    Mockito.verify(accumulator, Mockito.times(2)).accept(CONTAINER);
    Mockito.verify(report, Mockito.times(2)).recordTask();
  }

  @Test
  public void testAddIfAbsentSecondTime() throws Exception {
    tasks.addIfAbsent(OPERATION_A, containerFactory, compoundTask).add(ID, accumulator);

    Mockito.reset(containerFactory);
    Mockito.reset(accumulator);
    Mockito.reset(report);

    tasks.addIfAbsent(OPERATION_A, containerFactory, compoundTask).add(ID2, accumulator);

    Assert.assertThat(
        tasks.getCompoundTasks(),
        Matchers.allOf(
            Matchers.aMapWithSize(1),
            Matchers.hasEntry(
                Matchers.equalTo(OPERATION_A),
                Matchers.equalTo(
                    tasks.new CompoundTask<>(OPERATION_A, compoundTask, CONTAINER, 2)))));

    Mockito.verify(containerFactory, Mockito.never()).get();
    Mockito.verify(accumulator).accept(CONTAINER);
    Mockito.verify(report).recordTask();
  }

  @Test
  public void testAddIfAbsentWithNullOperation() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null operation"));

    tasks.addIfAbsent(null, containerFactory, compoundTask);
  }

  @Test
  public void testAddIfAbsentWithNullContainerFactory() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null container factory"));

    tasks.addIfAbsent(OPERATION_A, null, compoundTask);
  }

  @Test
  public void testAddIfAbsentWithNullTask() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null task"));

    tasks.addIfAbsent(OPERATION_A, containerFactory, null);
  }

  @Test
  public void testAddIfAbsentWithNullId() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null subtask id"));

    tasks.addIfAbsent(OPERATION_A, containerFactory, compoundTask).add(null, accumulator);
  }

  @Test
  public void testAddIfAbsentWithNullAccumulator() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null accumulator"));

    tasks.addIfAbsent(OPERATION_A, containerFactory, compoundTask).add(ID, null);
  }

  @Test
  public void testIsEmptyWhenEmpty() throws Exception {
    Assert.assertThat(tasks.isEmpty(), Matchers.equalTo(true));
  }

  @Test
  public void testIsEmptyWhenTasksWereAdded() throws Exception {
    tasks.add(OPERATION_A, ID, task);

    Assert.assertThat(tasks.isEmpty(), Matchers.equalTo(false));
  }

  @Test
  public void testIsEmptyWhenCompoundTasksWereAdded() throws Exception {
    tasks.addIfAbsent(OPERATION_A, containerFactory, compoundTask).add(ID, accumulator);

    Assert.assertThat(tasks.isEmpty(), Matchers.equalTo(false));
  }

  @Test
  public void testIsEmptyWhenEmptyCompoundTasksWereAdded() throws Exception {
    tasks.addIfAbsent(OPERATION_A, containerFactory, compoundTask);

    Assert.assertThat(tasks.isEmpty(), Matchers.equalTo(true));
  }

  @Test
  public void testGetOperationWhenEmpty() throws Exception {
    Assert.assertThat(tasks.getOperation(), OptionalMatchers.isEmpty());
  }

  @Test
  public void testGetOperationWhenInstallTaskAdded() throws Exception {
    tasks.add(OPERATION_A, ID, task);
    tasks.add(OPERATION_B, ID, task);
    tasks.add(OPERATION_C, ID, task);
    tasks.add(OPERATION_D, ID, task);
    tasks.addIfAbsent(OPERATION_B, containerFactory, compoundTask).add(ID, accumulator);
    tasks.addIfAbsent(OPERATION_C, containerFactory, compoundTask).add(ID, accumulator);
    tasks.addIfAbsent(OPERATION_D, containerFactory, compoundTask).add(ID, accumulator);

    Assert.assertThat(tasks.getOperation(), OptionalMatchers.hasValue(OPERATION_A));
  }

  @Test
  public void testGetOperationWhenCompoundInstallTaskAdded() throws Exception {
    tasks.add(OPERATION_B, ID, task);
    tasks.add(OPERATION_C, ID, task);
    tasks.add(OPERATION_D, ID, task);
    tasks.addIfAbsent(OPERATION_A, containerFactory, compoundTask).add(ID, accumulator);
    tasks.addIfAbsent(OPERATION_B, containerFactory, compoundTask).add(ID, accumulator);
    tasks.addIfAbsent(OPERATION_C, containerFactory, compoundTask).add(ID, accumulator);
    tasks.addIfAbsent(OPERATION_D, containerFactory, compoundTask).add(ID, accumulator);

    Assert.assertThat(tasks.getOperation(), OptionalMatchers.hasValue(OPERATION_A));
  }

  @Test
  public void testGetOperationWhenStartTaskAddedAndNoInstallTasksAdded() throws Exception {
    tasks.add(OPERATION_B, ID, task);
    tasks.add(OPERATION_C, ID, task);
    tasks.add(OPERATION_D, ID, task);
    tasks.addIfAbsent(OPERATION_C, containerFactory, compoundTask).add(ID, accumulator);
    tasks.addIfAbsent(OPERATION_D, containerFactory, compoundTask).add(ID, accumulator);

    Assert.assertThat(tasks.getOperation(), OptionalMatchers.hasValue(OPERATION_B));
  }

  @Test
  public void testGetOperationWhenStartCompoundTaskAddedAndNoInstallTasksAdded() throws Exception {
    tasks.add(OPERATION_C, ID, task);
    tasks.add(OPERATION_D, ID, task);
    tasks.addIfAbsent(OPERATION_B, containerFactory, compoundTask).add(ID, accumulator);
    tasks.addIfAbsent(OPERATION_C, containerFactory, compoundTask).add(ID, accumulator);
    tasks.addIfAbsent(OPERATION_D, containerFactory, compoundTask).add(ID, accumulator);

    Assert.assertThat(tasks.getOperation(), OptionalMatchers.hasValue(OPERATION_B));
  }

  @Test
  public void testGetOperationWhenStopTaskAddedAndNoInstallOrStartTasksAdded() throws Exception {
    tasks.add(OPERATION_C, ID, task);
    tasks.add(OPERATION_D, ID, task);
    tasks.addIfAbsent(OPERATION_D, containerFactory, compoundTask).add(ID, accumulator);

    Assert.assertThat(tasks.getOperation(), OptionalMatchers.hasValue(OPERATION_C));
  }

  @Test
  public void testGetOperationWhenStopCompoundTaskAddedAndNoInstallOrStartTasksAdded()
      throws Exception {
    tasks.add(OPERATION_D, ID, task);
    tasks.addIfAbsent(OPERATION_C, containerFactory, compoundTask).add(ID, accumulator);
    tasks.addIfAbsent(OPERATION_D, containerFactory, compoundTask).add(ID, accumulator);

    Assert.assertThat(tasks.getOperation(), OptionalMatchers.hasValue(OPERATION_C));
  }

  @Test
  public void testGetOperationWhenUninstallTaskAddedAndNoInstallOrStartOrStopTasksAdded()
      throws Exception {
    tasks.add(OPERATION_D, ID, task);

    Assert.assertThat(tasks.getOperation(), OptionalMatchers.hasValue(OPERATION_D));
  }

  @Test
  public void testGetOperationWhenUninstallCompoundTaskAddedAndNoInstallOrStartOrStopTasksAdded()
      throws Exception {
    tasks.addIfAbsent(OPERATION_D, containerFactory, compoundTask).add(ID, accumulator);

    Assert.assertThat(tasks.getOperation(), OptionalMatchers.hasValue(OPERATION_D));
  }

  @Test
  public void testExecuteWhenAllTasksSucceeds() throws Exception {
    final Predicate<ProfileMigrationReport> taskB = Mockito.mock(Predicate.class);
    final Predicate<ProfileMigrationReport> taskB2 = Mockito.mock(Predicate.class);
    final Predicate<ProfileMigrationReport> taskC = Mockito.mock(Predicate.class);
    final BiPredicate<Object, ProfileMigrationReport> compoundTaskB =
        Mockito.mock(BiPredicate.class);
    final BiPredicate<Object, ProfileMigrationReport> compoundTaskC =
        Mockito.mock(BiPredicate.class);

    tasks.add(OPERATION_B, "1", taskB);
    tasks.add(OPERATION_B, "2", taskB2);
    tasks.add(OPERATION_C, "1", taskC);
    tasks.addIfAbsent(OPERATION_B, containerFactory, compoundTaskB).add(ID, accumulator);
    tasks.addIfAbsent(OPERATION_C, containerFactory, compoundTaskC).add(ID, accumulator);

    Mockito.doReturn(true).when(taskB).test(report);
    Mockito.doReturn(true).when(taskB2).test(report);
    Mockito.doReturn(false).when(taskC).test(report);
    Mockito.doReturn(true).when(compoundTaskB).test(CONTAINER, report);
    Mockito.doReturn(false).when(compoundTaskC).test(CONTAINER, report);

    Assert.assertThat(tasks.execute(), Matchers.equalTo(true));
    Assert.assertThat(tasks.isEmpty(), Matchers.equalTo(true));

    verifyAttempts(0, 1, 0, 0);
    Mockito.verify(taskB).test(report);
    Mockito.verify(taskB2).test(report);
    Mockito.verify(taskC, Mockito.never()).test(report);
    Mockito.verify(compoundTaskB).test(CONTAINER, report);
    Mockito.verify(compoundTaskC, Mockito.never()).test(CONTAINER, report);
  }

  @Test
  public void testExecuteWhenAttemptsAreLeftAndSomeTasksFails() throws Exception {
    final Predicate<ProfileMigrationReport> opB = Mockito.mock(Predicate.class);
    final Predicate<ProfileMigrationReport> opB2 = Mockito.mock(Predicate.class);
    final Predicate<ProfileMigrationReport> opB3 = Mockito.mock(Predicate.class);
    final Predicate<ProfileMigrationReport> opC = Mockito.mock(Predicate.class);
    final BiPredicate<Object, ProfileMigrationReport> compoundTaskB =
        Mockito.mock(BiPredicate.class);
    final BiPredicate<Object, ProfileMigrationReport> compoundTaskC =
        Mockito.mock(BiPredicate.class);

    tasks.add(OPERATION_B, "1", opB);
    tasks.add(OPERATION_B, "2", opB2);
    tasks.add(OPERATION_B, "3", opB3);
    tasks.add(OPERATION_C, "1", opC);
    tasks.addIfAbsent(OPERATION_B, containerFactory, compoundTaskB).add(ID, accumulator);
    tasks.addIfAbsent(OPERATION_C, containerFactory, compoundTaskC).add(ID, accumulator);

    Mockito.doReturn(false).when(opB).test(report);
    Mockito.doReturn(true).when(opB2).test(report);
    Mockito.doReturn(false).when(opB3).test(report);
    Mockito.doReturn(true).when(opC).test(report);
    Mockito.doReturn(false).when(compoundTaskB).test(CONTAINER, report);
    Mockito.doReturn(true).when(compoundTaskC).test(CONTAINER, report);

    Assert.assertThat(tasks.execute(), Matchers.equalTo(false));
    Assert.assertThat(tasks.isEmpty(), Matchers.equalTo(true));

    verifyAttempts(0, 1, 0, 0);
    Mockito.verify(opB).test(report);
    Mockito.verify(opB2).test(report);
    Mockito.verify(opB3).test(report);
    Mockito.verify(opC, Mockito.never()).test(report);
    Mockito.verify(compoundTaskB).test(CONTAINER, report);
    Mockito.verify(compoundTaskC, Mockito.never()).test(CONTAINER, report);
  }

  @Test
  public void testExecuteWhenAllTasksSucceedsWhenOneAttemptLeft() throws Exception {
    final Predicate<ProfileMigrationReport> opB = Mockito.mock(Predicate.class);
    final Predicate<ProfileMigrationReport> opB2 = Mockito.mock(Predicate.class);
    final Predicate<ProfileMigrationReport> opC = Mockito.mock(Predicate.class);
    final BiPredicate<Object, ProfileMigrationReport> compoundTaskB =
        Mockito.mock(BiPredicate.class);
    final BiPredicate<Object, ProfileMigrationReport> compoundTaskC =
        Mockito.mock(BiPredicate.class);

    tasks.add(OPERATION_B, "1", opB);
    tasks.add(OPERATION_B, "2", opB2);
    tasks.add(OPERATION_C, "1", opC);
    tasks.addIfAbsent(OPERATION_B, containerFactory, compoundTaskB).add(ID, accumulator);
    tasks.addIfAbsent(OPERATION_C, containerFactory, compoundTaskC).add(ID, accumulator);

    tasks.getAttemptsLeft(OPERATION_B).set(1);

    Mockito.doReturn(true).when(opB).test(report);
    Mockito.doReturn(true).when(opB2).test(report);
    Mockito.doReturn(false).when(opC).test(report);
    Mockito.doReturn(true).when(compoundTaskB).test(CONTAINER, report);
    Mockito.doReturn(false).when(compoundTaskC).test(CONTAINER, report);

    Assert.assertThat(tasks.execute(), Matchers.equalTo(true));
    Assert.assertThat(tasks.isEmpty(), Matchers.equalTo(true));

    verifyAttempts(0, ProfileMigratable.ATTEMPT_COUNT, 0, 0);
    Mockito.verify(opB).test(report);
    Mockito.verify(opB2).test(report);
    Mockito.verify(opC, Mockito.never()).test(report);
    Mockito.verify(compoundTaskB).test(CONTAINER, report);
    Mockito.verify(compoundTaskC, Mockito.never()).test(CONTAINER, report);
  }

  @Test
  public void testExecuteWhenNoMoreAtemptsLeft() throws Exception {
    final Predicate<ProfileMigrationReport> opB = Mockito.mock(Predicate.class);
    final Predicate<ProfileMigrationReport> opB2 = Mockito.mock(Predicate.class);
    final Predicate<ProfileMigrationReport> opC = Mockito.mock(Predicate.class);
    final BiPredicate<Object, ProfileMigrationReport> compoundTaskB =
        Mockito.mock(BiPredicate.class);
    final BiPredicate<Object, ProfileMigrationReport> compoundTaskC =
        Mockito.mock(BiPredicate.class);

    tasks.add(OPERATION_B, "1", opB);
    tasks.add(OPERATION_B, "2", opB2);
    tasks.add(OPERATION_C, "1", opC);
    tasks.addIfAbsent(OPERATION_B, containerFactory, compoundTaskB).add(ID, accumulator);
    tasks.addIfAbsent(OPERATION_C, containerFactory, compoundTaskC).add(ID, accumulator);

    tasks.getAttemptsLeft(OPERATION_B).set(0);

    Mockito.doReturn(true).when(opB).test(report);
    Mockito.doReturn(true).when(opB2).test(report);
    Mockito.doReturn(true).when(opC).test(report);
    Mockito.doReturn(true).when(compoundTaskB).test(CONTAINER, report);
    Mockito.doReturn(true).when(compoundTaskC).test(CONTAINER, report);
    Mockito.doReturn(report).when(report).recordOnFinalAttempt(Mockito.notNull());

    Assert.assertThat(tasks.execute(), Matchers.equalTo(false));
    Assert.assertThat(tasks.isEmpty(), Matchers.equalTo(false));

    final ArgumentCaptor<MigrationException> error =
        ArgumentCaptor.forClass(MigrationException.class);

    verifyAttempts(0, ProfileMigratable.ATTEMPT_COUNT + 1, 0, 0);
    Mockito.verify(opB, Mockito.never()).test(report);
    Mockito.verify(opB2, Mockito.never()).test(report);
    Mockito.verify(opC, Mockito.never()).test(report);
    Mockito.verify(compoundTaskB, Mockito.never()).test(CONTAINER, report);
    Mockito.verify(compoundTaskC, Mockito.never()).test(CONTAINER, report);
    Mockito.verify(report).recordOnFinalAttempt(error.capture());

    Assert.assertThat(
        error.getValue(),
        ThrowableMessageMatcher.hasMessage(
            Matchers.matchesPattern(".*too many " + TYPE + "s .* attempts")));
  }

  @Test
  public void testExecuteWhenEmpty() throws Exception {
    Assert.assertThat(tasks.execute(), Matchers.equalTo(true));
    Assert.assertThat(tasks.isEmpty(), Matchers.equalTo(true));

    verifyAttempts(0, 0, 0, 0);
  }

  private void verifyAttempts(int attemptsA, int attemptsB, int attemptsC, int attemptsD) {
    Assert.assertThat(
        tasks.getAttemptsLeft(OPERATION_A).get(),
        Matchers.equalTo(ProfileMigratable.ATTEMPT_COUNT - attemptsA));
    Assert.assertThat(
        tasks.getAttemptsLeft(OPERATION_B).get(),
        Matchers.equalTo(ProfileMigratable.ATTEMPT_COUNT - attemptsB));
    Assert.assertThat(
        tasks.getAttemptsLeft(OPERATION_C).get(),
        Matchers.equalTo(ProfileMigratable.ATTEMPT_COUNT - attemptsC));
    Assert.assertThat(
        tasks.getAttemptsLeft(OPERATION_D).get(),
        Matchers.equalTo(ProfileMigratable.ATTEMPT_COUNT - attemptsD));
  }
}
