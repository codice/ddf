/*
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package ddf.catalog.transformer.csv;

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

/**
 * An implementation of QueryResponseTransformer that produces CSV output.
 *
 * @see ddf.catalog.transform.QueryResponseTransformer
 */
public class CsvQueryResponseTransformer implements QueryResponseTransformer {
  /**
   * @param upstreamResponse the SourceResponse to be converted.
   * @param arguments this transformer accepts 2 parameters in the 'arguments' map.
   *     <ol>
   *       <li>key: 'columnOrder' value: a {@link List} of attribute names (as strings) that
   *           specifies the order in which the columns will appear in the output.
   *       <li>key: 'aliases' value: a {@link Map} with keys that are attribute names and with
   *           values that are the corresponding column headers that will be printed in the output.
   *           For example, if the key is 'title' and the value is 'Product' then the resulting CSV
   *           will have a column name of 'Product' instead of 'title'.
   *     </ol>
   *
   * @return a BinaryContent object that contains an InputStream with the CSV content.
   * @throws CatalogTransformerException during processing, the CSV output is written to an
   *     Appendable, whose 'append()' method signature declares that it throws IOException. When
   *     that Appendable throws IOException, this class will theoretically convert that into a
   *     CatalogTransformerException and raise that. Because this implementation uses a
   *     StringBuilder which doesn't throw IOException, this will never occur.
   */
  @Override
  public BinaryContent transform(
      SourceResponse upstreamResponse, Map<String, Serializable> arguments)
      throws CatalogTransformerException {

    List<Metacard> metacards =
        upstreamResponse.getResults().stream()
            .map(Result::getMetacard)
            .collect(Collectors.toList());

    return CsvTransformerSupport.transformWithArguments(metacards, arguments);
  }
}
