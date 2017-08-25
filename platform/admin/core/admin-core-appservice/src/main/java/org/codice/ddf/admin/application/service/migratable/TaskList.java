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

import com.google.common.annotations.VisibleForTesting;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.MigrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Task list class is used to keep track of tasks that needs to be done. A task can be one for
 * installing, uninstalling, starting, or stopping an application, a feature, or a bundle for
 * example.
 *
 * <p><i>Note:</i> Order of execution for recorded tasks will always be in the order: installing,
 * starting, stopping, and uninstalling. If tasks are recorded in one of the group then the other
 * groups will be skipped and the client will be notified that the tasks should be recomputed. The
 * only way a group can be executed is if the previous group in the order is empty to start with.
 * This approach is adopted to verify that the execution of tasks in one group doesn't affect the
 * other groups as is normally the case with features and bundles where starting some may start
 * others that needed to be stopped. It will be required for the client to rebuild a new task list
 * in a loop until the task list becomes empty.
 */
public class TaskList {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskList.class);

  /**
   * Holds all group of tasks grouped per operation. The key in the group of tasks will be some
   * unique identifier for each task to prevent duplication.
   */
  private final Map<Operation, Map<String, Predicate<ProfileMigrationReport>>> groups =
      new EnumMap<>(Operation.class);

  /**
   * Holds attempts counters for each operations.
   *
   * <p>The counters stored here will start at the defined retry counter and once they reached 0,
   * the maximum attempts on the corresponding operation will have been reached and we shall stop
   * executing a particular group/operation of tasks for a given type of entities (i.e. apps,
   * features, or bundles).
   */
  private final Map<Operation, AtomicInteger> attemptsLeft = new EnumMap<>(Operation.class);

  private final String type;

  private final ProfileMigrationReport report;

  /**
   * Constructs a new task list for the given type of objects.
   *
   * @param type the type of object managed by this task list
   * @param report the report where to report that tasks are being recorded
   * @throws IllegalArgumentException if <code>type</code> or <code>report</code> is <code>null
   *     </code>
   */
  public TaskList(String type, ProfileMigrationReport report) {
    Validate.notNull(type, "invalid null task type");
    Validate.notNull(report, "invalid null report");
    this.type = type;
    this.report = report;
    for (final Operation op : Operation.values()) {
      attemptsLeft.put(op, new AtomicInteger(ProfileMigratable.RETRY_COUNT));
    }
  }

  /**
   * Adds a task to be executed for a specific operation. The task will be provided with a profile
   * migration report where to record messages. It should return <code>true</code> if the task was
   * successful or <code>false</code> if not.
   *
   * @param op the operation for which to record the task
   * @param id a unique identifier for the task (e.g. feature name)
   * @param task the task to add for later execution
   * @throws IllegalArgumentException if <code>op</code>, <code>id</code>, or <code>task</code> is
   *     <code>null</code>
   */
  public void add(Operation op, String id, Predicate<ProfileMigrationReport> task) {
    Validate.notNull(op, "invalid null operation");
    Validate.notNull(id, "invalid null task id");
    Validate.notNull(task, "invalid null task");
    LOGGER.debug("Recording {} task for {} '{}'", op, type, id);
    report.recordTask();
    groups
        .computeIfAbsent(op, o -> new LinkedHashMap<>()) // preserve order of execution
        .put(id, task);
  }

  /**
   * Increases the number of attempts left for the specified operation
   *
   * @param op the operation to increase the number of attempts for
   * @throws IllegalArgumentException if <code>op</code> is <code>null</code>
   */
  public void increaseAttemptsFor(Operation op) {
    Validate.notNull(op, "invalid null operation");
    final int n = attemptsLeft.get(op).updateAndGet(a -> (a <= 0) ? 1 : ++a);

    LOGGER.debug("increasing {} task attempts left for {}s to: {}", op, type, n);
  }

  /**
   * Checks if this task list is empty.
   *
   * @return <code>true</code> if no tasks were added to this task list; <code>false</code> if at
   *     least one was registered
   */
  public boolean isEmpty() {
    return groups.isEmpty();
  }

  /**
   * Gets the the first available operation/group of registered tasks.
   *
   * @return the operation associated with the first available group of tasks or empty if no tasks
   *     were added
   */
  public Optional<Operation> getOperation() {
    return Stream.of(Operation.values()) // search based on defined operation order
        .filter(groups::containsKey)
        .findFirst();
  }

  /**
   * Executes all tasks defined in the first available operation/group of tasks.
   *
   * <p><i>Note:</i> The task list will be cleared unless we have exceeded the maximum number of
   * attempts for the first available operation.
   *
   * @return <code>true</code> if all tasks in the first available operation group were successful;
   *     <code>false</code> otherwise or if we have exceeded the maximum number of attempts for the
   *     first available operation
   * @throws IllegalArgumentException if <code>report</code> is <code>null</code>
   */
  public boolean execute() {
    Validate.notNull(report, "invalid null report");
    LOGGER.debug("Executing {}s import", type);
    final Operation op = getOperation().orElse(null);

    if (op == null) { // if no groups have tasks
      LOGGER.debug("No {} tasks recorded", type);
      return true;
    }
    if (LOGGER.isDebugEnabled()) {
      groups
          .entrySet()
          .forEach(
              e ->
                  LOGGER.debug(
                      "{} tasks recorded for {}s: {}", e.getKey(), type, e.getValue().keySet()));
    }
    final int n = attemptsLeft.get(op).getAndDecrement();

    if (n <= 0) { // too many attempts for this operation already, fail!
      LOGGER.debug("No more {} tasks attempts left for {}s", op, type);
      report.recordOnFinalAttempt(
          new MigrationException(
              "Import error: too many %ss %s attempts",
              type, op.name().toLowerCase(Locale.getDefault())));
      return false;
    }
    LOGGER.debug("{} tasks attempts left for {}s: {}", op, type, n);
    final Map<String, Predicate<ProfileMigrationReport>> tasks = groups.get(op);

    // clear all other tasks since we only want to execute the first group each time we fill the
    // list to ensure we re-compute based on whatever would have changed as a result of executing
    // the tasks for a group
    groups.clear();
    // tasks map cannot be null by design since we do not store null in groups
    return tasks
        .entrySet()
        .stream()
        .peek(e -> LOGGER.debug("Executing {} task for {} '{}'", op, type, e.getKey()))
        .map(Map.Entry::getValue)
        .map(t -> t.test(report)) // execute each tasks in the first group found
        .reduce(true, (a, b) -> a && b); // 'and' all tasks' results
  }

  @VisibleForTesting
  String getType() {
    return type;
  }

  @VisibleForTesting
  AtomicInteger getAttemptsLeft(Operation op) {
    return attemptsLeft.get(op);
  }

  @VisibleForTesting
  Map<Operation, Map<String, Predicate<ProfileMigrationReport>>> getTasks() {
    return groups;
  }
}
