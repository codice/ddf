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

import java.io.File;
import org.apache.camel.Exchange;
import org.apache.camel.spi.Synchronization;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileDeletionSynchronization implements Synchronization {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileDeletionSynchronization.class);

  private final File file;

  public FileDeletionSynchronization(final File file) {
    this.file = file;
  }

  @Override
  public void onComplete(Exchange exchange) {
    FileUtils.deleteQuietly(file);
  }

  @Override
  public void onFailure(Exchange exchange) {
    LOGGER.debug(
        "Error while processing exchange [{}]. File [{}] won't be deleted by this synchronization.",
        exchange.getExchangeId(),
        file.toPath());
  }
}
