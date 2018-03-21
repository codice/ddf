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
import com.google.common.base.Objects;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.MigrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Task list class is used to keep track of tasks that needs to be done. A task can be one for
 * installing, uninstalling, starting, or stopping an application, a feature, or a bundle for
 * example.
 *
 * <p><i>Note:</i> Order of execution for recorded tasks will always be in the order determined by
 * the {@link Operation} enumeration. If tasks are recorded in one of the group then the other
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
   * Holds all groups of tasks grouped per operation. The key in the group of tasks will be some
   * unique identifier for each task to prevent duplication.
   */
  private final Map<Operation, Map<String, Predicate<ProfileMigrationReport>>> groups =
      new EnumMap<>(Operation.class);

  /** Holds all groups of compound info grouped per operation. */
  private final Map<Operation, CompoundTask<?>> compoundGroups = new EnumMap<>(Operation.class);

  /**
   * Holds attempts counters for each operations.
   *
   * <p>The counters stored here will start at the defined retry counter and once they reached 0,
   * the maximum attempts on the corresponding operation will have been reached and we shall stop
   * executing a particular group/operation of tasks for a given type of entities (i.e. features or
   * bundles).
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
      attemptsLeft.put(op, new AtomicInteger(ProfileMigratable.ATTEMPT_COUNT));
    }
  }

  /**
   * Adds a task to be executed for a specific operation. The task will be provided with a profile
   * migration report where to record messages. It should return <code>true</code> if the task was
   * successful or <code>false</code> if not.
   *
   * <p><i>Note:</i> Compound tasks can still be added while executing normal tasks. However, only
   * compound tasks registered for the same operation will actually get executed after all normal
   * tasks for that operation are completed successfully.
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
   * Adds a new or retrieves an already registered compound task to be executed for a specific
   * operation. This method expects to be called consistently for each subtasks of a given operation
   * as the container and the compound task will only be created and registered the first time this
   * method is called. The returned {@link CompoundTask} object provides a way for the client to add
   * subtask information to the container that will later be provided to the registered compound
   * task when it is executed.
   *
   * <p><i>Note:</i> It is recommended to call this method for a given operation in one single place
   * in your code to ensure consistency of the container type and the compound task for each
   * subtasks being added.
   *
   * <p>
   *
   * <pre><code>
   *   private final TaskList tasks = new TaskList();
   *   ...
   *   tasks.addIfAbsent(
   *     Operation.INSTALL,
   *     HashSet<MyObjectClass>::new,
   *     (objects, r) -> [do something with the accumulated objects]
   *   ).add(name, objects -> objects.add(obj));
   * </code></pre>
   *
   * @param <T> the type of the container used to accumulate recorded subtasks
   * @param op the operation for which to record the subtask
   * @param containerFactory a supplier which returns a new, empty container object (only called the
   *     first time this method is called)
   * @param task the compound task to add for later execution (the task will only be registered the
   *     first time this method is called for a given operation)
   * @throws IllegalArgumentException if <code>op</code>, <code>containerFactory</code>, or <code>
   *     task</code>, is <code>null</code>
   */
  public <T> CompoundTask<T> addIfAbsent(
      Operation op, Supplier<T> containerFactory, BiPredicate<T, ProfileMigrationReport> task) {
    Validate.notNull(op, "invalid null operation");
    Validate.notNull(containerFactory, "invalid null container factory");
    Validate.notNull(task, "invalid null task");
    return (CompoundTask<T>)
        compoundGroups.computeIfAbsent(
            op, o -> new CompoundTask<>(op, task, containerFactory.get()));
  }

  /**
   * Checks if this task list is empty.
   *
   * @return <code>true</code> if no tasks were added to this task list; <code>false</code> if at
   *     least one was registered
   */
  public boolean isEmpty() {
    return groups.isEmpty() && compoundGroups.values().stream().allMatch(CompoundTask::isEmpty);
  }

  /**
   * Gets the the first available operation/group of registered tasks.
   *
   * @return the operation associated with the first available group of tasks or empty if no tasks
   *     were added
   */
  public Optional<Operation> getOperation() {
    return Stream.of(Operation.values()) // search based on defined operation order
        .filter(this::isNotEmpty)
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
   */
  public boolean execute() {
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
      compoundGroups
          .entrySet()
          .forEach(
              e ->
                  LOGGER.debug(
                      "{} compound tasks recorded for {}s: {}", e.getKey(), type, e.getValue()));
    }
    final String opName = op.name().toLowerCase(Locale.getDefault());
    final int n = attemptsLeft.get(op).getAndDecrement();

    if (n <= 0) { // too many attempts for this operation already, fail!
      LOGGER.debug("No more {} tasks attempts left for {}s", opName, type);
      report.recordOnFinalAttempt(
          new MigrationException("Import error: too many %ss %s attempts", type, opName));
      return false;
    }
    LOGGER.debug("{} tasks attempts left for {}s: {}", opName, type, n);
    final Map<String, Predicate<ProfileMigrationReport>> tasks = groups.get(op);

    try {
      boolean result = true; // until proven otherwise

      if (tasks != null) {
        Stream<Map.Entry<String, Predicate<ProfileMigrationReport>>> s = tasks.entrySet().stream();

        if (LOGGER.isDebugEnabled()) {
          s = s.peek(e -> LOGGER.debug("Executing {} task for {} '{}'", opName, type, e.getKey()));
        }
        result &=
            s.map(Map.Entry::getValue)
                .map(t -> t.test(report)) // execute each tasks in the first group found
                .reduce(true, (a, b) -> a && b); // 'and' all tasks' results
      }
      final CompoundTask<?> compoundTask = compoundGroups.get(op);

      if (compoundTask != null) {
        LOGGER.debug("Executing {} compound task for {}s", opName, type);
        result &= compoundTask.test(report);
      }
      return result;
    } finally {
      // clear all other tasks since we only want to execute the first group each time we fill the
      // list to ensure we re-compute based on whatever would have changed as a result of executing
      // the tasks for a group
      groups.clear();
      compoundGroups.clear();
    }
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

  @SuppressWarnings("squid:S1452" /* Using wildcards to simplify testing */)
  @VisibleForTesting
  @Nullable
  Map<Operation, CompoundTask<?>> getCompoundTasks() {
    return compoundGroups;
  }

  private boolean isNotEmpty(Operation op) {
    if (groups.containsKey(op)) {
      return true;
    }
    final CompoundTask<?> compoundTask = compoundGroups.get(op);

    return (compoundTask != null) && !compoundTask.isEmpty();
  }

  /**
   * This class represents a given compound task where subtasks can be registered for later
   * execution.
   *
   * @param <T> the type of container used to collect subtasks data.
   */
  public class CompoundTask<T> {
    private final Operation operation;
    private final BiPredicate<T, ProfileMigrationReport> task;
    private final T container;
    private volatile int size;

    private CompoundTask(Operation op, BiPredicate<T, ProfileMigrationReport> task, T container) {
      this(op, task, container, 0);
    }

    @VisibleForTesting
    CompoundTask(Operation op, BiPredicate<T, ProfileMigrationReport> task, T container, int size) {
      this.operation = op;
      this.task = task;
      this.container = container;
      this.size = size;
    }

    /**
     * Adds a subtask to be executed by the corresponding compound task.
     *
     * <p><i>Note:</i> The provided accumulator is called back with the registered container so data
     * specific to the subtask can be added.
     *
     * @param id a unique identifier for the subtask (e.g. feature name)
     * @param accumulator a consumer capable of accumulating data related to this particular subtask
     *     in the provided container for later execution by the compound task; it will receive the
     *     container
     * @return this for chaining
     * @throws IllegalArgumentException if <code>id</code> or <code>accumulator</code> is <code>null
     *     </code>
     */
    public CompoundTask<T> add(String id, Consumer<T> accumulator) {
      Validate.notNull(id, "invalid null subtask id");
      Validate.notNull(accumulator, "invalid null accumulator");
      LOGGER.debug("Recording {} subtask for {} '{}'", operation, type, id);
      report.recordTask();
      accumulator.accept(container);
      size++;
      return this;
    }

    public boolean isEmpty() {
      return size == 0;
    }

    public int size() {
      return size;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(size, operation, task, container);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      } else if (obj instanceof CompoundTask) {
        final CompoundTask<T> ctask = (CompoundTask<T>) obj;

        return ((size == ctask.size)
            && operation.equals(ctask.operation)
            && task.equals(ctask.task)
            && container.equals(ctask.container));
      }
      return false;
    }

    @Override
    public String toString() {
      return container.toString();
    }

    private boolean test(ProfileMigrationReport report) {
      return task.test(container, report);
    }
  }
}
