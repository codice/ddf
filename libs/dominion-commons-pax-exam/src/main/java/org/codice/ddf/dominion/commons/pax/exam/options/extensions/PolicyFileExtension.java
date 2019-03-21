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
import org.apache.commons.io.FilenameUtils;
import org.codice.ddf.dominion.commons.options.DDFCommonOptions.PolicyFile;
import org.codice.dominion.options.SourceType;
import org.codice.dominion.pax.exam.interpolate.PaxExamInterpolator;
import org.codice.dominion.pax.exam.options.KarafDistributionConfigurationFileReplaceOption;
import org.codice.dominion.pax.exam.options.PaxExamOption.Extension;
import org.codice.dominion.resources.ResourceLoader;
import org.ops4j.pax.exam.Option;

/** Defines the extension point for the {@link PolicyFile} extension. */
public class PolicyFileExtension implements Extension<PolicyFile> {
  @Override
  public Option[] options(
      PolicyFile annotation, PaxExamInterpolator interpolator, ResourceLoader resourceLoader)
      throws IOException {
    return new Option[] {
      new KarafDistributionConfigurationFileReplaceOption(
          // separators to Unix is on purpose as PaxExam will analyze the target based on it
          // containing / and not \ and then convert it properly
          FilenameUtils.separatorsToUnix("security/" + annotation.name() + ".policy"),
          SourceType.fromAnnotationToFile(annotation, interpolator, resourceLoader))
    };
  }
}
