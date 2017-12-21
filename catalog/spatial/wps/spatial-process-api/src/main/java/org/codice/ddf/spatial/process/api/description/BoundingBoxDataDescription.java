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
package org.codice.ddf.spatial.process.api.description;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** This class is Experimental and subject to change */
public class BoundingBoxDataDescription extends AbstractDataDescription {
  private List<String> supportedCoordRefSys;

  public BoundingBoxDataDescription(
      String id, String name, String description, List<String> supportedCoordRefSys) {
    super(id, name, description);
    this.supportedCoordRefSys = new ArrayList<>(supportedCoordRefSys);
  }

  public List<String> getSupportedCoordRefSys() {
    return Collections.unmodifiableList(supportedCoordRefSys);
  }
}
