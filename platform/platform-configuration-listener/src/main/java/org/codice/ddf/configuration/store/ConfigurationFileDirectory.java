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

import static org.apache.commons.lang.Validate.isTrue;
import static org.apache.commons.lang.Validate.notNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Class that provides utility methods to access the configuration files in the configuration
 * directory.
 * <p/>
 * Note: Since this class is meant to only be used by {@link FileHandlerImpl} and is package
 * private, it assumes that all input validation has already been performed.
 */
class ConfigurationFileDirectory {

    private File configurationDirectory;

    private String fileExtension;

    /**
     * Constructor.
     *
     * @param configurationDirectory directory that contains the configuration files.
     *                               The directory must exist and be readable and writable.
     * @param fileExtension          configuration files extension with preceding period, e.g.,
     *                               ".cfg"
     * @throws IllegalArgumentException thrown if any of the arguments is invalid
     */
    public ConfigurationFileDirectory(@NotNull File configurationDirectory,
            @NotNull @Min(2) String fileExtension) {
        notNull(configurationDirectory, "Configuration directory cannot be null");
        notNull(fileExtension, "File extension is required");
        isTrue(fileExtension.length() >= 2, "Invalid file extension: ", fileExtension);
        isTrue(configurationDirectory.exists() && configurationDirectory.canRead()
                        && configurationDirectory.canWrite(),
                "Directory does not exist or is not readable/writable: ", configurationDirectory);

        this.configurationDirectory = configurationDirectory;
        this.fileExtension = fileExtension;
    }

    /**
     * Creates a {@link FileInputStream} that can be used to read the configuration file associated
     * with a specific PID.
     *
     * @param pid persistent ID of the configuration file to read
     * @return input stream
     * @throws FileNotFoundException thrown if the {@link FileInputStream} couldn't be created
     */
    public FileInputStream createFileInputStream(String pid) throws FileNotFoundException {
        return new FileInputStream(getFileNameFromPid(pid));
    }

    /**
     * Creates a {@link FileOutputStream} that can be used to write to the configuration file
     * associated with a specific PID.
     *
     * @param pid persistent ID of the configuration file to write
     * @return output stream
     * @throws FileNotFoundException thrown if the {@link FileOutputStream} couldn't be created
     */
    public FileOutputStream createFileOutputStream(String pid) throws FileNotFoundException {
        return new FileOutputStream(getFileNameFromPid(pid));
    }

    /**
     * Gets the list of configuration file PIDs in the configuration directory.
     *
     * @return list of configuration file PIDs
     */
    public Collection<String> listFiles() {
        final Collection<String> pids = new ArrayList<>();

        configurationDirectory.listFiles(createFilenameFilter(pids));

        return pids;
    }

    /**
     * Tells whether a configuration file exists for a specific PID
     *
     * @param pid persistence ID of the configuration file
     * @return {@code true} if a configuration file exists for the PID provided, {@code false}
     * otherwise
     */
    public boolean exists(String pid) {
        return getFileNameFromPid(pid).exists();
    }

    /**
     * Deletes the configuration file associated with a persistence ID.
     *
     * @param pid persistence ID of the configuration file to delete
     * @return {@code true} if the file was successfully deleted, {@code false} otherwise
     */
    public boolean delete(String pid) {
        return getFileNameFromPid(pid).delete();
    }

    // Package-private for unit testing purposes
    ConfigurationFileFilter createFilenameFilter(Collection<String> pids) {
        return new ConfigurationFileFilter(pids, fileExtension);
    }

    private File getFileNameFromPid(String pid) {
        return new File(
                configurationDirectory.getAbsolutePath() + File.separator + pid + fileExtension);
    }

    /**
     * Filter used to get the list of configuration files that have a specific extension.
     * <p/>
     * Package-private for unit testing purposes.
     */
    static class ConfigurationFileFilter implements FilenameFilter {
        private final Collection<String> pids;

        private String fileExtension;

        /**
         * Constructor.
         *
         * @param pids          collection that will contain the list of PIDs for which a
         *                      configuration file exists
         * @param fileExtension configuration file extension to look for
         */
        public ConfigurationFileFilter(Collection<String> pids, String fileExtension) {
            this.pids = pids;
            this.fileExtension = fileExtension;
        }

        /**
         * {@inheritDoc}
         * <p/>
         * Returns {@code true} only if the file has the proper file extension.
         */
        @Override
        public boolean accept(File dir, String name) {
            if (name.endsWith(fileExtension)) {
                pids.add(getPidFromFileName(name));
                return true;
            }
            return false;
        }

        private String getPidFromFileName(String fileName) {
            return fileName.substring(0, fileName.lastIndexOf("."));
        }
    }
}
