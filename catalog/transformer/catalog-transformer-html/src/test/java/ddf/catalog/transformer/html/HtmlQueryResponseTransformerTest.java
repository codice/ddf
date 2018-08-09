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
package ddf.catalog.transformer.html;

import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.html.models.HtmlCategoryModel;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class HtmlQueryResponseTransformerTest {

  private static final List<HtmlCategoryModel> EMPTY_CATEGORY_LIST = Collections.emptyList();

  @Test(expected = CatalogTransformerException.class)
  public void testNullMetacardTransform() throws CatalogTransformerException {
    HtmlQueryResponseTransformer htmlTransformer = new HtmlQueryResponseTransformer(EMPTY_CATEGORY_LIST);
    htmlTransformer.transform(null, Collections.emptyMap());
  }
}
