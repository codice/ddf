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
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Core;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMetacardTagsPlugin implements CachePutPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMetacardTagsPlugin.class);

  @Override
  public Optional<Metacard> process(Metacard metacard) {
    if (isMetacardTagsSet(metacard)) {
      LOGGER.trace(
          "The metacard {} already has the {} attribute. Therefore, not modifying the metacard.",
          metacard.getId(),
          Core.METACARD_TAGS);
      return Optional.of(metacard);
    }

    LOGGER.trace(
        "Setting the attribute {} with the value \"{}\" on metacard {}.",
        Core.METACARD_TAGS,
        Metacard.DEFAULT_TAG,
        metacard.getId());
    metacard.setAttribute(new AttributeImpl(Core.METACARD_TAGS, Metacard.DEFAULT_TAG));

    return Optional.of(metacard);
  }

  private boolean isMetacardTagsSet(Metacard metacard) {
    return !metacard.getTags().isEmpty();
  }
}
