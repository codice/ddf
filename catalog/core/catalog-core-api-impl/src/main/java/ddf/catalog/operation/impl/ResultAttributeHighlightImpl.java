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
package ddf.catalog.operation.impl;

import ddf.catalog.operation.ResultAttributeHighlight;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResultAttributeHighlightImpl implements ResultAttributeHighlight {

  private String attributeName;

  private List<String> highlights;

  public ResultAttributeHighlightImpl(String attributeName) {
    this.attributeName = attributeName;
    highlights = new ArrayList<>();
  }

  public ResultAttributeHighlightImpl(String attributeName, List<String> highlights) {
    this(attributeName);
    this.highlights.addAll(highlights);
  }

  @Override
  public String getAttributeName() {
    return attributeName;
  }

  @Override
  public List<String> getHighlights() {
    return Collections.unmodifiableList(highlights);
  }
}
