/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.admin.core.impl;

import static org.codice.gsonsupport.GsonTypeAdapters.MAP_STRING_TO_OBJECT_TYPE;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.codice.ddf.platform.util.properties.PropertiesFileReader;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GuestClaimsHandlerExt is an extension for the AdminConsoleServiceMBean that adds capabilities to
 * read/handle claim and profiles for the guest claims handler. The state in the class mirrors the
 * loaded files exactly, and the data is transformed as it leaves the class to whatever format is
 * expected.
 *
 * <p>The governing data structure lives in {@code ~/etc/ws-security/profiles.json} where the
 * outter-most map represents the profile, and each profile must have inner-maps {@code
 * guestClaims}, {@code systemClaims}, and {@code configs}. Each have a respective destination:
 * guest claims are sent to the guest claims handler using the appropriate configuration, system
 * claims are written to {@code ~/etc/users.attributes}, and configs allow arbitrary configuration
 * info to be submitted to {@code ConfigurationAdmin}.
 *
 * <p>An example use case for the configs entity: setting UI banners based upon the selected
 * profile.
 */
public class GuestClaimsHandlerExt {

  private static final Logger LOGGER = LoggerFactory.getLogger(GuestClaimsHandlerExt.class);

  private static final String GUEST_CLAIMS = "guestClaims";

  private static final String SYSTEM_CLAIMS = "systemClaims";

  private static final String CONFIGS = "configs";

  public static final String PID_KEY = "pid";

  public static final String PROPERTIES_KEY = "properties";

  public static final String AVAILABLE_PROFILES = "availableProfiles";

  public static final String PROFILE_NAMES = "profileNames";

  public static final String AVAILABLE_CLAIMS = "availableClaims";

  public static final String IMMUTABLE_CLAIMS = "immutableClaims";

  public static final String DEFAULT_NAME = "Default";

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .create();

  private Map<String, String> availableClaimsMap;

  private final PropertiesFileReader propertiesFileReader;

  // ** Holds names of claims that if modified would cause guest login failures **
  private final List<String> immutableClaims;

  private final String availableClaimsFile;

  private String selectedClaimsProfileName;

  private final String profilesFilePath;

  private Map<String, Object> profiles;

  /** Constructor. */
  public GuestClaimsHandlerExt(
      PropertiesFileReader propertiesFileReader,
      List<String> immutableClaims,
      String availableClaimsFile,
      String profilesFilePath) {

    this.propertiesFileReader = propertiesFileReader;
    this.immutableClaims = ImmutableList.copyOf(immutableClaims);
    this.availableClaimsFile = availableClaimsFile;
    this.profilesFilePath = profilesFilePath;

    this.selectedClaimsProfileName = null;
    this.profiles = null;
  }

  /** Called by the container to initialize the object. */
  public void init() {
    try (InputStream inputStream = new FileInputStream(profilesFilePath)) {
      this.profiles = GSON.fromJson(new InputStreamReader(inputStream), MAP_STRING_TO_OBJECT_TYPE);
    } catch (IOException e) {
      LOGGER.debug("Could not find profiles.json during installation: ", e);
      this.profiles = new HashMap<>();
    }

    availableClaimsMap = propertiesFileReader.loadSinglePropertiesFile(availableClaimsFile);
  }

  /**
   * Submit the name of the profile listed in each configuration to select that configuration for
   * use. Applies retroactively for system-high and banner markings.
   *
   * @param selectedClaimsProfileName the name of the profile to be used.
   */
  public void setSelectedClaimsProfileName(String selectedClaimsProfileName) {
    if (!profiles.containsKey(selectedClaimsProfileName)
        && !selectedClaimsProfileName.equals(DEFAULT_NAME)) {
      throw new IllegalArgumentException("Invalid guest claims profile specified");
    }
    this.selectedClaimsProfileName = selectedClaimsProfileName;
  }

  /**
   * Returns a map of guest claims that should be applied to all anonymous users, or {@code null} if
   * no profile has been selected.
   *
   * @return a map of claims for all guests.
   */
  @Nullable
  public Map<String, Object> getProfileGuestClaims() {
    return (Map<String, Object>) getProfileAttributes(GUEST_CLAIMS);
  }

  /**
   * Returns a map of system claims that should be written to users.attributes upon installation, or
   * {@code null} if no profile has been selected.
   *
   * @return a map of claims for the system user only.
   * @see org.codice.ddf.admin.core.impl.SystemPropertiesAdmin
   */
  @Nullable
  public Map<String, Object> getProfileSystemClaims() {
    return (Map<String, Object>) getProfileAttributes(SYSTEM_CLAIMS);
  }

  /**
   * Returns a list of maps representing misc configs that should be sent to {@link
   * ConfigurationAdminImpl} when initializing the selected security profile, or {@code null} if no
   * profile has been selected.
   *
   * @return a list of maps with string field "pid" and value field "properties", which are the
   *     actual configuration properties to be submitted to the config with the corresponding pid.
   */
  @Nullable
  public List<Map<String, Object>> getProfileConfigs() {
    return (List<Map<String, Object>>) getProfileAttributes(CONFIGS);
  }

  /**
   * Get a map of the claims profiles data. The resulting claims are flattened.
   *
   * @return a map with a list of profile names and the actual profile information.
   * @see #flatCopyProfileData(Map)
   */
  public Map<String, Object> getClaimsProfiles() {
    Map<String, Object> claimsProfiles = new HashMap<>();
    Map<String, Object> flatClaims = flatCopyProfileData(profiles);
    claimsProfiles.put(AVAILABLE_PROFILES, flatClaims);
    claimsProfiles.put(PROFILE_NAMES, flatClaims.keySet());
    return claimsProfiles;
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
   * Helper method for extracting a profiles.json sub-component.
   *
   * @param attributesIdentifier name of the sub-structure to return from profiles.json
   *     (guestClaims, systemClaims, or configs are valid values).
   * @return the given attribute component for the selected profile, or null if no profile was
   *     selected.
   */
  @Nullable
  private Object getProfileAttributes(String attributesIdentifier) {
    if (selectedClaimsProfileName != null && !selectedClaimsProfileName.equals(DEFAULT_NAME)) {
      Map<String, Object> profile = (Map<String, Object>) profiles.get(selectedClaimsProfileName);
      return profile.get(attributesIdentifier);
    }
    return null;
  }

  /** Transform a map into a new list of its keys. */
  private List<String> createListOfKeysFromMap(Map<String, String> map) {
    return map.keySet().stream().collect(Collectors.toList());
  }

  /** Convert the structure found in profiles.json to simple claims listings. */
  private Map<String, Object> flatCopyProfileData(Map<String, Object> profiles) {
    return profiles.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, entry -> convertGuestClaimsToList(entry.getValue())));
  }

  /** Support for responses that expect the claims format in key=value strings. */
  private List<String> convertGuestClaimsToList(Object profile) {
    Map<String, Object> innerMap = (Map<String, Object>) profile;
    Map<String, String> guestClaims = (Map<String, String>) innerMap.get(GUEST_CLAIMS);
    return guestClaims.entrySet().stream()
        .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }
}
