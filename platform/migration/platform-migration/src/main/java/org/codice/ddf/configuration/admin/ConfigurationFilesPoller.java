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

package org.codice.ddf.configuration.admin;

import static org.apache.commons.lang.Validate.notNull;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that monitors changes to files with a specific extension in a directory.
 */
public class ConfigurationFilesPoller implements FileAlterationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFilesPoller.class);

    public static final long POLLING_INTERVAL = 2000L;

    private FileAlterationMonitor watchService;

    private FileAlterationObserver fileAlterationObserver;

    private final Path configurationDirectoryPath;

    private final String fileExtension;

    private ChangeListener changeListener;

    /**
     * Constructor.
     *
     * @param configurationDirectoryPath directory to watch for changes
     * @param fileExtension              extension of of the files to watch
     */
    public ConfigurationFilesPoller(Path configurationDirectoryPath, String fileExtension) {
        notNull(configurationDirectoryPath, "configurationDirectoryPath cannot be null");
        notNull(fileExtension, "fileExtension cannot be null");

        this.configurationDirectoryPath = configurationDirectoryPath;
        this.fileExtension = fileExtension;
    }

    public void register(ChangeListener listener) {
        notNull(listener, "ChangeListener cannot be null");
        changeListener = listener;
        fileAlterationObserver =
                new FileAlterationObserver(configurationDirectoryPath.toAbsolutePath()
                        .toString(), new SuffixFileFilter(fileExtension));
        fileAlterationObserver.addListener(this);
        watchService = new FileAlterationMonitor(POLLING_INTERVAL, fileAlterationObserver);
        try {
            watchService.start();
        } catch (Exception e) {
            logStackAndMessageSeparately(e,
                    "Failed to start, 'Platform :: Migration' must be restarted: ");
        }
    }

    protected void logStackAndMessageSeparately(Exception e, String s) {
        LOGGER.debug(s, e);
        LOGGER.warn(s + e.getMessage());
    }

    @Override
    public void onStart(FileAlterationObserver fileAlterationObserver) {
    }

    @Override
    public void onDirectoryCreate(File file) {
    }

    @Override
    public void onDirectoryChange(File file) {
    }

    @Override
    public void onDirectoryDelete(File file) {
    }

    @Override
    public void onFileCreate(File file) {
        try {
            String filename = file.getName();
            LOGGER.debug("Watcher has been notified. Handling event for [{}].", filename);

            waitForFileToBeCompletelyWritten(file);
            LOGGER.debug("Notifying [{}] of creation for file [{}].",
                    changeListener.getClass()
                            .getName(),
                    file.getPath());
            changeListener.notify(Paths.get(file.toURI()));
        } catch (InterruptedException | RuntimeException e) {
            logStackAndMessageSeparately(e, "Error parsing " + file.getName() + " : ");
        }
    }

    // for unit testing
    void waitForFileToBeCompletelyWritten(File file) throws InterruptedException {
        long fileSizeBefore = 0L;
        long fileSizeAfter = 0L;
        do {
            fileSizeBefore = file.length();
            doSleep();
            fileSizeAfter = file.length();
            // just in case it's a buffered write, since most default to flushing in powers of 2
            if ((fileSizeAfter & -fileSizeAfter) == fileSizeAfter) {
                doSleep();
                fileSizeAfter = file.length();
            }
            LOGGER.debug("comparing file size " + fileSizeBefore + " with " + fileSizeAfter);
        } while (fileSizeBefore != fileSizeAfter);
    }

    // for unit testing
    void doSleep() throws InterruptedException {
        Thread.sleep(POLLING_INTERVAL);
    }

    @Override
    public void onFileChange(File file) {
    }

    @Override
    public void onFileDelete(File file) {
    }

    @Override
    public void onStop(FileAlterationObserver fileAlterationObserver) {
    }
}
