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
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileConsumer;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileOperations;
import org.apache.camel.spi.Synchronization;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.Constants;

public class DurableFileConsumer extends GenericFileConsumer<EventfulFileWrapper> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DurableFileConsumer.class);

    private static final Logger INGEST_LOGGER =
            LoggerFactory.getLogger(Constants.INGEST_LOGGER_NAME);

    private FileAlterationObserver observer;

    private FileSystemPersistenceProvider fileSystemPersistenceProvider;

    private DurableFileAlterationListener listener;

    public DurableFileConsumer(GenericFileEndpoint<EventfulFileWrapper> endpoint,
            Processor processor, GenericFileOperations<EventfulFileWrapper> operations) {
        super(endpoint, processor, operations);
    }

    @Override
    protected void updateFileHeaders(GenericFile<EventfulFileWrapper> file, Message message) {
        // noop
    }

    @Override
    protected boolean pollDirectory(String fileName, List list, int depth) {
        initialize(fileName);
        if (observer != null) {
            observer.addListener(listener);
            observer.checkAndNotify();
            observer.removeListener(listener);
            fileSystemPersistenceProvider.store(String.valueOf(fileName.hashCode()), observer);
            return true;
        } else {
            return false;
        }
    }

    private void initialize(String fileName) {
        if (fileSystemPersistenceProvider == null) {
            fileSystemPersistenceProvider =
                    new FileSystemPersistenceProvider(getClass().getSimpleName());
        }
        if (observer == null && fileName != null) {
            if (fileSystemPersistenceProvider.loadAllKeys()
                    .contains(String.valueOf(fileName.hashCode()))) {
                observer =
                        (FileAlterationObserver) fileSystemPersistenceProvider.loadFromPersistence(
                                String.valueOf(fileName.hashCode()));
            } else {
                observer = new FileAlterationObserver(new File(fileName));
            }
        }
        if (listener == null) {
            listener = new DurableFileAlterationListener();
        }
    }

    @Override
    protected boolean isMatched(GenericFile file, String doneFileName, List files) {
        return false;
    }

    private void createExchangeHelper(File file, WatchEvent.Kind<Path> fileEvent) {
        GenericFile<EventfulFileWrapper> genericFile = new GenericFile<>();
        genericFile.setFile(new EventfulFileWrapper(fileEvent, 1, file.toPath()));
        genericFile.setEndpointPath(endpoint.getConfiguration()
                .getDirectory());
        try {
            genericFile.setAbsoluteFilePath(file.getCanonicalPath());
        } catch (IOException e) {
            LOGGER.warn("Unable to canonicalize {}. Verify location is accessible.",
                    file.toString());
        }
        Exchange exchange = endpoint.createExchange(genericFile);
        exchange.addOnCompletion(new ErrorLoggingSynchronization(file, fileEvent));
        processExchange(exchange);
    }

    @Override
    public void shutdown() throws Exception {
        super.shutdown();
        if (observer != null) {
            observer.destroy();
        }
    }

    private static class ErrorLoggingSynchronization implements Synchronization {
        private final File file;

        private final WatchEvent.Kind<Path> fileEvent;

        public ErrorLoggingSynchronization(File file, WatchEvent.Kind<Path> fileEvent) {
            this.file = file;
            this.fileEvent = fileEvent;
        }

        @Override
        public void onComplete(Exchange exchange) {
            // no-op
        }

        @Override
        public void onFailure(Exchange exchange) {
            INGEST_LOGGER.error("Delivery failed for {} event on {}",
                    file,
                    fileEvent.name(),
                    exchange.getException());
        }
    }

    private class DurableFileAlterationListener extends FileAlterationListenerAdaptor {
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
