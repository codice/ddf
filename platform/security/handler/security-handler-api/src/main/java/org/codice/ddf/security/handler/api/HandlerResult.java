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
package org.codice.ddf.security.handler.api;

import org.apache.shiro.authc.AuthenticationToken;

public interface HandlerResult {

  Status getStatus();

  void setStatus(Status status);

  AuthenticationToken getToken();

  void setToken(AuthenticationToken token);

  String getSource();

  void setSource(String src);

  enum Status {
    // completed - auth tokens retrieved ready to move on
    COMPLETED,

    // no tokens found, no attempt made to obtain any
    NO_ACTION,

    // performing action to obtain auth tokens, stop processing
    REDIRECTED
  }
}
