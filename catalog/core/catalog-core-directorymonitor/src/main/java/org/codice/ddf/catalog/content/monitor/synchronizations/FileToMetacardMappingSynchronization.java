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

import ddf.catalog.data.Metacard;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.spi.Synchronization;
import org.apache.commons.codec.digest.DigestUtils;
import org.codice.ddf.catalog.content.monitor.FileSystemPersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileToMetacardMappingSynchronization implements Synchronization {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(FileToMetacardMappingSynchronization.class);

  private final String reference;

  private final FileSystemPersistenceProvider productToMetacardIdMap;

  public FileToMetacardMappingSynchronization(
      String fileReference, FileSystemPersistenceProvider productToMetacardIdMap) {
    this.reference = getShaFor(fileReference);
    this.productToMetacardIdMap = productToMetacardIdMap;
  }

  @Override
  public void onComplete(Exchange exchange) {
    List body = exchange.getIn().getBody(List.class);
    for (Object item : body) {
      if (item instanceof Metacard) {
        Metacard metacard = (Metacard) item;
        productToMetacardIdMap.store(reference, metacard.getId());
      } else {
        LOGGER.debug(
            "Received non Metacard body while processing exchange [{}] from route [{}]",
            exchange.getExchangeId(),
            exchange.getFromRouteId());
      }
    }
  }

  @Override
  public void onFailure(Exchange exchange) {
    LOGGER.debug(
        "No metacard id to store for reference [{}] in [{}] cache",
        reference,
        productToMetacardIdMap);
  }

  private String getShaFor(String value) {
    return DigestUtils.sha1Hex(value);
  }
}
