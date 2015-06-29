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
 **/
package ddf.security.pdp.xacml;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleXACML {
    protected static final String ACCESS_POLICY = "access-policy.xml";

    protected static final String POLICY_DIR = "/etc/pdp/policies/";

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleXACML.class);

    protected static final String[] ALL_POLICIES = {ACCESS_POLICY};

    protected static final String KARAF_HOME = "karaf.home";

    private BufferedInputStream[] copyStreams;

    private String[] copyFileNames;

    private String karafHomeDir;

    // Default constructor
    public SampleXACML() {
        LOGGER.trace("ENTERING: SampleXACML constructor");
        String karafHomeTemp = System.getProperty(KARAF_HOME);
        setPolicies(ALL_POLICIES, karafHomeTemp);
        copyPolicies();
        LOGGER.trace("EXITING: SampleXACML constructor");
    }

    // This constructor is for unit testing
    public SampleXACML(String[] policyFileNames, String karafHome) {
        LOGGER.trace("ENTERING: SampleXACML constructor");
        setPolicies(policyFileNames, karafHome);
        LOGGER.trace("EXITING: SampleXACML constructor");
    }

    private void setPolicies(String[] policyFileNames, String karafHome) {
        BufferedInputStream[] policyInputStreams = new BufferedInputStream[policyFileNames.length];

        for (int i = 0; i < policyFileNames.length; i++) {
            policyInputStreams[i] = new BufferedInputStream(
                    this.getClass().getClassLoader().getResourceAsStream(policyFileNames[i]));
        }

        // Get root directory of running instance
        karafHomeDir = karafHome;

        copyStreams = policyInputStreams;
        copyFileNames = policyFileNames;
    }

    public boolean copyPolicies() {
        boolean allSuccessful = true;

        if (StringUtils.isEmpty(karafHomeDir)) {
            LOGGER.info("Could not determine Karaf Home directory!");
            return false;
        }

        BufferedInputStream inStream;
        FileOutputStream outStream;
        for (int i = 0; i < copyStreams.length; i++) {
            // Write policy file to policies directory
            inStream = null;
            outStream = null;
            try {
                // Retrieve an input stream to the XACML policy file in the resource directory
                inStream = copyStreams[i];
                final File karafHomeDirFile = new File(karafHomeDir);
                if (karafHomeDirFile.exists()) {
                    String policyDirStr = karafHomeDir + POLICY_DIR;
                    File outputDir = new File(policyDirStr);
                    outputDir.mkdir();
                    File outputFile = new File(policyDirStr + copyFileNames[i]);
                    outStream = FileUtils.openOutputStream(outputFile);
                    if (null != outStream) {
                        IOUtils.copy(inStream, outStream);
                        LOGGER.info("Copying file to {}{}", policyDirStr, copyFileNames[i]);
                    } else {
                        LOGGER.info("Could not create policy file output stream! File: {}",
                                copyFileNames[i]);
                        allSuccessful = false;
                    }
                } else {
                    LOGGER.info("Karaf home directory does not exist! File: {}", copyFileNames[i]);
                    allSuccessful = false;
                }
            } catch (IOException ex) {
                LOGGER.info("Unable to write out policy file! File: {}", copyFileNames[i], ex);
                allSuccessful = false;
            } finally {
                IOUtils.closeQuietly(inStream);
                IOUtils.closeQuietly(outStream);
            }
        }

        if (allSuccessful) {
            LOGGER.info("All policies successfully copied.");
            return true;
        } else {
            LOGGER.info("One or more policy files failed to copy!");
            return false;
        }
    }
}