/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.security.pdp.xacml.processor;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.balana.finder.impl.FileBasedPolicyFinderModule;

/**
 * 
 * @author Dan Figliola
 * @author Shaun Morris
 * @author ddf.isgs@lmco.com
 * 
 * This class Polls Directories for policies. It is used with the PDP to
 * poll directories for changes to polices or the policy set in the
 * directories.
 * 
 */
public class PollingPolicyFinderModule extends FileBasedPolicyFinderModule
        implements FileAlterationListener {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(PollingPolicyFinderModule.class);

    private static final int MULTIPLIER = 1000;
    
    private FileAlterationMonitor monitor;

    private Set<String> xacmlPolicyDirectories;

    /**
     * 
     * @param xacmlPolicyDirectories
     *            - to search for policies
     * @param pollingInterval
     *            - in seconds
     */
    public PollingPolicyFinderModule(Set<String> xacmlPolicyDirectories,
            long pollingInterval) {
        super(xacmlPolicyDirectories);
        this.xacmlPolicyDirectories = xacmlPolicyDirectories;
        initialize(xacmlPolicyDirectories, pollingInterval);
    }

    private void initialize(Set<String> xacmlPolicyDirectories,
            long pollingInterval) {
        LOGGER.debug("initializing polling: {}, every {}",
                xacmlPolicyDirectories, pollingInterval);
        monitor = new FileAlterationMonitor(pollingInterval * MULTIPLIER);

        Iterator<String> iterator = xacmlPolicyDirectories.iterator();

        while (iterator.hasNext()) {
            File directoryToMonitor = new File(iterator.next());
            FileAlterationObserver observer = new FileAlterationObserver(
                    directoryToMonitor, getXmlFileFilter());
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
            LOGGER.debug("Directory " + changedDir.getCanonicalPath()
                    + " changed.");
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

        reloadPolicies();
    }

    public void onDirectoryCreate(File createdDir) {
        try {
            LOGGER.debug("Directory " + createdDir.getCanonicalPath()
                    + " was created.");
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void onDirectoryDelete(File deletedDir) {
        try {
            LOGGER.debug("Directory " + deletedDir.getCanonicalPath()
                    + " was deleted.");
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void onFileChange(File changedFile) {
        try {
            LOGGER.debug("File " + changedFile.getCanonicalPath() + " changed.");
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

        reloadPolicies();
    }

    public void onFileCreate(File createdFile) {
        try {
            LOGGER.debug("File " + createdFile.getCanonicalPath()
                    + " was created.");
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

        reloadPolicies();
    }

    public void onFileDelete(File deleteFile) {
        try {
            LOGGER.debug("File " + deleteFile.getCanonicalPath()
                    + " was deleted.");
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

        reloadPolicies();
    }

    public void onStart(FileAlterationObserver observer) {
        try {
            String directoryPath = observer.getDirectory().getCanonicalPath();
            LOGGER.trace("starting to check directory for xacml policy update(s) " + directoryPath);
                     
            if(!xacmlPolicyDirectories.isEmpty() && isXacmlPoliciesDirectoryEmpty(xacmlPolicyDirectories.iterator().next())) {
	            LOGGER.warn("No XACML Policies found in: {}", directoryPath);
            }       
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void onStop(FileAlterationObserver observer) {
        try {
            LOGGER.trace("Done checking directory "
                    + observer.getDirectory().getCanonicalPath());
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Checks if the XACML policy directory is empty.
     * 
     * @param xacmlPoliciesDirectory
     *            The directory containing the XACML policy.
     * @return true if the directory is empty and false otherwise.
     */
    private boolean isXacmlPoliciesDirectoryEmpty(File xacmlPoliciesDirectory) {
        return xacmlPoliciesDirectory.isDirectory()
                && xacmlPoliciesDirectory.listFiles(getXmlFilenameFilter()).length == 0;

    }
    
    /**
     * Checks if the XACML policy directory is empty.
     * 
     * @param xacmlPoliciesDirectory
     *            The directory containing the XACML policy.
     * @return true if the directory is empty and false otherwise.
     */
    private boolean isXacmlPoliciesDirectoryEmpty(String xacmlPoliciesDirectory) {
        return isXacmlPoliciesDirectoryEmpty(new File(xacmlPoliciesDirectory));

    }

    /**
     * Checks if the XACML policy directory is empty.
     * 
     * @param xacmlPoliciesDirectory
     *            The directory containing the XACML policy.
     * @return true if the directory is empty and false otherwise.
     */
    private boolean isXacmlPoliciesDirectoriesEmpty(
            Set<String> xacmlPoliciesDirectories) {
    	
    	//This method is currently not called, but remains in case
    	//multiple directories are supported
        for (String xacmlPolicyDirectory : xacmlPoliciesDirectories) {
            if (!isXacmlPoliciesDirectoryEmpty(xacmlPolicyDirectory)) {
                return false;
            }
        }
        return true;
    }

    private FileFilter getXmlFileFilter() {
        FileFilter xmlFileFilter = new FileFilter() {
            public boolean accept(File pathName) {
                return pathName.getName().toLowerCase().endsWith(".xml");
            }

        };

        return xmlFileFilter;
    }

    private FilenameFilter getXmlFilenameFilter() {
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".xml");
            }
        };

        return filter;
    }

    public void setPollingInterval(long pollingInterval) {
        initialize(xacmlPolicyDirectories, pollingInterval);
    }

    private void reloadPolicies() {
        LOGGER.debug("Reloading XACML policies");
        this.loadPolicies();
    }

}
