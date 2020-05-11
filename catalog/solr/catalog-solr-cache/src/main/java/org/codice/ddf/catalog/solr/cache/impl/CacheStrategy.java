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

package org.codice.ddf.catalog.solr.cache.impl;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.apache.commons.lang3.Validate;

public final class CacheStrategy {

  private static final Map<String, CacheStrategy> STRATEGY_MAP = new HashMap<>(3);

  // Caches both local and federated search results
  public static final CacheStrategy ALL = new CacheStrategy("ALL", m -> true);

  // Caches only metacards whose source-id doesn't match the id of localSourceIdSupplier.get()
  public static final CacheStrategy FEDERATED =
      new CacheStrategy(
          "FEDERATED", m -> !m.getSourceId().equals(CacheStrategy.localSourceIdSupplier.get()));

  public static final CacheStrategy NONE =
      new CacheStrategy(
          "NONE",
          (rs, c) -> {
            /*noop*/
          });

  private BiConsumer<Collection<Result>, Consumer<Metacard>> cacheStrategyFunction;

  private static Supplier<String> localSourceIdSupplier = () -> "ddf.distribution";

  private CacheStrategy(
      String instanceName,
      BiConsumer<Collection<Result>, Consumer<Metacard>> cacheStrategyFunction) {

    Validate.notNull(instanceName, "Valid name for cache strategy function required.");
    Validate.notNull(cacheStrategyFunction, "Valid cache strategy function required.");

    this.cacheStrategyFunction = cacheStrategyFunction;
    CacheStrategy.STRATEGY_MAP.put(instanceName, this);
  }

  private CacheStrategy(String instanceName, Predicate<Metacard> predicate) {
    this(
        instanceName,
        (rs, c) ->
            rs.stream()
                .filter(Objects::nonNull)
                .map(Result::getMetacard)
                .filter(Objects::nonNull)
                .filter(predicate)
                .forEach(c::accept)); // Caches only federated search results
  }

  public BiConsumer<Collection<Result>, Consumer<Metacard>> getCacheStrategyFunction() {
    return this.cacheStrategyFunction;
  }

  public static void setLocalSourceIdSupplier(Supplier<String> localSourceIdSupplier) {
    if (localSourceIdSupplier != null) {
      CacheStrategy.localSourceIdSupplier = localSourceIdSupplier;
    }
  }

  public static CacheStrategy valueOf(String instanceName) {
    return CacheStrategy.STRATEGY_MAP.get(instanceName);
  }
}
