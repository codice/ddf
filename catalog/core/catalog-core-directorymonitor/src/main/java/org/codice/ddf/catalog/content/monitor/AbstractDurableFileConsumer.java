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
package org.codice.ddf.catalog.content.monitor;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileConsumer;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileOperations;
import org.apache.camel.component.file.GenericFileProcessStrategy;
import org.apache.camel.spi.Synchronization;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDurableFileConsumer extends GenericFileConsumer<File> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDurableFileConsumer.class);

  //  Implement these to serializing the observers states.
  protected FileSystemPersistenceProvider fileSystemPersistenceProvider;

  protected ObjectPersistentStore jsonSerializer;

  private String remaining;

  AbstractDurableFileConsumer(
      GenericFileEndpoint<File> endpoint,
      String remaining,
      Processor processor,
      GenericFileOperations<File> operations,
      GenericFileProcessStrategy<File> processStrategy) {
    super(endpoint, processor, operations, processStrategy);
    this.remaining = remaining;
  }

  @Override
  protected void updateFileHeaders(GenericFile<File> file, Message message) {
    // noop
  }

  @Override
  protected boolean isMatched(GenericFile file, String doneFileName, List files) {
    return false;
  }

  @Override
  protected boolean pollDirectory(String fileName, List list, int depth) {
    if (remaining != null) {
      initialize(remaining);
      return doPoll(remaining);
    }

    return false;
  }

  protected abstract void initialize(String fileName);

  protected abstract boolean doPoll(String sha1);

  void submitExchange(Exchange exchange) {
    processExchange(exchange);
  }

  /** Utility class for building GenericFile exchanges from files. */
  public static class ExchangeHelper {

    private Exchange exchange;

    /**
     * If {@code file} is null, creates a null file exchange, otherwise the exchange's GenericFile
     * is populated with the file.
     *
     * @param file the file to populate the {@link GenericFile}
     * @param endpoint the consumer's endpoint
     */
    public ExchangeHelper(File file, GenericFileEndpoint<File> endpoint) {
      GenericFile<File> genericFile = new GenericFile<>();
      genericFile.setEndpointPath(endpoint.getConfiguration().getDirectory());

      if (file == null) {
        genericFile.setFile(null);
        exchange = endpoint.createExchange(genericFile);
      } else {
        try {
          genericFile.setFile(file);
          genericFile.setAbsoluteFilePath(file.getCanonicalPath());
          exchange = endpoint.createExchange(genericFile);
          setBody(genericFile);
          addHeader(Exchange.FILE_NAME, file.getName());
          addHeader(Exchange.FILE_LENGTH, Long.toString(file.length()));
        } catch (IOException e) {
          LOGGER.debug(
              "Error setting GenericFile's absolute path for resource [{}].", file.getName(), e);
          throw new UncheckedIOException(e);
        }
      }
    }

    ExchangeHelper addHeader(String key, Object value) {
      exchange.getIn().setHeader(key, value);
      return this;
    }

    ExchangeHelper addSynchronization(Synchronization synchronization) {
      exchange.addOnCompletion(synchronization);
      return this;
    }

    ExchangeHelper setBody(Object object) {
      exchange.getIn().setBody(object);
      return this;
    }

    Exchange getExchange() {
      return exchange;
    }
  }

  String getMetacardIdFromReference(
      String referenceKey,
      String catalogOperation,
      FileSystemPersistenceProvider productToMetacardIdMap) {
    String ref = getShaFor(referenceKey);
    if (!productToMetacardIdMap.loadAllKeys().contains(ref)) {
      LOGGER.debug(
          "Received a [{}] operation, but no mapped metacardIds were available for product [{}].",
          catalogOperation,
          referenceKey);
      return null;
    }

    return (String) productToMetacardIdMap.loadFromPersistence(ref);
  }

  private String getShaFor(String value) {
    return DigestUtils.sha1Hex(value);
  }
}
