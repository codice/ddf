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
package ddf.security.claims.impl;

import ddf.security.claims.Claim;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClaimImpl implements Claim {

  private String name;

  private ArrayList<String> values;

  public ClaimImpl(String name) {
    this.name = name;
    this.values = new ArrayList<>();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<String> getValues() {
    return Collections.unmodifiableList(values);
  }

  @Override
  public void addValue(String value) {
    values.add(value);
  }
}
