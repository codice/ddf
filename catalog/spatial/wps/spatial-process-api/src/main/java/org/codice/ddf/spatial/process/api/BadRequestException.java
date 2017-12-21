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

import java.util.Set;

public class BadRequestException extends ProcessException {

  public BadRequestException(Set<String> inputIds, String message) {
    super(inputIds, message);
  }

  public BadRequestException(String jobId, Set<String> inputIds, String message) {
    super(jobId, inputIds, message);
  }

  public BadRequestException(String jobId, Set<String> inputIds, String message, Throwable cause) {
    super(jobId, inputIds, message, cause);
  }
}
