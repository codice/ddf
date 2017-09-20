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
package org.codice.ddf.catalog.async.processingframework.api.internal;

import org.codice.ddf.catalog.async.data.api.internal.ProcessCreateItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessDeleteItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessRequest;
import org.codice.ddf.catalog.async.data.api.internal.ProcessUpdateItem;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 *
 * <p>The {@code ProcessingFramework} processes all requests submitted to it. A {@link
 * ddf.catalog.operation.Request} is converted to the appropriate data recognized by the {@code
 * ProcessingFramework}.
 *
 * <p>Available requests for processing are as follows:
 *
 * <ul>
 *   <li>{@link ProcessRequest<ProcessCreateItem>}
 *   <li>{@link ProcessRequest<ProcessUpdateItem>}
 *   <li>{@link ProcessRequest<ProcessDeleteItem>}
 * </ul>
 */
public interface ProcessingFramework {

  /**
   * Submits a {@link ProcessRequest<ProcessCreateItem>} to be processed by the {@code
   * ProcessingFramework}. Processing on the {@link ProcessRequest<ProcessCreateItem>} will never
   * block.
   *
   * @param input the {@link ProcessRequest<ProcessCreateItem>} to be processed
   */
  void submitCreate(ProcessRequest<ProcessCreateItem> input);

  /**
   * Submits a {@link ProcessRequest<ProcessUpdateItem>} to be processed by the {@code
   * ProcessingFramework}. Processing on the {@link ProcessRequest<ProcessUpdateItem>} will never
   * block.
   *
   * @param input the {@link ProcessRequest<ProcessUpdateItem>} to be processed
   */
  void submitUpdate(ProcessRequest<ProcessUpdateItem> input);

  /**
   * Submits a {@link ProcessRequest<ProcessDeleteItem>} to be processed by the {@code
   * ProcessingFramework}. Processing on the {@link ProcessRequest<ProcessDeleteItem>} will never
   * block.
   *
   * @param input the {@code ProcessDeleteRequest} to be processed
   */
  void submitDelete(ProcessRequest<ProcessDeleteItem> input);
}
