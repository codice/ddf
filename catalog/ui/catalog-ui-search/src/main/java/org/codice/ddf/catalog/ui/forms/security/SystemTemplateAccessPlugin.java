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
package org.codice.ddf.catalog.ui.forms.security;

import static org.codice.ddf.catalog.ui.security.Constants.SYSTEM_TEMPLATE;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.StopProcessingException;
import java.util.Map;

public class SystemTemplateAccessPlugin extends AbstractAccessPlugin {

  /**
   * Prevents two illegal operations from occuring across all endpoints:
   *
   * <ol>
   *   <li>ANY system template metacard is NOT ALLOWED to be the target of an update request
   *   <li>ANY metacard is NOT ALLOWED to be updated such that it BECOMES a system template metacard
   * </ol>
   *
   * @param input the {@link UpdateRequest} to permit if all conditions are met; contains the new
   *     metacard states.
   * @param existingMetacards the old metacard states.
   * @return the {@link UpdateRequest} if the operation is permitted.
   * @throws StopProcessingException if one or more of the above two conditions were detected.
   */
  @Override
  public UpdateRequest processPreUpdate(
      UpdateRequest input, Map<String, Metacard> existingMetacards) throws StopProcessingException {
    if (existingMetacards
        .values()
        .stream()
        .map(Metacard::getTags)
        .anyMatch(tags -> tags.contains(SYSTEM_TEMPLATE))) {
      throw new StopProcessingException("Cannot update system template metacards");
    }
    if (input
        .getUpdates()
        .stream()
        .map(Map.Entry::getValue)
        .map(Metacard::getTags)
        .anyMatch(tags -> tags.contains(SYSTEM_TEMPLATE))) {
      throw new StopProcessingException(
          "Cannot coerce existing metacard to be a system template metacard");
    }
    return input;
  }
}
