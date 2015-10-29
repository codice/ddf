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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationFilesPoller implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFilesPoller.class);

    private final WatchService watchService;

    private final ExecutorService executorService;

    private ChangeListener configurationDirectory;

    private final Path configurationDirectoryPath;

    private final String fileExtension;

    public ConfigurationFilesPoller(Path configurationDirectoryPath, String fileExtension,
            WatchService watchService, ExecutorService executorService) {
        this.configurationDirectoryPath = configurationDirectoryPath;
        this.watchService = watchService;
        this.executorService = executorService;
        this.fileExtension = fileExtension;
    }

    public void init() {
        LOGGER.debug("Starting {}...", this.getClass().getName());
        executorService.execute(this);
    }

    public void register(ChangeListener listener) {
        configurationDirectory = listener;
    }

    @Override
    public void run() {
        try {
            try {
                LOGGER.debug("Registering path [{}] with Watch Service.",
                        configurationDirectoryPath.toString());
                configurationDirectoryPath.register(watchService, ENTRY_CREATE);
            } catch (IOException e) {
                LOGGER.error("Unable to register path [{}] with Watch Service",
                        configurationDirectoryPath.toString(), e);
                return;
            }

            WatchKey key = null;
            while (!Thread.currentThread().isInterrupted()) {
                key = watchService.take(); // blocking
                LOGGER.debug("Key has been signalled.  Looping over events.");

                for (WatchEvent<?> genericEvent : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = genericEvent.kind();

                    if (kind == OVERFLOW || kind == ENTRY_MODIFY || kind == ENTRY_DELETE) {
                        LOGGER.debug("Skipping event [{}]", kind);
                        continue;
                    }

                    Path filename = (Path) genericEvent.context();

                    if (!filename.toString().endsWith(fileExtension)) {
                        LOGGER.debug(
                                "Skipping event for [{}] due to unsupported file extension of [{}].",
                                filename, fileExtension);
                        continue; // just skip to the next event
                    }

                    if (configurationDirectory != null) {
                        LOGGER.debug("Notifying [{}] of event [{}] for file [{}].",
                                configurationDirectory.getClass().getName(), kind,
                                configurationDirectoryPath.resolve(filename));
                        configurationDirectory.notify(configurationDirectoryPath.resolve(filename));
                    }
                }

                // Reset key, shutdown watcher if directory not able to be observed
                // (possibly deleted)
                if (!key.reset()) {
                    LOGGER.warn("Configurations in [{}] are no longer able to be observed.",
                            configurationDirectoryPath);
                    break;
                }
            }
        } catch (InterruptedException | RuntimeException e) {
            LOGGER.error("The [{}] was interrupted.", this.getClass().getName(), e);
            Thread.currentThread().interrupt();
        }
    }

    public void destroy() {
        try {
            watchService.close();
            executorService.shutdown();

            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    LOGGER.error("[{}] did not terminate correctly.", getClass().getName());
                }
            }
        } catch (IOException | InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
