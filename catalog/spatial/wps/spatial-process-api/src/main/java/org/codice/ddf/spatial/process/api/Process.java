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
package org.codice.ddf.spatial.process.api;

import java.util.Optional;
import java.util.Set;
import org.codice.ddf.spatial.process.api.description.ExecutionDescription;
import org.codice.ddf.spatial.process.api.description.Metadata;
import org.codice.ddf.spatial.process.api.request.ExecutionRequest;
import org.codice.ddf.spatial.process.api.request.ProcessResult;
import org.codice.ddf.spatial.process.api.request.ProcessStatus;

/**
 * This class is Experimental and subject to change. Process provides an interface for modeling the
 * inputs and outputs of a process so that they can be exposed in a consistent way. For asynchronous
 * processes it encapsulates the logic for interacting with a task framework to initiate or queue
 * execution.
 */
public interface Process {
  /** @return process name identifier */
  String getId();

  /** @return human readable description of the process */
  String getDescription();

  /** @return process version purely descriptive */
  String getVersion();

  /** @return human readable identifier for the process */
  String getTitle();

  /** @return metadata about the process used for discovery */
  Metadata getMetadata();

  /** @return set of Operations available for this process */
  Set<Operation> getOperations();

  /** @return execution description which describes the inputs and outputs of a process */
  ExecutionDescription getExecutionDescription();

  /**
   * @param executionRequest
   * @return ProcessStatus
   */
  ProcessStatus asyncExecute(ExecutionRequest executionRequest);

  /**
   * @param executionRequest
   * @return ProcessResult
   * @throws ProcessException
   */
  ProcessResult syncExecute(ExecutionRequest executionRequest);

  /**
   * get the processMonitor for a specific process this can be an empty optional if the process is
   * only synchronous or doesn't have any means to monitor process execution.
   */
  Optional<ProcessMonitor> getProcessMonitor();
}
