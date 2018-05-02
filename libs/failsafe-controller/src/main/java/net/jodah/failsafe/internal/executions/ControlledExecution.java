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
package net.jodah.failsafe.internal.executions;

import javax.annotation.Nullable;
import net.jodah.failsafe.ControlledExecutionException;

/**
 * Interface representing a failsafe execution that can be tracked.
 *
 * @param <R> the result type
 */
public interface ControlledExecution<R> {
  /**
   * Gets a unique identifier for this execution with the associated controller.
   *
   * @return a unique identifier for this execution
   */
  public int getId();

  /**
   * Checks if this execution completed successfully or not.
   *
   * @return <code>true</code> if the execution completed; <code>false</code> otherwise
   */
  public boolean hasCompleted();

  /**
   * Checks if this execution completed successfully.
   *
   * @return <code>true</code> if the execution completed successfully; <code>false</code> otherwise
   */
  public boolean hasSuccessfullyCompleted();

  /**
   * Gets the successful completion result if this execution has successfully completed.
   *
   * <p><code>null</code> Can be returned if the execution has not yet completed, completed
   * successfully with <code>null</code>, or completed with a failure.
   *
   * @return the successful completion result
   */
  @Nullable
  public R getResult();

  /**
   * Gets the completion result or throw an error if this execution failed.
   *
   * <p><code>null</code> Can be returned if the execution has not yet completed or if completed
   * successfully with <code>null</code>.
   *
   * @return the successful completion result
   * @throws ControlledExecutionException if the execution failed with an error
   */
  @Nullable
  public R getResultOrThrowError() throws ControlledExecutionException;

  /**
   * Gets the completion error if this execution failed.
   *
   * <p><code>null</code> Can be either be returned if the execution has not yet completed or
   * completed successfully.
   *
   * @return the completion error
   */
  @Nullable
  public Throwable getError();

  /**
   * Checks if the specified thread is associated with this execution at the moment.
   *
   * @param thread the thread to check for
   * @return <code>true</code> if the specified thread is currently associated with this execution;
   *     <code>false</code> otherwise
   */
  public boolean isAssociatedWith(Thread thread);

  /**
   * Indication that the execution has completed.
   *
   * @param result the result from the execution or <code>null</code> if it failed or if the result
   *     is <code>null</code>
   * @param error the error that caused the execution to fail or <code>null</code> if it didn't fail
   */
  public void onCompletion(@Nullable R result, @Nullable Throwable error);

  /** Indicates to shutdown this execution. */
  public void shutdown();
}
