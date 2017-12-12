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
package org.codice.ddf.security.filter.websso;

import com.google.common.annotations.VisibleForTesting;
import javax.ws.rs.core.Response.Status;
import org.ops4j.pax.web.service.WebContainer;

public class ErrorPage {

  private WebContainer webContainer;

  @VisibleForTesting
  static Status[] errorCodes = {
    Status.BAD_REQUEST,
    Status.UNAUTHORIZED,
    Status.FORBIDDEN,
    Status.NOT_FOUND,
    Status.METHOD_NOT_ALLOWED,
    Status.NOT_ACCEPTABLE,
    Status.INTERNAL_SERVER_ERROR,
    Status.NOT_IMPLEMENTED,
    Status.BAD_GATEWAY,
    Status.SERVICE_UNAVAILABLE,
    Status.GATEWAY_TIMEOUT
  };

  public ErrorPage(WebContainer webContainer) {
    this.webContainer = webContainer;
  }

  public void registerErrorCodes() {

    for (Status errorCode : errorCodes) {
      webContainer.registerErrorPage(
          errorCode.toString(), "/ErrorServlet", webContainer.getDefaultSharedHttpContext());
    }
  }
}
