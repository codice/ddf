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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.transformer.impl;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.transformer.CswActionTransformer;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.transformer.CswActionTransformerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 *
 * <p>Manages a reference list of {@link CswActionTransformer}'s by mapping them to the typenames
 * they apply to.
 */
public class CswActionTransformerProviderImpl implements CswActionTransformerProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(CswActionTransformerProvider.class);

  private final ConcurrentMap<String, CswActionTransformer> transformerMap =
      new ConcurrentHashMap<>();

  public void bind(CswActionTransformer cswActionTransformer) {
    if (cswActionTransformer == null) {
      return;
    }

    Set<String> typeNames = cswActionTransformer.getTypeNames();
    if (CollectionUtils.isEmpty(typeNames)) {
      LOGGER.warn("Unable to bind a CswActionTransformer with no typenames");
      return;
    }

    for (String typeName : typeNames) {
      transformerMap.putIfAbsent(typeName, cswActionTransformer);
    }
  }

  public void unbind(CswActionTransformer cswActionTransformer) {
    if (cswActionTransformer == null) {
      return;
    }

    Set<String> typeNames = cswActionTransformer.getTypeNames();
    if (CollectionUtils.isEmpty(typeNames)) {
      return;
    }

    for (String typeName : typeNames) {
      transformerMap.remove(typeName);
    }
  }

  @Override
  public Optional<CswActionTransformer> getTransformer(@Nullable String typeName) {
    if (StringUtils.isEmpty(typeName)) {
      return Optional.empty();
    }

    return Optional.ofNullable(transformerMap.get(typeName));
  }
}
