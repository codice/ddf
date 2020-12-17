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
package ddf.catalog.source.solr;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import ddf.catalog.data.Metacard;
import org.apache.curator.shaded.com.google.common.collect.Lists;
import org.junit.Test;

public class RealTimeGetDelegateTest {

  private static RealTimeGetDelegate delegate = new RealTimeGetDelegate();

  @Test
  public void defaultOperation() {
    assertFalse(delegate.defaultOperation(null, null, null, null));
  }

  @Test
  public void and() {
    assertTrue(delegate.and(Lists.newArrayList(true, true)));
    assertTrue(delegate.and(Lists.newArrayList(true, false)));
    assertFalse(delegate.and(Lists.newArrayList(false, false)));
  }

  @Test
  public void or() {
    assertTrue(delegate.or(Lists.newArrayList(true, true)));
    assertFalse(delegate.or(Lists.newArrayList(true, false)));
    assertFalse(delegate.or(Lists.newArrayList(false, false)));
  }

  @Test
  public void propertyIsEqualTo() {
    assertTrue(delegate.propertyIsEqualTo(Metacard.ID, null, false));
    assertFalse(delegate.propertyIsEqualTo(Metacard.TITLE, null, false));
  }
}
