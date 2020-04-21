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
package org.codice.ddf.catalog.ui.enumeration;

import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.codice.countrycode.standard.CountryCode;
import org.codice.countrycode.standard.StandardProvider;
import org.codice.countrycode.standard.StandardRegistryImpl;

public class CountryExtractor {
  private static final String DEFAULT_STANDARD = "ISO3166";
  private static final String DEFAULT_VERSION = "1";
  private static final String DEFAULT_FORMAT = "alpha3";

  private final StandardProvider standardProvider;

  public CountryExtractor() {
    this.standardProvider =
        StandardRegistryImpl.getInstance().lookup(DEFAULT_STANDARD, DEFAULT_VERSION);
  }

  private StandardProvider getStandardProvider(
      final @Nullable String standard, final @Nullable String version) {
    if (standard == null || version == null) return standardProvider;
    if (standard.equals(DEFAULT_STANDARD) && version.equals(DEFAULT_VERSION))
      return standardProvider;

    StandardProvider provider = StandardRegistryImpl.getInstance().lookup(standard, version);
    if (provider != null) return provider;
    return standardProvider;
  }

  private Map<String, String> getCountries(final StandardProvider provider, final String format) {
    return provider
        .getStandardEntries()
        .stream()
        .collect(Collectors.toMap(cc -> cc.getAsFormat(format), CountryCode::getName));
  }

  /*
   * Returns a map with key as countryCode and value as countryName
   */
  public Map<String, String> getCountries(
      @Nullable final String standard,
      @Nullable final String version,
      @Nullable final String format) {
    Map<String, String> countries =
        this.getCountries(this.getStandardProvider(standard, version), format);
    if (countries != null) return countries;
    return this.getCountries(this.getStandardProvider(standard, version), DEFAULT_FORMAT);
  }
}
