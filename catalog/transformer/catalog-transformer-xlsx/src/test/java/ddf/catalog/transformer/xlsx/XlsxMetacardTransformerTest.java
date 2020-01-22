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
package ddf.catalog.transformer.xlsx;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;
import java.util.Collections;
import org.junit.Test;

public class XlsxMetacardTransformerTest {

  @Test(expected = CatalogTransformerException.class)
  public void testNullMetacardTransform() throws CatalogTransformerException {
    XlsxMetacardTransformer xlsxMetacardTransformer = new XlsxMetacardTransformer();
    xlsxMetacardTransformer.transform(null, Collections.emptyMap());
  }

  @Test
  public void testNonNullMetacard() throws CatalogTransformerException {
    Metacard metacard = new MetacardImpl();

    XlsxMetacardTransformer xlsxMetacardTransformer = new XlsxMetacardTransformer();
    BinaryContent binaryContent =
        xlsxMetacardTransformer.transform(metacard, Collections.emptyMap());

    assertThat(binaryContent, notNullValue());
  }
}
