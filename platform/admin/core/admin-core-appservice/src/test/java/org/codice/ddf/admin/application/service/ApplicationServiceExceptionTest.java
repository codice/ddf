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
package org.codice.ddf.admin.application.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ApplicationServiceExceptionTest {

  private static final String TEST_MESSAGE = "TestMessage";

  private static final String TEST_THROWABLE_MESSAGE = "ThrowableMessage";

  /**
   * Tests the {@link ApplicationServiceException#ApplicationServiceException(String)} constructor
   */
  @Test
  public void testApplicationServiceExceptionStringParam() {
    try {
      throw new ApplicationServiceException(TEST_MESSAGE);
    } catch (Exception e) {
      assertEquals(TEST_MESSAGE, e.getMessage());
    }
  }

  /**
   * Tests the {@link ApplicationServiceException#ApplicationServiceException(String, Throwable)}
   * constructor
   */
  @Test
  public void testApplicationServiceExceptionStringThrowableParams() {
    try {
      Throwable testThrowable = new Throwable(TEST_THROWABLE_MESSAGE);
      throw new ApplicationServiceException(TEST_MESSAGE, testThrowable);
    } catch (Exception e) {
      assertEquals(TEST_MESSAGE, e.getMessage());
      assertEquals(TEST_THROWABLE_MESSAGE, e.getCause().getMessage());
    }
  }

  /**
   * Tests the {@link ApplicationServiceException#ApplicationServiceException(Throwable)}
   * constructor
   */
  @Test
  public void testApplicationServiceExceptionThrowableParam() {
    try {
      Throwable testThrowable = new Throwable(TEST_THROWABLE_MESSAGE);
      throw new ApplicationServiceException(testThrowable);
    } catch (Exception e) {
      assertEquals(TEST_THROWABLE_MESSAGE, e.getCause().getMessage());
    }
  }
}
