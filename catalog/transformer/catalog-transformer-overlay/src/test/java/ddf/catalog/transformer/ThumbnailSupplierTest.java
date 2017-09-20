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
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class ThumbnailSupplierTest {
  private final ThumbnailSupplier supplier = new ThumbnailSupplier();

  private byte[] getImageBytes() throws IOException {
    try (final InputStream inputStream =
        getClass().getClassLoader().getResourceAsStream("flower.jpg")) {
      return IOUtils.toByteArray(inputStream);
    }
  }

  @Test
  public void testSupplierWithThumbnail() throws IOException {
    final MetacardImpl metacard = new MetacardImpl();
    metacard.setThumbnail(getImageBytes());
    assertThat(supplier.apply(metacard, null).isPresent(), is(true));
  }

  @Test
  public void testSupplierWithoutThumbnail() {
    assertThat(supplier.apply(new MetacardImpl(), null).isPresent(), is(false));
  }
}
