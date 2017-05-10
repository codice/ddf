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
import java.nio.file.Paths;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileConsumer;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileOperationFailedException;
import org.apache.camel.component.file.GenericFileOperations;
import org.apache.camel.component.file.GenericFileProducer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriPath;
import org.apache.camel.util.ObjectHelper;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UriEndpoint(scheme = "durable", title = "Durable File Endpoint", syntax = "durable:directoryName", consumerClass = AbstractDurableFileConsumer.class, label = "codice,file")
public class DurableFileEndpoint extends GenericFileEndpoint<EventfulFileWrapper> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DurableFileEndpoint.class);

    private final Boolean isDav;

    @UriPath(name = "directoryName")
    @Metadata(required = "true")
    private File file;

    DurableFileEndpoint(String uri, DurableFileComponent durableFileComponent) {
        super(uri, durableFileComponent);
        this.isDav = durableFileComponent.isDav;
    }

    @Override
    public GenericFileConsumer<EventfulFileWrapper> createConsumer(Processor processor)
            throws Exception {
        ObjectHelper.notNull(file, "file");

        if (isDav) {
            return new DurableWebDavFileConsumer(this,
                    processor,
                    new EventfulFileWrapperGenericFileOperations());
        } else {
            return new DurableFileSystemFileConsumer(this,
                    processor,
                    new EventfulFileWrapperGenericFileOperations());
        }
    }

    @Override
    public GenericFileProducer<EventfulFileWrapper> createProducer() throws Exception {
        return null;
    }

    @Override
    public Exchange createExchange(GenericFile file) {
        Exchange exchange = createExchange();
        if (file != null) {
            file.bindToExchange(exchange);
        }
        return exchange;
    }

    @Override
    public String getScheme() {
        return "durable";
    }

    @Override
    public char getFileSeparator() {
        return File.separatorChar;
    }

    @Override
    public boolean isAbsolute(String name) {
        return Paths.get(name)
                .isAbsolute();
    }

    @Override
    protected String createEndpointUri() {
        return file.toURI()
                .toString();
    }

    public void setFile(File file) {
        this.file = file;
        // update configuration as well
        try {
            getConfiguration().setDirectory(file.getCanonicalPath());
        } catch (IOException e) {
            LOGGER.warn("Unable to canonicalize {}. Verify location is accessible.",
                    file.toString());
        }
    }

    private static class EventfulFileWrapperGenericFileOperations
            implements GenericFileOperations<EventfulFileWrapper> {
        @Override
        public void setEndpoint(GenericFileEndpoint<EventfulFileWrapper> endpoint) {

        }

        @Override
        public boolean deleteFile(String name) throws GenericFileOperationFailedException {
            return new File(name).delete();
        }

        @Override
        public boolean existsFile(String name) throws GenericFileOperationFailedException {
            return new File(name).exists();
        }

        @Override
        public boolean renameFile(String from, String to)
                throws GenericFileOperationFailedException {
            File srcFile = new File(from);
            File destFile = new File(to);
            try {
                FileUtils.moveFile(srcFile, destFile);
            } catch (IOException e) {
                return false;
            }
            return !srcFile.exists() && destFile.exists();
        }

        @Override
        public boolean buildDirectory(String directory, boolean absolute)
                throws GenericFileOperationFailedException {
            File dir = new File(directory);
            try {
                FileUtils.forceMkdir(dir);
            } catch (IOException e) {
                return false;
            }
            return dir.exists();
        }

        @Override
        public boolean retrieveFile(String name, Exchange exchange)
                throws GenericFileOperationFailedException {
            exchange.setOut(exchange.getIn());
            return true;
        }

        @Override
        public void releaseRetreivedFileResources(Exchange exchange)
                throws GenericFileOperationFailedException {

        }

        @Override
        public boolean storeFile(String name, Exchange exchange)
                throws GenericFileOperationFailedException {
            return false;
        }

        @Override
        public String getCurrentDirectory() throws GenericFileOperationFailedException {
            return null;
        }

        @Override
        public void changeCurrentDirectory(String path) throws GenericFileOperationFailedException {

        }

        @Override
        public void changeToParentDirectory() throws GenericFileOperationFailedException {

        }

        @Override
        public List<EventfulFileWrapper> listFiles() throws GenericFileOperationFailedException {
            return null;
        }

        @Override
        public List<EventfulFileWrapper> listFiles(String path)
                throws GenericFileOperationFailedException {
            return null;
        }
    }
}
