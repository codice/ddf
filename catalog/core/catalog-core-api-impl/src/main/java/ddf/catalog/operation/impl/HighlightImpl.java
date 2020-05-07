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

import ddf.catalog.operation.Highlight;
import java.util.List;

public class HighlightImpl implements Highlight {

  private int valueIndex;

  private int beginIndex;

  private int endIndex;

  public HighlightImpl() {}

  public HighlightImpl(int beginIndex, int endIndex) {
    this(beginIndex, endIndex, 0);
  }

  public HighlightImpl(int beginIndex, int endIndex, int valueIndex) {
    this.valueIndex = valueIndex;
    this.beginIndex = beginIndex;
    this.endIndex = endIndex;
  }

  @Override
  public int getValueIndex() {
    return valueIndex;
  }

  @Override
  public int getBeginIndex() {
    return beginIndex;
  }

  @Override
  public int getEndIndex() {
    return endIndex;
  }

  public void setValueIndex(int valueIndex) {
    this.valueIndex = valueIndex;
  }

  public void setBeginIndex(int beginIndex) {
    this.beginIndex = beginIndex;
  }

  public void setEndIndex(int endIndex) {
    this.endIndex = endIndex;
  }

  public String apply(String input, String beginTag, String endTag) {
    StringBuilder builder = new StringBuilder(input);
    builder.insert(endIndex, endTag);
    builder.insert(beginIndex, beginTag);
    return builder.toString();
  }

  public void apply(List<String> values, String beginTag, String endTag) {
    if (valueIndex < values.size()) {
      String highlighted = apply(values.get(valueIndex), beginTag, endTag);
      values.remove(valueIndex);
      values.add(valueIndex, highlighted);
    }
  }
}
