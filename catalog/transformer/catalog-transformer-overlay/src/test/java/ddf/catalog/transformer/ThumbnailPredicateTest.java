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
package ddf.catalog.transformer;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.impl.MetacardImpl;
import org.junit.Test;

public class ThumbnailPredicateTest {
  private final ThumbnailPredicate predicate = new ThumbnailPredicate();

  @Test
  public void testPredicateWithThumbnail() {
    final MetacardImpl metacard = new MetacardImpl();
    metacard.setThumbnail(new byte[1]);
    assertThat(predicate.test(metacard), is(true));
  }

  @Test
  public void testPredicateWithoutThumbnail() {
    assertThat(predicate.test(new MetacardImpl()), is(false));
  }
}
