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
package org.codice.ddf.cache.plugin.metacardtags;

import ddf.catalog.cache.CachePutPlugin;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Core;
import java.util.Optional;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetDefaultMetacardTags implements CachePutPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(SetDefaultMetacardTags.class);

  private static final String ATTR = Core.METACARD_TAGS;

  private static final String ATTR_VALUE = Metacard.DEFAULT_TAG;

  @Override
  public Optional<Metacard> process(Metacard metacard) {
    if (isMetacardTagsSet(metacard)) {
      LOGGER.trace(
          "The metacard {} already has the {} attribute. Therefore, not modifying the metacard.",
          metacard.getId(),
          ATTR);
      return Optional.of(metacard);
    }

    LOGGER.trace(
        "Setting the attribute {} with the value \"{}\" on metacard {}.",
        ATTR,
        ATTR_VALUE,
        metacard.getId());
    metacard.setAttribute(new AttributeImpl(ATTR, ATTR_VALUE));

    return Optional.of(metacard);
  }

  private boolean isMetacardTagsSet(Metacard metacard) {
    Attribute attribute = metacard.getAttribute(ATTR);
    if (attribute == null) {
      return false;
    }

    return CollectionUtils.isNotEmpty(attribute.getValues());
  }
}
