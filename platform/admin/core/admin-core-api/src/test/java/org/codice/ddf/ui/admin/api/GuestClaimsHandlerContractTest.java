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

import static org.codice.ddf.ui.admin.api.GuestClaimsHandlerExt.AVAILABLE_CLAIMS;
import static org.codice.ddf.ui.admin.api.GuestClaimsHandlerExt.AVAILABLE_PROFILES;
import static org.codice.ddf.ui.admin.api.GuestClaimsHandlerExt.DEFAULT_NAME;
import static org.codice.ddf.ui.admin.api.GuestClaimsHandlerExt.IMMUTABLE_CLAIMS;
import static org.codice.ddf.ui.admin.api.GuestClaimsHandlerExt.PROFILE_NAMES;
import static org.codice.ddf.ui.admin.api.GuestClaimsHandlerExt.PROFILE_NAME_KEY;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codice.ddf.ui.admin.api.util.PropertiesFileReader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

/**
 * TODO: DDF-2731
 * Temporary test class to be removed and refactored as part of moving {@link PropertiesFileReader}
 * to a common module. The unit tests will need to be reworked and sorted appropriately.
 */
@RunWith(MockitoJUnitRunner.class)
public class GuestClaimsHandlerContractTest {
    private static final String IMMUTABLE_CLAIM_1 = "immutableClaim1";

    private static final String IMMUTABLE_CLAIM_2 = "immutableClaim2";

    private static final String GUEST = "guest";

    private static final String PROFILES_DIRECTORY = "profiles";

    private static final String PROFILES_BANNER_DIRECTORY = "profileBanners";

    private static final String AVAILABLE_CLAIMS_FILE = "availableClaims";

    private static final String PROFILE_A = "profileA";

    private static final String PROFILE_A_VALUE = "profileAValue";

    private static final String PROFILE_B = "profileB";

    private static final String PROFILE_B_VALUE = "profileBValue";

    @Mock
    private PropertiesFileReader mockPropertiesFileReader;

    private GuestClaimsHandlerExt handlerExt;

    @Before
    public void setup() throws Exception {
        when(mockPropertiesFileReader.loadSinglePropertiesFile(eq(AVAILABLE_CLAIMS_FILE))).thenReturn(
                ImmutableMap.of(IMMUTABLE_CLAIM_1, GUEST, IMMUTABLE_CLAIM_2, GUEST));

        when(mockPropertiesFileReader.loadPropertiesFilesInDirectory(eq(PROFILES_DIRECTORY))).thenReturn(
                Arrays.asList(ImmutableMap.of(PROFILE_NAME_KEY,
                        PROFILE_A,
                        PROFILE_A,
                        PROFILE_A_VALUE),
                        ImmutableMap.of(PROFILE_NAME_KEY, PROFILE_B, PROFILE_B, PROFILE_B_VALUE),
                        ImmutableMap.of(PROFILE_B, PROFILE_B_VALUE)));

        when(mockPropertiesFileReader.loadPropertiesFilesInDirectory(eq(PROFILES_BANNER_DIRECTORY))).thenReturn(
                Arrays.asList(ImmutableMap.of(PROFILE_NAME_KEY,
                        PROFILE_A,
                        PROFILE_A,
                        PROFILE_A_VALUE),
                        ImmutableMap.of(PROFILE_NAME_KEY, PROFILE_B, PROFILE_B, PROFILE_B_VALUE)));

        handlerExt = new GuestClaimsHandlerExt(mockPropertiesFileReader,
                Arrays.asList(IMMUTABLE_CLAIM_1, IMMUTABLE_CLAIM_2),
                PROFILES_DIRECTORY,
                PROFILES_BANNER_DIRECTORY,
                AVAILABLE_CLAIMS_FILE);

        handlerExt.init();
    }

    @Test
    public void testFieldNullability() throws Exception {
        assertThat(handlerExt.getSelectedClaimsProfileAttributes(), is(nullValue()));
        assertThat(handlerExt.getSelectedClaimsProfileBannerConfigs(), is(nullValue()));
    }

    @Test
    public void testDefaultClaimsProfile() throws Exception {
        handlerExt.setSelectedClaimsProfileName(DEFAULT_NAME);
        testFieldNullability();
    }

    @Test
    public void testSelectClaimsProfileRoundTrip() throws Exception {
        handlerExt.setSelectedClaimsProfileName(PROFILE_A);
        verifyMapValidity(handlerExt.getSelectedClaimsProfileAttributes());
        verifyMapValidity(handlerExt.getSelectedClaimsProfileBannerConfigs());
    }

    @Test
    public void testGetClaims() throws Exception {
        Map<String, Object> claimsMap = handlerExt.getClaims();
        List<String> availableClaims = (List<String>) claimsMap.get(AVAILABLE_CLAIMS);
        List<String> immutableClaims = (List<String>) claimsMap.get(IMMUTABLE_CLAIMS);
        assertThat(availableClaims, hasItems(IMMUTABLE_CLAIM_1, IMMUTABLE_CLAIM_2));
        assertThat(immutableClaims, hasItems(IMMUTABLE_CLAIM_1, IMMUTABLE_CLAIM_2));
    }

    @Test
    public void testGetClaimsProfiles() throws Exception {
        Map<String, Object> claimsProfiles = handlerExt.getClaimsProfiles();
        Map<String, Object> availableProfilesFlattened = (Map<String, Object>) claimsProfiles.get(
                AVAILABLE_PROFILES);

        List<String> profileAdata = (List<String>) availableProfilesFlattened.get(PROFILE_A);
        List<String> profileBdata = (List<String>) availableProfilesFlattened.get(PROFILE_B);

        assertThat(profileAdata.size(), is(1));
        assertThat(profileAdata, hasItems(String.format("%s=%s", PROFILE_A, PROFILE_A_VALUE)));
        assertThat(profileBdata.size(), is(1));
        assertThat(profileBdata, hasItems(String.format("%s=%s", PROFILE_B, PROFILE_B_VALUE)));

        Set<String> profileNames = (Set<String>) claimsProfiles.get(PROFILE_NAMES);
        assertThat(profileNames, hasItems(PROFILE_A, PROFILE_B));
    }

    private void verifyMapValidity(Map inputMap) throws Exception {
        assertThat(inputMap.size(), is(1));
        assertThat(inputMap.get(PROFILE_A), is(PROFILE_A_VALUE));
    }
}
