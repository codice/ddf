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
package org.codice.ddf.pax.web.jetty;

import ch.qos.logback.access.jetty.RequestLogImpl;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

/**
 * Extends {@link RequestLogImpl} to enable/disable logging and to add more information to the logs.
 */
public class AccessRequestLog extends RequestLogImpl {

  private boolean isAccessLogEnabled = true;

  public AccessRequestLog() {
    super();
    isAccessLogEnabled =
        Boolean.valueOf(System.getProperty("org.codice.ddf.http.access.log.enabled"));
  }

  @Override
  public void log(Request jettyRequest, Response jettyResponse) {
    if (isAccessLogEnabled) {
      super.log(jettyRequest, jettyResponse);
    }
  }
}
