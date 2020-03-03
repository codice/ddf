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

public class TokenInformationImpl implements TokenInformation {
  private Map<String, TokenEntry> tokenEntryMap;
  private Set<String> discoveryUrls;
  private String json;
  private String id;

  public TokenInformationImpl(
      String id, Map<String, TokenEntry> tokenEntryMap, Set<String> discoveryUrls, String json) {
    this.id = id;
    this.tokenEntryMap = tokenEntryMap;
    this.discoveryUrls = discoveryUrls;
    this.json = json;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public Map<String, TokenEntry> getTokenEntries() {
    return tokenEntryMap;
  }

  @Override
  public Set<String> getDiscoveryUrls() {
    return discoveryUrls;
  }

  @Override
  public String getTokenJson() {
    return json;
  }

  public static class TokenEntryImpl implements TokenEntry {

    private String accessToken;
    private String refreshToken;
    private String discoveryUrl;

    public TokenEntryImpl(String accessToken, String refreshToken, String discoveryUrl) {
      this.accessToken = accessToken;
      this.refreshToken = refreshToken;
      this.discoveryUrl = discoveryUrl;
    }

    @Override
    public String getAccessToken() {
      return accessToken;
    }

    @Override
    public String getRefreshToken() {
      return refreshToken;
    }

    @Override
    public String getDiscoveryUrl() {
      return discoveryUrl;
    }
  }
}
