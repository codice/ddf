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
package org.codice.ddf.spatial.kml.actions;

import ddf.catalog.data.Metacard;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.rest.impl.action.MetacardTransformerActionProvider;

public class KmlActionProvider extends MetacardTransformerActionProvider {
  /**
   * Constructor that accepts the values to be used when a new {@link Action} is created by this
   * {@link ddf.action.ActionProvider}.
   *
   * @param actionProviderId ID that will be assigned to the {@link Action} that will be created.
   *     Cannot be empty or blank.
   */
  public KmlActionProvider(String actionProviderId, String title) {
    super(actionProviderId, title);
  }

  @Override
  protected boolean canHandleMetacard(Metacard metacard) {
    return !StringUtils.isEmpty(metacard.getLocation());
  }
}
