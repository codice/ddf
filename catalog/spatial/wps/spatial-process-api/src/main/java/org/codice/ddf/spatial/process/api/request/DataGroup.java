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
package org.codice.ddf.spatial.process.api.request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** This class is Experimental and subject to change */
public class DataGroup implements Data {

  private final String id;

  private final List<Data> data;

  public DataGroup(String id, List<Data> data) {
    this.data = new ArrayList<>(data);
    this.id = id;
  }

  public List<Data> getData() {
    return Collections.unmodifiableList(data);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public DataFormat getFormat() {
    return new DataFormat();
  }
}
