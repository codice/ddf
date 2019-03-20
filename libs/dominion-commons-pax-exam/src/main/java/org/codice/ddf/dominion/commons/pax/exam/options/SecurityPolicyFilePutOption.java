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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.codice.dominion.options.Permission;
import org.codice.dominion.pax.exam.interpolate.PaxExamInterpolator;
import org.codice.dominion.pax.exam.options.KarafDistributionConfigurationFileContentOption;

/**
 * Provides an extension to PaxExam's KarafDistributionConfigurationFileContentOption which supports
 * updating policies in the <code>security/dominion.policy</code> file.
 */
@SuppressWarnings("squid:MaximumInheritanceDepth" /* cannot control hierarchy for PaxExam */)
public class SecurityPolicyFilePutOption extends KarafDistributionConfigurationFileContentOption {
  private static final String DOMINION_POLICY = "security/dominion.policy";

  private final Map<String, List<Permission>> grants = new HashMap<>();

  /**
   * Creates a new dominion.policy content PaxExam option.
   *
   * @param interpolator the interpolator from which to retrieve Karaf directory locations
   */
  public SecurityPolicyFilePutOption(PaxExamInterpolator interpolator) {
    super(interpolator, SecurityPolicyFilePutOption.DOMINION_POLICY);
  }

  /**
   * Adds permissions to be granted for the specified codebase.
   *
   * @param codebase the codebase to grant the permission to
   * @param permissions the permissions to be granted
   * @return this for chaining
   * @throws IllegalArgumentException if one of the permissions is not properly defined
   */
  public SecurityPolicyFilePutOption addPermissions(String codebase, Permission... permissions) {
    final List<Permission> list = grants.computeIfAbsent(codebase, cb -> new ArrayList<>());

    Stream.of(permissions).peek(SecurityPolicyFilePutOption::validate).forEach(list::add);
    return this;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{grants=" + grants + "}";
  }

  @Override
  protected File getSource(File original) throws Exception {
    final Map<String, Map<String, Object>> json;
    final String lineSeparator = interpolator.getLineSeparator();
    File tmp = null;

    try {
      tmp = Files.createTempFile(SecurityPolicyFilePutOption.class.getName(), ".tmp").toFile();
      tmp.deleteOnExit();
      final InputStream initial;

      if (original.exists()) {
        initial = new FileInputStream(original);
      } else {
        initial =
            new ByteArrayInputStream(("priority \"grant\";" + lineSeparator).getBytes("UTF-8"));
      }
      FileUtils.copyInputStreamToFile(
          new SequenceInputStream(
              initial, new ByteArrayInputStream(getGrantInfo(lineSeparator).getBytes("UTF-8"))),
          tmp);
    } catch (IOException e) {
      FileUtils.deleteQuietly(tmp);
      throw e;
    }
    return tmp;
  }

  private String getGrantInfo(String lineSeparator) {
    final StringBuilder sb = new StringBuilder();

    for (final Map.Entry<String, List<Permission>> e : grants.entrySet()) {
      final String codebase = e.getKey();
      final List<Permission> permissions = e.getValue();

      sb.append(lineSeparator)
          .append("grant codeBase \"")
          .append(codebase)
          .append("\" {")
          .append(lineSeparator);
      for (final Permission p : permissions) {
        sb.append("    permission ");
        if (!p.clazz().equals(java.security.Permission.class)) {
          sb.append(p.clazz().getName());
        } else {
          sb.append(p.type());
        }
        sb.append(" \"").append(p.name()).append('"');
        if (!p.actions().isEmpty()) {
          sb.append(", \"").append(p.actions()).append("\";");
        } else {
          sb.append(';');
        }
        sb.append(lineSeparator);
      }
      sb.append("}").append(lineSeparator);
    }
    return sb.toString();
  }

  private static void validate(Permission permission) {
    if (!permission.type().isEmpty()) {
      if (!permission.clazz().equals(java.security.Permission.class)) {
        throw new IllegalArgumentException(
            "specify only one of type() or clazz() for " + permission);
      }
    } else if (permission.clazz().equals(java.security.Permission.class)) {
      throw new IllegalArgumentException("must specify one of type() or clazz() for " + permission);
    }
  }
}
