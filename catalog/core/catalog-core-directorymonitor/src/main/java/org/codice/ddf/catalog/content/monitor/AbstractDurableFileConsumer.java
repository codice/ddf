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
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.camel.Component;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileConsumer;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileOperations;
import org.apache.camel.spi.Synchronization;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.Constants;

public abstract class AbstractDurableFileConsumer extends GenericFileConsumer<EventfulFileWrapper> {
    private static final Logger INGEST_LOGGER =
            LoggerFactory.getLogger(Constants.INGEST_LOGGER_NAME);

    FileSystemPersistenceProvider fileSystemPersistenceProvider;

    AbstractDurableFileConsumer(GenericFileEndpoint<EventfulFileWrapper> endpoint,
            Processor processor, GenericFileOperations<EventfulFileWrapper> operations) {
        super(endpoint, processor, operations);
    }

    @Override
    protected void updateFileHeaders(GenericFile<EventfulFileWrapper> file, Message message) {
        // noop
    }

    @Override
    protected boolean isMatched(GenericFile file, String doneFileName, List files) {
        return false;
    }

    @Override
    protected boolean pollDirectory(String fileName, List list, int depth) {
        Component component = endpoint.getComponent();
        String remaining;
        if (component != null) {
            remaining = ((DurableFileComponent) component).remaining;
            if (remaining != null) {
                String sha1 = DigestUtils.sha1Hex(remaining);
                initialize(remaining, sha1);
                return doPoll(sha1);
            }
        }
        return false;
    }

    protected abstract void initialize(@NotNull String remaining, @NotNull String sha1);

    protected abstract boolean doPoll(@NotNull String sha1);

    void createExchangeHelper(File file, WatchEvent.Kind<Path> fileEvent) {
        Exchange exchange;
        try {
            exchange = getExchange(file, fileEvent, file.getCanonicalPath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        processExchange(exchange);
    }

    Exchange getExchange(File file, WatchEvent.Kind<Path> fileEvent, String reference) {
        GenericFile<EventfulFileWrapper> genericFile = new GenericFile<>();
        genericFile.setEndpointPath(endpoint.getConfiguration()
                .getDirectory());
        try {
            if (file != null) {
                genericFile.setFile(new EventfulFileWrapper(fileEvent, 1, file.toPath()));
                genericFile.setAbsoluteFilePath(file.getCanonicalPath());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Exchange exchange = endpoint.createExchange(genericFile);
        exchange.getIn()
                .setHeader(Constants.STORE_REFERENCE_KEY, reference);
        exchange.addOnCompletion(new ErrorLoggingSynchronization(reference, fileEvent));
        return exchange;
    }

    private static class ErrorLoggingSynchronization implements Synchronization {
        private final String file;

        private final WatchEvent.Kind<Path> fileEvent;

        ErrorLoggingSynchronization(String file, WatchEvent.Kind<Path> fileEvent) {
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

}
