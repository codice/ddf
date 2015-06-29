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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSampleXACML {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleXACML.class);

    private static TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testConstructorWithCorrectParameters() {
        createTempFolder();
        SampleXACML testInstance = new SampleXACML(SampleXACML.ALL_POLICIES,
                tempFolder.getRoot().getPath());
        assertThat(testInstance.copyPolicies(), is(true));
        removeTempFolder();
    }

    @Test
    public void testEmptyKarafHome() {
        createTempFolder();
        SampleXACML testInstance = new SampleXACML(SampleXACML.ALL_POLICIES, null);
        assertThat(testInstance.copyPolicies(), is(false));
        removeTempFolder();
    }

    @Test
    public void testBadPolicyFileName() {
        createTempFolder();
        String[] badPolicy = new String[1];
        badPolicy[0] = "nonexistant-policy.xml";
        SampleXACML testInstance = new SampleXACML(badPolicy, tempFolder.getRoot().getPath());
        assertThat(testInstance.copyPolicies(), is(false));
        removeTempFolder();
    }

    @Test
    public void testPoliciesAlreadyExist() {
        createTempFolder();
        SampleXACML testInstance = new SampleXACML(SampleXACML.ALL_POLICIES,
                tempFolder.getRoot().getPath());
        testInstance.copyPolicies();
        assertThat(testInstance.copyPolicies(), is(false));
        removeTempFolder();
    }

    @Test
    public void testInvalidDirectory() {
        createTempFolder();
        SampleXACML testInstance = new SampleXACML(SampleXACML.ALL_POLICIES, "!@#$%^&*()");
        assertThat(testInstance.copyPolicies(), is(false));
        removeTempFolder();
    }

    // Temporary Folder setup and tear down
    public void createTempFolder() {
        try {
            tempFolder.create();
        } catch (IOException ex) {
            LOGGER.trace("Unable to create temporary folder. {}", ex);
        }
    }

    public void removeTempFolder() {
        tempFolder.delete();
    }
}