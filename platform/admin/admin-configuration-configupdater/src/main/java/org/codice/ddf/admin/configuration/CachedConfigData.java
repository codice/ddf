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

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Objects;
import javax.annotation.Nullable;
import org.codice.felix.cm.internal.ConfigurationContext;

/** Data structure stored in the {@link ConfigurationUpdater}'s cache. */
class CachedConfigData {

  private File felixFile;

  // DDF-3413: Future performance/memory improvement - replace with checksum
  private Dictionary<String, Object> props;

  CachedConfigData(ConfigurationContext context) {
    this.felixFile = context.getConfigFile();
    this.props = context.getSanitizedProperties();
  }

  /** @return the File object pointing to this configuration on disk, or null if none exists. */
  @Nullable
  File getFelixFile() {
    return felixFile;
  }

  /** @return the properties of the config stored on disk. */
  @VisibleForTesting
  Dictionary<String, Object> getProps() {
    return props;
  }

  /** @param props the property dictionary to cache. */
  void setProps(Dictionary<String, Object> props) {
    this.props = props;
  }

  /**
   * Determine if this {@link CachedConfigData}'s properties are equal to the given properties.
   *
   * @param comparator input dictionary of props to compare with the config data's cached props.
   * @return true if the dictionaries are deeply equal, false otherwise.
   */
  boolean equalProps(Dictionary comparator) {
    return equalDictionaries(comparator, props);
  }

  // DDF-3413: This method (and the above) would no longer be necessary
  private static boolean equalDictionaries(Dictionary x, Dictionary y) {
    if (x.size() != y.size()) {
      return false;
    }
    for (final Enumeration e = x.keys(); e.hasMoreElements(); ) {
      final Object key = e.nextElement();
      if (!Objects.deepEquals(x.get(key), y.get(key))) {
        return false;
      }
    }
    return true;
  }
}
