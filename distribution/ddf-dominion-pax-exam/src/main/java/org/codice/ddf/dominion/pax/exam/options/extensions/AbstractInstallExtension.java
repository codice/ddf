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
package org.codice.ddf.dominion.pax.exam.options.extensions;

import org.apache.commons.io.FilenameUtils;
import org.codice.dominion.options.Options.MavenUrl;
import org.codice.dominion.options.karaf.KarafOptions;
import org.codice.dominion.pax.exam.options.PaxExamOption.Extension;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;

/**
 * Base class for DDF installation extensions.
 *
 * @param <A> The type of annotation this extension is used with
 */
@KarafOptions.Feature(
  repository =
      @MavenUrl(
        groupId = "org.codice.ddf",
        artifactId = "ddf-dominion",
        version = MavenUrl.AS_IN_PROJECT,
        type = "xml",
        classifier = "features"
      ),
  names = "ddf-dominion"
)
public abstract class AbstractInstallExtension<A extends java.lang.annotation.Annotation>
    implements Extension<A> {
  private static final String SYSTEM_PROPERTIES_FILE_PATH = "etc/custom.system.properties";

  /**
   * Gets a PaxExam option capable of starting Solr based on the provided flag.
   *
   * @param solr <code>true</code> to have Solr started along with DDF; <code>false</code> not to
   *     have it started
   * @return a corresponding PaxExam option
   */
  public Option solrOption(boolean solr) {
    return KarafDistributionOption.editConfigurationFilePut(
        FilenameUtils.separatorsToSystem(AbstractInstallExtension.SYSTEM_PROPERTIES_FILE_PATH),
        "start.solr",
        Boolean.toString(solr));
  }
}
