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
package ddf.security.assertion.impl;

import ddf.security.assertion.AuthenticationStatement;
import org.joda.time.DateTime;

public class AuthenticationStatementDefault implements AuthenticationStatement {

  private DateTime authnInstant;

  private String sessionIndex;

  private DateTime sessionNotOnOrAfter;

  private String authnContextClassRef;

  @Override
  public DateTime getAuthnInstant() {
    return authnInstant;
  }

  @Override
  public void setAuthnInstant(DateTime authnInstant) {
    this.authnInstant = authnInstant;
  }

  @Override
  public String getSessionIndex() {
    return sessionIndex;
  }

  @Override
  public void setSessionIndex(String sessionIndex) {
    this.sessionIndex = sessionIndex;
  }

  @Override
  public DateTime getSessionNotOnOrAfter() {
    return sessionNotOnOrAfter;
  }

  @Override
  public void setSessionNotOnOrAfter(DateTime sessionNotOnOrAfter) {
    this.sessionNotOnOrAfter = sessionNotOnOrAfter;
  }

  @Override
  public String getAuthnContextClassRef() {
    return authnContextClassRef;
  }

  @Override
  public void setAuthnContextClassRef(String authnContextClassRef) {
    this.authnContextClassRef = authnContextClassRef;
  }
}
