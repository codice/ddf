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

import java.util.Objects;
import org.apache.shiro.authc.AuthenticationToken;

/**
 * Encapsulates the return status for each handler. Consists of the status of any action taken by
 * the handler (successfully retrieved desired tokens, responded to a client in order to obtain
 * missing tokens, or no action taken), as well as the actual tokens retrieved from the header.
 */
public class HandlerResult {
  private Status status;

  private String source;

  private AuthenticationToken token;

  public HandlerResult() {
    status = Status.NO_ACTION;
  }

  public HandlerResult(Status fs, BaseAuthenticationToken t) {
    this.status = fs;
    this.token = t;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {

    this.status = status;
  }

  public AuthenticationToken getToken() {
    return this.token;
  }

  public void setToken(AuthenticationToken token) {
    this.token = token;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String src) {
    this.source = src;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Status: ");
    sb.append(status.toString());
    sb.append("; Source: ");
    sb.append(source);
    sb.append("; Token: ");
    sb.append(Objects.isNull(token) ? "null" : token.toString());
    return sb.toString();
  }

  public enum Status {
    // completed - auth tokens retrieved ready to move on
    COMPLETED,

    // no tokens found, no attempt made to obtain any
    NO_ACTION,

    // performing action to obtain auth tokens, stop processing
    REDIRECTED
  }
}
