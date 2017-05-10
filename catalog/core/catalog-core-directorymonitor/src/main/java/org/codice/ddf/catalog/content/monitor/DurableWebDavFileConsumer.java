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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileOperations;
import org.apache.camel.spi.Synchronization;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;

public class DurableWebDavFileConsumer extends AbstractDurableFileConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DurableWebDavFileConsumer.class);

    private DavAlterationObserver observer;

    private EntryAlterationListener listener = new EntryAlterationListenerImpl();

    private Sardine sardine = SardineFactory.begin();

    DurableWebDavFileConsumer(GenericFileEndpoint<EventfulFileWrapper> endpoint,
            Processor processor, GenericFileOperations<EventfulFileWrapper> operations) {
        super(endpoint, processor, operations);
    }

    @Override
    protected boolean doPoll(String sha1) {
        if (observer != null) {
            observer.addListener(listener);
            observer.checkAndNotify(sardine);
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
                        (DavAlterationObserver) fileSystemPersistenceProvider.loadFromPersistence(
                                sha1);
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

    private Exchange getExchange(DavEntry entry, WatchEvent.Kind<Path> fileEvent) {
        Exchange exchange = null;
        try {
            File file = entry.getFile(SardineFactory.begin());
            exchange = super.getExchange(file, fileEvent, entry.getLocation());
            exchange.addOnCompletion(new Synchronization() {
                @Override
                public void onComplete(Exchange exchange) {
                    FileUtils.deleteQuietly(file.getParentFile());
                }

                @Override
                public void onFailure(Exchange exchange) {
                    // noop
                }
            });
        } catch (IOException e) {
            LOGGER.error("failed to retrieve file " + entry.getLocation(), e);
        }
        return exchange;
    }

    private class EntryAlterationListenerImpl implements EntryAlterationListener {
        @Override
        public void onDirectoryCreate(DavEntry entry) {
            // noop
        }

        @Override
        public void onFileCreate(DavEntry entry) {
            processExchange(getExchange(entry, StandardWatchEventKinds.ENTRY_CREATE));
        }

        @Override
        public void onDirectoryChange(DavEntry entry) {
            // noop
        }

        @Override
        public void onFileChange(DavEntry entry) {
            processExchange(getExchange(entry, StandardWatchEventKinds.ENTRY_MODIFY));
        }

        @Override
        public void onDirectoryDelete(DavEntry entry) {
            // noop
        }

        @Override
        public void onFileDelete(DavEntry entry) {
            processExchange(getExchange(entry, StandardWatchEventKinds.ENTRY_DELETE));
        }

    }
}
