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

import ddf.catalog.data.types.Core;
import java.io.File;
import java.util.Collections;
import javax.validation.constraints.NotNull;
import org.apache.camel.Exchange;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.spi.Synchronization;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.catalog.content.monitor.AbstractDurableFileConsumer.ExchangeHelper;
import org.codice.ddf.catalog.content.monitor.synchronizations.DeletionSynchronization;
import org.codice.ddf.catalog.content.monitor.synchronizations.FileToMetacardMappingSynchronization;
import org.codice.ddf.catalog.content.monitor.watcher.FileWatcher;
import org.codice.ddf.catalog.content.monitor.watcher.FilesWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DurableFileAlterationListener implements AsyncFileAlterationListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(DurableFileAlterationListener.class);

  private static final String FILE_EXTENSION_HEADER = "org.codice.ddf.camel.FileExtension";

  private static final String OPERATION_HEADER = "operation";

  private static final String CATALOG_UPDATE = "UPDATE";

  private static final String CATALOG_DELETE = "DELETE";

  private FileSystemPersistenceProvider productToMetacardIdMap;

  private AbstractDurableFileConsumer consumer;

  private FilesWatcher filesWatcher;

  public DurableFileAlterationListener(@NotNull AbstractDurableFileConsumer consumer) {
    this(consumer, new FilesWatcher());
  }

  public DurableFileAlterationListener(
      @NotNull AbstractDurableFileConsumer consumer, @NotNull FilesWatcher fileWatcher) {
    this.consumer = consumer;
    this.filesWatcher = fileWatcher;
    init();
  }

  private void init() {
    if (productToMetacardIdMap == null) {
      productToMetacardIdMap =
          new FileSystemPersistenceProvider(getClass().getSimpleName() + "-processed");
    }
  }

  public void onFileChange(File file, Synchronization cb) {
    filesWatcher.watch(new FileWatcher(file, (e) -> fileUpdate(e, cb)));
  }

  private void fileUpdate(File file, Synchronization cb) {
    String fileUri = file.toURI().toASCIIString();
    String metacardId = getMetacardIdFromReference(fileUri, CATALOG_UPDATE, productToMetacardIdMap);

    if (StringUtils.isEmpty(metacardId)) {
      cb.onComplete(null);
      return;
    }

    Exchange exchange =
        new ExchangeHelper(file, (GenericFileEndpoint) consumer.getEndpoint())
            .addHeader(OPERATION_HEADER, CATALOG_UPDATE)
            .addHeader(FILE_EXTENSION_HEADER, FilenameUtils.getExtension(file.getName()))
            .addHeader(Core.RESOURCE_URI, fileUri)
            .addHeader("org.codice.ddf.camel.transformer.MetacardUpdateId", metacardId)
            .addSynchronization(
                new FileToMetacardMappingSynchronization(fileUri, productToMetacardIdMap))
            .addSynchronization(cb)
            .getExchange();

    consumer.submitExchange(exchange);
  }

  public void onFileCreate(File file, Synchronization cb) {
    filesWatcher.watch(new FileWatcher(file, (e) -> fileCreate(e, cb)));
  }

  private void fileCreate(File file, Synchronization cb) {
    String fileUri = file.toURI().toASCIIString();

    Exchange exchange =
        new ExchangeHelper(file, (GenericFileEndpoint) consumer.getEndpoint())
            .addHeader(OPERATION_HEADER, "CREATE")
            .addHeader(FILE_EXTENSION_HEADER, FilenameUtils.getExtension(file.getName()))
            .addHeader(Core.RESOURCE_URI, fileUri)
            .addSynchronization(
                new FileToMetacardMappingSynchronization(fileUri, productToMetacardIdMap))
            .addSynchronization(cb)
            .getExchange();

    consumer.submitExchange(exchange);
  }

  public void onFileDelete(File file, Synchronization cb) {
    String referenceKey = file.toURI().toASCIIString();
    String metacardId =
        getMetacardIdFromReference(referenceKey, CATALOG_DELETE, productToMetacardIdMap);

    if (StringUtils.isEmpty(metacardId)) {
      cb.onComplete(null);
      return;
    }

    Exchange exchange =
        new ExchangeHelper(file, (GenericFileEndpoint) consumer.getEndpoint())
            .setBody(Collections.singletonList(metacardId))
            .addHeader(OPERATION_HEADER, CATALOG_DELETE)
            .addSynchronization(new DeletionSynchronization(referenceKey, productToMetacardIdMap))
            .addSynchronization(cb)
            .getExchange();

    consumer.submitExchange(exchange);
  }

  public void destroy() {
    filesWatcher.destroy();
  }

  private String getMetacardIdFromReference(
      String referenceKey,
      String catalogOperation,
      FileSystemPersistenceProvider productToMetacardIdMap) {
    String ref = DigestUtils.sha1Hex(referenceKey);
    if (!productToMetacardIdMap.loadAllKeys().contains(ref)) {
      LOGGER.debug(
          "Received a [{}] operation, but no mapped metacardIds were available for product [{}].",
          catalogOperation,
          referenceKey);
      return null;
    }

    return (String) productToMetacardIdMap.loadFromPersistence(ref);
  }

  @Override
  public void onStart(AsyncFileAlterationObserver observer) {}

  @Override
  public void onStop(AsyncFileAlterationObserver observer) {}
}
