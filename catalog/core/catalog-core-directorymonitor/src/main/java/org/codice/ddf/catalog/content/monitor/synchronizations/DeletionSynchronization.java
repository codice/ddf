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

import static ddf.catalog.Constants.CDM_LOGGER_NAME;

import org.apache.camel.Exchange;
import org.apache.camel.spi.Synchronization;
import org.apache.commons.codec.digest.DigestUtils;
import org.codice.ddf.catalog.content.monitor.FileSystemPersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeletionSynchronization implements Synchronization {

  private static final Logger LOGGER = LoggerFactory.getLogger(CDM_LOGGER_NAME);
  private final String reference;

  private final FileSystemPersistenceProvider productToMetacardIdMap;

  public DeletionSynchronization(
      String fileReference, FileSystemPersistenceProvider productToMetacardIdMap) {
    this.reference = getShaFor(fileReference);
    this.productToMetacardIdMap = productToMetacardIdMap;
  }

  @Override
  public void onComplete(Exchange exchange) {
    if (productToMetacardIdMap.loadAllKeys().contains(reference)) {
      productToMetacardIdMap.delete(reference);
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Received deletion for key [{}] that was not in cache [{}].",
            reference,
            productToMetacardIdMap.toString());
      }
    }
  }

  @Override
  public void onFailure(Exchange exchange) {
    LOGGER.debug(
        "Reference [{}] to metacardId may not be removed from cache [{}].",
        reference,
        productToMetacardIdMap.toString());
  }

  private String getShaFor(String value) {
    return DigestUtils.sha1Hex(value);
  }
}
