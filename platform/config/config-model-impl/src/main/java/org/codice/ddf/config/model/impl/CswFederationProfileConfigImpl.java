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
import java.util.Objects;
import org.codice.ddf.config.model.CswFederationProfileConfig;

public class CswFederationProfileConfigImpl extends SourceConfigImpl
    implements CswFederationProfileConfig {

  public CswFederationProfileConfigImpl() {}

  public CswFederationProfileConfigImpl(String id, String name, URL url, int version) {
    super(id, name, url, version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), getId(), getName(), getUrl(), getVersion());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof CswFederationProfileConfigImpl) {
      final CswFederationProfileConfigImpl cfg = (CswFederationProfileConfigImpl) obj;

      return getType().equals(cfg.getType())
          && getId().equals(cfg.getId())
          && getName().equals(cfg.getName())
          && getUrl().equals(cfg.getUrl())
          && getVersion() == cfg.getVersion();
    }
    return false;
  }
}
