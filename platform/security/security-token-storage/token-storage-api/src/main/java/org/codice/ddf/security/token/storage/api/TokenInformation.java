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
package org.codice.ddf.security.token.storage.api;

import java.util.Map;
import java.util.Set;

/** Storage for a user's information */
public interface TokenInformation {

  /** @return the user's email address or username (if an email address is not available) hashed */
  String getId();

  /**
   * @return the user's unique id. Using email address or username (if an email address is not
   *     available)
   */
  String getUserId();

  /** @return a map of sources with their corresponding tokens */
  Map<String, TokenEntry> getTokenEntries();

  /** @return a list of the metadata urls of all the oauth providers of all the sources */
  Set<String> getDiscoveryUrls();

  /** @return a JSON representation of the user's token */
  String getTokenJson();

  interface TokenEntry {

    /** @return the user's access token */
    String getAccessToken();

    /** @return the user's refresh token */
    String getRefreshToken();

    /** @return the oauth provider's url */
    String getDiscoveryUrl();
  }
}
