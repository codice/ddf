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

import com.vividsolutions.jts.geom.Geometry;
import ddf.catalog.data.impl.MetacardImpl;
import java.util.Optional;
import org.junit.Test;

public class ThumbnailBoundarySupplierTest {

  private ThumbnailBoundarySupplier supplier = new ThumbnailBoundarySupplier();

  @Test
  public void testValidWkt() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setLocation("POLYGON ((0 1, 1 0, 0 -1, -1 0, 0 1))");
    Optional<Geometry> geometry = supplier.apply(metacard, null);
    assertThat(geometry.isPresent(), is(true));
  }

  @Test
  public void testInvalidWkt() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setLocation("INVALID WKT");
    Optional<Geometry> geometry = supplier.apply(metacard, null);
    assertThat(geometry.isPresent(), is(false));
  }
}
