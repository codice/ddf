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
package org.codice.ddf.spatial.ogc.csw.catalog.actions;

import ddf.catalog.data.Metacard;
import java.io.Serializable;
import java.util.Map;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;

public interface UpdateAction {

  Metacard getMetacard();

  QueryConstraintType getConstraint();

  Map<String, Serializable> getRecordProperties();

  String getTypeName();

  String getHandle();
}
