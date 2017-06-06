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
package org.codice.ddf.admin.core.impl;

import static org.boon.Boon.toJson;
import static org.codice.ddf.admin.core.impl.GuestClaimsHandlerExt.DEFAULT_NAME;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.codice.ddf.platform.util.properties.PropertiesFileReader;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableMap;

public class GuestClaimsHandlerExtTest {
    @ClassRule
    public static TemporaryFolder testFolder = new TemporaryFolder();

    private static final String GUEST_CLAIMS = "guestClaims";

    private static String profilesDotJsonPath;

    private static String availableClaimsFilePath;

    @BeforeClass
    public static void setUp() throws Exception {
        File availableClaimsFile = testFolder.newFile("attributeMap.properties");
        availableClaimsFilePath = availableClaimsFile.getCanonicalPath();

        Properties availableClaims = new Properties();
        availableClaims.put("testClaim1", "testValue1");
        availableClaims.put("testClaim2", "testValue2");
        availableClaims.put("testClaim3", "testValue3");
        availableClaims.store(new FileWriter(availableClaimsFile), "test");

        File profilesJson = testFolder.newFile("profiles.json");
        profilesDotJsonPath = profilesJson.getCanonicalPath();

        Map<String, Object> outterMap = new HashMap<>();
        Map<String, Object> profile1Map = new HashMap<>();
        Map<String, Object> profile2Map = new HashMap<>();

        outterMap.put("profile1", ImmutableMap.of(GUEST_CLAIMS, profile1Map));
        outterMap.put("profile2", ImmutableMap.of(GUEST_CLAIMS, profile2Map));

        profile1Map.put("testClaim1", "profile1Value1");
        profile1Map.put("testClaim2", "profile1Value2");
        profile1Map.put("testClaim3", "profile1Value3");
        profile1Map.put("testClaim4", "profile1Value4");

        profile2Map.put("testClaim1", "profile2Value1");
        profile2Map.put("testClaim2", "profile2Value2");
        profile2Map.put("testClaim3", "profile2Value3");
        profile2Map.put("testClaim4", "profile2Value4");

        String json = toJson(outterMap);
        FileUtils.write(profilesJson, json);
    }

    @Test
    public void testInitNormal() throws Exception {
        GuestClaimsHandlerExt handlerExt = new GuestClaimsHandlerUnderTest(profilesDotJsonPath,
                availableClaimsFilePath);
        handlerExt.init();
        assertThat(handlerExt.getClaims()
                .size(), equalTo(2));
        assertThat(handlerExt.getClaimsProfiles()
                .size(), equalTo(2));
    }

    @Test
    public void testInitBadClaimsPath() throws Exception {
        GuestClaimsHandlerExt handlerExt = new GuestClaimsHandlerUnderTest(profilesDotJsonPath,
                "/this/path/is/bad/12321231");
        handlerExt.init();
        assertThat(handlerExt.getClaims()
                .size(), equalTo(2));
        assertThat(handlerExt.getClaimsProfiles()
                .size(), equalTo(2));
    }

    @Test
    public void testInitBadProfilePath() throws Exception {
        GuestClaimsHandlerExt handlerExt = new GuestClaimsHandlerUnderTest(
                "/this/path/is/bad/12321231",
                availableClaimsFilePath);
        handlerExt.init();
        assertThat(handlerExt.getClaims()
                .size(), equalTo(2));

        Map<String, Object> claimsProfiles = handlerExt.getClaimsProfiles();
        assertThat(claimsProfiles.size(), equalTo(2));
        /* The frontend is expecting the object returned to have these fields */
        assertTrue(claimsProfiles.containsKey("availableProfiles"));
        assertTrue(claimsProfiles.containsKey("profileNames"));
    }

    @Test
    public void testGetClaimsProfilesNormal() throws Exception {
        GuestClaimsHandlerExt handlerExt = new GuestClaimsHandlerUnderTest(profilesDotJsonPath,
                availableClaimsFilePath);
        handlerExt.init();
        Map<String, Object> profiles = handlerExt.getClaimsProfiles();
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
        GuestClaimsHandlerExt handlerExt = new GuestClaimsHandlerUnderTest(profilesDotJsonPath,
                availableClaimsFilePath);
        handlerExt.init();
        Map<String, Object> claims = handlerExt.getClaims();
        assertThat(claims.size(), equalTo(2));
        assertNotNull(claims.get("availableClaims"));
        assertNotNull(claims.get("immutableClaims"));
        List<String> availClaims = (List<String>) claims.get("availableClaims");
        assertThat(availClaims.size(), equalTo(3));
        List<String> immutableClaims = (List<String>) claims.get("immutableClaims");
        assertThat(immutableClaims.size(), equalTo(2));
    }

    @Test
    public void testFieldNullability() throws Exception {
        GuestClaimsHandlerExt handlerExt = new GuestClaimsHandlerUnderTest(profilesDotJsonPath,
                availableClaimsFilePath);
        handlerExt.init();
        assertThat(handlerExt.getProfileGuestClaims(), is(nullValue()));
        assertThat(handlerExt.getProfileSystemClaims(), is(nullValue()));
        assertThat(handlerExt.getProfileConfigs(), is(nullValue()));
    }

    @Test
    public void testDefaultClaimsProfile() throws Exception {
        GuestClaimsHandlerExt handlerExt = new GuestClaimsHandlerUnderTest(profilesDotJsonPath,
                availableClaimsFilePath);
        handlerExt.init();
        handlerExt.setSelectedClaimsProfileName(DEFAULT_NAME);
        assertThat(handlerExt.getProfileGuestClaims(), is(nullValue()));
        assertThat(handlerExt.getProfileSystemClaims(), is(nullValue()));
        assertThat(handlerExt.getProfileConfigs(), is(nullValue()));
    }

    @Test
    public void testSelectClaimsProfileRoundTrip() throws Exception {
        GuestClaimsHandlerExt handlerExt = new GuestClaimsHandlerUnderTest(profilesDotJsonPath,
                availableClaimsFilePath);
        handlerExt.init();
        handlerExt.setSelectedClaimsProfileName("profile1");
        verifyMapValidity(handlerExt.getProfileGuestClaims());
        assertNull(handlerExt.getProfileSystemClaims());
        assertNull(handlerExt.getProfileConfigs());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSelectClaimsProfileBadName() throws Exception {
        GuestClaimsHandlerExt handlerExt = new GuestClaimsHandlerUnderTest(profilesDotJsonPath,
                availableClaimsFilePath);
        handlerExt.init();
        handlerExt.setSelectedClaimsProfileName("profile0");
    }

    private void verifyMapValidity(Map inputMap) throws Exception {
        assertThat(inputMap.size(), is(4));
        assertThat(inputMap.get("testClaim1"), is("profile1Value1"));
        assertThat(inputMap.get("testClaim2"), is("profile1Value2"));
        assertThat(inputMap.get("testClaim3"), is("profile1Value3"));
        assertThat(inputMap.get("testClaim4"), is("profile1Value4"));
    }

    private static class GuestClaimsHandlerUnderTest extends GuestClaimsHandlerExt {
        public GuestClaimsHandlerUnderTest(String profilesDirectory, String availableClaimsFile) {
            super(new PropertiesFileReader(),
                    Arrays.asList("testClaim1", "testClaim2"),
                    availableClaimsFile,
                    profilesDirectory);
        }
    }
}