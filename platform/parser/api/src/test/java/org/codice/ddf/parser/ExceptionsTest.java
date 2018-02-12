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
package org.codice.ddf.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

public class ExceptionsTest {
  private Exception testCause;

  private String causeMsg;

  private String msg;

  @Before
  public void setUp() throws Exception {
    causeMsg = "Bart";
    msg = "Doh!";
    testCause = new Exception(causeMsg);
  }

  @Test
  public void testParserException() {
    ParserException exception = new ParserException();
    assertNotNull(exception);

    exception = new ParserException(msg);
    assertEquals(msg, exception.getMessage());

    exception = new ParserException(testCause);
    assertEquals(testCause, exception.getCause());

    exception = new ParserException(msg, testCause);
    assertEquals(msg, exception.getMessage());
    assertEquals(testCause, exception.getCause());
  }
}
