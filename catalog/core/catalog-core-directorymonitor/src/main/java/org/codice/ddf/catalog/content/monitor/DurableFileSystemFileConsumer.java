/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.content.monitor;

import java.io.File;
import java.nio.file.StandardWatchEventKinds;

import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileOperations;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationObserver;

public class DurableFileSystemFileConsumer extends AbstractDurableFileConsumer {
    private DurableFileAlterationListener listener = new DurableFileAlterationListener();

    private FileAlterationObserver observer;

    DurableFileSystemFileConsumer(GenericFileEndpoint<EventfulFileWrapper> endpoint,
            Processor processor, GenericFileOperations<EventfulFileWrapper> operations) {
        super(endpoint, processor, operations);
    }

    @Override
    protected boolean doPoll(String sha1) {
        if (observer != null) {
            observer.addListener(listener);
            observer.checkAndNotify();
            observer.removeListener(listener);
            fileSystemPersistenceProvider.store(sha1, observer);
            return true;
        } else {
            return isMatched(null, null, null);
        }
    }

    @Override
    protected void initialize(String fileName, String sha1) {
        if (fileSystemPersistenceProvider == null) {
            fileSystemPersistenceProvider =
                    new FileSystemPersistenceProvider(getClass().getSimpleName());
        }
        if (observer == null && fileName != null) {
            if (fileSystemPersistenceProvider.loadAllKeys()
                    .contains(sha1)) {
                observer =
                        (FileAlterationObserver) fileSystemPersistenceProvider.loadFromPersistence(
                                sha1);
            } else {
                observer = new FileAlterationObserver(new File(fileName));
            }
        }
    }

    @Override
    public void shutdown() throws Exception {
        super.shutdown();
        if (observer != null) {
            observer.destroy();
        }
    }

    protected class DurableFileAlterationListener extends FileAlterationListenerAdaptor {
        @Override
        public void onFileChange(File file) {
            createExchangeHelper(file, StandardWatchEventKinds.ENTRY_MODIFY);
        }

        @Override
        public void onFileCreate(File file) {
            createExchangeHelper(file, StandardWatchEventKinds.ENTRY_CREATE);
        }

        @Override
        public void onFileDelete(File file) {
            createExchangeHelper(file, StandardWatchEventKinds.ENTRY_DELETE);
        }
    }
}
