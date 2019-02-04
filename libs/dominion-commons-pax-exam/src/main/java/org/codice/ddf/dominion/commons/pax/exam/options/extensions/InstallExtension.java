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

import org.codice.ddf.dominion.commons.options.CommonsOptions.Install;
import org.codice.dominion.options.Options.MavenUrl;
import org.codice.dominion.options.karaf.KarafOptions;
import org.codice.dominion.pax.exam.options.PaxExamOption.Extension;
import org.codice.dominion.resources.ResourceLoader;
import org.ops4j.pax.exam.Option;

@KarafOptions.Feature(
  repository =
      @MavenUrl(
        groupId = "ddf.lib",
        artifactId = "dominion-commons",
        version = MavenUrl.AS_IN_PROJECT,
        type = "xml",
        classifier = "features"
      ),
  names = "dominion-commons"
)
public class InstallExtension implements Extension<Install> {
  @Override
  public Option[] options(Install install, Class<?> aClass, ResourceLoader resourceLoader)
      throws Throwable {
    return new Option[0];
  }
}
