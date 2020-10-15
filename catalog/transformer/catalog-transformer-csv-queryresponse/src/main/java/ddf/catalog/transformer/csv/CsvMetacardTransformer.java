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
 */
package ddf.catalog.transformer.csv;

import static ddf.catalog.transformer.csv.common.CsvTransformer.createResponse;
import static ddf.catalog.transformer.csv.common.CsvTransformer.writeMetacardsToCsv;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvMetacardTransformer implements MetacardTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(CsvMetacardTransformer.class);

  @Override
  public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
      throws CatalogTransformerException {

    if (metacard == null) {
      LOGGER.debug("Attempted to transform null metacard");
      throw new CatalogTransformerException("Unable to transform null metacard");
    }

    Map<String, String> aliases =
        (Map<String, String>) arguments.getOrDefault("aliases", new HashMap<>());
    String attributeString =
        arguments.get(CsvQueryResponseTransformer.COLUMN_ORDER_KEY) != null
            ? (String) arguments.get(CsvQueryResponseTransformer.COLUMN_ORDER_KEY)
            : "";
    List<String> attributes = Arrays.asList((attributeString).split(","));
    List<AttributeDescriptor> allAttributes =
        new ArrayList<AttributeDescriptor>(metacard.getMetacardType().getAttributeDescriptors());
    List<AttributeDescriptor> descriptors =
        CollectionUtils.isEmpty(attributes)
            ? allAttributes
            : allAttributes.stream()
                .filter(attr -> attributes.contains(attr.getName()))
                .collect(Collectors.toList());

    if (shouldInjectMetacardType(attributes)) {
      injectMetacardType(descriptors);
    }

    Appendable appendable =
        writeMetacardsToCsv(Collections.singletonList(metacard), descriptors, aliases);
    return createResponse(appendable);
  }

  private void injectMetacardType(List<AttributeDescriptor> descriptors) {
    descriptors.add(
        new AttributeDescriptorImpl(
            MetacardType.METACARD_TYPE, false, false, false, false, BasicTypes.STRING_TYPE));
  }

  private boolean shouldInjectMetacardType(List<String> attributes) {
    return CollectionUtils.isEmpty(attributes) || attributes.contains(MetacardType.METACARD_TYPE);
  }
}
