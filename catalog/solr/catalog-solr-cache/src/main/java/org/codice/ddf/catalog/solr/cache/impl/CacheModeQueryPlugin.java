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
package org.codice.ddf.catalog.solr.cache.impl;

import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheModeQueryPlugin implements PreQueryPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheModeQueryPlugin.class);

  @Override
  public QueryRequest process(QueryRequest input)
      throws PluginExecutionException, StopProcessingException {
    if (!"cache".equals(input.getProperties().get("mode"))) {
      return input;
    }

    HashMap<String, Serializable> props = new HashMap<>(input.getProperties());
    props.put("cache-sources", String.join(",", input.getSourceIds()));

    LOGGER.debug("Converting cache mode query to a cache source query.");
    return new QueryRequestImpl(
        input.getQuery(), input.isEnterprise(), Collections.singleton("cache"), props);
  }
}
