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
        productToMetacardIdMap.toString());
  }

  private String getShaFor(String value) {
    return DigestUtils.sha1Hex(value);
  }
}
