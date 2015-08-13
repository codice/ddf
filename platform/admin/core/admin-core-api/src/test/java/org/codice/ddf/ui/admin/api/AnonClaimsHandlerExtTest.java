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
package org.codice.ddf.ui.admin.api;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AnonClaimsHandlerExtTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private boolean filesCreated = false;

    private String profileDirPath;

    private String availableClaimsFilePath;

    @Before
    public void setUp() throws Exception {
        if (!filesCreated) {
            File availableClaimsFile = testFolder.newFile("attributeMap.properties");
            availableClaimsFilePath = availableClaimsFile.getCanonicalPath();
            Properties availableClaims = new Properties();
            availableClaims.put("testClaim1", "testValue1");
            availableClaims.put("testClaim2", "testValue2");
            availableClaims.put("testClaim3", "testValue3");
            availableClaims.store(new FileWriter(availableClaimsFile), "test");

            File profileDir = testFolder.newFolder("profiles");
            profileDirPath = profileDir.getCanonicalPath();
            File profile1 = new File(profileDir, "profile1.properties");
            File profile2 = new File(profileDir, "profile2.properties");
            File profile3 = new File(profileDir, "profile3.properties");
            File randomeFile = new File(profileDir, "random.cfg");
            randomeFile.createNewFile();

            Properties profileClaims1 = new Properties();
            profileClaims1.put("profileName", "profile1");
            profileClaims1.put("testClaim1", "profile1Value1");
            profileClaims1.put("testClaim2", "profile1Value2");
            profileClaims1.put("testClaim3", "profile1Value3");
            profileClaims1.put("testClaim4", "profile1Value4");
            profileClaims1.store(new FileWriter(profile1), "test");

            Properties profileClaims2 = new Properties();
            profileClaims2.put("profileName", "profile2");
            profileClaims2.put("testClaim1", "profile2Value1");
            profileClaims2.put("testClaim2", "profile2Value2");
            profileClaims2.put("testClaim3", "profile2Value3");
            profileClaims2.put("testClaim4", "profile2Value4");
            profileClaims2.store(new FileWriter(profile2), "test");

            //no name profile will be excluded
            Properties profileClaims3 = new Properties();
            profileClaims3.put("testClaim1", "profile2Value1");
            profileClaims3.put("testClaim2", "profile2Value2");
            profileClaims3.put("testClaim3", "profile2Value3");
            profileClaims3.put("testClaim4", "profile2Value4");
            profileClaims3.store(new FileWriter(profile3), "test");

            filesCreated = true;
        }

    }

    @Test
    public void testInitNormal() throws Exception {
        AnonClaimsHandlerExt ache = new AnonClaimsHandlerExt();
        ache.setAvailableClaimsFile(availableClaimsFilePath);
        ache.setProfileDir(profileDirPath);
        ache.setImmutableClaims("testClaim1,testClaim2");
        ache.init();
        assertThat(ache.getClaims().size(), equalTo(2));
        assertThat(ache.getClaimsProfiles().size(), equalTo(2));
    }

    @Test
    public void testInitBadClaimsPath() throws Exception {
        AnonClaimsHandlerExt ache = new AnonClaimsHandlerExt();
        ache.setAvailableClaimsFile("/this/path/is/bad/12321231");
        ache.setProfileDir(profileDirPath);
        ache.setImmutableClaims("testClaim1,testClaim2");
        ache.init();
        assertThat(ache.getClaims().size(), equalTo(2));
        assertThat(ache.getClaimsProfiles().size(), equalTo(2));
    }

    @Test
    public void testInitBadProfilePath() throws Exception {
        AnonClaimsHandlerExt ache = new AnonClaimsHandlerExt();
        ache.setAvailableClaimsFile(availableClaimsFilePath);
        ache.setProfileDir("/this/path/is/bad/12321231");
        ache.setImmutableClaims("testClaim1,testClaim2");
        ache.init();
        assertThat(ache.getClaims().size(), equalTo(2));
        assertThat(ache.getClaimsProfiles().size(), equalTo(2));
    }

    @Test
    public void testGetClaimsProfilesNormal() throws Exception {
        AnonClaimsHandlerExt ache = new AnonClaimsHandlerExt();
        ache.setAvailableClaimsFile(availableClaimsFilePath);
        ache.setProfileDir(profileDirPath);
        ache.setImmutableClaims("testClaim1,testClaim2");
        ache.init();
        Map<String, Object> profiles = ache.getClaimsProfiles();
        assertThat(profiles.size(), equalTo(2));
        assertNotNull(profiles.get("availableProfiles"));
        assertNotNull(profiles.get("profileNames"));
        Map<String, Object> availProfiles = (Map<String, Object>) profiles.get("availableProfiles");
        assertNotNull(availProfiles.get("profile1"));
        assertNotNull(availProfiles.get("profile2"));
        Set<String> names = (Set<String>) profiles.get("profileNames");
        assertThat(names.size(), equalTo(2));
    }

    @Test
    public void testGetClaimsNormal() throws Exception {
        AnonClaimsHandlerExt ache = new AnonClaimsHandlerExt();
        ache.setAvailableClaimsFile(availableClaimsFilePath);
        ache.setProfileDir(profileDirPath);
        ache.setImmutableClaims("testClaim1,testClaim2");
        ache.init();
        Map<String, Object> claims = ache.getClaims();
        assertThat(claims.size(), equalTo(2));
        assertNotNull(claims.get("availableClaims"));
        assertNotNull(claims.get("immutableClaims"));
        List<String> availClaims = (List<String>) claims.get("availableClaims");
        assertThat(availClaims.size(), equalTo(3));
        List<String> immutableClaims = (List<String>) claims.get("immutableClaims");
        assertThat(immutableClaims.size(), equalTo(2));
    }
}