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

import java.util.Collections;
import java.util.Set;
import javax.annotation.Nullable;

public class ProcessException extends RuntimeException {
  private final String jobId;

  private final Set<String> inputIds;

  public ProcessException(Throwable cause) {
    super(cause);
    this.jobId = null;
    this.inputIds = Collections.emptySet();
  }

  public ProcessException(String message) {
    super(message);
    this.jobId = null;
    this.inputIds = Collections.emptySet();
  }

  public ProcessException(String message, Throwable cause) {
    super(message, cause);
    this.jobId = null;
    this.inputIds = Collections.emptySet();
  }

  public ProcessException(Set<String> inputIds, String message) {
    super(message);
    this.jobId = null;
    this.inputIds = inputIds;
  }

  public ProcessException(Set<String> inputIds, String message, Throwable cause) {
    super(message, cause);
    this.jobId = null;
    this.inputIds = inputIds;
  }

  public ProcessException(String jobId, Set<String> inputIds, String message) {
    super(message);
    this.jobId = jobId;
    this.inputIds = inputIds;
  }

  public ProcessException(String jobId, Set<String> inputIds, String message, Throwable cause) {
    super(message, cause);
    this.jobId = jobId;
    this.inputIds = inputIds;
  }

  @Nullable
  public String getJobId() {
    return jobId;
  }

  public Set<String> getInputIds() {
    return inputIds;
  }
}
