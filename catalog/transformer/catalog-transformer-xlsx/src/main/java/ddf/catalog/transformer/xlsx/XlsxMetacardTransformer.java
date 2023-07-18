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
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XlsxMetacardTransformer implements MetacardTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(XlsxMetacardTransformer.class);

  @Override
  public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
      throws CatalogTransformerException {

    if (metacard == null) {
      LOGGER.debug("Attempted to transform null metacard");
      throw new CatalogTransformerException("Null metacard cannot be transformed to XLSX");
    }

    String aliasesArg = (String) arguments.getOrDefault("aliases", new String());
    Map<String, String> aliases =
        (StringUtils.isNotBlank(aliasesArg))
            ? Arrays.stream(aliasesArg.split(","))
                .map(s -> s.split("="))
                .collect(Collectors.toMap(k -> k[0], k -> k[1]))
            : Collections.EMPTY_MAP;
    String attributeString =
        arguments.get(XlsxMetacardUtility.COLUMN_ORDER_KEY) != null
            ? (String) arguments.get(XlsxMetacardUtility.COLUMN_ORDER_KEY)
            : "";

    List<String> attributes =
        Arrays.asList((attributeString).split(",")).stream()
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());

    List<Metacard> metacards = Collections.singletonList(metacard);

    return XlsxMetacardUtility.buildSpreadSheet(metacards, aliases, attributes);
  }
}
