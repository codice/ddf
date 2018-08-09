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

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transformer.html.models.HtmlExportCategory;
import ddf.catalog.transformer.html.models.HtmlMetacardModel;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HtmlMetacardTransformer implements MetacardTransformer {

  private HtmlMetacardUtility htmlMetacardUtility;

  public HtmlMetacardTransformer(List<HtmlExportCategory> categoryList) {
    this.htmlMetacardUtility = new HtmlMetacardUtility(categoryList);
  }

  @Override
  public BinaryContent transform(Metacard metacard, Map<String, Serializable> map)
      throws CatalogTransformerException {

    if (metacard == null) {
      throw new CatalogTransformerException("Null metacard cannot be transformed to HTML");
    }

    List<HtmlMetacardModel> metacardModelList = new ArrayList<>();
    metacardModelList.add(new HtmlMetacardModel(metacard, htmlMetacardUtility.getCategoryList()));

    String html = htmlMetacardUtility.buildHtml(metacardModelList);

    if (html == null) {
      throw new CatalogTransformerException("Metacard cannot be transformed to HTML");
    } else {
      return new BinaryContentImpl(new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)));
    }
  }
}
