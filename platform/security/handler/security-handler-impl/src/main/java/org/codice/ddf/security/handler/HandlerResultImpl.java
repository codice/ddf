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
package org.codice.ddf.security.handler;

import java.util.Objects;
import org.apache.shiro.authc.AuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;

/**
 * Encapsulates the return status for each handler. Consists of the status of any action taken by
 * the handler (successfully retrieved desired tokens, responded to a client in order to obtain
 * missing tokens, or no action taken), as well as the actual tokens retrieved from the header.
 */
public class HandlerResultImpl implements HandlerResult {
  private Status status;

  private String source;

  private AuthenticationToken token;

  public HandlerResultImpl() {
    status = Status.NO_ACTION;
  }

  public HandlerResultImpl(Status fs, BaseAuthenticationToken t) {
    this.status = fs;
    this.token = t;
  }

  @Override
  public Status getStatus() {
    return status;
  }

  @Override
  public void setStatus(Status status) {

    this.status = status;
  }

  @Override
  public AuthenticationToken getToken() {
    return this.token;
  }

  @Override
  public void setToken(AuthenticationToken token) {
    this.token = token;
  }

  @Override
  public String getSource() {
    return source;
  }

  @Override
  public void setSource(String src) {
    this.source = src;
  }

  @Override
  public String toString() {
    return String.format(
        "Status: %s; Source: %s; Token: %s",
        status.toString(), source, Objects.isNull(token) ? "null" : token.toString());
  }
}
