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

import com.sun.tools.javac.util.List;
import ddf.catalog.data.Metacard;
import org.junit.Test;

public class RealTimeGetDelegateTest {

  private static RealTimeGetDelegate delegate = new RealTimeGetDelegate();

  @Test
  public void defaultOperation() {
    assertFalse(delegate.defaultOperation(null, null, null, null));
  }

  @Test
  public void and() {
    assertTrue(delegate.and(List.of(true, true)));
    assertTrue(delegate.and(List.of(true, false)));
    assertFalse(delegate.and(List.of(false, false)));
  }

  @Test
  public void or() {
    assertTrue(delegate.or(List.of(true, true)));
    assertFalse(delegate.or(List.of(true, false)));
    assertFalse(delegate.or(List.of(false, false)));
  }

  @Test
  public void propertyIsEqualTo() {
    assertTrue(delegate.propertyIsEqualTo(Metacard.ID, null, false));
    assertFalse(delegate.propertyIsEqualTo(Metacard.TITLE, null, false));
  }
}
