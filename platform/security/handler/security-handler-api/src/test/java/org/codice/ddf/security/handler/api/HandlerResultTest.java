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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HandlerResultTest {

  /**
   * This test ensures that when the constructor is called, the data members are properly
   * initialized.
   */
  @Test
  public void testHandlerResultConstructor() {
    HandlerResult result = new HandlerResult();
    assertEquals(HandlerResult.Status.NO_ACTION, result.getStatus());

    STSAuthenticationToken token = new STSAuthenticationToken("x", "y", "127.0.0.1");
    result = new HandlerResult(HandlerResult.Status.COMPLETED, token);
    assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
    assertEquals(result.getToken(), token);
  }
}
