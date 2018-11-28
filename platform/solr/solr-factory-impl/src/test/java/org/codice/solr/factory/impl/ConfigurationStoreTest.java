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
package org.codice.solr.factory.impl;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class ConfigurationStoreTest {

  private ConfigurationStore store;

  @Before
  public void beforeTests() {
    store = ConfigurationStore.getInstance();
  }

  @Test
  public void isASingleton() {
    store.setInMemory(true);
    store.setForceAutoCommit(true);

    ConfigurationStore store2 = ConfigurationStore.getInstance();

    assertThat(store.isInMemory(), is(store2.isInMemory()));
    assertThat(store.isForceAutoCommit(), is(store2.isForceAutoCommit()));
  }

  @Test
  public void nearestNeighborLimitIsAlwaysPositive() {
    store.setNearestNeighborDistanceLimit(-1.0);

    assertThat(store.getNearestNeighborDistanceLimit(), closeTo(1.0, 0.00001));
  }
}
