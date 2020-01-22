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
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.transform.CatalogTransformerException;
import java.util.Collections;
import java.util.List;
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

  @Test
  public void testNonNullSourceResponse() throws CatalogTransformerException {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setId("metacardId");
    List<Result> results = Collections.singletonList(new ResultImpl(metacard));
    SourceResponse sourceResponse = new SourceResponseImpl(null, results);

    BinaryContent binaryContent =
        xlsxQueryResponseTransformer.transform(sourceResponse, Collections.emptyMap());

    assertThat(binaryContent, notNullValue());
  }
}
