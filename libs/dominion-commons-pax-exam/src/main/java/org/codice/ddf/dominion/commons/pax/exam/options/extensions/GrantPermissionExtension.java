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
package org.codice.ddf.dominion.commons.pax.exam.options.extensions;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.codice.ddf.dominion.commons.pax.exam.options.SecurityPolicyFilePutOption;
import org.codice.dominion.options.OptionException;
import org.codice.dominion.options.Options.GrantPermission;
import org.codice.dominion.pax.exam.interpolate.PaxExamInterpolator;
import org.codice.dominion.pax.exam.options.PaxExamOption.Extension;
import org.codice.dominion.resources.ResourceLoader;
import org.codice.maven.MavenUrlReference;
import org.codice.maven.Utilities;
import org.ops4j.pax.exam.Option;

/** Extension point for Dominion's {@link GrantPermission} option annotation. */
public class GrantPermissionExtension implements Extension<GrantPermission> {
  @Override
  public Option[] options(
      GrantPermission annotation, PaxExamInterpolator interpolator, ResourceLoader resourceLoader) {
    String codebase =
        Stream.concat(
                Stream.of(annotation.codebase())
                    .filter(c -> org.codice.dominion.options.Utilities.isDefined(c)),
                Stream.of(annotation.artifact())
                    .filter(a -> org.codice.dominion.options.Utilities.isDefined(a.groupId()))
                    .map(a -> MavenUrlReference.resolve(a, annotation, resourceLoader))
                    .map(GrantPermissionExtension::getCodebase))
            .collect(Collectors.joining("/"));

    if (codebase.isEmpty()) {
      final Properties dependencies = Utilities.getDependencies(annotation, resourceLoader, null);
      final MavenUrlReference projectUrl =
          Utilities.getProjectUrl(annotation, resourceLoader, dependencies);

      codebase = GrantPermissionExtension.getCodebase(projectUrl);
    }
    return new Option[] {
      new SecurityPolicyFilePutOption(interpolator)
          .addPermissions("file:/" + codebase, annotation.permission())
    };
  }

  private static String getCodebase(MavenUrlReference url) {
    try (final JarInputStream jar = new JarInputStream(new URL(url.getURL()).openStream())) {
      final Manifest mf = jar.getManifest();

      final Attributes attributes = mf.getMainAttributes();
      final String codebase = attributes.getValue("Bundle-SymbolicName");

      // fallback to artifact id if unable to find the bundle symbolic name
      // this is a convention in DDF right now
      return (codebase != null) ? codebase : url.getArtifactId();
    } catch (IOException e) {
      throw new OptionException("unable to retrieve bundle symbolic name for: " + url, e);
    }
  }
}
