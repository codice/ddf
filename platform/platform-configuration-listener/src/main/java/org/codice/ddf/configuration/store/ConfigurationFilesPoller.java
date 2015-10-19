/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package org.codice.ddf.configuration.store;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static org.codice.ddf.configuration.store.ChangeListener.ChangeType.CREATED;
import static org.codice.ddf.configuration.store.ChangeListener.ChangeType.DELETED;
import static org.codice.ddf.configuration.store.ChangeListener.ChangeType.UPDATED;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.codice.ddf.configuration.listener.ConfigurationFileListener;
import org.codice.ddf.configuration.store.ChangeListener.ChangeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

public class ConfigurationFilesPoller implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFilesPoller.class);

    private static final Map<Kind, ChangeType> KIND_CHANGE_TYPE_MAP = ImmutableMap
            .of(ENTRY_CREATE, CREATED, ENTRY_MODIFY, UPDATED, ENTRY_DELETE, DELETED);

    private final WatchService watchService;

    private final ExecutorService watchThread;

    private final Path configurationDirectory;

    private final String fileExtension;

    private ChangeListener listener;

    public ConfigurationFilesPoller(Path configurationDirectory, String fileExtension,
            WatchService watchService, ExecutorService watchThread) {
        this.configurationDirectory = configurationDirectory;
        this.watchService = watchService;
        this.watchThread = watchThread;
        this.fileExtension = fileExtension;
        this.listener = null;
        LOGGER.debug(
                "Configuration directory for [{}] is [{}].  Files with an extension of [{}] will be observed.",
                ConfigurationFileListener.class.getName(), configurationDirectory, fileExtension);
    }

    public void register(ChangeListener listener) throws IOException {
        LOGGER.debug(
                "{} initializing by reading/updating all the configurations in [{}] with the file extension of [{}]",
                getClass().getSimpleName(), configurationDirectory, fileExtension);

        this.listener = listener;
        this.configurationDirectory
                .register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        watchThread.execute(this);

        LOGGER.debug(
                "Starting the Configuration Observer.  From this point onward, any updates to files with the extension of [{}] in [{}] will be detected and acted on.",
                fileExtension, configurationDirectory);
    }

    @Override
    public void run() {
        try {
            WatchKey key;
            while (!Thread.currentThread().isInterrupted()) {
                key = watchService.take();  //blocking
                LOGGER.debug("Key has been signalled.  Looping over events.");

                for (WatchEvent<?> genericEvent : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = genericEvent.kind();

                    if (kind.equals(OVERFLOW)) {
                        LOGGER.debug("Skipping event due to overflow");
                        continue;
                    }

                    String filename = genericEvent.context().toString();

                    if (!filename.endsWith(fileExtension)) {
                        LOGGER.debug("Skipping event for [{}] due to file extension.", filename);
                        continue;  //just skip to the next event
                    }

                    LOGGER.debug("Processing [{}] event for for [{}].", kind, filename);

                    String pid = filename.substring(0, filename.lastIndexOf("."));

                    try {
                        listener.update(pid, KIND_CHANGE_TYPE_MAP.get(kind));
                    } catch (RuntimeException e) {
                        LOGGER.error(
                                "A runtime exception occured"); // TODO is this try catch needed anymore?
                    }
                }
                // reset key, shutdown watcher if directory no able to be observed (possibly deleted, who knows)
                if (!key.reset()) {
                    LOGGER.warn("Configurations in [{}] are no longer able to be observed.",
                            configurationDirectory);
                    break;
                }
            }
        } catch (InterruptedException | RuntimeException ex) {
            LOGGER.error("The Configuration Observer was interrupted.", ex);
            Thread.currentThread().interrupt();
        }
    }

    public void destroy() {
        try {
            watchService.close();
            watchThread.shutdown();

            if (!watchThread.awaitTermination(10, TimeUnit.SECONDS)) {
                watchThread.shutdownNow();
                if (!watchThread.awaitTermination(10, TimeUnit.SECONDS)) {
                    LOGGER.error("[{}] did not terminate correctly.", getClass().getName());
                }
            }
        } catch (IOException | InterruptedException ex) {
            watchThread.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
