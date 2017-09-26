/*
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.spatial.kml.transformer;

import static org.mockito.Mockito.mock;

import ddf.catalog.transform.CatalogTransformerException;
import java.io.InputStream;
import org.junit.Test;

public class TestKmzInputTransformer {

  @Test
  public void testKmzWithKmlInside() throws Exception {
    KmzInputTransformer kmzInputTransformer =
        new KmzInputTransformer(mock(KmlInputTransformer.class));
    InputStream stream = TestKmzInputTransformer.class.getResourceAsStream("/kml_inside.kmz");
    kmzInputTransformer.transform(stream);
  }

  @Test(expected = CatalogTransformerException.class)
  public void testKmzWithoutKmlInside() throws Exception {
    KmzInputTransformer kmzInputTransformer =
        new KmzInputTransformer(mock(KmlInputTransformer.class));
    InputStream stream = TestKmzInputTransformer.class.getResourceAsStream("/no_kml_inside.kmz");
    kmzInputTransformer.transform(stream);
  }
}
