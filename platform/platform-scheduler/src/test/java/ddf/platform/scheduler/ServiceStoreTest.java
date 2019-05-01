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
package ddf.platform.scheduler;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class ServiceStoreTest {

  private ServiceStore serviceStore;

  @Before
  public void setUp() {
    serviceStore = ServiceStore.getInstance();
    serviceStore.setObject(new CommandJob());
  }

  @Test
  public void testGetObject() {
    Object obj1 = serviceStore.getObject("Job");
    Object obj2 = serviceStore.getObject("NotJob");
    assertThat(obj1, notNullValue());
    assertThat(obj2, nullValue());
  }
}
