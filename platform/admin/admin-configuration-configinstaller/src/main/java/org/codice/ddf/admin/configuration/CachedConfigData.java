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
package org.codice.ddf.admin.configuration;

import java.io.File;
import java.util.Dictionary;
import javax.annotation.Nullable;
import org.codice.felix.cm.internal.ConfigurationContext;

public class CachedConfigData {

  private File felixFile;

  // Future performance/memory improvement - replace with checksum
  private Dictionary<String, Object> props;

  public CachedConfigData(ConfigurationContext context) {
    this.felixFile = context.getConfigFile();
    this.props = context.getSanitizedProperties();
  }

  /** @return the File object pointing to this configuration on disk, or null if none exists. */
  @Nullable
  File getFelixFile() {
    return felixFile;
  }

  /** @return the properties of the config stored on disk. */
  Dictionary<String, Object> getProps() {
    return props;
  }

  /** @param props the property dictionary to cache. */
  void setProps(Dictionary<String, Object> props) {
    this.props = props;
  }
}
