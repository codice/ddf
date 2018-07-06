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
package org.codice.ddf.config.model.impl;

import java.net.URL;
import org.codice.ddf.config.model.RegistryConfig;

public class RegistryConfigImpl extends AbstractConfigGroup implements RegistryConfig {
  private String name;

  private URL url;

  private boolean push;

  private boolean pull;

  private boolean publish;

  public RegistryConfigImpl() {}

  public RegistryConfigImpl(
      String id, String name, URL url, boolean push, boolean pull, boolean publish, int version) {
    super(id, version);
    this.name = name;
    this.url = url;
    this.push = push;
    this.pull = pull;
    this.publish = publish;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public URL getUrl() {
    return url;
  }

  public void setUrl(URL url) {
    this.url = url;
  }

  @Override
  public boolean getPush() {
    return push;
  }

  public void setPush(boolean push) {
    this.push = push;
  }

  @Override
  public boolean getPull() {
    return pull;
  }

  public void setPull(boolean pull) {
    this.pull = pull;
  }

  @Override
  public boolean getPublish() {
    return publish;
  }

  public void setPublish(boolean publish) {
    this.publish = publish;
  }
}
