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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.PropertiesLoader;

/**
 * GuestClaimsHandlerExt is an extension for the ConfigurationAdminMBean
 * that adds capabilities to read/handle claim and profiles for the
 * guest claims handler.
 */
public class GuestClaimsHandlerExt {
    public static final String PROFILE_NAME_KEY = "profileName";

    private static final Logger LOGGER = LoggerFactory.getLogger(GuestClaimsHandlerExt.class);

    private Map<String, Object> claimsProfiles;

    private List<String> availableClaims;

    //holds names of claims that if modified would cause guest login failures
    private List<String> immutableClaims;

    private String profileDir;

    private String availableClaimsFile;

    public GuestClaimsHandlerExt() {
        immutableClaims = new ArrayList<>();
    }

    /**
     * Load the profiles from disk
     */
    private Map<String, Object> loadClaimsProfiles(String profileDirPath) {
        Map<String, Object> claimsProfilesMap = new HashMap<>();
        if (!StringUtils.isEmpty(profileDirPath)) {
            File dir = new File(profileDirPath);
            if (dir.exists()) {
                File[] propertyFiles = dir.listFiles((dir1, name) -> {
                    return name.endsWith(".properties");
                });
                if (propertyFiles != null) {
                    for (File profileFile : propertyFiles) {
                        Map<String, String> map = loadPropertiesFile(profileFile);
                        String profileName = map.get(PROFILE_NAME_KEY);
                        if (!StringUtils.isEmpty(profileName)) {
                            map.remove(PROFILE_NAME_KEY);
                            claimsProfilesMap.put(profileName, convertMapToList(map));
                        }
                    }
                }
            }
        }
        return claimsProfilesMap;
    }

    private List<String> convertMapToList(Map<String, String> map) {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            list.add(entry.getKey() + "=" + entry.getValue());
        }
        return list;
    }

    private List<String> loadAvailableClaims(String filePath) {
        List<String> claimsList = new ArrayList<>();
        if (!StringUtils.isEmpty(filePath)) {
            File file = new File(filePath);
            if (file.exists()) {
                claimsList.addAll(loadPropertiesFile(file).keySet());
            }
        }
        return claimsList;
    }

    private Map<String, String> loadPropertiesFile(File propFile) {
        Map<String, String> propertyMap = new HashMap<>();
        if (propFile != null && propFile.exists()) {
            Properties properties = PropertiesLoader.loadProperties(propFile.getAbsolutePath());
            propertyMap  = PropertiesLoader.toMap(properties);
        }
        return propertyMap;
    }

    public void setAvailableClaimsFile(String availableClaimsFile) {
        this.availableClaimsFile = availableClaimsFile;
    }

    public void setImmutableClaims(String claims) {
        if (!StringUtils.isEmpty(claims)) {
            immutableClaims.addAll(Arrays.asList(claims.split(",")));
        }
    }

    public void setProfileDir(String profileDir) {
        this.profileDir = profileDir;
    }

    public void init() {
        availableClaims = loadAvailableClaims(availableClaimsFile);
        claimsProfiles = loadClaimsProfiles(profileDir);
    }

    public Map<String, Object> getClaimsProfiles() {
        Map<String, Object> profiles = new HashMap<>();
        profiles.put("availableProfiles", claimsProfiles);
        profiles.put("profileNames", claimsProfiles.keySet());
        return profiles;
    }

    public Map<String, Object> getClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("availableClaims", availableClaims);
        claims.put("immutableClaims", immutableClaims);
        return claims;
    }
}
