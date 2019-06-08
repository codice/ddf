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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingSynchronization implements Synchronization {

  private final File file;
  private final String task;
  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingSynchronization.class);

  public LoggingSynchronization(File file, String task) {
    this.file = file;
    this.task = task;
  }

  @Override
  public void onComplete(Exchange exchange) {}

  @Override
  public void onFailure(Exchange exchange) {
    LOGGER.info(
        "[{}] failed to be properly updated during a [{}] call. CDM will be {}out of sync.",
        file.getName(),
        task,
        "MODIFY".equals(task) ? "temporarily " : "");
  }
}
