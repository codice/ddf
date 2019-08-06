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
import ddf.security.assertion.AttributeStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AttributeStatementDefault implements AttributeStatement {
  private List<Attribute> attributes = new ArrayList<>();

  @Override
  public List<Attribute> getAttributes() {
    return Collections.unmodifiableList(attributes);
  }

  @Override
  public void addAttribute(Attribute attribute) {
    this.attributes.add(attribute);
  }
}
