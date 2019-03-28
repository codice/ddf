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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.File;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.codice.dominion.pax.exam.interpolate.PaxExamInterpolator;
import org.codice.dominion.pax.exam.options.KarafDistributionConfigurationFileContentOption;

/**
 * Provides an extension to PaxExam's KarafDistributionConfigurationFileContentOption which supports
 * updating claims to the <code>etc/users.attributes</code> file.
 */
// check
// /git/ddf/platform/admin/core/admin-core-impl/src/main/java/org/codice/ddf/admin/core/impl/SystemPropertiesAdmin.java
// for help on implementation.
// **** will need to be converted to GSON when moved to master
public abstract class UsersAttributesFileContentOption
    extends KarafDistributionConfigurationFileContentOption {
  private static final String USERS_ATTRIBUTES = "etc/users.attributes";

  protected static final Gson GSON = new Gson();

  /**
   * Creates a new users.attributes content PaxExam option.
   *
   * @param interpolator the interpolator from which to retrieve Karaf directory locations
   */
  public UsersAttributesFileContentOption(PaxExamInterpolator interpolator) {
    super(interpolator, UsersAttributesFileContentOption.USERS_ATTRIBUTES);
  }

  @Override
  protected File getSource(File original) throws Exception {
    final Map<String, Map<String, Object>> json;

    if (original.exists()) {
      try (final BufferedReader stream = Files.newBufferedReader(Paths.get(original.toURI()))) {
        Type collectionType = new TypeToken<Map<String, Object>>() {}.getType();
        json = GSON.fromJson(stream, collectionType);
      }
    } else {
      json = new HashMap<>();
    }
    update(json);
    final File temp =
        Files.createTempFile(UsersAttributesFileContentOption.class.getName(), ".tmp").toFile();

    temp.deleteOnExit();
    FileUtils.writeStringToFile(temp, GSON.toJson(json), Charset.defaultCharset());
    return temp;
  }

  /**
   * Called to update the current set of claims keyed by either user ids or external system regular
   * expressions.
   *
   * <p>The updated map will be written back to the <code>etc/users.attributes</code> file.
   *
   * @param claims the current set of claims
   * @throws Exception if a failure occurs while updating the claims
   */
  @SuppressWarnings("squid:S00112" /* intended to allow subclasses to throw anything out */)
  protected abstract void update(Map<String, Map<String, Object>> claims) throws Exception;
}
