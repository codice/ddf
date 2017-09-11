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
import javax.annotation.Nullable;

/** This class is Experimental and subject to change */
public class Metadata {

  private List<String> keywords = Collections.emptyList();

  private String description;

  private String url;

  private String role;

  public List<String> getKeywords() {
    return Collections.unmodifiableList(keywords);
  }

  public void setKeywords(List<String> keywords) {
    this.keywords = new ArrayList<>(keywords);
  }

  public Metadata keywords(List<String> keywords) {
    this.keywords = new ArrayList<>(keywords);
    return this;
  }

  @Nullable
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Metadata description(String description) {
    this.description = description;
    return this;
  }

  @Nullable
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Metadata url(String url) {
    this.url = url;
    return this;
  }

  @Nullable
  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public Metadata role(String role) {
    this.role = role;
    return this;
  }
}
