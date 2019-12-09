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
package ddf.security.assertion.impl;

import ddf.security.assertion.Attribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AttributeDefault implements Attribute {
  private String name;

  private String nameFormat;

  private Set<String> values = new HashSet<>();

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getNameFormat() {
    return nameFormat;
  }

  @Override
  public void setNameFormat(String nameFormat) {
    this.nameFormat = nameFormat;
  }

  @Override
  public List<String> getValues() {
    return Collections.unmodifiableList(new ArrayList<>(values));
  }

  @Override
  public void setValues(List<String> values) {
    this.values.clear();
    this.values.addAll(values);
  }

  @Override
  public void addValue(String value) {
    this.values.add(value);
  }
}
