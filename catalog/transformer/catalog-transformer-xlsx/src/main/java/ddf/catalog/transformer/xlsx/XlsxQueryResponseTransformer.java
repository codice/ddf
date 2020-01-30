/*
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

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XlsxQueryResponseTransformer implements QueryResponseTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(XlsxQueryResponseTransformer.class);

  @Override
  public BinaryContent transform(SourceResponse sourceResponse, Map<String, Serializable> arguments)
      throws CatalogTransformerException {

    if (sourceResponse == null) {
      LOGGER.debug("Attempted to transform null result set");
      throw new CatalogTransformerException("Null result set cannot be transformed to XLSX");
    }

    List<Metacard> metacards =
        sourceResponse.getResults().stream().map(Result::getMetacard).collect(Collectors.toList());

    return XlsxMetacardUtility.buildSpreadSheet(metacards);
  }
}
