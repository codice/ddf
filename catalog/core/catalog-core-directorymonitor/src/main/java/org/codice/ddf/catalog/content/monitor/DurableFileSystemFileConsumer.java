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

public class DurableFileSystemFileConsumer extends AbstractDurableFileConsumer {
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
        observer =
            (AsyncFileAlterationObserver) fileSystemPersistenceProvider.loadFromPersistence(sha1);
        observer.clearCache();
      } else {
        observer = new AsyncFileAlterationObserver(new File(fileName));
      }
    }
  }

  @Override
  public void shutdown() throws Exception {
    super.shutdown();
    listener.destroy();
  }
}
