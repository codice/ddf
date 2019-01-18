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
package org.codice.ddf.catalog.content.monitor.synchronizations;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.BiConsumer;
import org.apache.camel.Exchange;
import org.apache.camel.spi.Synchronization;
import org.codice.ddf.catalog.content.monitor.AsyncFileEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompletionSynchronization implements Synchronization {

  private static final Logger LOGGER = LoggerFactory.getLogger(CompletionSynchronization.class);

  private AsyncFileEntry contentFile;

  private BiConsumer<AsyncFileEntry, Boolean> cb;

  public CompletionSynchronization(
          AsyncFileEntry entry, BiConsumer<AsyncFileEntry, Boolean> removeFromProcessors) {
    contentFile = entry;
    cb = removeFromProcessors;
  }

  @Override
  public void onComplete(Exchange exchange) {
    cb.accept(contentFile, true);
  }

  @Override
  public void onFailure(Exchange exchange) {
    boolean connected =
        AccessController.doPrivileged((PrivilegedAction<Boolean>) contentFile::checkNetwork);

    if (!connected) {
      LOGGER.warn("a network error occurred. The Content Directory Monitor may be out of sync!");
    }

    cb.accept(contentFile, false);
  }
}
