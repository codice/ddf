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
package ddf.security.pdp.realm.xacml.processor;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connexta.arbitro.finder.impl.FileBasedPolicyFinderModule;

import ddf.security.common.audit.SecurityLogger;

/**
 *         This class Polls Directories for policies. It is used with the PDP to poll directories
 *         for changes to polices or the policy set in the directories.
 */
public class PollingPolicyFinderModule extends FileBasedPolicyFinderModule
        implements FileAlterationListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingPolicyFinderModule.class);

    /*
     * Milliseconds
     */
    private static final int MULTIPLIER = 1000;

    private FileAlterationMonitor monitor;

    private Set<String> xacmlPolicyDirectories;

    /**
     * @param xacmlPolicyDirectories - to search for policies
     * @param pollingInterval        - in seconds
     */
    public PollingPolicyFinderModule(Set<String> xacmlPolicyDirectories, long pollingInterval) {
        super(xacmlPolicyDirectories);
        this.xacmlPolicyDirectories = xacmlPolicyDirectories;
        initialize(pollingInterval);
    }

    private void initialize(long pollingInterval) {
        LOGGER.debug("initializing polling: {}, every {}", xacmlPolicyDirectories, pollingInterval);
        monitor = new FileAlterationMonitor(pollingInterval * MULTIPLIER);

        for (String xacmlPolicyDirectory : xacmlPolicyDirectories) {
            File directoryToMonitor = new File(xacmlPolicyDirectory);
            FileAlterationObserver observer = new FileAlterationObserver(directoryToMonitor,
                    getXmlFileFilter());
            observer.addListener(this);
            monitor.addObserver(observer);
            LOGGER.debug("Monitoring directory: " + directoryToMonitor);
        }
    }

    public void start() {
        try {
            monitor.start();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void onDirectoryChange(File changedDir) {
        try {
            SecurityLogger.audit("Directory {} changed.", changedDir.getCanonicalPath());
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

        reloadPolicies();
    }

    public void onDirectoryCreate(File createdDir) {
        try {
            SecurityLogger.audit("Directory {} was created.", createdDir.getCanonicalPath());
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void onDirectoryDelete(File deletedDir) {
        try {
            SecurityLogger.audit("Directory {} was deleted.", deletedDir.getCanonicalPath());
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void onFileChange(File changedFile) {
        try {
            SecurityLogger.audit("File {} changed to:\n{}",
                    changedFile.getCanonicalPath(),
                    new String(Files.readAllBytes(Paths.get(changedFile.getCanonicalPath())),
                            StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

        reloadPolicies();
    }

    public void onFileCreate(File createdFile) {
        try {
            SecurityLogger.audit("File {} was created with content:\n{}",
                    createdFile.getCanonicalPath(),
                    new String(Files.readAllBytes(Paths.get(createdFile.getCanonicalPath())),
                            StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

        reloadPolicies();
    }

    public void onFileDelete(File deleteFile) {
        try {
            SecurityLogger.audit("File {} was deleted.", deleteFile.getCanonicalPath());
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

        reloadPolicies();
    }

    public void onStart(FileAlterationObserver observer) {
        try {
            String directoryPath = observer.getDirectory()
                    .getCanonicalPath();
            LOGGER.trace("starting to check directory for xacml policy update(s) " + directoryPath);

            if (!xacmlPolicyDirectories.isEmpty() && isXacmlPoliciesDirectoryEmpty(
                    xacmlPolicyDirectories.iterator()
                            .next())) {
                LOGGER.warn("No XACML Policies found in: {}", directoryPath);
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void onStop(FileAlterationObserver observer) {
        try {
            LOGGER.trace("Done checking directory " + observer.getDirectory()
                    .getCanonicalPath());
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Checks if the XACML policy directory is empty.
     *
     * @param xacmlPoliciesDirectory The directory containing the XACML policy.
     * @return true if the directory is empty and false otherwise.
     */
    private boolean isXacmlPoliciesDirectoryEmpty(File xacmlPoliciesDirectory) {
        boolean empty = false;
        if (null != xacmlPoliciesDirectory && xacmlPoliciesDirectory.isDirectory()) {
            File[] files = xacmlPoliciesDirectory.listFiles();
            empty = files == null || files.length == 0;
        }

        return empty;
    }

    /**
     * Checks if the XACML policy directory is empty.
     *
     * @param xacmlPoliciesDirectory The directory containing the XACML policy.
     * @return true if the directory is empty and false otherwise.
     */
    private boolean isXacmlPoliciesDirectoryEmpty(String xacmlPoliciesDirectory) {
        return isXacmlPoliciesDirectoryEmpty(new File(xacmlPoliciesDirectory));

    }

    private FileFilter getXmlFileFilter() {
        return pathName -> pathName.getName()
                .toLowerCase()
                .endsWith(".xml");
    }

    public void reloadPolicies() {
        LOGGER.debug("Reloading XACML policies");
        this.loadPolicies();
    }

}
