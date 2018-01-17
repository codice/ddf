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
package org.codice.ddf.admin.application.service.migratable;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

// Not adding more test cases here since the applications will be removed in an upcoming PR
public class JsonApplicationTest {

  private static final String NAME = "test.name";

  private static final Boolean STATE = true;

  private final JsonApplication japp = new JsonApplication(NAME, STATE);

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testConstructor() throws Exception {
    Assert.assertThat(japp.getName(), Matchers.equalTo(NAME));
    Assert.assertThat(japp.isStarted(), Matchers.equalTo(STATE));
  }

  @Test
  public void testToString() throws Exception {
    Assert.assertThat(japp.toString(), Matchers.equalTo("application [" + NAME + ']'));
  }
}
