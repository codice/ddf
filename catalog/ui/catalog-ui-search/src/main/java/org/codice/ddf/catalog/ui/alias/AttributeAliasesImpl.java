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
package org.codice.ddf.catalog.ui.alias;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributeAliasesImpl implements AttributeAliases {

  private static final Logger LOGGER = LoggerFactory.getLogger(AttributeAliasesImpl.class);

  private ImmutableMap<String, String> aliases = ImmutableMap.of();

  @Override
  public String getAlias(String attributeName) {
    return aliases.get(attributeName);
  }

  @Override
  public boolean hasAlias(String attributeName) {
    return aliases.containsKey(attributeName);
  }

  @Override
  public ImmutableMap<String, String> getAliasMap() {
    return aliases;
  }

  public void setAttributeAliases(List<String> attributeAliases) {
    LOGGER.trace("Setting attribute aliases: {}", attributeAliases);
    this.aliases = parseAttributeAndValuePairs(attributeAliases);
  }

  private ImmutableMap<String, String> parseAttributeAndValuePairs(List<String> pairs) {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    pairs
        .stream()
        .map(str -> str.split("=", 2))
        .filter(AttributeAliasesImpl::isValidConfig)
        .forEach(list -> builder.put(list[0].trim(), list[1].trim()));
    return builder.build();
  }

  private static boolean isValidConfig(String[] attrValPair) {
    if (attrValPair.length <= 1) {
      LOGGER.debug("Filtering out invalid attribute/value pair: {}", attrValPair[0]);
      return false;
    }
    return true;
  }
}
