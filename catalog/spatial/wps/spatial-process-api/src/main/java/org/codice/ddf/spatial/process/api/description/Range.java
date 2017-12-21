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

import javax.annotation.Nullable;

/** This class is Experimental and subject to change */
public class Range {

  private String maximumValue;

  private String minimumValue;

  private String spacing;

  private RangeClosure closure = RangeClosure.OPEN;

  @Nullable
  public String getMaximumValue() {
    return maximumValue;
  }

  public void setMaximumValue(String maximumValue) {
    this.maximumValue = maximumValue;
  }

  public Range maximumValue(String maximumValue) {
    this.maximumValue = maximumValue;
    return this;
  }

  @Nullable
  public String getMinimumValue() {
    return minimumValue;
  }

  public void setMinimumValue(String minimumValue) {
    this.minimumValue = minimumValue;
  }

  public Range minimumValue(String minimumValue) {
    this.minimumValue = minimumValue;
    return this;
  }

  @Nullable
  public String getSpacing() {
    return spacing;
  }

  public void setSpacing(String spacing) {
    this.spacing = spacing;
  }

  public Range spacing(String spacing) {
    this.spacing = spacing;
    return this;
  }

  public RangeClosure getClosure() {
    return closure;
  }

  public void setClosure(RangeClosure closure) {
    this.closure = closure;
  }

  public Range closure(RangeClosure closure) {
    this.closure = closure;
    return this;
  }
}
