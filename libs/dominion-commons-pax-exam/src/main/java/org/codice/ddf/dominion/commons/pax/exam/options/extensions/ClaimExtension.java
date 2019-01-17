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

import org.codice.ddf.dominion.commons.options.CommonOptions.Claim;
import org.codice.ddf.dominion.commons.pax.exam.options.UsersAttributesFileClaimPutOption;
import org.codice.dominion.options.Utilities;
import org.codice.dominion.pax.exam.interpolate.PaxExamInterpolator;
import org.codice.dominion.pax.exam.options.PaxExamOption.Extension;
import org.codice.dominion.resources.ResourceLoader;
import org.ops4j.pax.exam.Option;

/** Defines the extension point for the {@link Claim} extension. */
public class ClaimExtension implements Extension<Claim> {
  @Override
  public Option[] options(
      Claim annotation, PaxExamInterpolator interpolator, ResourceLoader resourceLoader) {
    final String userId = annotation.userId();
    final String system = annotation.system();
    final boolean userIdDefined = Utilities.isDefined(userId);
    final boolean systemDefined = Utilities.isDefined(system);
    final String id;

    if (userIdDefined) {
      if (systemDefined) {
        throw new IllegalArgumentException(
            "specify only one of userId() or system() in "
                + annotation
                + " for "
                + resourceLoader.getLocationClass().getName());
      }
      id = userId;
    } else if (!systemDefined) {
      throw new IllegalArgumentException(
          "must specify one of userId() or system() in "
              + annotation
              + " for "
              + resourceLoader.getLocationClass().getName());
    } else {
      id = system;
    }
    return new Option[] {
      new UsersAttributesFileClaimPutOption(interpolator, id)
          .addClaim(annotation.name(), annotation.value())
    };
  }
}
