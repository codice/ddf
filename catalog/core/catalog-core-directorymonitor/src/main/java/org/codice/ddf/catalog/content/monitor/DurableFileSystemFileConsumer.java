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
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileOperations;
import org.apache.camel.component.file.GenericFileProcessStrategy;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DurableFileSystemFileConsumer extends AbstractDurableFileConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(DurableFileSystemFileConsumer.class);

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
      observer.addListener(listener);
      observer.checkAndNotify();
      observer.removeListener();
      fileSystemPersistenceProvider.store(sha1, observer);
      return true;
    } else {
      return isMatched(null, null, null);
    }
  }

  @Override
  protected void initialize(String fileName, String sha1) {
    if (fileSystemPersistenceProvider == null) {
      fileSystemPersistenceProvider = new FileSystemPersistenceProvider(getClass().getSimpleName());
    }
    if (observer == null && fileName != null) {
      if (fileSystemPersistenceProvider.loadAllKeys().contains(sha1)) {
        Object tempObserver = fileSystemPersistenceProvider.loadFromPersistence(sha1);
        if (tempObserver instanceof AsyncFileAlterationObserver) {
          observer = (AsyncFileAlterationObserver) tempObserver;
        } else {
          backwardsCompatibility(
              (FileAlterationObserver) tempObserver,
              new AsyncFileAlterationObserver(new File(fileName)));
        }
      } else {
        observer = new AsyncFileAlterationObserver(new File(fileName));
      }
    }
  }

  //  We got an old version.
  private void backwardsCompatibility(
      FileAlterationObserver oldBoye, AsyncFileAlterationObserver newBoye) {
    boolean success = newBoye.initialize();
    if (!success) {
      //  Screams internally.
      //  There was an IO error setting up the initial state of the observer
      LOGGER.info("Error initializing the new state of the CDM. retrying on next poll");
      return;
    }
    oldBoye.addListener(listener);
    oldBoye.checkAndNotify();
    oldBoye.removeListener(listener);

    observer = newBoye;
  }

  @Override
  public void shutdown() throws Exception {
    super.shutdown();
    listener.destroy();
  }
}
