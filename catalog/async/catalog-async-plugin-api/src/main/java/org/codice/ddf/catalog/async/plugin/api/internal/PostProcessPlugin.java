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
package org.codice.ddf.catalog.async.plugin.api.internal;

import ddf.catalog.plugin.PluginExecutionException;
import org.codice.ddf.catalog.async.data.api.internal.ProcessCreateItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessDeleteItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessRequest;
import org.codice.ddf.catalog.async.data.api.internal.ProcessUpdateItem;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 *
 * <p>A {@link PostProcessPlugin} represents a process that will be run asynchronously by the {@link
 * ProcessingFramework}. The {@link PostProcessPlugin}s may only be run after the {@link
 * ddf.catalog.data.Metacard}s have been ingested into the catalog. This will allow the {@link
 * PostProcessPlugin}s to be able to use a copy of the {@link ddf.catalog.data.Metacard}, and
 * optionally a copy of the associated {@link ddf.catalog.resource.Resource} of the catalog-stored
 * record, in order to perform processing asynchronously.
 */
public interface PostProcessPlugin {

  /**
   * Submits a {@link ProcessRequest<ProcessCreateItem>} to be processed by the {@link
   * PostProcessPlugin}. The returned {@link ProcessRequest<ProcessCreateItem>} must be the same
   * instance as the {@param input}.
   *
   * @param input the {@link ProcessRequest<ProcessCreateItem>} to be processed
   * @return the modified {@link ProcessRequest<ProcessCreateItem>} after processing
   */
  ProcessRequest<ProcessCreateItem> processCreate(ProcessRequest<ProcessCreateItem> input)
      throws PluginExecutionException;

  /**
   * Submits a {@link ProcessRequest<ProcessUpdateItem>} to be processed by the {@link
   * PostProcessPlugin}. The returned {@link ProcessRequest<ProcessUpdateItem>} must be the same
   * instance as the {@param input}.
   *
   * @param input the {@link ProcessRequest<ProcessUpdateItem>} to be processed
   * @return the modified {@link ProcessRequest<ProcessUpdateItem>} after processing
   */
  ProcessRequest<ProcessUpdateItem> processUpdate(ProcessRequest<ProcessUpdateItem> input)
      throws PluginExecutionException;

  /**
   * Submits a {@link ProcessRequest<ProcessDeleteItem>} to be processed by the {@link
   * PostProcessPlugin}. The returned {@link ProcessRequest<ProcessDeleteItem>} must be the same
   * instance as the {@param input}.
   *
   * @param input the {@link ProcessRequest<ProcessDeleteItem>} to be processed
   * @return the modified {@link ProcessRequest<ProcessDeleteItem>} after processing
   */
  ProcessRequest<ProcessDeleteItem> processDelete(ProcessRequest<ProcessDeleteItem> input)
      throws PluginExecutionException;
}
