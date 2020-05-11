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

import static ddf.catalog.Constants.CDM_LOGGER_NAME;

import java.io.File;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileOperations;
import org.apache.camel.component.file.GenericFileProcessStrategy;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DurableFileSystemFileConsumer extends AbstractDurableFileConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(CDM_LOGGER_NAME);

  private DurableFileAlterationListener listener;

  private AsyncFileAlterationObserver observer;

  DurableFileSystemFileConsumer(
      GenericFileEndpoint<File> endpoint,
      String remaining,
      Processor processor,
      GenericFileOperations<File> operations,
      GenericFileProcessStrategy<File> processStrategy) {
    super(endpoint, remaining, processor, operations, processStrategy);
    listener = new DurableFileAlterationListener(this);
  }

  @Override
  protected boolean doPoll(String sha1) {
    if (observer != null) {
      observer.setListener(listener);
      observer.checkAndNotify();
      observer.removeListener();
      return true;
    } else {
      return false;
    }
  }

  @Override
  protected void initialize(String fileName) {
    if (fileSystemPersistenceProvider == null) {
      fileSystemPersistenceProvider = new FileSystemPersistenceProvider(getClass().getSimpleName());
    }
    if (jsonSerializer == null) {
      jsonSerializer = new JsonPersistantStore(getClass().getSimpleName());
    }

    if (observer == null && fileName != null) {

      observer = AsyncFileAlterationObserver.load(new File(fileName), jsonSerializer);

      //  Backwards Compatibility
      if (observer == null && isOldVersion(fileName)) {
        observer = backwardsCompatibility(fileName);
      } else if (observer == null) {
        observer = new AsyncFileAlterationObserver(new File(fileName), jsonSerializer);
      }
    }
  }

  private boolean isOldVersion(String fileName) {
    String sha1 = DigestUtils.sha1Hex(fileName);
    return fileSystemPersistenceProvider.loadAllKeys().contains(sha1);
  }

  private AsyncFileAlterationObserver backwardsCompatibility(String fileName) {

    String sha1 = DigestUtils.sha1Hex(fileName);
    AsyncFileAlterationObserver newObserver =
        new AsyncFileAlterationObserver(new File(fileName), jsonSerializer);
    FileAlterationObserver oldObserver =
        (FileAlterationObserver) fileSystemPersistenceProvider.loadFromPersistence(sha1);

    try {
      newObserver.initialize();
    } catch (IllegalStateException e) {
      //  There was an IO error setting up the initial state of the observer
      LOGGER.info("Error initializing the new state of the CDM. retrying on next poll");
      return null;
    }
    oldObserver.addListener(listener);
    oldObserver.checkAndNotify();
    oldObserver.removeListener(listener);

    return newObserver;
  }

  @Override
  public void shutdown() throws Exception {
    super.shutdown();
    listener.destroy();
  }
}
