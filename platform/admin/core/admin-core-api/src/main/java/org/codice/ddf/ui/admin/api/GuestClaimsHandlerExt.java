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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.codice.ddf.ui.admin.api.util.PropertiesFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * GuestClaimsHandlerExt is an extension for the ConfigurationAdminMBean that adds capabilities to
 * read/handle claim and profiles for the guest claims handler. The state in the class mirrors the
 * loaded properties files exactly, and the data is transformed as it leaves the class to whatever
 * format is expected.
 */
public class GuestClaimsHandlerExt {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuestClaimsHandlerExt.class);

    public static final String PROFILE_NAME_KEY = "profileName";

    public static final String AVAILABLE_PROFILES = "availableProfiles";

    public static final String PROFILE_NAMES = "profileNames";

    public static final String AVAILABLE_CLAIMS = "availableClaims";

    public static final String IMMUTABLE_CLAIMS = "immutableClaims";

    public static final String DEFAULT_NAME = "Default";

    private Map<String, String> availableClaimsMap;

    private List<Map<String, String>> profileMapsRaw;

    private Map<String, Object> profilesProcessed;

    private Map<String, Object> profileBannersProcessed;

    private final PropertiesFileReader propertiesFileReader;

    // ** Holds names of claims that if modified would cause guest login failures **
    private final List<String> immutableClaims;

    private final String profilesDirectory;

    private final String profilesBannerDirectory;

    private final String availableClaimsFile;

    private String selectedClaimsProfileName;

    /**
     * Constructor.
     */
    public GuestClaimsHandlerExt(PropertiesFileReader propertiesFileReader,
            List<String> immutableClaims, String profilesDirectory, String profilesBannerDirectory,
            String availableClaimsFile) {

        this.propertiesFileReader = propertiesFileReader;
        this.immutableClaims = ImmutableList.copyOf(immutableClaims);
        this.profilesDirectory = profilesDirectory;
        this.profilesBannerDirectory = profilesBannerDirectory;
        this.availableClaimsFile = availableClaimsFile;

        selectedClaimsProfileName = null;
    }

    /**
     * Called by the container to initialize the object.
     */
    public void init() {
        Function<String, Consumer<Map<String, String>>> loggingConsumerFactory =
                directory -> map -> {
                    if (!map.containsKey(PROFILE_NAME_KEY)) {
                        LOGGER.debug(
                                "Ignoring properties file without a profile name in directory {}",
                                directory);
                    }
                };

        availableClaimsMap = propertiesFileReader.loadSinglePropertiesFile(availableClaimsFile);

        profileMapsRaw = propertiesFileReader.loadPropertiesFilesInDirectory(profilesDirectory)
                .stream()
                .peek(loggingConsumerFactory.apply(profilesDirectory))
                .filter(map -> map.containsKey(PROFILE_NAME_KEY))
                .collect(Collectors.toList());

        profilesProcessed = extractProfileNamePreserveResult(profileMapsRaw);

        List<Map<String, String>> profileBannerMapsRaw =
                propertiesFileReader.loadPropertiesFilesInDirectory(profilesBannerDirectory)
                        .stream()
                        .peek(loggingConsumerFactory.apply(profilesBannerDirectory))
                        .filter(map -> map.containsKey(PROFILE_NAME_KEY))
                        .collect(Collectors.toList());

        profileBannersProcessed = extractProfileNamePreserveResult(profileBannerMapsRaw);
    }

    /**
     * Submit the name of the profile listed in each configuration to select that configuration
     * for use. Applies retroactively for system-high and banner markings.
     *
     * @param selectedClaimsProfileName the name of the profile to be used.
     */
    public void setSelectedClaimsProfileName(String selectedClaimsProfileName) {
        if (!profilesProcessed.containsKey(selectedClaimsProfileName)
                && !selectedClaimsProfileName.equals(DEFAULT_NAME)) {
            throw new IllegalArgumentException("Invalid guest claims profile specified");
        }
        this.selectedClaimsProfileName = selectedClaimsProfileName;
    }

    /**
     * Returns a map of claims configurations for the selected profile, or {@code null} if no
     * profile has been selected.
     *
     * @return a map of claims configurations and attributes for writing to {@code users.attributes}.
     */
    @Nullable
    public Map<String, Object> getSelectedClaimsProfileAttributes() {
        if (selectedClaimsProfileName != null && !selectedClaimsProfileName.equals(DEFAULT_NAME)) {
            Object attributeListObject = profilesProcessed.get(selectedClaimsProfileName);
            return ((Map<String, Object>) attributeListObject);
        }
        return null;
    }

    /**
     * Returns a map of banner configurations for the selected profile, or {@code null} if no
     * profile has been selected.
     *
     * @return a map of banner configurations ready for submission to {@link ConfigurationAdmin}.
     */
    @Nullable
    public Map<String, Object> getSelectedClaimsProfileBannerConfigs() {
        if (selectedClaimsProfileName != null && !selectedClaimsProfileName.equals(DEFAULT_NAME)) {
            Object bannerConfigsObject = profileBannersProcessed.get(selectedClaimsProfileName);
            return ((Map<String, Object>) bannerConfigsObject);
        }
        return null;
    }

    /**
     * Get a map of the claims profiles data. The resulting claims are flattened.
     *
     * @return a map with a list of profile names and the actual profile information.
     * @see #extractProfileNameFlattenResult(List)
     */
    public Map<String, Object> getClaimsProfiles() {
        Map<String, Object> claimsProfiles = extractProfileNameFlattenResult(profileMapsRaw);
        Map<String, Object> profiles = new HashMap<>();
        profiles.put(AVAILABLE_PROFILES, claimsProfiles);
        profiles.put(PROFILE_NAMES, claimsProfiles.keySet());
        return profiles;
    }

    /**
     * Get a map of both the available and immutable claims.
     *
     * @return a map with a list of available claims and immutable claims.
     */
    public Map<String, Object> getClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put(AVAILABLE_CLAIMS, createListOfKeysFromMap(availableClaimsMap));
        claims.put(IMMUTABLE_CLAIMS, immutableClaims);
        return claims;
    }

    /**
     * Transform a map into a list of its keys.
     */
    private List<String> createListOfKeysFromMap(Map<String, String> map) {
        return map.keySet()
                .stream()
                .collect(Collectors.toList());
    }

    /**
     * Transform the raw list of config data into a consolidated map of lists where each string in
     * the list is a claim in the form 'key=value'.
     */
    private Map<String, Object> extractProfileNameFlattenResult(List<Map<String, String>> maps) {
        List<Map<String, String>> mapsCopy = new ArrayList<>();
        maps.forEach(map -> mapsCopy.add(new HashMap<>(map)));
        return mapsCopy.stream()
                .collect(Collectors.toMap(map -> map.remove(PROFILE_NAME_KEY), this::asList));
    }

    /**
     * Transform the raw list of config data into a consolidated map of maps for easier manipulation.
     */
    private Map<String, Object> extractProfileNamePreserveResult(List<Map<String, String>> maps) {
        List<Map<String, String>> mapsCopy = new ArrayList<>();
        maps.forEach(map -> mapsCopy.add(new HashMap<>(map)));
        return mapsCopy.stream()
                .collect(Collectors.toMap(map -> map.remove(PROFILE_NAME_KEY),
                        Function.identity()));
    }

    /**
     * Support for responses that expect the claims format in key=value strings.
     */
    private List<String> asList(Map<String, String> map) {
        return map.entrySet()
                .stream()
                .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
}
