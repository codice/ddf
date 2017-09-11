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

import org.codice.ddf.spatial.process.api.request.ProcessResult;
import org.codice.ddf.spatial.process.api.request.ProcessStatus;

/** This class is Experimental and subject to change */
public interface ProcessEventMonitor extends ProcessMonitor {

  /**
   * notify a process monitor of an update to a request
   *
   * @param requestId
   * @param processStatus
   */
  void updateRequestStatus(String requestId, ProcessStatus processStatus);

  /**
   * notify a process monitor of result for a request
   *
   * @param requestId
   * @param processResult
   */
  void setRequestResult(String requestId, ProcessResult processResult);
}
