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
package ddf.sdk.xacml;

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
    private static final String POLICY_FILE = "access-policy.xml";

    private static final String POLICY_DIR = "/etc/pdp/policies/";

    private static final String KARAF_HOME = "karaf.home";

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleXACML.class);

    public SampleXACML() {
        LOGGER.trace("ENTERING: SampleXACML constructor");

        // Get root directory of running instance
        String karafHomeDirStr = System.getProperty(KARAF_HOME);

        if (StringUtils.isNotEmpty(karafHomeDirStr)) {
            // Write policy file to policies directory
            BufferedInputStream inStream = null;
            FileOutputStream outStream = null;
            try {
                // Retrieve an input stream to the XACML policy file in the resource directory
                inStream = new BufferedInputStream(
                        this.getClass().getClassLoader().getResourceAsStream(POLICY_FILE));

                if (null != inStream) {
                    final File karafHomeDir = new File(karafHomeDirStr);
                    if (karafHomeDir.exists()) {
                        String policyDirStr = karafHomeDirStr + POLICY_DIR;
                        File outputDir = new File(policyDirStr);
                        boolean outputDirExists = false;
                        if (!outputDir.exists()) {
                            if (outputDir.mkdirs()) {
                                LOGGER.debug("Creating directory {}", policyDirStr);
                                outputDirExists = true;
                            } else {
                                LOGGER.error("Could not make the policy output directory!");
                            }
                        } else {
                            outputDirExists = true;
                        }
                        if (outputDirExists) {
                            File outputFile = new File(policyDirStr + POLICY_FILE);
                            outStream = FileUtils.openOutputStream(outputFile);
                            if (null != outStream) {
                                LOGGER.debug("Copying file {} to {}", POLICY_FILE, policyDirStr);
                                IOUtils.copy(inStream, outStream);
                            } else {
                                LOGGER.error("Could not create policy file output stream!");
                            }
                        }
                    } else {
                        LOGGER.error("Karaf home directory does not exist!");
                    }
                } else {
                    LOGGER.error("Could not retrieve policy file!");
                }
            } catch (IOException ex) {
                LOGGER.error("Unable to write out policy file!", ex);
            } finally {
                IOUtils.closeQuietly(inStream);
                IOUtils.closeQuietly(outStream);
            }
        } else {
            LOGGER.error("Could not determine Karaf home directory!");
        }

        LOGGER.trace("EXITING: SampleXACML constructor");
    }
}
