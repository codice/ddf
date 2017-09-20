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
package ddf.action.impl;

import ddf.action.Action;
import java.net.URL;

public class ActionImpl implements Action {

  private URL url;

  private String title;

  private String description;

  private String id;

  public ActionImpl(String providerId, String title, String description, URL url) {
    this.id = providerId;
    this.title = title;
    this.description = description;
    this.url = url;
  }

  @Override
  public URL getUrl() {
    return this.url;
  }

  public void setUrl(URL url) {
    this.url = url;
  }

  @Override
  public String getTitle() {
    return this.title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
