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
import org.codice.ddf.spatial.process.api.request.ProcessResult;
import org.codice.ddf.spatial.process.api.request.ProcessStatus;

/**
 * This class is Experimental and subject to change. ProcessMonitor provides an interface for
 * interacting .with a task framework implementation to monitor and manage process requests.
 */
public interface ProcessMonitor {

  /**
   * polling status for requests, if request for this request id is not found an empty optional
   * should be returned
   *
   * @param requestId to status
   * @return ProcessStatus
   */
  Optional<ProcessStatus> getRequestStatus(String requestId);

  /**
   * dismiss requests, if request for this request id is not found an empty optional should be
   * returned
   *
   * @param requestId to dismiss
   * @return ProcessStatus
   */
  Optional<ProcessStatus> dismissRequest(String requestId);

  /**
   * get results for a request, if request for this request id is not found an empty optional should
   * be returned
   *
   * @param requestId to request results for
   * @return ProcessResult
   */
  Optional<ProcessResult> getRequestResult(String requestId);
}
