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
package org.codice.ddf.spatial.process.api.request;

import java.time.LocalDateTime;
import javax.annotation.Nullable;

/** This class is Experimental and subject to change */
public class ProcessStatus {

  private LocalDateTime statusTime = LocalDateTime.now();

  private Status status;

  private String requestId;

  private Integer percentComplete;

  private LocalDateTime estimatedCompletionTime;

  private String message;

  public ProcessStatus(String requestId, Status status) {
    this.requestId = requestId;
    this.status = status;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  @Nullable
  public Integer getPercentComplete() {
    return percentComplete;
  }

  public void setPercentComplete(Integer percentComplete) {
    this.percentComplete = percentComplete;
  }

  @Nullable
  public LocalDateTime getEstimatedCompletionTime() {
    return estimatedCompletionTime;
  }

  public void setEstimatedCompletionTime(LocalDateTime estimatedCompletionTime) {
    this.estimatedCompletionTime = estimatedCompletionTime;
  }

  @Nullable
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Nullable
  public LocalDateTime getStatusTime() {
    return statusTime;
  }

  public void setStatusTime(LocalDateTime statusTime) {
    this.statusTime = statusTime;
  }
}
