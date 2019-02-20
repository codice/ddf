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
package ddf.catalog.util.impl;

import ddf.catalog.data.ContentType;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * The {@link ContentTypesSourcePoller} is a non-blocking alternative to {@link
 * ddf.catalog.source.Source#getContentTypes()}. Unlike the {@link StatusSourcePoller}, the cache is
 * not updated when there are timeouts and {@link RuntimeException}s when calling {@link
 * ddf.catalog.source.Source#getContentTypes()}.
 */
public class ContentTypesSourcePoller extends SourcePoller<Set<ContentType>> {

  public ContentTypesSourcePoller(
      final ExecutorService pollThreadPool, final ExecutorService pollTimeoutWatcherThreadPool) {
    super(pollThreadPool, pollTimeoutWatcherThreadPool);
  }
}
