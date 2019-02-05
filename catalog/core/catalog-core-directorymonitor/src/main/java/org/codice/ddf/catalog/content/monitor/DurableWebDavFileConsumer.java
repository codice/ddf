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

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import ddf.catalog.data.types.Core;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileOperations;
import org.apache.camel.component.file.GenericFileProcessStrategy;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.catalog.content.monitor.synchronizations.DeletionSynchronization;
import org.codice.ddf.catalog.content.monitor.synchronizations.FileDeletionSynchronization;
import org.codice.ddf.catalog.content.monitor.synchronizations.FileToMetacardMappingSynchronization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DurableWebDavFileConsumer extends AbstractDurableFileConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(DurableWebDavFileConsumer.class);

  private static final String FILE_EXTENSION_HEADER = "org.codice.ddf.camel.FileExtension";

  private DavAlterationObserver observer;

  private EntryAlterationListener listener = new EntryAlterationListenerImpl();

  private Sardine sardine = SardineFactory.begin();

  private static final String CATALOG_OPERATION_HEADER_KEY = "operation";

  private FileSystemPersistenceProvider productToMetacardIdMap;

  DurableWebDavFileConsumer(
      GenericFileEndpoint<File> endpoint,
      String remaining,
      Processor processor,
      GenericFileOperations<File> operations,
      GenericFileProcessStrategy<File> processStrategy) {
    super(endpoint, remaining, processor, operations, processStrategy);
    init();
  }

  private void init() {
    if (productToMetacardIdMap == null) {
      productToMetacardIdMap =
          new FileSystemPersistenceProvider(getClass().getSimpleName() + "-processed");
    }
  }

  @Override
  protected boolean doPoll(String remaining) {
    if (observer != null) {
      observer.addListener(listener);
      observer.checkAndNotify(sardine);
      observer.removeListener(listener);
      String sha1 = DigestUtils.sha1Hex(remaining);
      fileSystemPersistenceProvider.store(sha1, observer);
      return true;
    } else {
      return isMatched(null, null, null);
    }
  }

  @Override
  protected void initialize(String fileName) {
    if (fileSystemPersistenceProvider == null) {
      fileSystemPersistenceProvider = new FileSystemPersistenceProvider(getClass().getSimpleName());
    }
    if (observer == null && fileName != null) {
      String sha1 = DigestUtils.sha1Hex(fileName);
      if (fileSystemPersistenceProvider.loadAllKeys().contains(sha1)) {
        observer = (DavAlterationObserver) fileSystemPersistenceProvider.loadFromPersistence(sha1);
      } else {
        observer = new DavAlterationObserver(new DavEntry(fileName));
      }
    }
  }

  @Override
  public void shutdown() throws Exception {
    super.shutdown();
    sardine.shutdown();
  }

  private class EntryAlterationListenerImpl implements EntryAlterationListener {
    @Override
    public void onDirectoryCreate(DavEntry entry) {
      // noop
    }

    @Override
    public void onFileCreate(DavEntry entry) {
      final File davFile = getDavFile(entry);

      Exchange exchange =
          new ExchangeHelper(davFile, endpoint)
              .addHeader(CATALOG_OPERATION_HEADER_KEY, "CREATE")
              .addHeader(FILE_EXTENSION_HEADER, FilenameUtils.getExtension(entry.getLocation()))
              .addHeader(Core.RESOURCE_URI, entry.getLocation())
              .addSynchronization(
                  new FileToMetacardMappingSynchronization(
                      entry.getLocation(), productToMetacardIdMap))
              .addSynchronization(new FileDeletionSynchronization(davFile.getParentFile()))
              .getExchange();

      submitExchange(exchange);
    }

    @Override
    public void onDirectoryChange(DavEntry entry) {
      // noop
    }

    @Override
    public void onFileChange(DavEntry entry) {
      String referenceKey = entry.getLocation();
      String metacardId =
          getMetacardIdFromReference(referenceKey, "UPDATE", productToMetacardIdMap);

      if (StringUtils.isEmpty(metacardId)) {
        return;
      }

      final File davFile = getDavFile(entry);

      Exchange exchange =
          new ExchangeHelper(davFile, endpoint)
              .addHeader(CATALOG_OPERATION_HEADER_KEY, "UPDATE")
              .addHeader("org.codice.ddf.camel.transformer.MetacardUpdateId", metacardId)
              .addHeader(FILE_EXTENSION_HEADER, FilenameUtils.getExtension(entry.getLocation()))
              .addHeader(Core.RESOURCE_URI, entry.getLocation())
              .addSynchronization(
                  new FileToMetacardMappingSynchronization(referenceKey, productToMetacardIdMap))
              .addSynchronization(new FileDeletionSynchronization(davFile.getParentFile()))
              .getExchange();

      submitExchange(exchange);
    }

    @Override
    public void onDirectoryDelete(DavEntry entry) {
      // noop
    }

    /**
     * Looks up a map of DavEntry locations to metacardIds and populates the exchange body with the
     * ids of the metacards to be deleted.
     *
     * @param entry dav resource entry to process
     */
    @Override
    public void onFileDelete(DavEntry entry) {
      String referenceKey = entry.getLocation();
      String metacardId =
          getMetacardIdFromReference(referenceKey, "DELETE", productToMetacardIdMap);

      if (StringUtils.isEmpty(metacardId)) {
        return;
      }

      Exchange exchange =
          new ExchangeHelper(null, endpoint)
              .addHeader(CATALOG_OPERATION_HEADER_KEY, "DELETE")
              .setBody(Collections.singletonList(metacardId))
              .addSynchronization(new DeletionSynchronization(referenceKey, productToMetacardIdMap))
              .getExchange();

      submitExchange(exchange);
    }
  }

  private File getDavFile(DavEntry entry) {
    try {
      return entry.getFile(SardineFactory.begin());
    } catch (IOException e) {
      LOGGER.debug("Failed to get file for dav entry [{}].", entry.getLocation(), e);
      throw new UncheckedIOException(e);
    }
  }
}
