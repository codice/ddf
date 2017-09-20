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
package org.codice.ddf.admin.insecure.defaults.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class AlertTest {

  public static final String TEST_MSG1 = "TestMessage";

  public static final String TEST_MSG2 = "TestMessage2";

  /** Tests the alert class and its getters and setters */
  @Test
  public void testAlert() throws Exception {
    Alert testAlert = new Alert(Alert.Level.WARN, TEST_MSG1);

    assertThat("Should return the given level.", testAlert.getLevel(), is(Alert.Level.WARN));
    assertThat("Should return the given message.", testAlert.getMessage(), is(TEST_MSG1));

    testAlert.setMessage(TEST_MSG2);
    testAlert.setLevel(Alert.Level.ERROR);

    assertThat("Should return the new level.", testAlert.getLevel(), is(Alert.Level.ERROR));
    assertThat("Should return the new message.", testAlert.getMessage(), is(TEST_MSG2));
  }
}
