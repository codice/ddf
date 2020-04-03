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

import static java.util.stream.Collectors.toList;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import ddf.catalog.transformer.html.models.HtmlExportCategory;
import ddf.catalog.transformer.html.models.HtmlMetacardModel;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

public class HtmlQueryResponseTransformer implements QueryResponseTransformer {

  private HtmlMetacardUtility htmlMetacardUtility;

  public HtmlQueryResponseTransformer(List<HtmlExportCategory> categoryList) {
    this.htmlMetacardUtility = new HtmlMetacardUtility(categoryList);
  }

  @Override
  public BinaryContent transform(SourceResponse sourceResponse, Map<String, Serializable> map)
      throws CatalogTransformerException {

    if (sourceResponse == null) {
      throw new CatalogTransformerException("Null result set cannot be transformed to HTML");
    }

    List<HtmlExportCategory> categoryList =
        HtmlMetacardUtility.sortCategoryList(htmlMetacardUtility.getCategoryList());

    final List<HtmlMetacardModel> metacardModels =
        sourceResponse.getResults().stream()
            .map(Result::getMetacard)
            .map(metacard -> new HtmlMetacardModel(metacard, categoryList))
            .collect(toList());

    final String html = htmlMetacardUtility.buildHtml(metacardModels);
    if (StringUtils.isEmpty(html)) {
      throw new CatalogTransformerException("Result set cannot be transformed to HTML");
    }

    return new BinaryContentImpl(
        new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)),
        htmlMetacardUtility.getMimeType());
  }
}
