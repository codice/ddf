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
package org.codice.ddf.dominion.commons.pax.exam.options;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.boon.Boon;
import org.codice.dominion.pax.exam.interpolate.PaxExamInterpolator;
import org.codice.dominion.pax.exam.options.KarafDistributionConfigurationFileContentOption;

/**
 * Provides an extension to PaxExam's KarafDistributionConfigurationFileContentOption which supports
 * adding new claims to the <code>etc/users.attributes</code> file as extracted from the system
 * claims defined by a given security profile.
 */
// check
// /git/ddf/platform/admin/core/admin-core-impl/src/main/java/org/codice/ddf/admin/core/impl/SystemPropertiesAdmin.java
// for help on implementation.
// **** will need to be converted to GSON when moved to master
@SuppressWarnings("squid:MaximumInheritanceDepth" /* cannot control hierarchy for PaxExam */)
public class UsersAttributesFileSystemClaimsOption extends UsersAttributesFileContentOption {
  private static final String PROFILES_JSON = "etc/ws-security/profiles.json";

  private static final String SYSTEM_CLAIMS = "systemClaims";

  private static final String DEFAULT_LOCALHOST_DN = "localhost.local";

  private static final String LOCAL_HOST = "localhost";

  private final String profile;

  /**
   * Creates a new system claims PaxExam option.
   *
   * @param interpolator the interpolator from which to retrieve Karaf directory locations
   * @param profile the security profile to retrieve system claims from or empty if none need to be
   *     installed
   */
  public UsersAttributesFileSystemClaimsOption(PaxExamInterpolator interpolator, String profile) {
    super(interpolator);
    this.profile = profile;
  }

  @Override
  protected void update(Map<String, Map<String, Object>> claims) throws IOException {
    final Map<String, Object> systemClaims = getSystemClaims();

    if (systemClaims != null) {
      final Map<String, Object> localhost =
          claims.get(UsersAttributesFileSystemClaimsOption.LOCAL_HOST);

      if (localhost != null) {
        localhost.putAll(systemClaims);
      }
    }
    for (final Map.Entry<String, Map<String, Object>> e : claims.entrySet()) {
      e.setValue(replaceLocalhost(e.getValue()));
    }
  }

  @Nullable
  private Map<String, Object> getSystemClaims() throws IOException {
    if (StringUtils.isEmpty(profile)) { // nothing to be installed
      return null;
    }
    final File profilesFilePath =
        KarafDistributionConfigurationFileContentOption.toFile(
            interpolator.getKarafEtc(), UsersAttributesFileSystemClaimsOption.PROFILES_JSON);
    final Map<String, Map<String, Map<String, Object>>> profiles;

    try (final InputStream is = new FileInputStream(profilesFilePath)) {
      profiles =
          (Map<String, Map<String, Map<String, Object>>>)
              Boon.fromJson(IOUtils.toString(is, Charset.defaultCharset()));
    } catch (FileNotFoundException e) {
      throw new IllegalArgumentException("security profile '" + profile + "' not found", e);
    }
    final Map<String, Map<String, Object>> profileInfo = profiles.get(profile);

    if (profileInfo == null) {
      throw new IllegalArgumentException(
          "security profile '"
              + profile
              + "' not found in: "
              + UsersAttributesFileSystemClaimsOption.PROFILES_JSON);
    }
    return profileInfo.get(UsersAttributesFileSystemClaimsOption.SYSTEM_CLAIMS);
  }

  private Map<String, Object> replaceLocalhost(Map<String, Object> map) {
    for (final Map.Entry<String, Object> e : map.entrySet()) {
      final Object val = e.getValue();

      if (!(val instanceof String)) {
        continue;
      }
      final String sval = (String) val;

      if (sval.contains(UsersAttributesFileSystemClaimsOption.DEFAULT_LOCALHOST_DN)) {
        e.setValue(
            sval.replace(
                UsersAttributesFileSystemClaimsOption.DEFAULT_LOCALHOST_DN,
                UsersAttributesFileSystemClaimsOption.LOCAL_HOST));
      }
    }
    return map;
  }

  @Override
  public String toString() {
    return UsersAttributesFileSystemClaimsOption.class.getSimpleName()
        + "{profile="
        + profile
        + "}";
  }
}
