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

import ddf.catalog.transform.CatalogTransformerException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

public class XlsxQueryResponseTransformerTest {

  private XlsxQueryResponseTransformer xlsxQueryResponseTransformer;

  @Before
  public void setup() {
    xlsxQueryResponseTransformer = new XlsxQueryResponseTransformer();
  }

  @Test(expected = CatalogTransformerException.class)
  public void testNullMetacardTransform() throws CatalogTransformerException {
    xlsxQueryResponseTransformer.transform(null, Collections.emptyMap());
  }
}
