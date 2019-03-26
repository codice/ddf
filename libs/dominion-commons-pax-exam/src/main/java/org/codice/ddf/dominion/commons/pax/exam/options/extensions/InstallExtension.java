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

import org.codice.ddf.dominion.commons.options.DDFCommonOptions.Install;
import org.codice.dominion.options.karaf.KarafOptions;
import org.codice.dominion.pax.exam.interpolate.PaxExamInterpolator;
import org.codice.dominion.pax.exam.options.PaxExamOption.Extension;
import org.codice.dominion.resources.ResourceLoader;
import org.codice.maven.MavenUrl;
import org.ops4j.pax.exam.Option;

/** Defines the extension point for the {@link Install} option. */
@KarafOptions.InstallFeature(
  repository =
      @MavenUrl(
        groupId = "ddf.lib",
        artifactId = "dominion-commons",
        version = MavenUrl.AS_IN_PROJECT,
        type = "xml",
        classifier = "features"
      ),
  name = "ddf-dominion-commons"
)
public class InstallExtension implements Extension<Install> {
  @Override
  public Option[] options(
      Install install, PaxExamInterpolator interpolator, ResourceLoader resourceLoader) {
    return new Option[0];
  }
}
